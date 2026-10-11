package rw.afriteck.wms.common.dtos;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        List<FieldViolation> violations) {

    public record FieldViolation(String field, String message) {}
}

