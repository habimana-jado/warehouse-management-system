package rw.afriteck.wms.outbound.support;

public final class OutboundReferences {
    private OutboundReferences() {}
    public static final String PICK_LIST = "PICK_LIST";         // ISSUE ledger lines and PICK tasks: referenceId = pick list id,
    //   task line referenceLineId = pick line id
    public static final String RTV = "RTV";                     // supplier return ISSUE lines: referenceId = return id
    public static final String SALES_ORDER = "SALES_ORDER";     // declared, not used in the ledger or in tasks yet
}
