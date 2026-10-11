package rw.afriteck.wms.auth.dtos;

import jakarta.validation.constraints.*;
import rw.afriteck.wms.auth.enums.RoleName;

import java.util.Set;
import java.util.UUID;

public record CreateUserRequest(
        @NotBlank @Size(min = 3, max = 60) @Pattern(regexp = "[A-Za-z0-9._-]+") String username,
        @NotBlank @Size(max = 120) String fullName,
        @Email String email,
        @NotBlank String password,
        @NotEmpty Set<RoleName> roles,
        Set<UUID> warehouseIds) {}
