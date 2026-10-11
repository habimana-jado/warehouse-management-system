package rw.afriteck.wms.foundation.mapper;

import rw.afriteck.wms.foundation.dtos.WarehouseResponse;
import rw.afriteck.wms.foundation.entity.Warehouse;

public final class WarehouseMapper {
    private WarehouseMapper() {}

    public static WarehouseResponse toResponse(Warehouse w) {
        return new WarehouseResponse(w.getId(), w.getCode(), w.getName(),
                w.getAddressLine(), w.getCity(), w.getCountry(), w.isActive());
    }
}
