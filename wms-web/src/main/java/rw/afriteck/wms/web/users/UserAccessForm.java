package rw.afriteck.wms.web.users;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import rw.afriteck.wms.auth.dtos.UserResponse;
import rw.afriteck.wms.auth.enums.RoleName;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
public class UserAccessForm {
    @NotEmpty
    private Set<RoleName> roles = new LinkedHashSet<>();
    private Set<UUID> warehouseIds = new LinkedHashSet<>();

    public static UserAccessForm from(UserResponse u) {
        UserAccessForm f = new UserAccessForm();
        f.roles = new LinkedHashSet<>(u.roles());
        f.warehouseIds = new LinkedHashSet<>(u.warehouseIds());
        return f;
    }
}
