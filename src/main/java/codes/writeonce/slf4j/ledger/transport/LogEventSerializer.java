package codes.writeonce.slf4j.ledger.transport;

import codes.writeonce.slf4j.ledger.Level;
import codes.writeonce.slf4j.ledger.transport.serializer.ByteSerializer;
import codes.writeonce.slf4j.ledger.transport.serializer.CompactingStringSerializer;
import codes.writeonce.slf4j.ledger.transport.serializer.IntSerializer;
import codes.writeonce.slf4j.ledger.transport.serializer.LongSerializer;
import codes.writeonce.slf4j.ledger.transport.serializer.MapSerializer;
import codes.writeonce.slf4j.ledger.transport.serializer.Serializer;
import codes.writeonce.slf4j.ledger.transport.serializer.SerializerContext;
import codes.writeonce.slf4j.ledger.transport.serializer.StringSerializer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

public final class LogEventSerializer implements Serializer {

    @Nonnull
    private final SerializerContext context;

    @Nonnull
    private final ByteSerializer byteSerializer;

    @Nonnull
    private final IntSerializer intSerializer;

    @Nonnull
    private final LongSerializer longSerializer;

    @Nonnull
    private final StringSerializer stringSerializer;

    @Nonnull
    private final MapSerializer mapSerializer;

    private final CharsetEncoder charsetEncoder = StandardCharsets.UTF_8.newEncoder();

    private int state;

    private int textSize;

    private long timestampMillis;

    private Level level;

    @Nullable
    private Map<String, String> mdc;

    @Nullable
    private String threadName;

    @Nullable
    private String throwableString;

    private int remained;

    private CharBuffer charBuffer;

    private boolean last;

    public LogEventSerializer(@Nonnull SerializerContext context) {

        this.context = context;

        intSerializer = new IntSerializer();
        longSerializer = new LongSerializer();
        stringSerializer = new CompactingStringSerializer(intSerializer);
        byteSerializer = new ByteSerializer();
        mapSerializer = new MapSerializer(intSerializer, stringSerializer);
    }

    @Override
    public void reset() {

        mapSerializer.reset();
        stringSerializer.reset();
        intSerializer.reset();
        longSerializer.reset();
        byteSerializer.reset();
        state = 0;
        mdc = null;
        threadName = null;
        throwableString = null;
        charBuffer = null;
    }

    @Override
    public int consume(@Nonnull ByteBuffer byteBuffer, int remaining) {

        switch (state) {
            case 0:
                byte byteValue = (byte) level.ordinal();
                if (threadName != null) {
                    byteValue |= 8;
                }
                if (throwableString != null) {
                    byteValue |= 0x10;
                }
                byteSerializer.value(byteValue);
                remaining = byteSerializer.consume(byteBuffer, remaining);
                if (remaining == -1) {
                    context.push(byteSerializer);
                    state = 1;
                    return remaining;
                }
            case 1:
                longSerializer.value(timestampMillis);
                remaining = longSerializer.consume(byteBuffer, remaining);
                if (remaining == -1) {
                    context.push(longSerializer);
                    state = 2;
                    return remaining;
                }
            case 2:
                mapSerializer.value(mdc == null ? Collections.emptyMap() : mdc);
                remaining = mapSerializer.consume(byteBuffer, remaining);
                if (remaining == -1) {
                    context.push(mapSerializer);
                    state = 3;
                    return remaining;
                }
            case 3:
                mdc = null;
                if (threadName != null) {
                    stringSerializer.value(threadName);
                    remaining = stringSerializer.consume(byteBuffer, remaining);
                    if (remaining == -1) {
                        context.push(stringSerializer);
                        state = 4;
                        return remaining;
                    }
                }
            case 4:
                threadName = null;
                if (throwableString != null) {
                    stringSerializer.value(throwableString);
                    remaining = stringSerializer.consume(byteBuffer, remaining);
                    if (remaining == -1) {
                        context.push(stringSerializer);
                        state = 5;
                        return remaining;
                    }
                }
            case 5:
                throwableString = null;
                intSerializer.value(textSize);
                remaining = intSerializer.consume(byteBuffer, remaining);
                if (remaining == -1) {
                    context.push(intSerializer);
                    state = 6;
                    return remaining;
                }
            case 6:
                if (textSize == 0) {
                    state = 0;
                    return remaining;
                }
                charsetEncoder.reset();
                remained = textSize;
                state = 7;
                return remaining;
            case 8: {
                final var result = charsetEncoder.encode(charBuffer, byteBuffer, last);
                if (result.isOverflow()) {
                    return -1;
                }
                charBuffer = null;
                ensureUnderflow(result);
                if (!last) {
                    state = 7;
                    return byteBuffer.remaining();
                }
            }
            case 9: {
                final var result = charsetEncoder.flush(byteBuffer);
                if (result.isOverflow()) {
                    state = 9;
                    return -1;
                }
                ensureUnderflow(result);
                state = 0;
                return byteBuffer.remaining();
            }
            default:
                throw new IllegalStateException();
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

    public void init(
            int textSize,
            long timestampMillis,
            @Nonnull Level level,
            @Nullable Map<String, String> mdc,
            @Nullable String threadName,
            @Nullable String throwableString
    ) {
        if (state != 0) {
            throw new IllegalStateException();
        }
        this.textSize = textSize;
        this.timestampMillis = timestampMillis;
        this.level = level;
        this.mdc = mdc;
        this.threadName = threadName;
        this.throwableString = throwableString;
    }

    public void chunk(boolean last, @Nonnull CharBuffer charBuffer) {

        if (state != 7) {
            throw new IllegalStateException();
        }

        final var remaining = charBuffer.remaining();

        if (last) {
            if (remained != remaining) {
                throw new IllegalArgumentException();
            }
        } else {
            if (remained <= remaining) {
                throw new IllegalArgumentException();
            }
        }

        remained -= remaining;
        this.charBuffer = charBuffer;
        this.last = last;
        state = 8;
    }
}
