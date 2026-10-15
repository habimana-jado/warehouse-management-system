package rw.afriteck.wms.inbound.support;

import java.util.Locale;

/** Formats sequence values into human-readable document numbers. Gaps are acceptable. */
public final class InboundNumbers {

    private InboundNumbers() {}

    public static String purchaseOrder(long seq)  { return String.format(Locale.ROOT, "PO-%06d", seq); }
    public static String goodsReceipt(long seq)   { return String.format(Locale.ROOT, "GRN-%06d", seq); }
    public static String customerReturn(long seq) { return String.format(Locale.ROOT, "RMA-%06d", seq); }
}