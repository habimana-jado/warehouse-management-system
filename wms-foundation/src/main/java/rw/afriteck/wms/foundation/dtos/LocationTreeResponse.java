package rw.afriteck.wms.foundation.dtos;

import rw.afriteck.wms.foundation.enums.LocationType;
import rw.afriteck.wms.foundation.enums.LocationUsage;

import java.util.List;
import java.util.UUID;

public record LocationTreeResponse(
        UUID id, String code, String name,
        LocationType type,
        LocationUsage usage, boolean active,
        List<LocationTreeResponse> children) {
}
