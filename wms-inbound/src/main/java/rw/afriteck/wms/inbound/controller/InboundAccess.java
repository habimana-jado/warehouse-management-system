package rw.afriteck.wms.inbound.controller;

final class InboundAccess {

    private InboundAccess() {}

    /** Create, edit, confirm, cancel and close POs; create and cancel customer returns. */
    static final String MANAGER = "hasAnyRole('ADMIN','WAREHOUSE_MANAGER')";

    /** Read POs, receipts and customer returns. */
    static final String READ = "hasAnyRole('ADMIN','WAREHOUSE_MANAGER','VIEWER','RECEIVER')";

    /** Receive goods and customer returns, scan barcodes against a PO. */
    static final String RECEIVE = "hasAnyRole('ADMIN','WAREHOUSE_MANAGER','RECEIVER')";

    /** Confirm a put-away line. The service also requires the caller to be the task's assignee unless privileged. */
    static final String WORKER = "hasAnyRole('ADMIN','WAREHOUSE_MANAGER','RECEIVER','PICKER')";
}