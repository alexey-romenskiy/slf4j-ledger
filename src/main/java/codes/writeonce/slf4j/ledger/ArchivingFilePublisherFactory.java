package codes.writeonce.slf4j.ledger;

import org.slf4j.helpers.Util;

import javax.annotation.Nonnull;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import static java.lang.ProcessBuilder.Redirect.DISCARD;
import static java.lang.ProcessBuilder.Redirect.PIPE;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

public class ArchivingFilePublisherFactory {

    private static final PrefixThreadFactory ARCHIVER_THREAD_FACTORY = new PrefixThreadFactory("log-archiver-", false);

    private static final PrefixThreadFactory ARCHIVE_WRITER_THREAD_FACTORY =
            new PrefixThreadFactory("log-archive-writer-", false);

    @Nonnull
    private final String prefix;

    @Nonnull
    private final Path archiveDir;

    @Nonnull
    private final Path currentDir;

    private final int compressionLevel;

    private final int compressionThreads;

    private final Pattern pattern;

    private boolean initialized;

    public ArchivingFilePublisherFactory(
            @Nonnull String prefix,
            @Nonnull Path archiveDir,
            @Nonnull Path currentDir,
            int compressionLevel,
            int compressionThreads
    ) {
        this.prefix = prefix;
        this.archiveDir = archiveDir;
        this.currentDir = currentDir;
        this.compressionLevel = compressionLevel;
        this.compressionThreads = compressionThreads;
        pattern = Pattern.compile("^" + Pattern.quote(prefix) + "\\.(\\d{4})-(\\d{2})-(\\d{2})-(\\d{2})\\.log$");
    }

    @Nonnull
    public ArchivingFilePublisher newInstance(
            int year,
            int month,
            int day,
            int hour
    ) {
        final var logPath = getLogPath(year, month, day, hour);
        final var tmpPath = getTmpPath(year, month, day, hour);
        final var archPath = getArchPath(year, month, day, hour);

        if (!initialized) {
            initialize(logPath);
            initialized = true;
        }

        return open(logPath, tmpPath, archPath);
    }

    private void initialize(@Nonnull Path logPath) {

        try {
            final var logName = logPath.getFileName().toString();
            final var matcher = pattern.matcher("");

            final var paths = Files.list(currentDir).filter(path -> {
                final var name = path.getFileName().toString();
                return !name.equals(logName) && matcher.reset(name).matches() && Files.isRegularFile(path);
            }).sorted().toArray(Path[]::new);

            if (paths.length != 0) {
                ARCHIVER_THREAD_FACTORY.newThread(() -> archiveOld(paths)).start();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void archiveOld(@Nonnull Path[] paths) {

        try {
            final var matcher = pattern.matcher("");
            for (final var path : paths) {
                try {
                    final var name = path.getFileName().toString();
                    if (!matcher.reset(name).find()) {
                        throw new IllegalStateException();
                    }

                    final var year = Integer.parseInt(matcher.group(1));
                    final var month = Integer.parseInt(matcher.group(2));
                    final var day = Integer.parseInt(matcher.group(3));
                    final var hour = Integer.parseInt(matcher.group(4));

                    final var tmpPath = getTmpPath(year, month, day, hour);
                    final var archPath = getArchPath(year, month, day, hour);

                    try (var logStream = Files.newInputStream(path, READ);
                         var tmpStream = Files.newOutputStream(tmpPath, CREATE, TRUNCATE_EXISTING, WRITE)) {

                        final var process = spawnArchiver();

                        try (var stdoutStream = process.getInputStream()) {

                            final var future = new CompletableFuture<Void>();

                            ARCHIVE_WRITER_THREAD_FACTORY.newThread(() -> writeArchive(stdoutStream, tmpStream, future))
                                    .start();

                            try (var stdinStream = process.getOutputStream()) {
                                logStream.transferTo(stdinStream);
                            }

                            future.get();

                            final var exitValue = process.waitFor();
                            if (exitValue != 0) {
                                Util.report("Compressor process exited with code: " + exitValue);
                                Util.report("Failed to compress log file: " + path);
                                Util.report("Failed to compress logs");
                                break;
                            }
                        } catch (Throwable e) {
                            final var exitValue = process.waitFor();
                            if (exitValue != 0) {
                                Util.report("Compressor process exited with code: " + exitValue);
                            }
                            throw e;
                        }
                    }

                    Files.move(tmpPath, archPath, ATOMIC_MOVE);
                    Files.delete(path);
                } catch (Throwable e) {
                    Util.report("Failed to compress log file: " + path, e);
                    Util.report("Failed to compress logs");
                    break;
                }
            }
        } catch (Throwable e) {
            Util.report("Failed to compress logs", e);
        }
    }

    @Nonnull
    private ArchivingFilePublisher open(@Nonnull Path logPath, @Nonnull Path tmpPath, @Nonnull Path archPath) {

        try {

            OutputStream logStream = null;
            InputStream inputStream = null;

            while (true) {
                try {
                    logStream = Files.newOutputStream(logPath, CREATE_NEW, WRITE);
                    break;
                } catch (FileAlreadyExistsException e) {
                    try {
                        logStream = Files.newOutputStream(logPath, WRITE, APPEND);
                    } catch (FileNotFoundException ignored) {
                        continue;
                    }
                    try {
                        inputStream = Files.newInputStream(logPath, READ);
                        break;
                    } catch (FileNotFoundException ignored) {
                        logStream.close();
                    }
                }
            }

            final var tmpStream = Files.newOutputStream(tmpPath, CREATE, TRUNCATE_EXISTING, WRITE);
            final var process = spawnArchiver();

            if (inputStream == null) {

            } else {

            }

            return new ArchivingFilePublisher(logPath, tmpPath, archPath, logStream, tmpStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void writeArchive(
            @Nonnull InputStream inputStream,
            @Nonnull OutputStream outputStream,
            @Nonnull CompletableFuture<Void> future
    ) {
        try {
            inputStream.transferTo(outputStream);
            future.complete(null);
        } catch (Throwable e) {
            future.completeExceptionally(e);
        }
    }

    @Nonnull
    private Process spawnArchiver() throws IOException {

        return new ProcessBuilder("xz", "-" + compressionLevel, "-T", String.valueOf(compressionThreads))
                .redirectInput(PIPE)
                .redirectError(DISCARD)
                .redirectOutput(PIPE)
                .start();
    }

    @Nonnull
    private Path getArchPath(int year, int month, int day, int hour) {

        return archiveDir
                .resolve(String.format("%04d", year))
                .resolve(String.format("%02d", month))
                .resolve(String.format("%02d", day))
                .resolve(String.format("%s.%04d-%02d-%02d-%02d.log.xz", prefix, year, month, day, hour));
    }

    @Nonnull
    private Path getTmpPath(int year, int month, int day, int hour) {

        return archiveDir
                .resolve(String.format("%04d", year))
                .resolve(String.format("%02d", month))
                .resolve(String.format("%02d", day))
                .resolve(String.format("%s.%04d-%02d-%02d-%02d.log.xz.tmp", prefix, year, month, day, hour));
    }

    @Nonnull
    private Path getLogPath(int year, int month, int day, int hour) {

        return currentDir.resolve(String.format("%s.%04d-%02d-%02d-%02d.log", prefix, year, month, day, hour));
    }
}
