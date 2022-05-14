package codes.writeonce.slf4j.ledger.transport.deserializer;

import codes.writeonce.slf4j.ledger.transport.StringInterner;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;

public class CompactingStringDeserializer extends StringDeserializer {

    public CompactingStringDeserializer(
            @Nonnull DeserializerContext context,
            @Nonnull IntDeserializer intDeserializer
    ) {
        super(context, intDeserializer);
    }

    @Override
    protected int startLatin1(@Nonnull ByteBuffer byteBuffer, int remaining) {

        if (remaining >= size) {
            remaining -= size;
            state = 0;

            if (byteBuffer.hasArray()) {
                final int position = byteBuffer.position();
                byteBuffer.position(position + size);
                value = hashIntern(byteBuffer.array(), byteBuffer.arrayOffset() + position, size);
            } else {
                if (bytes == null || bytes.length < size) {
                    bytes = new byte[size];
                }
                byteBuffer.get(bytes, 0, size);
                value = hashIntern(bytes, 0, size);
            }

            return remaining;
        } else {
            if (bytes == null || bytes.length < size) {
                bytes = new byte[size];
            }
            position = 0;
            hash = 0;
            copyLatin1(byteBuffer, remaining);
            return -1;
        }
    }

    @Override
    protected int startLatin1(@Nonnull byte[] source, int start, int end) {

        final int remaining = end - start;
        if (remaining >= size) {
            state = 0;
            value = hashIntern(source, start, size);
            return start + size;
        } else {
            if (bytes == null || bytes.length < size) {
                bytes = new byte[size];
            }
            position = 0;
            hash = 0;
            copyLatin1(source, start, remaining);
            return -1;
        }
    }

    @Override
    protected void copyLatin1(@Nonnull ByteBuffer byteBuffer, int length) {

        byteBuffer.get(bytes, position, length);
        updateHash(length);
    }

    @Override
    protected void copyLatin1(@Nonnull byte[] source, int start, int length) {

        System.arraycopy(source, start, bytes, position, length);
        updateHash(length);
    }

    private void updateHash(int length) {

        final byte[] source = bytes;
        int i = position;
        final int end = i + length;
        position = end;
        int h = hash;

        while (i < end) {
            h = h * 31 + (0xff & source[i++]);
        }

        hash = h;
    }

    private static int hash(@Nonnull byte[] source, int start, int length) {

        final int end = start + length;
        int h = 0;

        while (start < end) {
            h = h * 31 + (0xff & source[start++]);
        }

        return h;
    }

    @Nonnull
    private static String hashIntern(@Nonnull byte[] source, int start, int length) {
        return StringInterner.intern(source, start, length, hash(source, start, length));
    }

    @Override
    @Nonnull
    protected String buildString() {
        return StringInterner.intern(bytes, 0, size, hash);
    }
}
