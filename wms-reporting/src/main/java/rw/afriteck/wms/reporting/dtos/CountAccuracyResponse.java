package rw.afriteck.wms.reporting.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CountAccuracyResponse(UUID warehouseId, LocalDate from, LocalDate toExclusive,
                                    long counts, long lines, long matchingLines, BigDecimal lineAccuracy,
                                    BigDecimal netVariance, BigDecimal absVariance,
                                    List<CountAccuracyRowResponse> countList, List<VarianceItemResponse> topVarianceItems) {}
