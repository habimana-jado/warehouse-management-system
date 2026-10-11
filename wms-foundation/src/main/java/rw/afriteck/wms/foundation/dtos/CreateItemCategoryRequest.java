package rw.afriteck.wms.foundation.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateItemCategoryRequest(@NotBlank @Size(max = 100) String name, String description) {}

