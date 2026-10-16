package rw.afriteck.wms.reporting.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import rw.afriteck.wms.reporting.enums.AuditSource;
import rw.afriteck.wms.reporting.support.SqlTimes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AuditReportRepository {

    private final JdbcClient jdbc;

    public record AuditRow(Instant occurredAt, String source, String event, UUID actorId, UUID itemId,
                           UUID locationId, BigDecimal quantity, String referenceType, UUID referenceId,
                           String referenceNumber, String detail) {}

    // Every branch returns the same 12 columns; the fragments are fixed text (no user input is concatenated).
    private static final String MOVEMENTS = """
            select m.occurred_at, 'STOCK_MOVEMENT' as source, m.type as event, m.created_by as actor_id,
                   m.item_id, m.location_id, m.quantity,
                   m.reference_type, m.reference_id, m.reference_number, m.note as detail, m.id as row_id
            from stock_movements m
            where m.warehouse_id = :warehouseId and m.occurred_at >= :fromTs and m.occurred_at < :toTs
            """;

    private static final String ADJ_REQUESTED = """
            select a.created_at, 'ADJUSTMENT', 'ADJUSTMENT_REQUESTED', a.requested_by,
                   cast(null as uuid), cast(null as uuid), cast(null as numeric),
                   'STOCK_ADJUSTMENT', a.id, a.adjustment_number, a.notes, a.id
            from stock_adjustments a
            where a.warehouse_id = :warehouseId and a.created_at >= :fromTs and a.created_at < :toTs
            """;

    private static final String ADJ_DECIDED = """
            select a.decided_at, 'ADJUSTMENT',
                   case a.status when 'REJECTED' then 'ADJUSTMENT_REJECTED' else 'ADJUSTMENT_APPROVED' end,
                   a.decided_by, cast(null as uuid), cast(null as uuid), cast(null as numeric),
                   'STOCK_ADJUSTMENT', a.id, a.adjustment_number, a.rejection_reason, a.id
            from stock_adjustments a
            where a.warehouse_id = :warehouseId and a.decided_at is not null
              and a.decided_at >= :fromTs and a.decided_at < :toTs
            """;

    private static final String ADJ_CANCELLED = """
            select a.updated_at, 'ADJUSTMENT', 'ADJUSTMENT_CANCELLED', a.updated_by,
                   cast(null as uuid), cast(null as uuid), cast(null as numeric),
                   'STOCK_ADJUSTMENT', a.id, a.adjustment_number, a.notes, a.id
            from stock_adjustments a
            where a.warehouse_id = :warehouseId and a.status = 'CANCELLED'
              and a.updated_at >= :fromTs and a.updated_at < :toTs
            """;

    private static final String TASK_EVENTS = """
            select e.occurred_at, 'TASK', 'TASK_' || e.event_type, e.actor_user_id,
                   cast(null as uuid), cast(null as uuid), cast(null as numeric),
                   'TASK', t.id, t.task_number, e.note, e.id
            from task_events e
            join tasks t on t.id = e.task_id
            where t.warehouse_id = :warehouseId and e.occurred_at >= :fromTs and e.occurred_at < :toTs
            """;

    private static final String COUNTS = """
            select c.completed_at, 'COUNT',
                   case c.status when 'CANCELLED' then 'COUNT_CANCELLED' else 'COUNT_APPROVED' end,
                   c.approved_by, cast(null as uuid), cast(null as uuid), cast(null as numeric),
                   'STOCK_COUNT', c.id, c.count_number, c.cancel_reason, c.id
            from stock_counts c
            where c.warehouse_id = :warehouseId and c.status in ('COMPLETED', 'CANCELLED')
              and c.completed_at >= :fromTs and c.completed_at < :toTs
            """;

    public List<AuditRow> search(UUID warehouseId, Instant from, Instant to, Set<AuditSource> sources,
                                 UUID actorId, int limit, long offset) {
        List<String> parts = new ArrayList<>();
        if (sources.contains(AuditSource.STOCK_MOVEMENT)) parts.add(MOVEMENTS);
        if (sources.contains(AuditSource.ADJUSTMENT)) { parts.add(ADJ_REQUESTED); parts.add(ADJ_DECIDED); parts.add(ADJ_CANCELLED); }
        if (sources.contains(AuditSource.TASK)) parts.add(TASK_EVENTS);
        if (sources.contains(AuditSource.COUNT)) parts.add(COUNTS);
        if (parts.isEmpty()) return List.of();

        String sql = "select * from (" + String.join(" union all ", parts) + ") u "
                + (actorId != null ? "where u.actor_id = :actorId " : "")
                + "order by u.occurred_at desc, u.source, u.event, u.row_id limit :limit offset :offset";

        JdbcClient.StatementSpec spec = jdbc.sql(sql)
                .param("warehouseId", warehouseId)
                .param("fromTs", SqlTimes.of(from)).param("toTs", SqlTimes.of(to))
                .param("limit", limit).param("offset", offset);
        if (actorId != null) spec = spec.param("actorId", actorId);

        return spec.query((rs, n) -> new AuditRow(
                        rs.getObject("occurred_at", OffsetDateTime.class).toInstant(),
                        rs.getString("source"), rs.getString("event"), rs.getObject("actor_id", UUID.class),
                        rs.getObject("item_id", UUID.class), rs.getObject("location_id", UUID.class),
                        rs.getBigDecimal("quantity"), rs.getString("reference_type"),
                        rs.getObject("reference_id", UUID.class), rs.getString("reference_number"),
                        rs.getString("detail")))
                .list();
    }
}
