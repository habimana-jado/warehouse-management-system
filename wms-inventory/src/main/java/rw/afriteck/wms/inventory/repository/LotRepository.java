package rw.afriteck.wms.inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rw.afriteck.wms.inventory.entity.Lot;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LotRepository extends JpaRepository<Lot, UUID> {
    Optional<Lot> findByItemIdAndLotNumber(UUID itemId, String lotNumber);

    List<Lot> findByItemIdAndLotNumberIn(UUID itemId, Collection<String> lotNumbers);   // batch for multi-line posts
}
