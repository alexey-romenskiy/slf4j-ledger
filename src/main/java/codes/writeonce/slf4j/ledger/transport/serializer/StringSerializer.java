package codes.writeonce.slf4j.ledger.transport.serializer;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;

public abstract class StringSerializer implements Serializer {

    @Nonnull
    protected final IntSerializer intSerializer;

    protected int state;

    private int position;

    protected int length;

    protected char[] chars;

    private byte[] bytes;

    public StringSerializer(@Nonnull IntSerializer intSerializer) {
        this.intSerializer = intSerializer;
    }

    @Override
    public void reset() {
        intSerializer.reset();
    }

    @Override
    public int consume(@Nonnull ByteBuffer byteBuffer, int remaining) {

        while (true) {
            switch (state) {
                case 0:
                case 1:
                    remaining = intSerializer.consume(byteBuffer, remaining);
                    if (remaining == -1) {
                        return remaining;
                    }
                    state += 2;
                    if (remaining == 0) {
                        return remaining;
                    }
                    break;
                case 2: {
                    final int length = this.length;
                    int position = this.position;

                    final int valueRemaining = length - position;
                    if (valueRemaining > remaining) {
                        if (bytes == null || bytes.length < remaining) {
                            bytes = new byte[remaining];
                        }
                        int start = 0;
                        while (start < remaining) {
                            bytes[start++] = (byte) chars[position++];
                        }
                        byteBuffer.put(bytes, 0, remaining);
                        this.position = position;
                        return -1;
                    } else {
                        if (bytes == null || bytes.length < valueRemaining) {
                            bytes = new byte[valueRemaining];
                        }
                        int start = 0;
                        while (position < length) {
                            bytes[start++] = (byte) chars[position++];
                        }
                        byteBuffer.put(bytes, 0, valueRemaining);
                        return remaining - valueRemaining;
                    }
                }
                case 3: {
                    final int length = this.length;
                    int position = this.position;

                    final int valueRemaining = (length - position) * 2;
                    if (valueRemaining > remaining) {
                        if (bytes == null || bytes.length < remaining) {
                            bytes = new byte[remaining];
                        }
                        if ((remaining & 1) == 0) {
                            int start = 0;
                            while (start < remaining) {
                                final char c = chars[position];
                                bytes[start] = (byte) (c >> 8);
                                bytes[start + 1] = (byte) c;
                                position++;
                                start += 2;
                            }
                        } else {
                            final int end2 = remaining - 1;
                            int start = 0;
                            while (start < end2) {
                                final char c = chars[position];
                                bytes[start] = (byte) (c >> 8);
                                bytes[start + 1] = (byte) c;
                                position++;
                                start += 2;
                            }
                            final char c = chars[position];
                            bytes[start] = (byte) (c >> 8);
                            state = 4;
                        }
                        byteBuffer.put(bytes, 0, remaining);
                        this.position = position;
                        return -1;
                    } else {
                        if (bytes == null || bytes.length < valueRemaining) {
                            bytes = new byte[valueRemaining];
                        }
                        int start = 0;
                        while (position < length) {
                            final char c = chars[position];
                            bytes[start] = (byte) (c >> 8);
                            bytes[start + 1] = (byte) c;
                            position++;
                            start += 2;
                        }
                        byteBuffer.put(bytes, 0, valueRemaining);
                        return remaining - valueRemaining;
                    }
                }
                case 4: {
                    final int length = this.length;
                    int position = this.position;

                    final int valueRemaining = (length - position) * 2 - 1;
                    if (valueRemaining > remaining) {
                        if (remaining > 0) {
                            if (bytes == null || bytes.length < remaining) {
                                bytes = new byte[remaining];
                            }
                            int start = 0;
                            bytes[start++] = (byte) chars[position++];
                            if ((remaining & 1) != 0) {
                                while (start < remaining) {
                                    final char c = chars[position];
                                    bytes[start] = (byte) (c >> 8);
                                    bytes[start + 1] = (byte) c;
                                    position++;
                                    start += 2;
                                }
                                state = 3;
                            } else {
                                final int end2 = remaining - 1;
                                while (start < end2) {
                                    final char c = chars[position];
                                    bytes[start] = (byte) (c >> 8);
                                    bytes[start + 1] = (byte) c;
                                    position++;
                                    start += 2;
                                }
                                final char c = chars[position];
                                bytes[start] = (byte) (c >> 8);
                            }
                            byteBuffer.put(bytes, 0, remaining);
                            this.position = position;
                        }
                        return -1;
                    } else {
                        if (bytes == null || bytes.length < valueRemaining) {
                            bytes = new byte[valueRemaining];
                        }
                        int start = 0;
                        bytes[start++] = (byte) chars[position++];
                        while (position < length) {
                            final char c = chars[position];
                            bytes[start] = (byte) (c >> 8);
                            bytes[start + 1] = (byte) c;
                            position++;
                            start += 2;
                        }
                        byteBuffer.put(bytes, 0, valueRemaining);
                        return remaining - valueRemaining;
                    }
                }
                default:
                    throw new IllegalStateException();
            }
        }
    }

    public void value(@Nonnull String value) {

        this.position = 0;

        length = value.length();

        if (chars == null || chars.length < length) {
            chars = new char[length];
        }

        value.getChars(0, length, chars, 0);

        initState();
    }

    protected abstract void initState();
}
