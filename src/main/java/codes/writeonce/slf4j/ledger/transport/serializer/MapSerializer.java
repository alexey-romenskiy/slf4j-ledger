package codes.writeonce.slf4j.ledger.transport.serializer;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;

public class MapSerializer implements Serializer {

    @Nonnull
    private final IntSerializer intSerializer;

    @Nonnull
    private final StringSerializer stringSerializer;

    private int state;

    private Map<String, String> map;

    private Iterator<Map.Entry<String, String>> iterator;

    private Map.Entry<String, String> entry;

    public MapSerializer(@Nonnull IntSerializer intSerializer, @Nonnull StringSerializer stringSerializer) {
        this.intSerializer = intSerializer;
        this.stringSerializer = stringSerializer;
    }

    @Override
    public void reset() {

        intSerializer.reset();
        stringSerializer.reset();
        state = 0;
        map = null;
        iterator = null;
    }

    @Override
    public int consume(@Nonnull ByteBuffer byteBuffer, int remaining) {

        while (true) {
            switch (state) {
                case 0:
                    remaining = intSerializer.consume(byteBuffer, remaining);
                    if (remaining == -1) {
                        return remaining;
                    }
                    if (map.isEmpty()) {
                        map = null;
                        return remaining;
                    }
                    state = 1;
                    iterator = map.entrySet().iterator();
                    entry = iterator.next();
                    stringSerializer.value(entry.getKey());
                case 1:
                    remaining = stringSerializer.consume(byteBuffer, remaining);
                    if (remaining == -1) {
                        return remaining;
                    }
                    state = 2;
                    stringSerializer.value(entry.getValue());
                case 2:
                    remaining = stringSerializer.consume(byteBuffer, remaining);
                    if (remaining == -1) {
                        return remaining;
                    }
                    if (!iterator.hasNext()) {
                        state = 0;
                        map = null;
                        iterator = null;
                        return remaining;
                    }
                    state = 1;
                    entry = iterator.next();
                    stringSerializer.value(entry.getKey());
                    break;
                default:
                    throw new IllegalStateException();
            }
        }
    }

    public void value(@Nonnull Map<String, String> value) {

        intSerializer.value(value.size());
        this.map = value;
    }
}
