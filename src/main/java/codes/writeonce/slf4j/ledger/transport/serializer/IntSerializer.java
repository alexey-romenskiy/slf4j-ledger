package codes.writeonce.slf4j.ledger.transport.serializer;

public class IntSerializer extends AbstractIntSerializer<Integer> {

    public void value(int value) {
        this.value = value;
    }
}
