package codes.writeonce.slf4j.ledger.transport;

import codes.writeonce.slf4j.ledger.ChunkWriter;
import codes.writeonce.slf4j.ledger.Level;
import codes.writeonce.slf4j.ledger.Publisher;
import codes.writeonce.slf4j.ledger.transport.serializer.Serializer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public final class LogEventPublisher implements Publisher {

    private final ArrayList<Serializer> stack = new ArrayList<>();

    @Nonnull
    private final ChunkWriter chunkWriter;

    private final LogEventSerializer rootSerializer;

    private Serializer serializer;

    private long sequence;

    private ByteBuffer byteBuffer;

    public LogEventPublisher(@Nonnull ChunkWriter chunkWriter) {

        this.chunkWriter = chunkWriter;
        rootSerializer = new LogEventSerializer(this::push);
        serializer = rootSerializer;
    }

    @Override
    public void next(
            int textSize,
            long timestampMillis,
            @Nonnull Level level,
            @Nullable Map<String, String> mdc,
            @Nullable String threadName,
            @Nullable String throwableString
    ) {
        try {
            sequence++;
            chunkWriter.sequence(sequence);

            rootSerializer.init(textSize, timestampMillis, level, mdc, threadName, throwableString);

            var byteBuffer = chunkWriter.chunk();
            var remaining = byteBuffer.remaining();
            while (true) {
                remaining = serializer.consume(byteBuffer, remaining);
                if (remaining == -1) {
                    // all bytes consumed but parsing not completed
                    byteBuffer = chunkWriter.chunk();
                    remaining = byteBuffer.remaining();
                } else {
                    if (stack.isEmpty()) {
                        // parsing is completed
                        if (textSize != 0) {
                            this.byteBuffer = byteBuffer;
                        }
                        return;
                    }
                    pop();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void chunk(boolean last, @Nonnull CharBuffer charBuffer) {

        try {
            rootSerializer.chunk(last, charBuffer);

            var remaining = byteBuffer.remaining();
            while (true) {
                remaining = serializer.consume(byteBuffer, remaining);
                if (remaining == -1) {
                    byteBuffer = chunkWriter.chunk();
                    remaining = byteBuffer.remaining();
                } else {
                    if (stack.isEmpty()) {
                        if (last) {
                            if (remaining != 0) {
                                throw new IllegalStateException();
                            }
                            this.byteBuffer = null;
                        }
                        return;
                    }
                    pop();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private void push(@Nonnull Serializer serializer) {

        requireNonNull(serializer);
        stack.add(this.serializer);
        this.serializer = serializer;
    }

    private void pop() {
        this.serializer = stack.remove(stack.size() - 1);
    }
}
