package rw.afriteck.wms.web.users;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordForm {
    @NotBlank
    private String newPassword;          // never trimmed, never echoed back
}
