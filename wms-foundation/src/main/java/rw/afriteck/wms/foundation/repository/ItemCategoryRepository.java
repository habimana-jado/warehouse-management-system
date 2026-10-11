package rw.afriteck.wms.foundation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rw.afriteck.wms.foundation.entity.ItemCategory;

import java.util.List;
import java.util.UUID;

public interface ItemCategoryRepository extends JpaRepository<ItemCategory, UUID> {
    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    List<ItemCategory> findByActiveTrueOrderByName();
}
