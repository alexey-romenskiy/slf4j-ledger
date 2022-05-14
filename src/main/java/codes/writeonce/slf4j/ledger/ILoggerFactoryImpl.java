package codes.writeonce.slf4j.ledger;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.util.concurrent.ConcurrentHashMap;

public final class ILoggerFactoryImpl implements ILoggerFactory {

    private static final LogQueue LOG_QUEUE = new LogQueue(createPublisher());

    private final Logger root = new LoggerImpl(null, LOG_QUEUE);

    private final ConcurrentHashMap<String, Logger> loggers = new ConcurrentHashMap<>();

    @Nonnull
    private static Publisher createPublisher() {

        final var config = new Config();
        final var mode = config.getProperty("mode", "stderr");

        return switch (mode) {
            case "stdout" -> new StreamPublisher(System.out);
            case "stderr" -> new StreamPublisher(System.err);
            case "rolling" -> new RollingFilePublisher(config);
            default -> throw new IllegalArgumentException("Invalid mode selected: " + mode);
        };
    }

    public ILoggerFactoryImpl() {
        // empty
    }

    @Override
    public Logger getLogger(String name) {

        if (name == null) {
            throw new IllegalArgumentException("name argument cannot be null");
        }

        if (Logger.ROOT_LOGGER_NAME.equalsIgnoreCase(name)) {
            return root;
        }

        return loggers.computeIfAbsent(name, n -> new LoggerImpl(n, LOG_QUEUE));
    }
}
