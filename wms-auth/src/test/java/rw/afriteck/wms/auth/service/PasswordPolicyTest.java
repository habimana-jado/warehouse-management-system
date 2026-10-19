package rw.afriteck.wms.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rw.afriteck.wms.common.exception.BusinessException;

import static org.junit.jupiter.api.Assertions.*;

class PasswordPolicyTest {

    private PasswordPolicy passwordPolicy;

    @BeforeEach
    void setUp() {
        passwordPolicy = new PasswordPolicy();
    }

    @Test
    void validate_ValidPassword_ShouldNotThrowException() {
        assertDoesNotThrow(() -> passwordPolicy.validate("P@ssword123", "john_doe"));
    }

    @Test
    void validate_NullPassword_ShouldThrowBusinessException() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> passwordPolicy.validate(null, "john_doe"));
        assertEquals("WEAK_PASSWORD", ex.getCode());
    }

    @Test
    void validate_ShortPassword_ShouldThrowBusinessException() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> passwordPolicy.validate("Pass1", "john_doe"));
        assertEquals("WEAK_PASSWORD", ex.getCode());
    }

    @Test
    void validate_PasswordWithoutDigits_ShouldThrowBusinessException() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> passwordPolicy.validate("PasswordOnly", "john_doe"));
        assertEquals("WEAK_PASSWORD", ex.getCode());
    }

    @Test
    void validate_PasswordWithoutLetters_ShouldThrowBusinessException() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> passwordPolicy.validate("12345678", "john_doe"));
        assertEquals("WEAK_PASSWORD", ex.getCode());
    }

    @Test
    void validate_PasswordContainingUsername_ShouldThrowBusinessException() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> passwordPolicy.validate("john_doe1234", "john_doe"));
        assertEquals("WEAK_PASSWORD", ex.getCode());
    }

    @Test
    void validate_NullUsername_ShouldSucceedWhenPasswordIsValid() {
        assertDoesNotThrow(() -> passwordPolicy.validate("P@ssword123", null));
    }
}
