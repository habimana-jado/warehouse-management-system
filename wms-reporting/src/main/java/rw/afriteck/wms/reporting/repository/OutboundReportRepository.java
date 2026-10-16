package rw.afriteck.wms.reporting.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import rw.afriteck.wms.reporting.support.SqlTimes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OutboundReportRepository {

    private final JdbcClient jdbc;

    public record LineAgg(long lines, long fullyShipped, BigDecimal orderedQty, BigDecimal shippedQty) {}
    public record OnTimeAgg(long dated, long onTime) {}
    public record PickLineAgg(long lines, long shortLines, BigDecimal plannedQty, BigDecimal pickedQty,
                              BigDecimal shortQty) {}
    public record PickListAgg(long lists, Long avgCycleSeconds) {}
    public record DayAgg(LocalDate day, long shipments, long packages, long delivered) {}
    public record DemandRow(UUID itemId, long orderLines, BigDecimal toPickQty, LocalDate earliestRequired) {}

    public Map<String, Long> ordersByStatus(UUID warehouseId, LocalDate fromDate, LocalDate toDate) {
        Map<String, Long> result = new LinkedHashMap<>();
        jdbc.sql("""
                select status, count(*) as n from sales_orders
                where warehouse_id = :warehouseId and order_date >= :fromDate and order_date < :toDate
                group by status order by status
                """)
                .param("warehouseId", warehouseId).param("fromDate", fromDate).param("toDate", toDate)
                .query((rs, n) -> Map.entry(rs.getString("status"), rs.getLong("n")))
                .list().forEach(e -> result.put(e.getKey(), e.getValue()));
        return result;
    }

    /** Lines of finished orders (SHIPPED, CLOSED) in the cohort. */
    public LineAgg finishedLines(UUID warehouseId, LocalDate fromDate, LocalDate toDate) {
        return jdbc.sql("""
                select count(l.id) as lines,
                       count(l.id) filter (where l.shipped_qty >= l.ordered_qty) as fully_shipped,
                       coalesce(sum(l.ordered_qty), 0) as ordered_qty,
                       coalesce(sum(l.shipped_qty), 0) as shipped_qty
                from sales_orders o join sales_order_lines l on l.sales_order_id = o.id
                where o.warehouse_id = :warehouseId and o.order_date >= :fromDate and o.order_date < :toDate
                  and o.status in ('SHIPPED', 'CLOSED')
                """)
                .param("warehouseId", warehouseId).param("fromDate", fromDate).param("toDate", toDate)
                .query((rs, n) -> new LineAgg(rs.getLong("lines"), rs.getLong("fully_shipped"),
                        rs.getBigDecimal("ordered_qty"), rs.getBigDecimal("shipped_qty")))
                .single();
    }

    public OnTimeAgg onTime(UUID warehouseId, LocalDate fromDate, LocalDate toDate, String zone) {
        return jdbc.sql("""
                select count(*) as dated,
                       count(*) filter (where cast(o.closed_at at time zone :zone as date) <= o.required_date) as on_time
                from sales_orders o
                where o.warehouse_id = :warehouseId and o.order_date >= :fromDate and o.order_date < :toDate
                  and o.status = 'SHIPPED' and o.required_date is not null and o.closed_at is not null
                """)
                .param("warehouseId", warehouseId).param("fromDate", fromDate).param("toDate", toDate)
                .param("zone", zone)
                .query((rs, n) -> new OnTimeAgg(rs.getLong("dated"), rs.getLong("on_time")))
                .single();
    }

    public long overdueOpenOrders(UUID warehouseId, LocalDate today) {
        return jdbc.sql("""
                select count(*) from sales_orders
                where warehouse_id = :warehouseId and status in ('CONFIRMED', 'PARTIALLY_SHIPPED')
                  and required_date is not null and required_date < :today
                """)
                .param("warehouseId", warehouseId).param("today", today)
                .query(Long.class).single();
    }

    /** Completed (PICKED) lists only: cancelled lists turn PENDING lines into SHORT, which is not a real short pick. */
    public PickLineAgg pickLines(UUID warehouseId, Instant from, Instant to) {
        return jdbc.sql("""
                select count(pl.id) as lines,
                       count(pl.id) filter (where pl.status = 'SHORT') as short_lines,
                       coalesce(sum(pl.planned_qty), 0) as planned_qty,
                       coalesce(sum(pl.picked_qty), 0) as picked_qty,
                       coalesce(sum(pl.planned_qty - pl.picked_qty) filter (where pl.status = 'SHORT'), 0) as short_qty
                from pick_lists p join pick_lines pl on pl.pick_list_id = p.id
                where p.warehouse_id = :warehouseId and p.status = 'PICKED'
                  and p.completed_at >= :fromTs and p.completed_at < :toTs
                """)
                .param("warehouseId", warehouseId)
                .param("fromTs", SqlTimes.of(from)).param("toTs", SqlTimes.of(to))
                .query((rs, n) -> new PickLineAgg(rs.getLong("lines"), rs.getLong("short_lines"),
                        rs.getBigDecimal("planned_qty"), rs.getBigDecimal("picked_qty"),
                        rs.getBigDecimal("short_qty")))
                .single();
    }

    public PickListAgg pickLists(UUID warehouseId, Instant from, Instant to) {
        return jdbc.sql("""
                select count(*) as lists,
                       avg(extract(epoch from (completed_at - released_at))) as avg_seconds
                from pick_lists
                where warehouse_id = :warehouseId and status = 'PICKED'
                  and completed_at >= :fromTs and completed_at < :toTs
                """)
                .param("warehouseId", warehouseId)
                .param("fromTs", SqlTimes.of(from)).param("toTs", SqlTimes.of(to))
                .query((rs, n) -> {
                    BigDecimal avg = rs.getBigDecimal("avg_seconds");
                    return new PickListAgg(rs.getLong("lists"), avg == null ? null : avg.longValue());
                }).single();
    }

    public List<DayAgg> shipmentsPerDay(UUID warehouseId, Instant from, Instant to, String zone) {
        return jdbc.sql("""
                select cast(s.shipped_at at time zone :zone as date) as day,
                       count(*) as shipments,
                       coalesce(sum(s.package_count), 0) as packages,
                       count(*) filter (where s.status = 'DELIVERED') as delivered
                from shipments s
                where s.warehouse_id = :warehouseId and s.shipped_at >= :fromTs and s.shipped_at < :toTs
                group by 1 order by 1
                """)
                .param("warehouseId", warehouseId).param("zone", zone)
                .param("fromTs", SqlTimes.of(from)).param("toTs", SqlTimes.of(to))
                .query((rs, n) -> new DayAgg(rs.getObject("day", LocalDate.class), rs.getLong("shipments"),
                        rs.getLong("packages"), rs.getLong("delivered")))
                .list();
    }

    public List<DemandRow> openDemand(UUID warehouseId) {
        return jdbc.sql("""
                select l.item_id, count(*) as order_lines,
                       sum(l.ordered_qty - l.picked_qty) as to_pick_qty,
                       min(o.required_date) as earliest_required
                from sales_orders o join sales_order_lines l on l.sales_order_id = o.id
                where o.warehouse_id = :warehouseId and o.status in ('CONFIRMED', 'PARTIALLY_SHIPPED')
                  and l.ordered_qty > l.picked_qty
                group by l.item_id
                """)
                .param("warehouseId", warehouseId)
                .query((rs, n) -> new DemandRow(rs.getObject("item_id", UUID.class), rs.getLong("order_lines"),
                        rs.getBigDecimal("to_pick_qty"), rs.getObject("earliest_required", LocalDate.class)))
                .list();
    }
}
