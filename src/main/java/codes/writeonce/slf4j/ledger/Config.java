package codes.writeonce.slf4j.ledger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Properties;

class Config {

    @Nullable
    private final Properties properties;

    public Config() {

        final var classLoader = getClass().getClassLoader();

        var stream = classLoader.getResourceAsStream("slf4j-ledger-test.properties");
        if (stream == null) {
            stream = classLoader.getResourceAsStream("slf4j-ledger.properties");
            if (stream == null) {
                properties = null;
                return;
            }
        }

        try (final var in = stream) {
            properties = new Properties();
            properties.load(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    public String getProperty(@Nonnull String name) {

        final var value = System.getProperty("codes.writeonce.slf4j.ledger." + name);
        if (value != null) {
            return value;
        }

        if (properties != null) {
            return properties.getProperty(name);
        }

        return null;
    }

    @Nonnull
    public String getProperty(@Nonnull String name, @Nonnull String defaultValue) {

        final var value = getProperty(name);
        if (value == null) {
            return defaultValue;
        }

        return value;
    }
}
