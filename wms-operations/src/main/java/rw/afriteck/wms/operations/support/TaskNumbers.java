package rw.afriteck.wms.operations.support;

public final class TaskNumbers {
    private static final String PREFIX = "TSK-";
    private TaskNumbers() {}

    /** 123 -> TSK-000123. Past 999999 it simply grows (TSK-1000000); the column is wide enough. */
    public static String format(long seq) {
        return PREFIX + String.format("%06d", seq);
    }
}
