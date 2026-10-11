package rw.afriteck.wms.foundation.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import rw.afriteck.wms.foundation.entity.Warehouse;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface WarehouseRepository extends JpaRepository<Warehouse, UUID> {

    boolean existsByCodeIgnoreCase(String code);

    long countByIdIn(Collection<UUID> ids);

    List<Warehouse> findByActiveTrueOrderByName();

    List<Warehouse> findByIdInAndActiveTrueOrderByName(Collection<UUID> ids);

    Page<Warehouse> findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(String code, String name, Pageable p);
}
