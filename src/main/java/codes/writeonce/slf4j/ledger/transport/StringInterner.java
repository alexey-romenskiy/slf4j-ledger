package codes.writeonce.slf4j.ledger.transport;

import org.slf4j.helpers.Util;

import javax.annotation.Nonnull;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

public final class StringInterner {

    private static final int SHIFT =
            Integer.getInteger("codes.writeonce.slf4j.ledger.transport.StringInterner.table.size", 23);

    private static final int MASK = (1 << SHIFT) - 1;

    private static final AtomicReference<Object>[] INTERNER = newAtomicReferenceArray(1 << SHIFT);

    private static final ReferenceQueue<String> REFERENCE_QUEUE = new ReferenceQueue<>();

    private static final Thread COLLECTOR_THREAD = new Thread(
            () -> {
                try {
                    while (true) {
                        remove((Entry) REFERENCE_QUEUE.remove());
                    }
                } catch (Exception e) {
                    Util.report("Interrupted", e);
                }
            },
            "string-collector"
    );

    static {
        COLLECTOR_THREAD.setDaemon(true);
        COLLECTOR_THREAD.start();
    }

    @Nonnull
    @SuppressWarnings({"unchecked", "SynchronizationOnLocalVariableOrMethodParameter", "ForLoopReplaceableByForEach"})
    public static String intern(@Nonnull String source) {

        final int hash = source.hashCode();

        final AtomicReference<Object> atomic = INTERNER[hash & MASK];

        while (true) {

            final Object o = atomic.get();

            if (o == null) {
                if (atomic.compareAndSet(null, new Entry(source, hash, REFERENCE_QUEUE))) {
                    return source;
                }
            } else if (o instanceof Entry) {
                final Entry entry = (Entry) o;
                String value = entry.get();

                if (value == null) {
                    if (atomic.compareAndSet(o, new Entry(source, hash, REFERENCE_QUEUE))) {
                        return source;
                    }
                } else {
                    if (value.equals(source)) {
                        return value;
                    }

                    final ArrayList<Entry> list = new ArrayList<>(2);

                    synchronized (list) {
                        if (atomic.compareAndSet(o, list)) {
                            list.add(new Entry(value, hash, REFERENCE_QUEUE));
                            list.add(new Entry(source, hash, REFERENCE_QUEUE));
                            return source;
                        }
                    }
                }
            } else {
                final ArrayList<Entry> list = (ArrayList<Entry>) o;

                synchronized (list) {

                    String value;

                    final int size = list.size();

                    for (int i = 0; i < size; i++) {
                        final Entry entry = list.get(i);
                        value = entry.get();
                        if (value != null && value.equals(source)) {
                            return value;
                        }
                    }

                    if (atomic.get() == o) {
                        list.add(new Entry(source, hash, REFERENCE_QUEUE));
                        return source;
                    }
                }
            }
        }
    }

    @Nonnull
    public static String intern(@Nonnull CharSequence source) {
        return intern(source, hash(source, 0, source.length()));
    }

    @Nonnull
    @SuppressWarnings({"unchecked", "SynchronizationOnLocalVariableOrMethodParameter", "ForLoopReplaceableByForEach"})
    public static String intern(@Nonnull CharSequence source, int hash) {

        final AtomicReference<Object> atomic = INTERNER[hash & MASK];

        while (true) {

            final Object o = atomic.get();

            if (o == null) {
                final String value = source.toString();
                if (atomic.compareAndSet(null, new Entry(value, hash, REFERENCE_QUEUE))) {
                    return value;
                }
            } else if (o instanceof Entry) {
                final Entry entry = (Entry) o;
                String value = entry.get();

                if (value == null) {
                    value = source.toString();
                    if (atomic.compareAndSet(o, new Entry(value, hash, REFERENCE_QUEUE))) {
                        return value;
                    }
                } else {
                    if (equals(value, source, 0, source.length())) {
                        return value;
                    }

                    final ArrayList<Entry> list = new ArrayList<>(2);

                    synchronized (list) {
                        if (atomic.compareAndSet(o, list)) {
                            list.add(new Entry(value, hash, REFERENCE_QUEUE));
                            value = source.toString();
                            list.add(new Entry(value, hash, REFERENCE_QUEUE));
                            return value;
                        }
                    }
                }
            } else {
                final ArrayList<Entry> list = (ArrayList<Entry>) o;

                synchronized (list) {

                    String value;

                    final int size = list.size();
                    final int length = source.length();

                    for (int i = 0; i < size; i++) {
                        final Entry entry = list.get(i);
                        value = entry.get();
                        if (value != null && equals(value, source, 0, length)) {
                            return value;
                        }
                    }

                    if (atomic.get() == o) {
                        value = source.toString();
                        list.add(new Entry(value, hash, REFERENCE_QUEUE));
                        return value;
                    }
                }
            }
        }
    }

