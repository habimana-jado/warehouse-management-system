package rw.afriteck.wms.operations.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rw.afriteck.wms.operations.dtos.TaskAccessInfo;
import rw.afriteck.wms.operations.entity.Task;
import rw.afriteck.wms.operations.enums.TaskStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {

    // ---- task number ----
    @Query(value = "SELECT nextval('task_number_seq')", nativeQuery = true)
    long nextTaskSequence();

    // ---- single task with lines (getById, completeLine, assign, ...) ----
    @EntityGraph(attributePaths = "lines")
    Optional<Task> findWithLinesById(UUID id);

    // ---- by reference ----
    @Query("""
        select distinct t from Task t left join fetch t.lines
        where t.referenceType = :referenceType and t.referenceId = :referenceId
        order by t.createdAt
    """)
    List<Task> findWithLinesByReference(@Param("referenceType") String referenceType,
                                        @Param("referenceId") UUID referenceId);

    // cancelByReference: lines not needed
    List<Task> findByReferenceTypeAndReferenceIdAndStatusIn(String referenceType,
                                                            UUID referenceId,
                                                            Collection<TaskStatus> statuses);

    // ---- claim next (scanner flow) ----
    @Query(value = """
        SELECT * FROM task
        WHERE warehouse_id = :warehouseId
          AND status = 'OPEN'
          AND type IN (:types)
        ORDER BY CASE priority WHEN 'URGENT' THEN 4 WHEN 'HIGH' THEN 3 WHEN 'NORMAL' THEN 2 ELSE 1 END DESC,
                 due_at ASC NULLS LAST,
                 created_at ASC
        LIMIT 1
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    Optional<Task> lockNextOpen(@Param("warehouseId") UUID warehouseId,
                                @Param("types") Collection<String> types);

    // ---- board line counts: one grouped query for the whole page ----
    @Query(value = """
            SELECT l.task_id AS taskId,
                   COUNT(*) AS total,
                   COUNT(*) FILTER (WHERE l.status <> 'PENDING') AS done
            FROM task_line l
            WHERE l.task_id IN (:taskIds)
            GROUP BY l.task_id
            """, nativeQuery = true)
    List<TaskLineCount> countLinesByTaskIds(@Param("taskIds") Collection<UUID> taskIds);

    // ---- deactivation guard (WAREHOUSE) ----
    boolean existsByWarehouseIdAndStatusIn(UUID warehouseId, Collection<TaskStatus> statuses);

    @Query("select new rw.afriteck.wms.operations.dtos.TaskAccessInfo(t.warehouseId, t.assigneeId) "
            + "from Task t where t.id = :id")
    Optional<TaskAccessInfo> findAccessInfoById(@Param("id") UUID id);
}
