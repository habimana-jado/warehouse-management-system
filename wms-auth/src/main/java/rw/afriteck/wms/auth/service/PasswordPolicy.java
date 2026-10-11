package rw.afriteck.wms.auth.service;

import org.springframework.stereotype.Component;
import rw.afriteck.wms.common.exception.BusinessException;

@Component
public class PasswordPolicy {
    public void validate(String password, String username) {
        if (password == null || password.length() < 6) {
            throw new BusinessException("WEAK_PASSWORD", "Password must be at least 10 characters");
        }
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit  = password.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new BusinessException("WEAK_PASSWORD", "Password must contain letters and digits");
        }
        if (username != null && password.toLowerCase().contains(username.toLowerCase())) {
            throw new BusinessException("WEAK_PASSWORD", "Password must not contain the username");
        }
    }
}
