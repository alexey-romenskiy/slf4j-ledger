package codes.writeonce.slf4j.ledger.transport.deserializer;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;

public interface Deserializer {

    void reset();

    /**
     * @return <code>-1</code> if all bytes consumed but parsing not completed,
     * otherwise parsing is completed with non-negative number of bytes remaining
     */
    int consume(@Nonnull ByteBuffer byteBuffer, int remaining);
}
