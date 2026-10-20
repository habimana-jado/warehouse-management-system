package rw.afriteck.wms.web.users;

import rw.afriteck.wms.auth.enums.RoleName;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserRow(UUID id, String fullName, String email, String username, boolean active,
                      List<RoleName> roles, boolean allWarehouses, List<String> warehouses,
                      Instant lastLoginAt) {}
