package rw.afriteck.wms.reporting.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import rw.afriteck.wms.reporting.support.SqlTimes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AccuracyReportRepository {

    private final JdbcClient jdbc;

    public record CountRow(UUID countId, String countNumber, UUID zoneLocationId, Instant completedAt,
                           UUID approvedBy, long lines, long matching, long over, long under,
                           BigDecimal net, BigDecimal abs) {}
    public record VarianceRow(UUID itemId, long lines, BigDecimal net, BigDecimal abs) {}
    public record ReasonRow(String code, String name, long adjustments, long lines,
                            BigDecimal increased, BigDecimal decreased) {}
    public record PendingRow(long count, Instant oldest) {}

    public List<CountRow> countRows(UUID warehouseId, Instant from, Instant to) {
        return jdbc.sql("""
                select c.id, c.count_number, c.zone_location_id, c.completed_at, c.approved_by,
                       count(l.id) as lines,
                       count(l.id) filter (where l.counted_qty = l.expected_qty) as matching,
                       count(l.id) filter (where l.counted_qty > l.expected_qty) as over_lines,
                       count(l.id) filter (where l.counted_qty < l.expected_qty) as under_lines,
                       coalesce(sum(l.counted_qty - l.expected_qty), 0) as net_variance,
                       coalesce(sum(abs(l.counted_qty - l.expected_qty)), 0) as abs_variance
                from stock_counts c
                join stock_count_lines l on l.count_id = c.id and l.status = 'COUNTED'
                where c.warehouse_id = :warehouseId and c.status = 'COMPLETED'
                  and c.completed_at >= :fromTs and c.completed_at < :toTs
                group by c.id
                order by c.completed_at desc, c.id
                """)
                .param("warehouseId", warehouseId)
                .param("fromTs", SqlTimes.of(from)).param("toTs", SqlTimes.of(to))
                .query((rs, n) -> new CountRow(rs.getObject("id", UUID.class), rs.getString("count_number"),
                        rs.getObject("zone_location_id", UUID.class),
                        rs.getObject("completed_at", OffsetDateTime.class).toInstant(),
                        rs.getObject("approved_by", UUID.class),
                        rs.getLong("lines"), rs.getLong("matching"), rs.getLong("over_lines"),
                        rs.getLong("under_lines"), rs.getBigDecimal("net_variance"),
                        rs.getBigDecimal("abs_variance")))
                .list();
    }

    public List<VarianceRow> topVarianceItems(UUID warehouseId, Instant from, Instant to, int limit) {
        return jdbc.sql("""
                select l.item_id, count(*) as lines,
                       sum(l.counted_qty - l.expected_qty) as net_variance,
                       sum(abs(l.counted_qty - l.expected_qty)) as abs_variance
                from stock_counts c
                join stock_count_lines l on l.count_id = c.id and l.status = 'COUNTED'
                where c.warehouse_id = :warehouseId and c.status = 'COMPLETED'
                  and c.completed_at >= :fromTs and c.completed_at < :toTs
                  and l.counted_qty <> l.expected_qty
                group by l.item_id
                order by abs_variance desc, l.item_id
                limit :limit
                """)
                .param("warehouseId", warehouseId)
                .param("fromTs", SqlTimes.of(from)).param("toTs", SqlTimes.of(to)).param("limit", limit)
                .query((rs, n) -> new VarianceRow(rs.getObject("item_id", UUID.class), rs.getLong("lines"),
                        rs.getBigDecimal("net_variance"), rs.getBigDecimal("abs_variance")))
                .list();
    }

    /** POSTED adjustments; posting time = decided_at, or created_at when no approval was needed. */
    public List<ReasonRow> adjustmentsByReason(UUID warehouseId, Instant from, Instant to) {
        return jdbc.sql("""
                select r.code, r.name,
                       count(distinct a.id) as adjustments,
                       count(l.id) as lines,
                       coalesce(sum(l.quantity_delta) filter (where l.quantity_delta > 0), 0) as increased,
                       coalesce(-sum(l.quantity_delta) filter (where l.quantity_delta < 0), 0) as decreased
                from stock_adjustments a
                join adjustment_reasons r on r.id = a.reason_id
                join stock_adjustment_lines l on l.adjustment_id = a.id
                where a.warehouse_id = :warehouseId and a.status = 'POSTED'
                  and coalesce(a.decided_at, a.created_at) >= :fromTs
                  and coalesce(a.decided_at, a.created_at) < :toTs
                group by r.id
                order by adjustments desc, r.code
                """)
                .param("warehouseId", warehouseId)
                .param("fromTs", SqlTimes.of(from)).param("toTs", SqlTimes.of(to))
                .query((rs, n) -> new ReasonRow(rs.getString("code"), rs.getString("name"),
                        rs.getLong("adjustments"), rs.getLong("lines"),
                        rs.getBigDecimal("increased"), rs.getBigDecimal("decreased")))
                .list();
    }

    public PendingRow pendingAdjustments(UUID warehouseId) {
        return jdbc.sql("""
                select count(*) as n, min(created_at) as oldest
                from stock_adjustments
                where warehouse_id = :warehouseId and status = 'PENDING_APPROVAL'
                """)
                .param("warehouseId", warehouseId)
                .query((rs, n) -> {
                    OffsetDateTime oldest = rs.getObject("oldest", OffsetDateTime.class);
                    return new PendingRow(rs.getLong("n"), oldest == null ? null : oldest.toInstant());
                }).single();
    }
}
