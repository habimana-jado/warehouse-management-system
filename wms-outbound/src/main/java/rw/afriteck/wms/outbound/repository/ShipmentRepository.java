package rw.afriteck.wms.outbound.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rw.afriteck.wms.outbound.entity.Shipment;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {

    List<Shipment> findBySalesOrderIdOrderByShippedAtDesc(UUID salesOrderId);

    @Query(value = "select s from Shipment s join fetch s.salesOrder o "
            + "where s.warehouseId = :wh and (lower(s.shipmentNumber) like :pattern or lower(o.orderNumber) like :pattern)",
            countQuery = "select count(s) from Shipment s join s.salesOrder o "
                    + "where s.warehouseId = :wh and (lower(s.shipmentNumber) like :pattern or lower(o.orderNumber) like :pattern)")
    Page<Shipment> search(@Param("wh") UUID warehouseId, @Param("pattern") String pattern, Pageable pageable);

    @Query("select l.shipment.id as parentId, count(l) as total from ShipmentLine l "
            + "where l.shipment.id in :ids group by l.shipment.id")
    List<ParentCount> countLines(@Param("ids") Collection<UUID> ids);
}
