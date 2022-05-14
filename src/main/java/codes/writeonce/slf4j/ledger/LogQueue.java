package codes.writeonce.slf4j.ledger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.CharBuffer;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

final class LogQueue {

    private static final int ENTRY_QUEUE_SIZE = 512;

    private static final int TEXT_QUEUE_SIZE = 64 * 1024;

    private final LogEntry[] entryQueue =
            Stream.generate(LogEntry::new).limit(ENTRY_QUEUE_SIZE).toArray(LogEntry[]::new);

    private final char[] textQueue = new char[TEXT_QUEUE_SIZE];

    private final CharBuffer charBuffer = CharBuffer.wrap(textQueue);

    private final WorkerCursor consumerCursor = new WorkerCursor(ENTRY_QUEUE_SIZE, 0, 0, 1, TimeUnit.SECONDS,
            new PrefixThreadFactory("log-publisher-", false), this::consumerLoop);

    private final SimpleCursor freeCursor = new SimpleCursor(ENTRY_QUEUE_SIZE, ENTRY_QUEUE_SIZE, 0);

    private final AtomicInteger textFillIndex = new AtomicInteger();

    private final SimpleBatchCursor textFreeCursor = new SimpleBatchCursor(TEXT_QUEUE_SIZE, TEXT_QUEUE_SIZE, 0);

    private final SimpleBatchCursor textConsumerCursor = new SimpleBatchCursor(TEXT_QUEUE_SIZE, 0, 0);

    @Nonnull
    private final Publisher publisher;

    LogQueue(@Nonnull Publisher publisher) {
        this.publisher = publisher;
    }

    public void publish(
            long timestampMillis,
            @Nonnull Level level,
            @Nullable Map<String, String> mdc,
            @Nullable String threadName,
            @Nullable String throwableString,
            @Nonnull StringBuilder text
    ) {
        final int index = publishEntry(timestampMillis, level, mdc, threadName, throwableString, text);

        while (true) {
            if (textFillIndex.get() == index) {
                break;
            }
        }

        int start = 0;
        var remained = text.length();
        while (remained > 0) {
            final var size = textFreeCursor.allocate(remained);
            final var p = textFreeCursor.next(size);
            final var end = (p + size) % TEXT_QUEUE_SIZE;
            if (end > p || end == 0) {
                text.getChars(start, start + size, textQueue, p);
            } else {
                final var length = TEXT_QUEUE_SIZE - p;
                text.getChars(start, start + length, textQueue, p);
                text.getChars(start + length, start + size, textQueue, 0);
            }
            textConsumerCursor.publish(p, size);
            start += size;
            remained -= size;
        }

        textFillIndex.set((index + 1) % ENTRY_QUEUE_SIZE);
    }

    private void publishInternal(
            long timestampMillis,
            @Nonnull Level level,
            @Nullable Map<String, String> mdc,
            @Nullable String threadName,
            @Nullable String throwableString,
            int textSize
    ) {
        publisher.next(textSize, timestampMillis, level, mdc, threadName, throwableString);

        if (textSize != 0) {
            var remained = textSize;
            while (true) {
                final var size = textConsumerCursor.allocate(remained);
                final var p = textConsumerCursor.next(size);
                final var end = (p + size) % TEXT_QUEUE_SIZE;
                final var last = remained == size;
                charBuffer.position(p);
                if (end > p || end == 0) {
                    charBuffer.limit(end);
                    publisher.chunk(last, charBuffer);
                } else {
                    charBuffer.limit(TEXT_QUEUE_SIZE);
                    publisher.chunk(false, charBuffer);
                    charBuffer.position(0);
                    charBuffer.limit(end);
                    publisher.chunk(last, charBuffer);
                }
                textFreeCursor.publish(p, size);
                if (last) {
                    break;
                }
                remained -= size;
            }
        }
    }

    private int publishEntry(
            long timestampMillis,
            @Nonnull Level level,
            @Nullable Map<String, String> mdc,
            @Nullable String threadName,
            @Nullable String throwableString,
            @Nonnull StringBuilder text
    ) {
        final int index = freeCursor.next();

        final var logEntry = entryQueue[index];
        logEntry.timestampMillis = timestampMillis;
        logEntry.level = level;
        logEntry.mdc = mdc;
        logEntry.threadName = threadName;
        logEntry.throwableString = throwableString;
        logEntry.textSize = text.length();

        consumerCursor.publish(index);
        return index;
    }

    private void consumerLoop() {

        while (true) {
            final var index = consumerCursor.next();
            if (index == -1) {
                break;
            }

            final var logEntry = entryQueue[index];

            final var timestampMillis = logEntry.timestampMillis;
            final var level = logEntry.level;
            final var mdc = logEntry.mdc;
            final var threadName = logEntry.threadName;
            final var throwableString = logEntry.throwableString;
            final var textSize = logEntry.textSize;

            logEntry.mdc = null;
            logEntry.threadName = null;
            logEntry.throwableString = null;

            freeCursor.publish(index);

            publishInternal(timestampMillis, level, mdc, threadName, throwableString, textSize);
        }
    }
}
