package rw.afriteck.wms.reporting.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record OpenDemandRowResponse(UUID itemId, String itemSku, String itemName, String baseUom,
                                    long openOrderLines, BigDecimal toPickQty,// ordered - picked on CONFIRMED / PARTIALLY_SHIPPED orders
                                    BigDecimal availableInStorage, BigDecimal shortfallQty, LocalDate earliestRequiredDate) {}
