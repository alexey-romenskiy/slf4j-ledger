package codes.writeonce.slf4j.ledger.transport.serializer;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;

public interface Serializer {

    void reset();

    /**
     * @return <code>-1</code> if all bytes consumed but serialization not completed,
     * otherwise serialization is completed with non-negative number of bytes remaining
     */
    int consume(@Nonnull ByteBuffer byteBuffer, int remaining);
}
