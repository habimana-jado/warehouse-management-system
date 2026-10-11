package rw.afriteck.wms.operations.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelTaskRequest(@NotBlank @Size(max = 255) String reason) {}
