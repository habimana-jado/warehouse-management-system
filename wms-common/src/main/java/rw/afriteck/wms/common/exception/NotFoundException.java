package rw.afriteck.wms.common.exception;

public class NotFoundException extends BusinessException {
    public NotFoundException(String entity, Object id) {
        super("NOT_FOUND", entity + " not found: " + id);
    }
}
