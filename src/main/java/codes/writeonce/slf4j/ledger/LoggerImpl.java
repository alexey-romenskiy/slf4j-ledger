package codes.writeonce.slf4j.ledger;

import org.slf4j.Logger;
import org.slf4j.Marker;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class LoggerImpl implements Logger {

    @Nullable
    private final String name;

    private final boolean traceEnabled;

    private final boolean debugEnabled;

    private final ThreadLocal<LocalLogger> localLoggerThreadLocal;

    LoggerImpl(@Nullable String name, @Nonnull LogQueue logQueue) {
        this.name = name;
        traceEnabled = name != null && name.startsWith("com.cryptexclub");
        debugEnabled = name != null && name.startsWith("com.cryptexclub");
        localLoggerThreadLocal = ThreadLocal.withInitial(() -> new LocalLogger(logQueue));
    }

    private void log(@Nonnull Level level, @Nullable String format) {
        try (var localLogger = localLoggerThreadLocal.get()) {
            localLogger.initAppender(level);
            if (format == null) {
                localLogger.appendNull();
            } else {
                localLogger.appendTail(format, 0);
            }
        }
    }

    private void log(@Nonnull Level level, @Nullable String format, @Nullable Object arg) {
        try (var localLogger = localLoggerThreadLocal.get()) {
            localLogger.initAppender(level);
            if (format == null) {
                localLogger.appendNull(arg);
            } else {
                int start = 0;
                start = localLogger.appendNext(format, start, arg, true);
                localLogger.appendTail(format, start);
            }
        }
    }

    private void log(@Nonnull Level level, @Nullable String format, @Nullable Object arg1, @Nullable Object arg2) {
        try (var localLogger = localLoggerThreadLocal.get()) {
            localLogger.initAppender(level);
            if (format == null) {
                localLogger.appendNull(arg2);
            } else {
                int start = 0;
                start = localLogger.appendNext(format, start, arg1, false);
                start = localLogger.appendNext(format, start, arg2, true);
                localLogger.appendTail(format, start);
            }
        }
    }

    private void log(@Nonnull Level level, @Nullable String format, @Nullable Object... arguments) {
        try (var localLogger = localLoggerThreadLocal.get()) {
            localLogger.initAppender(level);
            if (format == null) {
                if (arguments == null || arguments.length == 0) {
                    localLogger.appendNull();
                } else {
                    localLogger.appendNull(arguments[arguments.length - 1]);
                }
            } else {
                int start = 0;
                if (arguments != null) {
                    var length = arguments.length;
                    if (length > 0) {
                        length--;
                        for (int i = 0; i < length; i++) {
                            start = localLogger.appendNext(format, start, arguments[i], false);
                        }
                        start = localLogger.appendNext(format, start, arguments[length], true);
                    }
                }
                localLogger.appendTail(format, start);
            }
        }
    }

    private void log(@Nonnull Level level, @Nullable String format, @Nullable Throwable throwable) {
        try (var localLogger = localLoggerThreadLocal.get()) {
            localLogger.initAppender(level);
            if (format == null) {
                localLogger.appendNull(throwable);
            } else {
                int start = 0;
                start = localLogger.appendNext(format, start, throwable, true);
                localLogger.appendTail(format, start);
            }
        }
    }

    @Override
    public String getName() {
        return name == null ? Logger.ROOT_LOGGER_NAME : name;
    }

    @Override
    public boolean isTraceEnabled() {
        return traceEnabled;
    }

    @Override
    public void trace(String msg) {
        if (traceEnabled) {
            log(Level.TRACE, msg);
        }
    }

    @Override
    public void trace(String format, Object arg) {
        if (traceEnabled) {
            log(Level.TRACE, format, arg);
        }
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        if (traceEnabled) {
            log(Level.TRACE, format, arg1, arg2);
        }
    }

    @Override
    public void trace(String format, Object... arguments) {
        if (traceEnabled) {
            log(Level.TRACE, format, arguments);
        }
    }

    @Override
    public void trace(String msg, Throwable t) {
        if (traceEnabled) {
            log(Level.TRACE, msg, t);
        }
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return isTraceEnabled();
    }

    @Override
    public void trace(Marker marker, String msg) {
        trace(msg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        trace(format, arg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        trace(format, arg1, arg2);
    }

    @Override
    public void trace(Marker marker, String format, Object... arguments) {
        trace(format, arguments);
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        trace(msg, t);
    }

    @Override
    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    @Override
    public void debug(String msg) {
        if (debugEnabled) {
            log(Level.DEBUG, msg);
        }
    }

    @Override
    public void debug(String format, Object arg) {
        if (debugEnabled) {
            log(Level.DEBUG, format, arg);
        }
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        if (debugEnabled) {
            log(Level.DEBUG, format, arg1, arg2);
        }
    }

    @Override
    public void debug(String format, Object... arguments) {
        if (debugEnabled) {
            log(Level.DEBUG, format, arguments);
        }
    }

    @Override
    public void debug(String msg, Throwable t) {
        if (debugEnabled) {
            log(Level.DEBUG, msg, t);
        }
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return isDebugEnabled();
    }

    @Override
    public void debug(Marker marker, String msg) {
        debug(msg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        debug(format, arg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        debug(format, arg1, arg2);
    }

    @Override
    public void debug(Marker marker, String format, Object... arguments) {
        debug(format, arguments);
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        debug(msg, t);
    }

    @Override
    public boolean isInfoEnabled() {
        return true;
    }

    @Override
    public void info(String msg) {
        log(Level.INFO, msg);
    }

    @Override
    public void info(String format, Object arg) {
        log(Level.INFO, format, arg);
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        log(Level.INFO, format, arg1, arg2);
    }

    @Override
    public void info(String format, Object... arguments) {
        log(Level.INFO, format, arguments);
    }

    @Override
    public void info(String msg, Throwable t) {
        log(Level.INFO, msg, t);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return isInfoEnabled();
    }

    @Override
    public void info(Marker marker, String msg) {
        info(msg);
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        info(format, arg);
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        info(format, arg1, arg2);
    }

    @Override
    public void info(Marker marker, String format, Object... arguments) {
        info(format, arguments);
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        info(msg, t);
    }

    @Override
    public boolean isWarnEnabled() {
        return true;
    }

    @Override
    public void warn(String msg) {
        log(Level.WARN, msg);
    }

    @Override
    public void warn(String format, Object arg) {
        log(Level.WARN, format, arg);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        log(Level.WARN, format, arg1, arg2);
    }

    @Override
    public void warn(String format, Object... arguments) {
        log(Level.WARN, format, arguments);
    }

    @Override
    public void warn(String msg, Throwable t) {
        log(Level.WARN, msg, t);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return isWarnEnabled();
    }

    @Override
    public void warn(Marker marker, String msg) {
        warn(msg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        warn(format, arg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        warn(format, arg1, arg2);
    }

    @Override
    public void warn(Marker marker, String format, Object... arguments) {
        warn(format, arguments);
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        warn(msg, t);
    }

    @Override
    public boolean isErrorEnabled() {
        return true;
    }

    @Override
    public void error(String msg) {
        log(Level.ERROR, msg);
    }

    @Override
    public void error(String format, Object arg) {
        log(Level.ERROR, format, arg);
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        log(Level.ERROR, format, arg1, arg2);
    }

    @Override
    public void error(String format, Object... arguments) {
        log(Level.ERROR, format, arguments);
    }

    @Override
    public void error(String msg, Throwable t) {
        log(Level.ERROR, msg, t);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return isErrorEnabled();
    }

    @Override
    public void error(Marker marker, String msg) {
        error(msg);
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        error(format, arg);
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        error(format, arg1, arg2);
    }

    @Override
    public void error(Marker marker, String format, Object... arguments) {
        error(format, arguments);
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        error(msg, t);
    }
}
