package codes.writeonce.slf4j.ledger.transport.deserializer;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;

public class MapDeserializer implements Deserializer {

    @Nonnull
    private final IntDeserializer intDeserializer;

    @Nonnull
    private final StringDeserializer stringDeserializer;

    private int state;

    private int size;

    private Map<String, String> value;

    private String key;

    public MapDeserializer(@Nonnull IntDeserializer intDeserializer, @Nonnull StringDeserializer stringDeserializer) {
        this.intDeserializer = intDeserializer;
        this.stringDeserializer = stringDeserializer;
    }

    @Override
    public void reset() {

        intDeserializer.reset();
        stringDeserializer.reset();
        state = 0;
        value = null;
        key = null;
    }

    @Override
    public int consume(@Nonnull ByteBuffer byteBuffer, int remaining) {

        while (true) {
            switch (state) {
                case 0:
                    remaining = intDeserializer.consume(byteBuffer, remaining);
                    if (remaining == -1) {
                        return remaining;
                    }
                    size = intDeserializer.intValue();
                    if (size < 0) {
                        throw new IllegalArgumentException();
                    }
                    if (size == 0) {
                        value = null;
                        return remaining;
                    }
                    value = new LinkedHashMap<>(size);
                    state = 1;
                case 1:
                    remaining = stringDeserializer.consume(byteBuffer, remaining);
                    if (remaining == -1) {
                        return remaining;
                    }
                    key = stringDeserializer.value();
                    state = 2;
                case 2:
                    remaining = stringDeserializer.consume(byteBuffer, remaining);
                    if (remaining == -1) {
                        return remaining;
                    }
                    value.put(key, stringDeserializer.value());
                    if (value.size() == size) {
                        state = 0;
                        return remaining;
                    }
                    state = 1;
                    break;
                default:
                    throw new IllegalStateException();
            }
        }
    }

    @Nonnull
    public Map<String, String> value() {

        if (size == 0) {
            return emptyMap();
        } else {
            return unmodifiableMap(value);
        }
    }
}
