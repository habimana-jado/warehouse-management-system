package rw.afriteck.wms.reporting.dtos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ValuationRowResponse(UUID itemId, String itemSku, String itemName, String baseUom,
                                   BigDecimal onHand, BigDecimal lastUnitCost, Instant lastCostAt, BigDecimal value) {}   // cost and value null = unknown
