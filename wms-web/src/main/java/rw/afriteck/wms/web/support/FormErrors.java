package rw.afriteck.wms.web.support;

import org.springframework.validation.BindingResult;
import rw.afriteck.wms.common.exception.BusinessException;

import java.util.Map;

public final class FormErrors {
    private FormErrors() {}
    public static void apply(BindingResult result, BusinessException e, Map<String, String> codeToField) {
        String field = codeToField.get(e.getCode());
        if (field != null) result.rejectValue(field, e.getCode(), e.getMessage());
        else               result.reject(e.getCode(), e.getMessage());
    }
}
