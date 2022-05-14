package codes.writeonce.slf4j.ledger;

import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.LinkedHashMap;

public class LogQueueTest {

    private static final LogQueue logQueue = new LogQueue(new StreamPublisher(System.err));

    @Test
    public void main() throws InterruptedException {

        final var mdc = new LinkedHashMap<String, String>();
        mdc.put("foo", "bar");

        log(Instant.parse("2021-07-29T01:02:03.000Z"), mdc, Level.INFO, "thread-name", "Hello, World!",
                new Exception());
        Thread.sleep(1500);
        log(Instant.parse("2021-07-29T01:02:03.001Z"), mdc, Level.WARN, null, "", null);
    }

    private static void log(
            @Nonnull Instant timestamp,
            @Nullable LinkedHashMap<String, String> mdc,
            @Nonnull Level level,
            @Nullable String threadName,
            @Nonnull String msg,
            @Nullable Throwable throwable
    ) {
        logQueue.publish(timestamp.toEpochMilli(), level, mdc, threadName, getThrowableString(throwable),
                new StringBuilder().append(msg));
    }

    private static String getThrowableString(@Nullable Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        final var stringWriter = new StringWriter();
        final var printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);
        printWriter.flush();
        stringWriter.flush();
        return stringWriter.toString();
    }
}
