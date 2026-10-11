package rw.afriteck.wms.foundation.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import rw.afriteck.wms.foundation.entity.ItemUom;

import java.util.Optional;
import java.util.UUID;

public interface ItemUomRepository extends JpaRepository<ItemUom, UUID> {
    boolean existsByBarcode(String barcode);

    boolean existsByBarcodeAndItemIdNot(String barcode, UUID itemId);

    @EntityGraph(attributePaths = {"item", "item.category"})
    Optional<ItemUom> findByBarcode(String barcode);
}
