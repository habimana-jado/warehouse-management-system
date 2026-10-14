package rw.afriteck.wms.inventory.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.Repository;
import rw.afriteck.wms.inventory.entity.StockMovement;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Extends Repository (not JpaRepository) on purpose: the ledger is append-only, so no delete methods exist.
 */
public interface StockMovementRepository extends Repository<StockMovement, UUID> {

    <S extends StockMovement> S save(S movement);

    <S extends StockMovement> List<S> saveAll(Iterable<S> movements);

    Optional<StockMovement> findById(UUID id);

    List<StockMovement> findByTransactionId(UUID transactionId);

    List<StockMovement> findByReferenceTypeAndReferenceId(String referenceType, UUID referenceId);

    Page<StockMovement> findByWarehouseIdAndItemId(UUID warehouseId, UUID itemId, Pageable pageable);

    Page<StockMovement> findAll(Specification<StockMovement> spec, Pageable pageable);
}
