package codes.writeonce.slf4j.ledger.transport.serializer;

import javax.annotation.Nonnull;

public class CompactingStringSerializer extends StringSerializer {

    public CompactingStringSerializer(@Nonnull IntSerializer intSerializer) {
        super(intSerializer);
    }

    @Override
    protected void initState() {

        int i = 0;
        while (i < length) {
            final char c = chars[i];
            if (c > 0xff) {
                intSerializer.value(length);
                state = 1;
                return;
            }
            i++;
        }

        intSerializer.value(-length);
        state = 0;
    }
}
