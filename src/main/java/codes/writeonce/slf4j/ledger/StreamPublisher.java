package codes.writeonce.slf4j.ledger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.PrintStream;
import java.nio.CharBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import static java.time.ZoneOffset.UTC;

final class StreamPublisher implements Publisher {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    @Nonnull
    private final PrintStream printStream;

    StreamPublisher(@Nonnull PrintStream printStream) {
        dateFormat.setTimeZone(TimeZone.getTimeZone(UTC));
        this.printStream = printStream;
    }

    @Nullable
    private String throwableString;

    @Override
    public void next(
            int textSize,
            long timestampMillis,
            @Nonnull Level level,
            @Nullable Map<String, String> mdc,
            @Nullable String threadName,
            @Nullable String throwableString
    ) {
        printStream.print("[");
        printStream.print(dateFormat.format(new Date(timestampMillis)));
        printStream.print("] ");
        printStream.print(level.getFormattedName());
        printStream.print(" (");
        printStream.print(threadName);
        printStream.print(") ");
        if (textSize == 0) {
            printStream.println(mdc);
            if (throwableString != null) {
                printStream.print(throwableString);
            }
        } else {
            printStream.print(mdc);
            printStream.print(" ");
            this.throwableString = throwableString;
        }
    }

    @Override
    public void chunk(boolean last, @Nonnull CharBuffer charBuffer) {
        printStream.append(charBuffer);
        if (last) {
            printStream.println();
            if (throwableString != null) {
                printStream.print(throwableString);
                throwableString = null;
            }
        }
    }
}
