package rw.afriteck.wms.outbound.dtos;

import java.math.BigDecimal;
import java.util.UUID;

/** Availability check at confirmation: a warning, never a lock or a block. STORAGE bins only. */
public record AvailabilityWarning(UUID itemId, String itemSku, String itemName,
                                  BigDecimal requiredQty, BigDecimal availableQty) {}