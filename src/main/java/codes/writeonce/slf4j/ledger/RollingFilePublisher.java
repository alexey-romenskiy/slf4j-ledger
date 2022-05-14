package codes.writeonce.slf4j.ledger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.CharBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;

import static java.time.ZoneOffset.UTC;

public class RollingFilePublisher implements Publisher, AutoCloseable {

    @Nonnull
    private final ArchivingFilePublisherFactory publisherFactory;

    private ArchivingFilePublisher publisher;

    private long nextMillis;

    public RollingFilePublisher(@Nonnull Config config) {

        try {
            final var prefix = config.getProperty("prefix", "main");
            final var baseDir = ensureDir(Paths.get(config.getProperty("baseDir", "")).toAbsolutePath().toRealPath());
            final var archiveDir = ensureDir(baseDir.resolve("archive"));
            final var currentDir = ensureDir(baseDir.resolve("current"));
            final var compressionLevel = Integer.parseInt(config.getProperty("compressionLevel", "0"));
            if (compressionLevel < 0 || compressionLevel > 9) {
                throw new IllegalArgumentException();
            }
            final var compressionThreads = Integer.parseInt(config.getProperty("compressionThreads", "1"));
            if (compressionThreads < 1) {
                throw new IllegalArgumentException();
            }
            publisherFactory = new ArchivingFilePublisherFactory(
                    prefix,
                    archiveDir,
                    currentDir,
                    compressionLevel,
                    compressionThreads
            );
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
        checkRoll(timestampMillis).next(textSize, timestampMillis, level, mdc, threadName, throwableString);
    }

    @Override
    public void chunk(boolean last, @Nonnull CharBuffer charBuffer) {
        publisher.chunk(last, charBuffer);
    }

    @Override
    public void close() {

        if (publisher != null) {
            publisher.close();
            publisher = null;
        }
    }

    @Nonnull
    private ArchivingFilePublisher checkRoll(long timestampMillis) {

        if (publisher != null) {
            if (timestampMillis < nextMillis) {
                return publisher;
            }

            publisher.closeAndRoll();
        }

        final var now = Instant.ofEpochMilli(timestampMillis).atOffset(UTC);
        final var year = now.getYear();
        final var month = now.getMonthValue();
        final var day = now.getDayOfMonth();
        final var hour = now.getHour();
        nextMillis = OffsetDateTime.of(year, month, day, hour, 0, 0, 0, UTC).plusHours(1).toInstant().toEpochMilli();

        return publisher = publisherFactory.newInstance(
                year,
                month,
                day,
                hour
        );
    }

    @Nonnull
    private static Path ensureDir(@Nonnull Path baseDir) throws IOException {

        if (Files.exists(baseDir)) {
            if (Files.isDirectory(baseDir)) {
                throw new IllegalArgumentException();
            }
        } else {
            Files.createDirectories(baseDir);
        }

        return baseDir;
    }
}
