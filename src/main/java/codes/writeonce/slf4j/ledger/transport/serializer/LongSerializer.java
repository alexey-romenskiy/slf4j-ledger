package codes.writeonce.slf4j.ledger.transport.serializer;

public class LongSerializer extends AbstractLongSerializer<Long> {

    public void value(long value) {
        this.value = value;
    }
}
