package rw.afriteck.wms.foundation.dtos;

import java.util.UUID;

public record ItemCategoryResponse(UUID id, String name, String description, boolean active) {}

