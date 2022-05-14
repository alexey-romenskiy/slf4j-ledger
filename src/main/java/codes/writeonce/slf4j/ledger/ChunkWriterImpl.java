package codes.writeonce.slf4j.ledger;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;

public class ChunkWriterImpl implements ChunkWriter {

    @Override
    public void chunk(boolean last, boolean endOfBatch, int start, int end, @Nonnull byte[] bytes)
            throws InterruptedException {
        // TODO:
    }

    @Override
    public void sequence(long sequence) {
        // TODO:
    }

    @Override
    public void last() throws InterruptedException {
        // TODO:
    }

    @Override
    public void endOfBatch() throws InterruptedException {
        // TODO:
    }

    @Nonnull
    @Override
    public ByteBuffer chunk() throws InterruptedException {
        return null; // TODO:
    }
}
