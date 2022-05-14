package codes.writeonce.slf4j.ledger;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;

public interface Receiver {

    void next(long sequence, long offset, boolean last, @Nonnull ByteBuffer byteBuffer);
}
