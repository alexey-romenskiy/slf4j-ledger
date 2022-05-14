package codes.writeonce.slf4j.ledger.transport.serializer;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;

public class ByteSerializer implements Serializer {

    private byte value;

    @Override
    public void reset() {
        // empty
    }

    @Override
    public int consume(@Nonnull ByteBuffer byteBuffer, int remaining) {

        if (remaining > 0) {
            byteBuffer.put(value);
            return remaining - 1;
        } else {
            return -1;
        }
    }

    public void value(byte value) {
        this.value = value;
    }
}
