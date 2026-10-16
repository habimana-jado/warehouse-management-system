package rw.afriteck.wms.reporting.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class InboundReportRepository {

    private final JdbcClient jdbc;

    public record OpenOrderRow(UUID id, String poNumber, UUID supplierId, String status,
                               LocalDate orderDate, LocalDate expectedDate, int lines, int fulfilledLines) {}
    public record SupplierAggRow(UUID supplierId, long purchaseOrders, long lines, long linesFulfilled,
                                 BigDecimal orderedQty, BigDecimal fulfilledQty, BigDecimal acceptedQty,
                                 BigDecimal rejectedQty, long datedReceived, long onTime) {}
    public record BacklogRow(UUID lineId, UUID receiptId, String receiptNumber, Instant receivedAt,
                             UUID itemId, BigDecimal acceptedQty, UUID suggestedLocationId) {}
    public record BacklogSummary(long pendingLines, Instant oldestReceivedAt) {}

    private static final String OPEN_ORDERS_WHERE = """
            where po.warehouse_id = :warehouseId
              and po.status in ('CONFIRMED', 'PARTIALLY_RECEIVED')
              and (not :overdueOnly or (po.expected_date is not null and po.expected_date < :today))
            """;

    public long countOpenOrders(UUID warehouseId, boolean overdueOnly, LocalDate today) {
        return jdbc.sql("select count(*) from purchase_orders po " + OPEN_ORDERS_WHERE)
                .param("warehouseId", warehouseId).param("overdueOnly", overdueOnly).param("today", today)
                .query(Long.class).single();
    }

    public List<OpenOrderRow> findOpenOrders(UUID warehouseId, boolean overdueOnly, LocalDate today,
                                             int limit, long offset) {
        return jdbc.sql("""
                select po.id, po.po_number, po.supplier_id, po.status, po.order_date, po.expected_date,
                       count(l.id) as line_count,
                       count(l.id) filter (where l.accepted_qty >= l.ordered_qty) as fulfilled_lines
                from purchase_orders po
                join purchase_order_lines l on l.purchase_order_id = po.id
                """ + OPEN_ORDERS_WHERE + """
                group by po.id
                order by po.expected_date nulls last, po.order_date, po.id
                limit :limit offset :offset
                """)
                .param("warehouseId", warehouseId).param("overdueOnly", overdueOnly).param("today", today)
                .param("limit", limit).param("offset", offset)
                .query((rs, n) -> new OpenOrderRow(rs.getObject("id", UUID.class), rs.getString("po_number"),
                        rs.getObject("supplier_id", UUID.class), rs.getString("status"),
                        rs.getObject("order_date", LocalDate.class), rs.getObject("expected_date", LocalDate.class),
                        rs.getInt("line_count"), rs.getInt("fulfilled_lines")))
                .list();
    }

    /** Cohort: POs with order_date in [fromDate, toDate), DRAFT and CANCELLED excluded. */
    public List<SupplierAggRow> supplierAggregates(UUID warehouseId, LocalDate fromDate, LocalDate toDate, String zone) {
        return jdbc.sql("""
                with po as (
                    select p.id, p.supplier_id, p.expected_date,
                           count(l.id) as lines,
                           count(l.id) filter (where l.accepted_qty >= l.ordered_qty) as lines_fulfilled,
                           sum(l.ordered_qty) as ordered_qty,
                           sum(least(l.accepted_qty, l.ordered_qty)) as fulfilled_qty,
                           sum(l.accepted_qty) as accepted_qty,
                           sum(l.rejected_qty) as rejected_qty
                    from purchase_orders p
                    join purchase_order_lines l on l.purchase_order_id = p.id
                    where p.warehouse_id = :warehouseId
                      and p.order_date >= :fromDate and p.order_date < :toDate
                      and p.status not in ('DRAFT', 'CANCELLED')
                    group by p.id),
                first_receipt as (
                    select gr.purchase_order_id,
                           min(cast(gr.received_at at time zone :zone as date)) as first_date
                    from goods_receipts gr
                    where gr.purchase_order_id in (select id from po)
                    group by gr.purchase_order_id)
                select po.supplier_id,
                       count(*) as po_count,
                       cast(sum(po.lines) as bigint) as lines,
                       cast(sum(po.lines_fulfilled) as bigint) as lines_fulfilled,
                       sum(po.ordered_qty) as ordered_qty,
                       sum(po.fulfilled_qty) as fulfilled_qty,
                       sum(po.accepted_qty) as accepted_qty,
                       sum(po.rejected_qty) as rejected_qty,
                       count(*) filter (where po.expected_date is not null and fr.first_date is not null) as dated_received,
                       count(*) filter (where po.expected_date is not null and fr.first_date is not null
                                          and fr.first_date <= po.expected_date) as on_time
                from po left join first_receipt fr on fr.purchase_order_id = po.id
                group by po.supplier_id
                """)
                .param("warehouseId", warehouseId).param("fromDate", fromDate).param("toDate", toDate)
                .param("zone", zone)
                .query((rs, n) -> new SupplierAggRow(rs.getObject("supplier_id", UUID.class),
                        rs.getLong("po_count"), rs.getLong("lines"), rs.getLong("lines_fulfilled"),
                        rs.getBigDecimal("ordered_qty"), rs.getBigDecimal("fulfilled_qty"),
                        rs.getBigDecimal("accepted_qty"), rs.getBigDecimal("rejected_qty"),
                        rs.getLong("dated_received"), rs.getLong("on_time")))
                .list();
    }

    private static final String BACKLOG_FROM = """
            from goods_receipt_lines gl
            join goods_receipts g on g.id = gl.goods_receipt_id
            where g.warehouse_id = :warehouseId and gl.putaway_status = 'PENDING'
            """;

    public BacklogSummary backlogSummary(UUID warehouseId) {
        return jdbc.sql("select count(*) as n, min(g.received_at) as oldest " + BACKLOG_FROM)
                .param("warehouseId", warehouseId)
                .query((rs, n) -> {
                    OffsetDateTime oldest = rs.getObject("oldest", OffsetDateTime.class);
                    return new BacklogSummary(rs.getLong("n"), oldest == null ? null : oldest.toInstant());
                }).single();
    }

    public List<BacklogRow> backlogRows(UUID warehouseId, int limit, long offset) {
        return jdbc.sql("""
                select gl.id as line_id, g.id as receipt_id, g.receipt_number, g.received_at,
                       gl.item_id, gl.accepted_qty, gl.suggested_location_id
                """ + BACKLOG_FROM + " order by g.received_at, g.id, gl.line_no limit :limit offset :offset")
                .param("warehouseId", warehouseId).param("limit", limit).param("offset", offset)
                .query((rs, n) -> new BacklogRow(rs.getObject("line_id", UUID.class),
                        rs.getObject("receipt_id", UUID.class), rs.getString("receipt_number"),
                        rs.getObject("received_at", OffsetDateTime.class).toInstant(),
                        rs.getObject("item_id", UUID.class), rs.getBigDecimal("accepted_qty"),
                        rs.getObject("suggested_location_id", UUID.class)))
                .list();
    }
}