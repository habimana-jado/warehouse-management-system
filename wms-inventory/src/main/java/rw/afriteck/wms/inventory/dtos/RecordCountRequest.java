package rw.afriteck.wms.inventory.dtos;

import java.math.BigDecimal;

public record RecordCountRequest(BigDecimal countedQty) {}                                  // >= 0, base UOM

