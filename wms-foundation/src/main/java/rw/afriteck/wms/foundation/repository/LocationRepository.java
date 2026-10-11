package rw.afriteck.wms.foundation.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rw.afriteck.wms.foundation.entity.Location;
import rw.afriteck.wms.foundation.enums.LocationType;
import rw.afriteck.wms.foundation.enums.LocationUsage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LocationRepository extends JpaRepository<Location, UUID>, JpaSpecificationExecutor<Location> {

    boolean existsByWarehouseIdAndCode(UUID warehouseId, String code);

    boolean existsByBarcode(String barcode);

    boolean existsByParentIdAndActiveTrue(UUID parentId);

    boolean existsByBarcodeAndIdNot(String barcode, UUID id);

    Optional<Location> findByWarehouseIdAndBarcode(UUID warehouseId, String barcode);

    List<Location> findByWarehouseIdOrderByCode(UUID warehouseId);

    List<Location> findByWarehouseIdAndTypeAndUsageAndActiveTrueOrderByCode(
            UUID warehouseId, LocationType type, LocationUsage usage);

    @Query("""
            select l from Location l
            where l.warehouse.id = :warehouseId
              and (lower(l.code) like :pattern
                or lower(l.name) like :pattern
                or lower(l.barcode) like :pattern)
            """)
    Page<Location> search(@Param("warehouseId") UUID warehouseId,
                          @Param("pattern") String pattern, Pageable pageable);
}
