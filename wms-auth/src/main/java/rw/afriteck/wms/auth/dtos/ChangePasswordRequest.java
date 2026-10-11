package rw.afriteck.wms.auth.dtos;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(@NotBlank String currentPassword,
                                    @NotBlank String newPassword) {}

