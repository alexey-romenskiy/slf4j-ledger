package codes.writeonce.slf4j.ledger;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;

public interface ChunkWriter {

    void chunk(boolean last, boolean endOfBatch, int start, int end, @Nonnull byte[] bytes) throws InterruptedException;

    void sequence(long sequence);

    void last() throws InterruptedException;

    void endOfBatch() throws InterruptedException;

    @Nonnull
    ByteBuffer chunk() throws InterruptedException;
}
