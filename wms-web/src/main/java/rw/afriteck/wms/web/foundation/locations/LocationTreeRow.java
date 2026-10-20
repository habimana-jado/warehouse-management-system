package rw.afriteck.wms.web.foundation.locations;

import rw.afriteck.wms.foundation.dtos.LocationTreeResponse;

public record LocationTreeRow(int depth, LocationTreeResponse node) {}
