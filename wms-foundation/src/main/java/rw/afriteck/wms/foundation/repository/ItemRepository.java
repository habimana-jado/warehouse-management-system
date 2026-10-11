package rw.afriteck.wms.foundation.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import rw.afriteck.wms.foundation.entity.Item;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItemRepository extends JpaRepository<Item, UUID>, JpaSpecificationExecutor<Item> {

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsByBarcode(String barcode);

    boolean existsByBarcodeAndIdNot(String barcode, UUID id);

    Optional<Item> findByBarcode(String barcode);

    @EntityGraph(attributePaths = {"uoms", "category"})
    Optional<Item> findWithUomsById(UUID id);

    @EntityGraph(attributePaths = "category")
    List<Item> findByIdIn(Collection<UUID> ids);
}
