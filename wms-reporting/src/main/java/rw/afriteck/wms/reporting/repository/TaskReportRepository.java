package rw.afriteck.wms.reporting.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import rw.afriteck.wms.reporting.support.SqlTimes;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class TaskReportRepository {

    private final JdbcClient jdbc;

    public record BacklogRow(String type, long open, long assigned, long inProgress, long overdue) {}
    public record OverdueRow(UUID id, String taskNumber, String type, String status, String priority,
                             String referenceType, String referenceNumber, UUID assigneeId,
                             Instant dueAt, Instant createdAt) {}

    public List<BacklogRow> backlogByType(UUID warehouseId, Instant now) {
        return jdbc.sql("""
                select type,
                       count(*) filter (where status = 'OPEN') as open_tasks,
                       count(*) filter (where status = 'ASSIGNED') as assigned_tasks,
                       count(*) filter (where status = 'IN_PROGRESS') as in_progress_tasks,
                       count(*) filter (where due_at is not null and due_at < :now) as overdue_tasks
                from tasks
                where warehouse_id = :warehouseId and status in ('OPEN', 'ASSIGNED', 'IN_PROGRESS')
                group by type order by type
                """)
                .param("warehouseId", warehouseId).param("now", SqlTimes.of(now))
                .query((rs, n) -> new BacklogRow(rs.getString("type"), rs.getLong("open_tasks"),
                        rs.getLong("assigned_tasks"), rs.getLong("in_progress_tasks"), rs.getLong("overdue_tasks")))
                .list();
    }

    public List<OverdueRow> findOverdue(UUID warehouseId, Instant now, int limit, long offset) {
        return jdbc.sql("""
                select id, task_number, type, status, priority, reference_type, reference_number,
                       assignee_id, due_at, created_at
                from tasks
                where warehouse_id = :warehouseId and status in ('OPEN', 'ASSIGNED', 'IN_PROGRESS')
                  and due_at is not null and due_at < :now
                order by due_at, id
                limit :limit offset :offset
                """)
                .param("warehouseId", warehouseId).param("now", SqlTimes.of(now))
                .param("limit", limit).param("offset", offset)
                .query((rs, n) -> new OverdueRow(rs.getObject("id", UUID.class), rs.getString("task_number"),
                        rs.getString("type"), rs.getString("status"), rs.getString("priority"),
                        rs.getString("reference_type"), rs.getString("reference_number"),
                        rs.getObject("assignee_id", UUID.class),
                        rs.getObject("due_at", OffsetDateTime.class).toInstant(),
                        rs.getObject("created_at", OffsetDateTime.class).toInstant()))
                .list();
    }
}
