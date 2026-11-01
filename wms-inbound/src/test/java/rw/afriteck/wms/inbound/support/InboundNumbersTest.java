package rw.afriteck.wms.inbound.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InboundNumbersTest {

    @Test
    void purchaseOrder_FormatsCorrectly() {
        assertEquals("PO-000001", InboundNumbers.purchaseOrder(1L));
        assertEquals("PO-000123", InboundNumbers.purchaseOrder(123L));
    }

    @Test
    void goodsReceipt_FormatsCorrectly() {
        assertEquals("GRN-000001", InboundNumbers.goodsReceipt(1L));
        assertEquals("GRN-000456", InboundNumbers.goodsReceipt(456L));
    }

    @Test
    void customerReturn_FormatsCorrectly() {
        assertEquals("RMA-000001", InboundNumbers.customerReturn(1L));
        assertEquals("RMA-000789", InboundNumbers.customerReturn(789L));
    }
}
