package rw.afriteck.wms.web.users;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import rw.afriteck.wms.auth.dtos.CreateUserRequest;
import rw.afriteck.wms.auth.enums.RoleName;
import rw.afriteck.wms.common.support.Codes;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
public class UserCreateForm {
    @NotBlank @Size(min = 3, max = 60) @Pattern(regexp = "[A-Za-z0-9._-]+")
    private String username;
    @NotBlank @Size(max = 120)
    private String fullName;
    @Email
    private String email;
    @NotBlank
    private String password;
    @NotEmpty
    private Set<RoleName> roles = new LinkedHashSet<>(Set.of(RoleName.VIEWER));   // least privilege preselected
    private Set<UUID> warehouseIds = new LinkedHashSet<>();

    public CreateUserRequest toRequest() {
        return new CreateUserRequest(username, fullName, Codes.blankToNull(email), password,
                Set.copyOf(roles), warehouseIds);
    }
}
