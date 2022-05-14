package codes.writeonce.slf4j.ledger.transport;

import codes.writeonce.slf4j.ledger.Level;
import codes.writeonce.slf4j.ledger.Publisher;
import codes.writeonce.slf4j.ledger.transport.deserializer.ByteDeserializer;
import codes.writeonce.slf4j.ledger.transport.deserializer.CompactingStringDeserializer;
import codes.writeonce.slf4j.ledger.transport.deserializer.Deserializer;
import codes.writeonce.slf4j.ledger.transport.deserializer.DeserializerContext;
import codes.writeonce.slf4j.ledger.transport.deserializer.IntDeserializer;
import codes.writeonce.slf4j.ledger.transport.deserializer.LongDeserializer;
import codes.writeonce.slf4j.ledger.transport.deserializer.MapDeserializer;
import codes.writeonce.slf4j.ledger.transport.deserializer.StringDeserializer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;
import java.util.Map;

final class LogEventDeserializer implements Deserializer {

    private static final int CHAR_BUFFER_SIZE = 8192;

    @Nonnull
    private final DeserializerContext context;

    @Nonnull
    private final Publisher publisher;

    @Nonnull
    private final ByteDeserializer byteDeserializer;

    @Nonnull
    private final IntDeserializer intDeserializer;

    @Nonnull
    private final LongDeserializer longDeserializer;

    @Nonnull
    private final StringDeserializer stringDeserializer;

    @Nonnull
    private final MapDeserializer mapDeserializer;

    private final CharsetDecoder charsetDecoder = StandardCharsets.UTF_8.newDecoder();

    private final char[] charArray = new char[CHAR_BUFFER_SIZE];

    private final CharBuffer charBuffer = CharBuffer.wrap(charArray);

    private final ByteBuffer emptyByteBuffer = ByteBuffer.allocate(0);

    private int state;

    private boolean hasThreadName;

    private boolean hasThrowableString;

    private long timestampMillis;

    @Nonnull
    private Level level = Level.TRACE;

    private Map<String, String> mdc;

    @Nullable
    private String threadName;

    @Nullable
    private String throwableString;

    private int remained;

    public LogEventDeserializer(@Nonnull DeserializerContext context, @Nonnull Publisher publisher) {

        this.context = context;
        this.publisher = publisher;

        intDeserializer = new IntDeserializer();
        byteDeserializer = new ByteDeserializer();
        longDeserializer = new LongDeserializer();
        stringDeserializer = new CompactingStringDeserializer(context, intDeserializer);
        mapDeserializer = new MapDeserializer(intDeserializer, stringDeserializer);
    }

    @Override
    public void reset() {

        mapDeserializer.reset();
        stringDeserializer.reset();
        intDeserializer.reset();
        longDeserializer.reset();
        byteDeserializer.reset();
        state = 0;
        mdc = null;
        threadName = null;
        throwableString = null;
    }

    @Override
    public int consume(@Nonnull ByteBuffer byteBuffer, int remaining) {

        while (true) {
            switch (state) {
                case 0:
                    remaining = byteDeserializer.consume(byteBuffer, remaining);
                    if (remaining == -1) {
                        context.push(byteDeserializer);
                        state = 1;
                        return remaining;
                    }
                case 1:
                    final var byteValue = byteDeserializer.byteValue();
                    level = Level.values()[byteValue & 7];
                    hasThreadName = (byteValue & 8) != 0;
                    hasThrowableString = (byteValue & 0x10) != 0;
                    remaining = longDeserializer.consume(byteBuffer, remaining);
                    if (remaining == -1) {
                        context.push(longDeserializer);
                        state = 2;
                        return remaining;
                    }
                case 2:
                    timestampMillis = longDeserializer.longValue();
                    remaining = mapDeserializer.consume(byteBuffer, remaining);
                    if (remaining == -1) {
                        context.push(mapDeserializer);
                        state = 3;
                        return remaining;
                    }
                case 3:
                    mdc = mapDeserializer.value();
                    if (hasThreadName) {
                        remaining = stringDeserializer.consume(byteBuffer, remaining);
                        if (remaining == -1) {
                            context.push(stringDeserializer);
                            state = 4;
                            return remaining;
                        }
                    } else {
                        threadName = null;
                        state = 5;
                        break;
                    }
                case 4:
                    threadName = stringDeserializer.value();
                case 5:
                    if (hasThrowableString) {
                        remaining = stringDeserializer.consume(byteBuffer, remaining);
                        if (remaining == -1) {
                            context.push(stringDeserializer);
                            state = 6;
                            return remaining;
                        }
                    } else {
                        throwableString = null;
                        state = 7;
                        break;
                    }
                case 6:
                    throwableString = stringDeserializer.value();
                case 7:
                    remaining = intDeserializer.consume(byteBuffer, remaining);
                    if (remaining == -1) {
                        context.push(intDeserializer);
                        state = 8;
                        return remaining;
                    }
                case 8:
                    final var textSize = intDeserializer.intValue();
                    publisher.next(textSize, timestampMillis, level, mdc, threadName, throwableString);
                    mdc = null;
                    threadName = null;
                    throwableString = null;
                    if (textSize == 0) {
                        state = 0;
                        return remaining;
                    }
                    charsetDecoder.reset();
                    remained = textSize;
                    charBuffer.position(0);
                    charBuffer.limit(Math.min(remained, CHAR_BUFFER_SIZE));
                case 9:
                    while (true) {
                        final var result = charsetDecoder.decode(byteBuffer, charBuffer, false);
                        if (!charBuffer.hasRemaining()) {
                            final var length = charBuffer.position();
                            final var last = remained == length;
                            if (last) {
                                ensureUnderflow(charsetDecoder.decode(emptyByteBuffer, charBuffer, true));
                                ensureUnderflow(charsetDecoder.flush(charBuffer));
                                charBuffer.flip();
                                publisher.chunk(last, charBuffer);
                                state = 0;
                                return byteBuffer.remaining();
                            } else {
                                charBuffer.flip();
                                publisher.chunk(last, charBuffer);
                                remained -= length;
                                charBuffer.position(0);
                                charBuffer.limit(Math.min(remained, CHAR_BUFFER_SIZE));
                            }
                        } else {
                            ensureUnderflow(result);
                            state = 9;
                            return -1;
                        }
                    }
                default:
                    throw new IllegalStateException();
            }
        }
    }

    private void ensureUnderflow(@Nonnull CoderResult result) {

        if (!result.isUnderflow()) {
            try {
                result.throwException();
            } catch (CharacterCodingException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
