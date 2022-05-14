package codes.writeonce.slf4j.ledger.transport.deserializer;

import codes.writeonce.slf4j.ledger.transport.StringInterner;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;

public abstract class StringDeserializer implements Deserializer {

    @Nonnull
    private final DeserializerContext context;

    @Nonnull
    private final IntDeserializer intDeserializer;

    protected int state;

    protected int size;

    protected byte[] bytes;

    private byte lastByte;

    protected char[] chars;

    protected int position;

    protected int hash;

    protected String value;

    public StringDeserializer(
            @Nonnull DeserializerContext context,
            @Nonnull IntDeserializer intDeserializer
    ) {
        this.context = context;
        this.intDeserializer = intDeserializer;
    }

    @Override
    public void reset() {

        intDeserializer.reset();
        state = 0;
        value = null;
    }

    @Override
    public int consume(@Nonnull ByteBuffer byteBuffer, int remaining) {

        switch (state) {
            case 0:
                remaining = intDeserializer.consume(byteBuffer, remaining);
                if (remaining == -1) {
                    context.push(intDeserializer);
                    state = 3;
                    return remaining;
                }
            case 3:
                final int value = intDeserializer.intValue();
                if (value < 0) {
                    size = -value;
                    state = 2;
                    return startLatin1(byteBuffer, remaining);
                } else if (value > 0) {
                    size = value;
                    if (chars == null || chars.length < value) {
                        chars = new char[value];
                    }
                    state = 1;
                    position = 0;
                    hash = 0;
                    return appendUtf16(byteBuffer, remaining);
                } else {
                    this.value = "";
                    state = 0;
                    return remaining;
                }
            case 1:
                return appendUtf16(byteBuffer, remaining);
            case 2:
                return appendLatin1(byteBuffer, remaining);
            default:
                throw new IllegalStateException();
        }
    }

    private int appendUtf16(@Nonnull ByteBuffer byteBuffer, int remaining) {

        final int length = size * 2 - position;
        if (remaining >= length) {
            remaining -= length;
            copyUtf16(byteBuffer, length);
            state = 0;
            value = StringInterner.intern(chars, 0, size, hash);
            return remaining;
        } else {
            copyUtf16(byteBuffer, remaining);
            return -1;
        }
    }

    private int appendUtf16(@Nonnull byte[] source, int start, int end) {

        final int length = size * 2 - position;
        final int remaining = end - start;
        if (remaining >= length) {
            copyUtf16(source, start, length);
            state = 0;
            value = StringInterner.intern(chars, 0, size, hash);
            return start + length;
        } else {
            copyUtf16(source, start, remaining);
            return -1;
        }
    }

    protected int appendLatin1(@Nonnull ByteBuffer byteBuffer, int remaining) {

        final int length = size - position;
        if (remaining >= length) {
            remaining -= length;
            copyLatin1(byteBuffer, length);
            state = 0;
            value = buildString();
            return remaining;
        } else {
            copyLatin1(byteBuffer, remaining);
            return -1;
        }
    }

    protected int appendLatin1(@Nonnull byte[] source, int start, int end) {

        final int length = size - position;
        final int remaining = end - start;
        if (remaining >= length) {
            copyLatin1(source, start, length);
            state = 0;
            value = buildString();
            return start + length;
        } else {
            copyLatin1(source, start, remaining);
            return -1;
        }
    }

    private void copyUtf16(@Nonnull ByteBuffer byteBuffer, int length) {

        if (byteBuffer.hasArray()) {
            final int position = byteBuffer.position();
            byteBuffer.position(position + length);
            copyUtf16(byteBuffer.array(), byteBuffer.arrayOffset() + position, length);
        } else {
            if (bytes == null || bytes.length < length) {
                bytes = new byte[length];
            }
            byteBuffer.get(bytes, position, length);
            copyUtf16(bytes, 0, length);
        }
    }

    private void copyUtf16(@Nonnull byte[] source, int start, int length) {

        int i = position >> 1;
        int h = hash;

        if ((position & 1) != 0) {
            final char c = (char) (lastByte << 8 | 0xff & source[start++]);
            chars[i++] = c;
            h = h * 31 + c;
        }

        position += length;

        final int end = position >> 1;

        while (i < end) {
            final byte b1 = source[start++];
            final byte b2 = source[start++];
            final char c = (char) (b1 << 8 | 0xff & b2);
            chars[i++] = c;
            h = h * 31 + c;
        }

        hash = h;

        if ((position & 1) != 0) {
            lastByte = source[start];
        }
    }

    @Nonnull
    public String value() {
        return value;
    }

    protected abstract void copyLatin1(@Nonnull ByteBuffer byteBuffer, int length);

    protected abstract void copyLatin1(@Nonnull byte[] source, int start, int length);

    protected abstract int startLatin1(@Nonnull ByteBuffer byteBuffer, int remaining);

    protected abstract int startLatin1(@Nonnull byte[] source, int start, int end);

    @Nonnull
    protected abstract String buildString();
}
