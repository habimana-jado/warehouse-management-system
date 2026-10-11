package rw.afriteck.wms.auth.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import rw.afriteck.wms.common.dtos.ErrorResponse;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Clock;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SecurityErrorWriter {
    private final ObjectMapper mapper;
    private final Clock clock;

    void write(HttpServletResponse res, HttpStatus status, String code, String message) throws IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(res.getOutputStream(),
                new ErrorResponse(clock.instant(), status.value(), code, message, List.of()));
    }
}
