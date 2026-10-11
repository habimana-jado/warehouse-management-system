package rw.afriteck.wms.auth.dtos;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(@NotBlank String newPassword) {}

