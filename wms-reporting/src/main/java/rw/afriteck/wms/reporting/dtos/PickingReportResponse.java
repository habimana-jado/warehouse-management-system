package rw.afriteck.wms.reporting.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PickingReportResponse(UUID warehouseId, LocalDate from, LocalDate toExclusive,
                                    long completedPickLists,
                                    Long avgCycleSeconds,// release to completion (includes waiting time)
                                    long lines, long shortLines, BigDecimal shortLineRate,
                                    BigDecimal plannedQty, BigDecimal pickedQty, BigDecimal shortQty) {}
