package rw.afriteck.wms.reporting.dtos;

import rw.afriteck.wms.foundation.enums.LocationUsage;
import rw.afriteck.wms.reporting.enums.ExpiryStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ExpiryRowResponse(UUID itemId, String itemSku, String itemName, String baseUom,
                                UUID lotId, String lotNumber, LocalDate expiryDate, long daysToExpiry, ExpiryStatus status,
                                UUID locationId, String locationCode, LocationUsage locationUsage, BigDecimal quantity) {}
