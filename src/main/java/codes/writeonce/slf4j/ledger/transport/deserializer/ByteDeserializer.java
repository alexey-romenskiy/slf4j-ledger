package codes.writeonce.slf4j.ledger.transport.deserializer;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;

public class ByteDeserializer implements Deserializer {

    private byte value;

    @Override
    public void reset() {
        // empty
    }

    @Override
    public int consume(@Nonnull ByteBuffer byteBuffer, int remaining) {

        if (remaining > 0) {
            value = byteBuffer.get();
            return remaining - 1;
        } else {
            return -1;
        }
    }

    public byte byteValue() {
        return value;
    }
}