    @Nonnull
    public static String intern(@Nonnull CharSequence source, int start, int length) {
        return intern(source, hash(source, start, length));
    }

    @Nonnull
    @SuppressWarnings({"unchecked", "SynchronizationOnLocalVariableOrMethodParameter", "ForLoopReplaceableByForEach"})
    public static String intern(@Nonnull CharSequence source, int start, int length, int hash) {

        final AtomicReference<Object> atomic = INTERNER[hash & MASK];

        while (true) {

            final Object o = atomic.get();

            if (o == null) {
                final String value = source.subSequence(start, start + length).toString();
                if (atomic.compareAndSet(null, new Entry(value, hash, REFERENCE_QUEUE))) {
                    return value;
                }
            } else if (o instanceof Entry) {
                final Entry entry = (Entry) o;
                String value = entry.get();

                if (value == null) {
                    value = source.subSequence(start, start + length).toString();
                    if (atomic.compareAndSet(o, new Entry(value, hash, REFERENCE_QUEUE))) {
                        return value;
                    }
                } else {
                    if (equals(value, source, start, length)) {
                        return value;
                    }

                    final ArrayList<Entry> list = new ArrayList<>(2);

                    synchronized (list) {
                        if (atomic.compareAndSet(o, list)) {
                            list.add(new Entry(value, hash, REFERENCE_QUEUE));
                            value = source.subSequence(start, start + length).toString();
                            list.add(new Entry(value, hash, REFERENCE_QUEUE));
                            return value;
                        }
                    }
                }
            } else {
                final ArrayList<Entry> list = (ArrayList<Entry>) o;

                synchronized (list) {

                    String value;

                    final int size = list.size();

                    for (int i = 0; i < size; i++) {
                        final Entry entry = list.get(i);
                        value = entry.get();
                        if (value != null && equals(value, source, start, length)) {
                            return value;
                        }
                    }

                    if (atomic.get() == o) {
                        value = source.subSequence(start, start + length).toString();
                        list.add(new Entry(value, hash, REFERENCE_QUEUE));
                        return value;
                    }
                }
            }
        }
    }

    @Nonnull
    public static String intern(@Nonnull char[] source, int start, int length) {
        return intern(source, start, length, hash(source, start, length));
    }

    @Nonnull
    @SuppressWarnings({"unchecked", "SynchronizationOnLocalVariableOrMethodParameter", "ForLoopReplaceableByForEach"})
    public static String intern(@Nonnull char[] source, int start, int length, int hash) {

        final AtomicReference<Object> atomic = INTERNER[hash & MASK];

        while (true) {

            final Object o = atomic.get();

            if (o == null) {
                final String value = new String(source, start, length);
                if (atomic.compareAndSet(null, new Entry(value, hash, REFERENCE_QUEUE))) {
                    return value;
                }
            } else if (o instanceof Entry) {
                final Entry entry = (Entry) o;
                String value = entry.get();

                if (value == null) {
                    value = new String(source, start, length);
                    if (atomic.compareAndSet(o, new Entry(value, hash, REFERENCE_QUEUE))) {
                        return value;
                    }
                } else {
                    if (equals(value, source, start, length)) {
                        return value;
                    }

                    final ArrayList<Entry> list = new ArrayList<>(2);

                    synchronized (list) {
                        if (atomic.compareAndSet(o, list)) {
                            list.add(new Entry(value, hash, REFERENCE_QUEUE));
                            value = new String(source, start, length);
                            list.add(new Entry(value, hash, REFERENCE_QUEUE));
                            return value;
                        }
                    }
                }
            } else {
                final ArrayList<Entry> list = (ArrayList<Entry>) o;

                synchronized (list) {

                    String value;

                    final int size = list.size();

                    for (int i = 0; i < size; i++) {
                        final Entry entry = list.get(i);
                        value = entry.get();
                        if (value != null && equals(value, source, start, length)) {
                            return value;
                        }
                    }

                    if (atomic.get() == o) {
                        value = new String(source, start, length);
                        list.add(new Entry(value, hash, REFERENCE_QUEUE));
                        return value;
                    }
                }
            }
        }
    }

    @Nonnull
    public static String intern(@Nonnull byte[] source, int start, int length) {
        return intern(source, start, length, hash(source, start, length));
    }

