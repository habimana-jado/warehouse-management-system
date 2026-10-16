package rw.afriteck.wms.reporting.dtos;

import java.math.BigDecimal;
import java.util.UUID;

public record VarianceItemResponse(UUID itemId, String itemSku, String itemName, String baseUom,
                                   long lines, BigDecimal netVariance, BigDecimal absVariance) {}
