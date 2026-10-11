package rw.afriteck.wms.foundation.dtos;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemUomResponse(UUID id, String uom, BigDecimal factorToBase, String barcode) {}

