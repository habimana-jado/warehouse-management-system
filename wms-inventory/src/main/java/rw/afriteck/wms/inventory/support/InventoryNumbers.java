package rw.afriteck.wms.inventory.support;

public final class InventoryNumbers {
    private InventoryNumbers() {}
    public static String adjustment(long seq) { return "ADJ-%06d".formatted(seq); }
    public static String transfer(long seq)   { return "TRF-%06d".formatted(seq); }
    public static String count(long seq)      { return "CNT-%06d".formatted(seq); }
}