    @Nonnull
    @SuppressWarnings({"unchecked", "SynchronizationOnLocalVariableOrMethodParameter", "ForLoopReplaceableByForEach"})
    public static String intern(@Nonnull byte[] source, int start, int length, int hash) {

        final AtomicReference<Object> atomic = INTERNER[hash & MASK];

        while (true) {

            final Object o = atomic.get();

            if (o == null) {
                final String value = new String(source, start, length, ISO_8859_1);
                if (atomic.compareAndSet(null, new Entry(value, hash, REFERENCE_QUEUE))) {
                    return value;
                }
            } else if (o instanceof Entry) {
                final Entry entry = (Entry) o;
                String value = entry.get();

                if (value == null) {
                    value = new String(source, start, length, ISO_8859_1);
                    if (atomic.compareAndSet(o, new Entry(value, hash, REFERENCE_QUEUE))) {
                        return value;
                    }
                } else {
                    if (equals(value, source, start, length)) {
                        return value;
                    }

                    final ArrayList<Entry> list = new ArrayList<>(2);

                    synchronized (list) {
                        if (atomic.compareAndSet(o, list)) {
                            list.add(new Entry(value, hash, REFERENCE_QUEUE));
                            value = new String(source, start, length, ISO_8859_1);
                            list.add(new Entry(value, hash, REFERENCE_QUEUE));
                            return value;
                        }
                    }
                }
            } else {
                final ArrayList<Entry> list = (ArrayList<Entry>) o;

                synchronized (list) {

                    String value;

                    final int size = list.size();

                    for (int i = 0; i < size; i++) {
                        final Entry entry = list.get(i);
                        value = entry.get();
                        if (value != null && equals(value, source, start, length)) {
                            return value;
                        }
                    }

                    if (atomic.get() == o) {
                        value = new String(source, start, length, ISO_8859_1);
                        list.add(new Entry(value, hash, REFERENCE_QUEUE));
                        return value;
                    }
                }
            }
        }
    }

    private static boolean equals(@Nonnull String value, @Nonnull CharSequence source, int start, int length) {

        if (value.length() != length) {
            return false;
        }

        for (int i = 0; i < length; i++) {
            if (value.charAt(i) != source.charAt(start++)) {
                return false;
            }
        }

        return true;
    }

    private static boolean equals(@Nonnull String value, @Nonnull char[] source, int start, int length) {

        if (value.length() != length) {
            return false;
        }

        for (int i = 0; i < length; i++) {
            if (value.charAt(i) != source[start++]) {
                return false;
            }
        }

        return true;
    }

    private static boolean equals(@Nonnull String value, @Nonnull byte[] source, int start, int length) {

        if (value.length() != length) {
            return false;
        }

        for (int i = 0; i < length; i++) {
            if (value.charAt(i) != (char) (0xff & source[start++])) {
                return false;
            }
        }

        return true;
    }

    @SuppressWarnings({"SynchronizationOnLocalVariableOrMethodParameter", "unchecked"})
    private static void remove(@Nonnull Entry reference) {

        final int hash = reference.hash;
        final AtomicReference<Object> atomic = INTERNER[hash & MASK];

        while (true) {

            final Object o = atomic.get();

            if (o == null) {
                break;
            }

            if (o == reference) {
                if (atomic.compareAndSet(o, null)) {
                    break;
                }
            } else if (o instanceof ArrayList) {
                final ArrayList<Entry> list = (ArrayList<Entry>) o;
                synchronized (list) {
                    if (list.remove(reference)) {
                        if (list.size() == 1) {
                            atomic.compareAndSet(o, list.get(0));
                            list.clear();
                        }
                    }
                }
                break;
            }
        }
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    private static <T> AtomicReference<T>[] newAtomicReferenceArray(int size) {

        final AtomicReference<T>[] array = new AtomicReference[size];
        for (int i = 0; i < size; i++) {
            array[i] = new AtomicReference<>();
        }
        return array;
    }

    private static int hash(@Nonnull CharSequence source, int start, int length) {

        final int end = start + length;
        int h = 0;

        while (start < end) {
            h = h * 31 + source.charAt(start++);
        }

        return h;
    }

    private static int hash(@Nonnull char[] source, int start, int length) {

        final int end = start + length;
        int h = 0;

        while (start < end) {
            h = h * 31 + source[start++];
        }

        return h;
    }

    private static int hash(@Nonnull byte[] source, int start, int length) {

        final int end = start + length;
        int h = 0;

        while (start < end) {
            h = h * 31 + (0xff & source[start++]);
        }

        return h;
    }

    private static class Entry extends WeakReference<String> {

        private final int hash;

        public Entry(@Nonnull String referent, int hash, @Nonnull ReferenceQueue<? super String> referenceQueue) {
            super(referent, referenceQueue);
            this.hash = hash;
        }
    }

    private StringInterner() {
        // empty
    }
}
