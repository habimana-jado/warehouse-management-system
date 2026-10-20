package rw.afriteck.wms.web.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import rw.afriteck.wms.auth.dtos.UpdateUserRequest;
import rw.afriteck.wms.auth.dtos.UserResponse;
import rw.afriteck.wms.common.support.Codes;

@Getter @Setter
public class UserProfileForm {
    @NotBlank @Size(max = 120) private String fullName;
    @Email private String email;

    public static UserProfileForm from(UserResponse u) {
        UserProfileForm f = new UserProfileForm();
        f.fullName = u.fullName();
        f.email = u.email();
        return f;
    }
    public UpdateUserRequest toRequest() { return new UpdateUserRequest(fullName, Codes.blankToNull(email)); }
}
