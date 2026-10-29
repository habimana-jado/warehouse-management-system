package rw.afriteck.wms.web.reporting;

import rw.afriteck.wms.common.dtos.PageResponse;

import java.util.List;

final class ReportPages {
    private ReportPages() {}
    static <T> PageResponse<T> empty(int size) { return new PageResponse<>(List.of(), 0, size, 0, 0); }
}
