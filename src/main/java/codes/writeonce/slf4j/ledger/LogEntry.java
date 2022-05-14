package codes.writeonce.slf4j.ledger;

import java.util.Map;

final class LogEntry {

    long timestampMillis;

    Level level;

    Map<String, String> mdc;

    String threadName;

    String throwableString;

    int textSize;

    LogEntry() {
        // empty
    }
}
