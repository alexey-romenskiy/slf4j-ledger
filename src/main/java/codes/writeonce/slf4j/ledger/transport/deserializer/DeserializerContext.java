package codes.writeonce.slf4j.ledger.transport.deserializer;

import javax.annotation.Nonnull;

public interface DeserializerContext {

    void push(@Nonnull Deserializer deserializer);
}
