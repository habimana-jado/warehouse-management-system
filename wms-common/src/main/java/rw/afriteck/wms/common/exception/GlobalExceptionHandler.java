package rw.afriteck.wms.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import rw.afriteck.wms.common.dtos.ErrorResponse;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ErrorResponse> notFound(NotFoundException e) {
        return build(HttpStatus.NOT_FOUND, e);
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ErrorResponse> conflict(ConflictException e) {
        return build(HttpStatus.CONFLICT, e);
    }

    @ExceptionHandler(ForbiddenException.class)
    ResponseEntity<ErrorResponse> forbidden(ForbiddenException e) {
        return build(HttpStatus.FORBIDDEN, e);
    }

    @ExceptionHandler(UnauthorizedException.class)
    ResponseEntity<ErrorResponse> unauthorized(UnauthorizedException e) {
        return build(HttpStatus.UNAUTHORIZED, e);
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ErrorResponse> business(BusinessException e) {
        return build(HttpStatus.UNPROCESSABLE_CONTENT, e);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ErrorResponse> locking(ObjectOptimisticLockingFailureException e) {
        return build(HttpStatus.CONFLICT, new ConflictException("CONCURRENT_UPDATE",
                "The record was changed by someone else. Reload and retry."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException e) {
        var violations = e.getBindingResult().getFieldErrors().stream()
                .map(f -> new ErrorResponse.FieldViolation(f.getField(), f.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest().body(new ErrorResponse(
                Instant.now(), 400, "VALIDATION_FAILED", "Invalid request", violations));
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, BusinessException e) {
        return ResponseEntity.status(status).body(new ErrorResponse(
                Instant.now(), status.value(), e.getCode(), e.getMessage(), List.of()));
    }
}