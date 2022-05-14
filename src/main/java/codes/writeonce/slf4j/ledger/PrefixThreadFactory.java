package codes.writeonce.slf4j.ledger;

import javax.annotation.Nonnull;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Objects.requireNonNull;

final class PrefixThreadFactory implements ThreadFactory {

    private final AtomicLong sequence = new AtomicLong();

    @Nonnull
    private final String prefix;

    private final boolean daemon;

    public PrefixThreadFactory(@Nonnull String prefix, boolean daemon) {
        this.prefix = requireNonNull(prefix);
        this.daemon = daemon;
    }

    @Override
    public Thread newThread(@Nonnull Runnable target) {
        final Thread thread = new Thread(target, prefix + sequence.incrementAndGet());
        thread.setDaemon(daemon);
        return thread;
    }
}
