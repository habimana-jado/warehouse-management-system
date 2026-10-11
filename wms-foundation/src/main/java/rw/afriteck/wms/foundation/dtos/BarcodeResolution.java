package rw.afriteck.wms.foundation.dtos;

import java.math.BigDecimal;

public record BarcodeResolution(ItemResponse item, String uom, BigDecimal factorToBase) {}
