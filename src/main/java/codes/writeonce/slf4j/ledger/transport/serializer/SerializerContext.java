package codes.writeonce.slf4j.ledger.transport.serializer;

import javax.annotation.Nonnull;

public interface SerializerContext {

    void push(@Nonnull Serializer serializer);
}
