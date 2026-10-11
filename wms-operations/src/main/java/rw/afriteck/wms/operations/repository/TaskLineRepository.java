package rw.afriteck.wms.operations.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import rw.afriteck.wms.operations.entity.TaskLine;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TaskLineRepository extends JpaRepository<TaskLine, UUID> {
    @Query("""
    select l.task.id as taskId, count(l) as total,
           sum(case when l.status <> 'PENDING' then 1 else 0 end) as done
    from TaskLine l where l.task.id in :taskIds group by l.task.id
""")
    List<TaskLine> countByTaskIds(Collection<UUID> taskIds);
}
