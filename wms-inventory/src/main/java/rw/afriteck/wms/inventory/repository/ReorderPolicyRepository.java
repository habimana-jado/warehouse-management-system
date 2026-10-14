package rw.afriteck.wms.inventory.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rw.afriteck.wms.inventory.entity.ReorderPolicy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReorderPolicyRepository extends JpaRepository<ReorderPolicy, UUID> {

    Optional<ReorderPolicy> findByWarehouseIdAndItemId(UUID warehouseId, UUID itemId);

    boolean existsByWarehouseIdAndItemId(UUID warehouseId, UUID itemId);

    Page<ReorderPolicy> findByWarehouseIdAndActive(UUID warehouseId, boolean active, Pageable pageable);

    Page<ReorderPolicy> findByWarehouseId(UUID warehouseId, Pageable pageable);

    List<ReorderPolicy> findByWarehouseIdAndActiveTrue(UUID warehouseId);
}
