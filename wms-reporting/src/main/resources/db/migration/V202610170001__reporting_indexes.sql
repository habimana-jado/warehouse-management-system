-- wms-reporting is read-only and owns no tables. These indexes only speed up reports; nothing depends on them.

-- valuation: last known receipt cost per item
create index if not exists ix_movement_item_receipt_cost
    on stock_movements (item_id, occurred_at desc)
    where type = 'RECEIPT' and unit_cost is not null;

-- audit feed: movements of one warehouse in a time window (existing index leads with warehouse_id, item_id)
create index if not exists ix_movement_wh_time
    on stock_movements (warehouse_id, occurred_at desc);

-- audit feed: task events by time (existing index is (task_id, occurred_at))
create index if not exists ix_task_events_time
    on task_events (occurred_at);