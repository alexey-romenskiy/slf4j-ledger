package codes.writeonce.slf4j.ledger.transport.serializer;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;

public abstract class AbstractIntSerializer<T> implements Serializer {

    protected int value;

    private int state = 4;

    @Override
    public void reset() {
        state = 4;
    }

    @Override
    public int consume(@Nonnull ByteBuffer byteBuffer, int remaining) {

        int state = this.state;

        if (state > remaining) {
            switch (remaining) {
                case 3: {
                    state -= remaining;
                    final int value = this.value >> (state << 3);
                    byteBuffer.put((byte) (value >> 16));
                    byteBuffer.putShort((short) value);
                    this.state = state;
                    return -1;
                }
                case 2: {
                    state -= remaining;
                    byteBuffer.putShort((short) (this.value >> (state << 3)));
                    this.state = state;
                    return -1;
                }
                case 1: {
                    state -= remaining;
                    byteBuffer.put((byte) (this.value >> (state << 3)));
                    this.state = state;
                    return -1;
                }
                case 0:
                    return -1;
                default:
                    throw new IllegalStateException();
            }
        } else {
            switch (state) {
                case 4:
                    byteBuffer.putInt(value);
                    return remaining - state;
                case 3: {
                    final int value = this.value;
                    byteBuffer.put((byte) (value >> 16));
                    byteBuffer.putShort((short) value);
                    this.state = 4;
                    return remaining - state;
                }
                case 2:
                    byteBuffer.putShort((short) value);
                    this.state = 4;
                    return remaining - state;
                case 1:
                    byteBuffer.put((byte) value);
                    this.state = 4;
                    return remaining - state;
                default:
                    throw new IllegalStateException();
            }
        }
    }
}
