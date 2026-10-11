package rw.afriteck.wms.common.exception;

public class ForbiddenException extends BusinessException {
    public ForbiddenException(String message) { super("FORBIDDEN", message); }
}
