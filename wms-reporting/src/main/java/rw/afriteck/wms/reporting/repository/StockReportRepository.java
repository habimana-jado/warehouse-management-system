package rw.afriteck.wms.reporting.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import rw.afriteck.wms.reporting.support.SqlArrays;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class StockReportRepository {

    private final JdbcClient jdbc;

    public record OnHandRow(UUID itemId, BigDecimal onHand, BigDecimal storageQty,
                            BigDecimal receivingQty, BigDecimal damagedQty) {}
    public record ExpiryRow(UUID itemId, UUID lotId, String lotNumber, LocalDate expiryDate,
                            UUID locationId, BigDecimal quantity) {}
    public record CostedOnHandRow(UUID itemId, BigDecimal onHand, BigDecimal lastUnitCost, Instant lastCostAt) {}

    /** One row per item with stock in the warehouse; the usage buckets come from bin-id arrays. */
    public List<OnHandRow> onHandByItem(UUID warehouseId, Collection<UUID> storageBins,
                                        Collection<UUID> receivingBins, Collection<UUID> damagedBins) {
        return jdbc.sql("""
                select item_id,
                       sum(quantity) as on_hand,
                       coalesce(sum(quantity) filter (where location_id = any(cast(:storage as uuid[]))), 0)   as storage_qty,
                       coalesce(sum(quantity) filter (where location_id = any(cast(:receiving as uuid[]))), 0) as receiving_qty,
                       coalesce(sum(quantity) filter (where location_id = any(cast(:damaged as uuid[]))), 0)   as damaged_qty
                from stock_balances
                where warehouse_id = :warehouseId and quantity > 0
                group by item_id
                """)
                .param("warehouseId", warehouseId)
                .param("storage", SqlArrays.uuids(storageBins))
                .param("receiving", SqlArrays.uuids(receivingBins))
                .param("damaged", SqlArrays.uuids(damagedBins))
                .query((rs, n) -> new OnHandRow(rs.getObject("item_id", UUID.class),
                        rs.getBigDecimal("on_hand"), rs.getBigDecimal("storage_qty"),
                        rs.getBigDecimal("receiving_qty"), rs.getBigDecimal("damaged_qty")))
                .list();
    }

    private static final String EXPIRY_FROM = """
            from stock_balances b
            join lots l on l.id = b.lot_id
            where b.warehouse_id = :warehouseId
              and b.quantity > 0
              and l.expiry_date is not null
              and l.expiry_date <= :cutoff
              and (:includeExpired or l.expiry_date >= :today)
            """;

    public long countExpiring(UUID warehouseId, LocalDate cutoff, LocalDate today, boolean includeExpired) {
        return jdbc.sql("select count(*) " + EXPIRY_FROM)
                .param("warehouseId", warehouseId).param("cutoff", cutoff)
                .param("today", today).param("includeExpired", includeExpired)
                .query(Long.class).single();
    }

    public List<ExpiryRow> findExpiring(UUID warehouseId, LocalDate cutoff, LocalDate today,
                                        boolean includeExpired, int limit, long offset) {
        return jdbc.sql("""
                select b.item_id, b.location_id, b.lot_id, l.lot_number, l.expiry_date, b.quantity
                """ + EXPIRY_FROM + " order by l.expiry_date, l.lot_number, b.id limit :limit offset :offset")
                .param("warehouseId", warehouseId).param("cutoff", cutoff)
                .param("today", today).param("includeExpired", includeExpired)
                .param("limit", limit).param("offset", offset)
                .query((rs, n) -> new ExpiryRow(rs.getObject("item_id", UUID.class),
                        rs.getObject("lot_id", UUID.class), rs.getString("lot_number"),
                        rs.getObject("expiry_date", LocalDate.class),
                        rs.getObject("location_id", UUID.class), rs.getBigDecimal("quantity")))
                .list();
    }

    /** On hand per item (all bins) with the last known RECEIPT cost of that item; cost null = never known. */
    public List<CostedOnHandRow> onHandWithLastCost(UUID warehouseId) {
        return jdbc.sql("""
                with held as (
                    select item_id, sum(quantity) as qty
                    from stock_balances
                    where warehouse_id = :warehouseId and quantity > 0
                    group by item_id),
                cost as (
                    select distinct on (m.item_id) m.item_id, m.unit_cost, m.occurred_at
                    from stock_movements m
                    where m.type = 'RECEIPT' and m.unit_cost is not null
                      and m.item_id in (select item_id from held)
                    order by m.item_id, m.occurred_at desc, m.id)
                select h.item_id, h.qty, c.unit_cost, c.occurred_at as cost_at
                from held h left join cost c on c.item_id = h.item_id
                """)
                .param("warehouseId", warehouseId)
                .query((rs, n) -> {
                    OffsetDateTime at = rs.getObject("cost_at", OffsetDateTime.class);
                    return new CostedOnHandRow(rs.getObject("item_id", UUID.class), rs.getBigDecimal("qty"),
                            rs.getBigDecimal("unit_cost"), at == null ? null : at.toInstant());
                })
                .list();
    }
}