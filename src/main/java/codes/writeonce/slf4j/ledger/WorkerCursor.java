package codes.writeonce.slf4j.ledger;

import javax.annotation.Nonnull;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

final class WorkerCursor implements Cursor {

    private final int size;

    @Nonnull
    private final AtomicInteger count;

    @Nonnull
    private final AtomicInteger head;

    @Nonnull
    private final AtomicInteger tail;

    @Nonnull
    private final Lock lock = new ReentrantLock();

    @Nonnull
    private final Condition condition = lock.newCondition();

    private final AtomicBoolean alive = new AtomicBoolean();

    @Nonnull
    private final ThreadFactory threadFactory;

    @Nonnull
    private final Runnable worker;

    private final long liveTimeout;

    @Nonnull
    private final TimeUnit timeoutUnits;

    WorkerCursor(
            int size,
            int count,
            int head,
            long liveTimeout,
            @Nonnull TimeUnit timeoutUnits,
            @Nonnull ThreadFactory threadFactory,
            @Nonnull Runnable worker
    ) {
        this.size = size;
        this.count = new AtomicInteger(count);
        this.head = new AtomicInteger(head);
        this.tail = new AtomicInteger((head + count) % size);
        this.threadFactory = threadFactory;
        this.worker = worker;
        this.liveTimeout = liveTimeout;
        this.timeoutUnits = timeoutUnits;
    }

    @Override
    public void publish(int index) {

        while (true) {
            final var n = tail.compareAndExchange(index, (index + 1) % size);
            if (n == index) {
                break;
            }
        }

        final var c = count.getAndIncrement();
        if (c == 0) {
            lock.lock();
            try {
                condition.signalAll();
                if (!alive.getAndSet(true)) {
                    threadFactory.newThread(worker).start();
                }
            } finally {
                lock.unlock();
            }
        }
    }

    public int next() {

        var c = count.get();
        while (c > 0) {
            final var n = count.compareAndExchange(c, c - 1);
            if (n == c) {
                return head.getAndUpdate(i -> (i + 1) % size);
            }
            c = n;
        }

        lock.lock();
        try {
            var c2 = count.get();
            while (true) {
                if (c2 > 0) {
                    final var n = count.compareAndExchange(c2, c2 - 1);
                    if (n == c2) {
                        return head.getAndUpdate(i -> (i + 1) % size);
                    }
                    c2 = n;
                } else {
                    try {
                        if (condition.await(liveTimeout, timeoutUnits)) {
                            c2 = count.get();
                            continue;
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    c2 = count.get();
                    if (c2 <= 0) {
                        alive.set(false);
                        return -1;
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }
}
