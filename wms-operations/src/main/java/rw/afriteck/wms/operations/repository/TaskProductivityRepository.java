package rw.afriteck.wms.operations.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rw.afriteck.wms.operations.entity.Task;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TaskProductivityRepository extends JpaRepository<Task, UUID> {

    @Query(value = """
            SELECT t.assignee_id AS userId,
                   COUNT(*) AS tasksCompleted,
                   CAST(AVG(EXTRACT(EPOCH FROM (t.completed_at - t.started_at))) AS double precision)
                       AS avgTaskSeconds
            FROM task t
            WHERE t.warehouse_id = :warehouseId
              AND t.status = 'COMPLETED'
              AND t.assignee_id IS NOT NULL
              AND t.type IN (:types)
              AND t.completed_at >= :from AND t.completed_at < :to
            GROUP BY t.assignee_id
            """, nativeQuery = true)
    List<WorkerTaskStats> taskStatsByWorker(@Param("warehouseId") UUID warehouseId,
                                            @Param("types") Collection<String> types,
                                            @Param("from") Instant from,
                                            @Param("to") Instant to);

    @Query(value = """
            SELECT t.assignee_id AS userId,
                   COUNT(*) FILTER (WHERE l.status IN ('DONE', 'SHORT')) AS linesCompleted,
                   COUNT(*) FILTER (WHERE l.status = 'SHORT') AS shortLines,
                   COALESCE(SUM(l.done_qty), 0) AS totalQtyDone
            FROM task_line l
            JOIN task t ON t.id = l.task_id
            WHERE t.warehouse_id = :warehouseId
              AND t.status = 'COMPLETED'
              AND t.assignee_id IS NOT NULL
              AND t.type IN (:types)
              AND t.completed_at >= :from AND t.completed_at < :to
            GROUP BY t.assignee_id
            """, nativeQuery = true)
    List<WorkerLineStats> lineStatsByWorker(@Param("warehouseId") UUID warehouseId,
                                            @Param("types") Collection<String> types,
                                            @Param("from") Instant from,
                                            @Param("to") Instant to);
}

