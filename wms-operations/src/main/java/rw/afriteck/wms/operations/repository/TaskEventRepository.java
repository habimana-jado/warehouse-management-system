package rw.afriteck.wms.operations.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rw.afriteck.wms.operations.entity.TaskEvent;

import java.util.List;
import java.util.UUID;

public interface TaskEventRepository extends JpaRepository<TaskEvent, UUID> {
    List<TaskEvent> findByTaskIdOrderByOccurredAtAscCreatedAtAsc(UUID taskId);
}
