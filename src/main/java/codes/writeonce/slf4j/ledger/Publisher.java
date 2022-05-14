package codes.writeonce.slf4j.ledger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.CharBuffer;
import java.util.Map;

public interface Publisher {

    void next(
            int textSize,
            long timestampMillis,
            @Nonnull Level level,
            @Nullable Map<String, String> mdc,
            @Nullable String threadName,
            @Nullable String throwableString
    );

    void chunk(boolean last, @Nonnull CharBuffer charBuffer);
}
