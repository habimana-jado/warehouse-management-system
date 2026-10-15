package rw.afriteck.wms.inbound.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rw.afriteck.wms.inbound.entity.CustomerReturnLine;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CustomerReturnLineRepository extends JpaRepository<CustomerReturnLine, UUID> {

    interface ReturnCount {
        UUID getReturnId();
        Long getLineCount();
    }

    @Query("""
            select l.customerReturn.id as returnId, count(l) as lineCount
              from CustomerReturnLine l
             where l.customerReturn.id in :returnIds
             group by l.customerReturn.id
            """)
    List<ReturnCount> countByReturn(@Param("returnIds") Collection<UUID> returnIds);
}