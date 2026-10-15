package rw.afriteck.wms.inbound.support;

/**
 * referenceType strings shared with inventory (ledger) and operations (tasks).
 * Declared once here and used for every post and every task call, as the backbone requires.
 */
public final class InboundReferences {

    private InboundReferences() {}

    /** Ledger receipts and PUTAWAY tasks: the goods receipt is the document that moved the stock. */
    public static final String GOODS_RECEIPT = "GOODS_RECEIPT";

    /** Ledger postings of customer returns (backbone: returns use RECEIPT with referenceType "RMA"). */
    public static final String RMA = "RMA";

    public static final String PURCHASE_ORDER = "PURCHASE_ORDER";
}