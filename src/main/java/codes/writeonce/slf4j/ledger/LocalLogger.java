package codes.writeonce.slf4j.ledger;

import org.slf4j.MDC;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

final class LocalLogger implements AutoCloseable {

    private final StringBuilder builder = new StringBuilder();

    private long timestampMillis;

    private Level level;

    private Map<String, String> mdc;

    private String threadName;

    private String throwableString;

    @Nonnull
    private final LogQueue logQueue;

    LocalLogger(@Nonnull LogQueue logQueue) {
        this.logQueue = logQueue;
    }

    private void finish() {
        logQueue.publish(timestampMillis, level, mdc, threadName, throwableString, builder);
    }

    void initAppender(@Nonnull Level level) {
        timestampMillis = System.currentTimeMillis();
        this.level = level;
        mdc = ((MDCAdapterImpl) MDC.getMDCAdapter()).getPropertyMap();
        threadName = Thread.currentThread().getName();
    }

    int appendNext(@Nonnull String format, int start, @Nullable Object arg, boolean last) {
        final var length = format.length();
        var fromIndex = start;
        while (fromIndex + 1 < length) {
            final var index = format.indexOf('{', fromIndex);
            if (index == -1) {
                break;
            }
            if (format.charAt(index + 1) == '}') {
                if (index > fromIndex && format.charAt(index - 1) == '\\') {
                    if (index - 1 > fromIndex && format.charAt(index - 2) == '\\') {
                        builder.append(format, start, index - 1);
                        deeplyAppendParameter(arg);
                        return index + 2;
                    } else {
                        builder.append(format, start, index - 1);
                        start = index;
                        fromIndex = index + 2;
                    }
                } else {
                    builder.append(format, start, index);
                    deeplyAppendParameter(arg);
                    return index + 2;
                }
            } else {
                fromIndex = index + 1;
            }
        }
        if (start < length) {
            builder.append(format, start, length);
        }
        if (last && arg instanceof Throwable throwable) {
            appendThrowable(throwable);
        }
        return length;
    }

    void appendTail(@Nonnull String format, int start) {
        builder.append(format, start, format.length());
        finish();
    }

    void appendNull() {
        builder.append("null");
        finish();
    }

    void appendNull(@Nullable Object arg) {
        builder.append("null");
        if (arg instanceof Throwable throwable) {
            appendThrowable(throwable);
        }
        finish();
    }

    void appendNull(@Nullable Throwable throwable) {
        builder.append("null");
        if (throwable != null) {
            appendThrowable(throwable);
        }
        finish();
    }

    private void appendThrowable(@Nonnull Throwable throwable) {
        final var stringWriter = new StringWriter();
        final var printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);
        printWriter.flush();
        stringWriter.flush();
        throwableString = stringWriter.toString();
    }

    @Override
    public void close() {
        mdc = null;
        threadName = null;
        throwableString = null;
        builder.setLength(0);
    }

    // special treatment of array values was suggested by 'lizongbo'
    private void deeplyAppendParameter(Object o) {
        if (o == null) {
            builder.append("null");
            return;
        }
        if (!o.getClass().isArray()) {
            safeObjectAppend(o);
        } else {
            // check for primitive array types because they
            // unfortunately cannot be cast to Object[]
            if (o instanceof boolean[]) {
                booleanArrayAppend((boolean[]) o);
            } else if (o instanceof byte[]) {
                byteArrayAppend((byte[]) o);
            } else if (o instanceof char[]) {
                charArrayAppend((char[]) o);
            } else if (o instanceof short[]) {
                shortArrayAppend((short[]) o);
            } else if (o instanceof int[]) {
                intArrayAppend((int[]) o);
            } else if (o instanceof long[]) {
                longArrayAppend((long[]) o);
            } else if (o instanceof float[]) {
                floatArrayAppend((float[]) o);
            } else if (o instanceof double[]) {
                doubleArrayAppend((double[]) o);
            } else {
                objectArrayAppend((Object[]) o);
            }
        }
    }

    private void safeObjectAppend(Object o) {
        try {
            String oAsString = o.toString();
            builder.append(oAsString);
        } catch (Throwable t) {
            report("SLF4J: Failed toString() invocation on an object of type [" + o.getClass().getName() + "]", t);
            builder.append("[FAILED toString()]");
        }
    }

    static private void report(String msg, Throwable t) {
        System.err.println(msg);
        System.err.println("Reported exception:");
        t.printStackTrace();
    }

    private void objectArrayAppend(Object[] a) {
        builder.append('[');
        final int len = a.length;
        for (int i = 0; i < len; i++) {
            deeplyAppendParameter(a[i]);
            if (i != len - 1) {
                builder.append(", ");
            }
        }
        builder.append(']');
    }

    private void booleanArrayAppend(boolean[] a) {
        builder.append('[');
        final int len = a.length;
        for (int i = 0; i < len; i++) {
            builder.append(a[i]);
            if (i != len - 1) {
                builder.append(", ");
            }
        }
        builder.append(']');
    }

    private void byteArrayAppend(byte[] a) {
        builder.append('[');
        final int len = a.length;
        for (int i = 0; i < len; i++) {
            builder.append(a[i]);
            if (i != len - 1) {
                builder.append(", ");
            }
        }
        builder.append(']');
    }

    private void charArrayAppend(char[] a) {
        builder.append('[');
        final int len = a.length;
        for (int i = 0; i < len; i++) {
            builder.append(a[i]);
            if (i != len - 1) {
                builder.append(", ");
            }
        }
        builder.append(']');
    }

    private void shortArrayAppend(short[] a) {
        builder.append('[');
        final int len = a.length;
        for (int i = 0; i < len; i++) {
            builder.append(a[i]);
            if (i != len - 1) {
                builder.append(", ");
            }
        }
        builder.append(']');
    }

    private void intArrayAppend(int[] a) {
        builder.append('[');
        final int len = a.length;
        for (int i = 0; i < len; i++) {
            builder.append(a[i]);
            if (i != len - 1) {
                builder.append(", ");
            }
        }
        builder.append(']');
    }

    private void longArrayAppend(long[] a) {
        builder.append('[');
        final int len = a.length;
        for (int i = 0; i < len; i++) {
            builder.append(a[i]);
            if (i != len - 1) {
                builder.append(", ");
            }
        }
        builder.append(']');
    }

    private void floatArrayAppend(float[] a) {
        builder.append('[');
        final int len = a.length;
        for (int i = 0; i < len; i++) {
            builder.append(a[i]);
            if (i != len - 1) {
                builder.append(", ");
            }
        }
        builder.append(']');
    }

    private void doubleArrayAppend(double[] a) {
        builder.append('[');
        final int len = a.length;
        for (int i = 0; i < len; i++) {
            builder.append(a[i]);
            if (i != len - 1) {
                builder.append(", ");
            }
        }
        builder.append(']');
    }
}
