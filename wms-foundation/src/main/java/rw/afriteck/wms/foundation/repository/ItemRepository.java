package rw.afriteck.wms.foundation.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("""
        select i.id from Item i
        where lower(i.sku) like :pattern or lower(i.name) like :pattern
           or lower(coalesce(i.barcode, '')) like :pattern
        order by i.active desc, i.sku""")
    List<UUID> findIdsByText(@Param("pattern") String pattern, Pageable pageable);
}
