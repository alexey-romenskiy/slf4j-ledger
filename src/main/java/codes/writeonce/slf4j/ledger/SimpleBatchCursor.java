package codes.writeonce.slf4j.ledger;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicInteger;

final class SimpleBatchCursor {

    private final int size;

    private final AtomicInteger count;

    @Nonnull
    private final AtomicInteger head;

    @Nonnull
    private final AtomicInteger tail;

    SimpleBatchCursor(int size, int count, int head) {
        this.size = size;
        this.count = new AtomicInteger(count);
        this.head = new AtomicInteger(head);
        this.tail = new AtomicInteger((head + count) % size);
    }

    public int allocate(int amount) {

        if (amount <= 0) {
            throw new IllegalArgumentException();
        }

        var c = count.get();

        while (true) {
            if (c > 0) {
                final var v = Math.min(c, amount);
                final var n = count.compareAndExchange(c, c - v);
                if (n == c) {
                    return v;
                }
                c = n;
            } else {
                c = count.get();
            }
        }
    }

    public int next(int amount) {
        return head.getAndUpdate(i -> (i + amount) % size);
    }

    public void publish(int index, int amount) {

        while (true) {
            final var n = tail.compareAndExchange(index, (index + amount) % size);
            if (n == index) {
                break;
            }
        }

        count.addAndGet(amount);
    }
}
