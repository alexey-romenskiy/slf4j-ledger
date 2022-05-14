package codes.writeonce.slf4j.ledger.transport.serializer;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;

public abstract class AbstractLongSerializer<T> implements Serializer {

    protected long value;

    protected int state = 8;

    @Override
    public void reset() {
        state = 8;
    }

    @Override
    public int consume(@Nonnull ByteBuffer byteBuffer, int remaining) {

        int state = this.state;

        if (state > remaining) {
            switch (remaining) {
                case 7: {
                    state -= remaining;
                    final long value = this.value >> (state << 3);
                    byteBuffer.put((byte) (value >> 48));
                    byteBuffer.putShort((short) (value >> 32));
                    byteBuffer.putInt((int) value);
                    this.state = state;
                    return -1;
                }
                case 6: {
                    state -= remaining;
                    final long value = this.value >> (state << 3);
                    byteBuffer.putShort((short) (value >> 32));
                    byteBuffer.putInt((int) value);
                    this.state = state;
                    return -1;
                }
                case 5: {
                    state -= remaining;
                    final long value = this.value >> (state << 3);
                    byteBuffer.put((byte) (value >> 32));
                    byteBuffer.putInt((int) value);
                    this.state = state;
                    return -1;
                }
                case 4:
                    state -= remaining;
                    byteBuffer.putInt((int) (this.value >> (state << 3)));
                    this.state = state;
                    return -1;
                case 3: {
                    state -= remaining;
                    final long value = this.value >> (state << 3);
                    byteBuffer.put((byte) (value >> 16));
                    byteBuffer.putShort((short) value);
                    this.state = state;
                    return -1;
                }
                case 2:
                    state -= remaining;
                    byteBuffer.putShort((short) (this.value >> (state << 3)));
                    this.state = state;
                    return -1;
                case 1:
                    state -= remaining;
                    byteBuffer.put((byte) (this.value >> (state << 3)));
                    this.state = state;
                    return -1;
                case 0:
                    return -1;
                default:
                    throw new IllegalStateException();
            }
        } else {
            switch (state) {
                case 8:
                    byteBuffer.putLong(value);
                    return remaining - state;
                case 7: {
                    final long value = this.value;
                    byteBuffer.put((byte) (value >> 48));
                    byteBuffer.putShort((short) (value >> 32));
                    byteBuffer.putInt((int) value);
                    this.state = 8;
                    return remaining - state;
                }
                case 6: {
                    final long value = this.value;
                    byteBuffer.putShort((short) (value >> 32));
                    byteBuffer.putInt((int) value);
                    this.state = 8;
                    return remaining - state;
                }
                case 5: {
                    final long value = this.value;
                    byteBuffer.put((byte) (value >> 32));
                    byteBuffer.putInt((int) value);
                    this.state = 8;
                    return remaining - state;
                }
                case 4:
                    byteBuffer.putInt((int) value);
                    this.state = 8;
                    return remaining - state;
                case 3: {
                    final long value = this.value;
                    byteBuffer.put((byte) (value >> 16));
                    byteBuffer.putShort((short) value);
                    this.state = 8;
                    return remaining - state;
                }
                case 2:
                    byteBuffer.putShort((short) value);
                    this.state = 8;
                    return remaining - state;
                case 1:
                    byteBuffer.put((byte) value);
                    this.state = 8;
                    return remaining - state;
                default:
                    throw new IllegalStateException();
            }
        }
    }
}
