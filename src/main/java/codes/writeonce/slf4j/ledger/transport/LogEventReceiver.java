package codes.writeonce.slf4j.ledger.transport;

import codes.writeonce.slf4j.ledger.Publisher;
import codes.writeonce.slf4j.ledger.Receiver;
import codes.writeonce.slf4j.ledger.transport.deserializer.Deserializer;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import static java.util.Objects.requireNonNull;

public final class LogEventReceiver implements Receiver {

    private final ArrayList<Deserializer> stack = new ArrayList<>();

    private Deserializer deserializer;

    public LogEventReceiver(@Nonnull Publisher publisher) {
        deserializer = new LogEventDeserializer(this::push, publisher);
    }

    @Override
    public void next(long sequence, long offset, boolean last, @Nonnull ByteBuffer byteBuffer) {

        var remaining = byteBuffer.remaining();
        while (true) {
            remaining = deserializer.consume(byteBuffer, remaining);
            if (remaining == -1) {
                // all bytes consumed but parsing not completed
                if (last) {
                    throw new IllegalStateException();
                }
                return;
            }
            if (stack.isEmpty()) {
                // parsing is completed
                if (!last || remaining != 0) {
                    throw new IllegalStateException();
                }
                return;
            }
            pop();
        }
    }

    private void push(@Nonnull Deserializer deserializer) {

        requireNonNull(deserializer);
        stack.add(this.deserializer);
        this.deserializer = deserializer;
    }

    private void pop() {
        this.deserializer = stack.remove(stack.size() - 1);
    }
}
