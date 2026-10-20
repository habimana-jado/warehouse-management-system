package rw.afriteck.wms.web.foundation.locations;

import rw.afriteck.wms.foundation.dtos.LocationTreeResponse;
import rw.afriteck.wms.foundation.enums.LocationType;
import rw.afriteck.wms.foundation.enums.LocationUsage;

import java.util.UUID;

public record LocationRow(UUID id, String code, String name, LocationType type, LocationUsage usage,
                          String parentCode, String barcode, boolean active) {}

