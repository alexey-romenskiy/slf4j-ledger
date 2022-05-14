package codes.writeonce.slf4j.ledger;

import javax.annotation.Nonnull;

public enum Level {
    TRACE("TRACE"),
    DEBUG("DEBUG"),
    INFO("INFO "),
    WARN("WARN "),
    ERROR("ERROR");

    @Nonnull
    private final String formattedName;

    Level(@Nonnull String formattedName) {
        this.formattedName = formattedName;
    }

    @Nonnull
    public String getFormattedName() {
        return formattedName;
    }
}
