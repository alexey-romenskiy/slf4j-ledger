package codes.writeonce.slf4j.ledger;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

final class SimpleCursor implements Cursor {

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

    SimpleCursor(int size, int count, int head) {

        this.size = size;
        this.count = new AtomicInteger(count);
        this.head = new AtomicInteger(head);
        this.tail = new AtomicInteger((head + count) % size);
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
                    condition.awaitUninterruptibly();
                    c2 = count.get();
                }
            }
        } finally {
            lock.unlock();
        }
    }
}
