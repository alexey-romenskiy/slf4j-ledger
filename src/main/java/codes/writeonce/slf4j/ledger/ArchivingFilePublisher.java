package codes.writeonce.slf4j.ledger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.CharBuffer;
import java.nio.file.Path;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ArchivingFilePublisher implements Publisher, AutoCloseable {

    @Nonnull
    private final Path logPath;

    @Nonnull
    private final Path tmpPath;

    @Nonnull
    private final Path archPath;

    @Nonnull
    private final OutputStream logStream;

    @Nonnull
    private final OutputStream tmpStream;

    public ArchivingFilePublisher(
            @Nonnull Path logPath,
            @Nonnull Path tmpPath,
            @Nonnull Path archPath,
            @Nonnull OutputStream logStream,
            @Nonnull OutputStream tmpStream
    ) {
        this.logPath = logPath;
        this.tmpPath = tmpPath;
        this.archPath = archPath;
        this.logStream = logStream;
        this.tmpStream = tmpStream;
    }

    @Override
    public void next(
            int textSize,
            long timestampMillis,
            @Nonnull Level level,
            @Nullable Map<String, String> mdc,
            @Nullable String threadName,
            @Nullable String throwableString
    ) {
        try {
            if (isRollNeded(timestampMillis)) {

                if (!INITIALIZED) {
                    initialize();
                }

                lastMillis = timestampMillis;
                outputStream = open(year, month, day, hour);
                streamPublisher = new StreamPublisher(new PrintStream(outputStream, true, UTF_8));
            }
            // TODO:

        } catch (IOException e) {

        }
    }

    @Override
    public void chunk(boolean last, @Nonnull CharBuffer charBuffer) {
        // TODO:
    }

    @Override
    public void close() {
        // TODO:
    }

    public void closeAndRoll() {
        // TODO:
    }
}
