package rw.afriteck.wms.inbound.enums;

public enum PutawayStatus {
    NOT_REQUIRED,   // DIRECT receipts, or nothing accepted on the line
    PENDING,        // waiting for the worker
    DONE,           // moved in full
    SHORT           // task line closed below plan: the rest stays in the receiving bin
}
