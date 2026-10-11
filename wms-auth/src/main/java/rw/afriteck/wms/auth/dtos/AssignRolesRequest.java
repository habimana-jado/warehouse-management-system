package rw.afriteck.wms.auth.dtos;

import jakarta.validation.constraints.NotEmpty;
import rw.afriteck.wms.auth.enums.RoleName;

import java.util.Set;

public record AssignRolesRequest(@NotEmpty Set<RoleName> roles) {}

