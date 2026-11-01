package rw.afriteck.wms.inbound.support;

import org.junit.jupiter.api.Test;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.foundation.enums.TrackingType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TrackingRulesTest {

    private final UUID itemId = UUID.randomUUID();

    @Test
    void check_UntrackedItem_Success() {
        ItemResponse item = new ItemResponse(itemId, "SKU1", "Item 1", null, null, "Cat", "PCS", TrackingType.NONE, false, true);

        assertDoesNotThrow(() -> TrackingRules.check(item, null, null, null, BigDecimal.TEN));
    }

    @Test
    void check_UntrackedItem_WithLot_ThrowsException() {
        ItemResponse item = new ItemResponse(itemId, "SKU1", "Item 1", null, null, "Cat", "PCS", TrackingType.NONE, false, true);

        BusinessException ex = assertThrows(BusinessException.class, () -> TrackingRules.check(item, "LOT123", null, null, BigDecimal.TEN));
        assertEquals("TRACKING_MISMATCH", ex.getCode());
    }

    @Test
    void check_LotTrackedItem_Success() {
        ItemResponse item = new ItemResponse(itemId, "SKU2", "Item 2", null, null, "Cat", "PCS", TrackingType.LOT, false, true);

        assertDoesNotThrow(() -> TrackingRules.check(item, "LOT123", null, null, BigDecimal.TEN));
    }

    @Test
    void check_LotTrackedItem_MissingLot_ThrowsException() {
        ItemResponse item = new ItemResponse(itemId, "SKU2", "Item 2", null, null, "Cat", "PCS", TrackingType.LOT, false, true);

        BusinessException ex = assertThrows(BusinessException.class, () -> TrackingRules.check(item, null, null, null, BigDecimal.TEN));
        assertEquals("LOT_REQUIRED", ex.getCode());
    }

    @Test
    void check_LotTrackedItemExpiry_MissingExpiry_ThrowsException() {
        ItemResponse item = new ItemResponse(itemId, "SKU2", "Item 2", null, null, "Cat", "PCS", TrackingType.LOT, true, true);

        BusinessException ex = assertThrows(BusinessException.class, () -> TrackingRules.check(item, "LOT123", null, null, BigDecimal.TEN));
        assertEquals("EXPIRY_REQUIRED", ex.getCode());
    }

    @Test
    void check_SerialTrackedItem_Success() {
        ItemResponse item = new ItemResponse(itemId, "SKU3", "Item 3", null, null, "Cat", "PCS", TrackingType.SERIAL, false, true);

        assertDoesNotThrow(() -> TrackingRules.check(item, null, null, "SN001", BigDecimal.ONE));
    }

    @Test
    void check_SerialTrackedItem_MultipleQty_ThrowsException() {
        ItemResponse item = new ItemResponse(itemId, "SKU3", "Item 3", null, null, "Cat", "PCS", TrackingType.SERIAL, false, true);

        BusinessException ex = assertThrows(BusinessException.class, () -> TrackingRules.check(item, null, null, "SN001", BigDecimal.TEN));
        assertEquals("SERIAL_QTY_MUST_BE_ONE", ex.getCode());
    }
}
