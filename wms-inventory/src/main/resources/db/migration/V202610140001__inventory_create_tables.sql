-- =====================================================================================================================
-- wms-inventory: lots, stock balances, movement ledger, adjustments, transfers, cycle counts, reorder policies.

-- =====================================================================================================================

-- ============================================================ sequences (human-readable document numbers)
create sequence adjustment_number_seq start 1 increment 1;   -- ADJ-000123
create sequence transfer_number_seq   start 1 increment 1;   -- TRF-000123
create sequence count_number_seq      start 1 increment 1;   -- CNT-000123

-- ============================================================ lot
create table lots (
    id          uuid        primary key,
    version     bigint      not null default 0,
    created_at  timestamptz not null,
    created_by  uuid,
    updated_at  timestamptz not null,
    updated_by  uuid,

    item_id     uuid        not null,
    lot_number  varchar(64) not null,                -- stored via Codes.normalize (trim + uppercase)
    expiry_date date,                                -- one lot number = one expiry date

    constraint uq_lot_item_number unique (item_id, lot_number)
);
create index ix_lot_expiry on lots (expiry_date);

-- ============================================================ stock_balance
create table stock_balances (
    id            uuid          primary key,
    version       bigint        not null default 0,
    created_at    timestamptz   not null,
    created_by    uuid,
    updated_at    timestamptz   not null,
    updated_by    uuid,

    warehouse_id  uuid          not null,            -- denormalized from the location
    location_id   uuid          not null,            -- an active BIN
    item_id       uuid          not null,
    lot_id        uuid,                              -- only for LOT tracked items
    serial_number varchar(64),                       -- only for SERIAL tracked items
    quantity      numeric(19,4) not null default 0,  -- base UOM; serial rows hold 0 or 1

    constraint fk_balance_lot           foreign key (lot_id) references lot (id),
    constraint ck_balance_quantity      check (quantity >= 0),
    constraint ck_balance_lot_or_serial check (lot_id is null or serial_number is null),
    constraint ck_balance_serial_qty    check (serial_number is null or quantity <= 1)
);

-- One row per (bin, item, lot, serial). Nulls are folded with coalesce so the key is unique on every PostgreSQL version.
create unique index uq_balance_key on stock_balance
    (location_id, item_id,
     coalesce(lot_id, '00000000-0000-0000-0000-000000000000'::uuid),
     coalesce(serial_number, ''));

-- A serial can be in stock in only one place at a time.
create unique index uq_balance_serial_in_stock on stock_balances (item_id, serial_number)
    where serial_number is not null and quantity > 0;

create index ix_balance_wh_item  on stock_balances (warehouse_id, item_id);
create index ix_balance_location on stock_balances (location_id);
create index ix_balance_item_lot on stock_balances (item_id, lot_id);

-- ============================================================ stock_movement (append-only ledger)
create table stock_movements (
    id               uuid          primary key,
    version          bigint        not null default 0,
    created_at       timestamptz   not null,
    created_by       uuid,
    updated_at       timestamptz   not null,
    updated_by       uuid,

    transaction_id   uuid          not null,         -- groups the lines of one posting (both sides of a transfer)
    type             varchar(24)   not null,
    occurred_at      timestamptz   not null,
    warehouse_id     uuid          not null,
    location_id      uuid          not null,
    item_id          uuid          not null,
    lot_id           uuid,
    serial_number    varchar(64),
    quantity         numeric(19,4) not null,         -- SIGNED delta, never zero
    balance_after    numeric(19,4) not null,         -- balance of this exact key after the move
    unit_cost        numeric(19,4),                  -- only when the caller supplied it; null = unknown
    reference_type   varchar(32),
    reference_id     uuid,
    reference_number varchar(64),
    note             varchar(500),

    constraint fk_movement_lot      foreign key (lot_id) references lots (id),
    constraint ck_movement_quantity check (quantity <> 0),
    constraint ck_movement_balance  check (balance_after >= 0),
    constraint ck_movement_cost     check (unit_cost is null or unit_cost >= 0),
    -- the sign of the delta must match the movement type; adjustments may go either way
    constraint ck_movement_sign check (
           (type in ('RECEIPT', 'TRANSFER_IN') and quantity > 0)
        or (type in ('ISSUE', 'TRANSFER_OUT')  and quantity < 0)
        or  type in ('ADJUSTMENT', 'COUNT_ADJUSTMENT'))
);
create index ix_movement_wh_item_time on stock_movements (warehouse_id, item_id, occurred_at);
create index ix_movement_location     on stock_movements (location_id);
create index ix_movement_reference    on stock_movements (reference_type, reference_id);
create index ix_movement_transaction  on stock_movements (transaction_id);

-- The ledger is the source of truth: refuse any update or delete, whatever the application does.
-- (truncate is not blocked by a row trigger, so test resets still work.)
create function wms_forbid_ledger_change() returns trigger as $$
begin
    raise exception 'stock_movement is append-only (% not allowed)', tg_op;
end;
$$ language plpgsql;

create trigger trg_stock_movement_append_only
    before update or delete on stock_movements
    for each row execute function wms_forbid_ledger_change();

-- ============================================================ adjustments
create table adjustment_reasons (
    id                uuid         primary key,
    version           bigint       not null default 0,
    created_at        timestamptz  not null,
    created_by        uuid,
    updated_at        timestamptz  not null,
    updated_by        uuid,

    code              varchar(32)  not null,         -- Codes.normalize, e.g. DAMAGED
    name              varchar(120) not null,
    direction         varchar(16)  not null default 'BOTH',      -- INCREASE | DECREASE | BOTH
    requires_approval boolean      not null default true,
    active            boolean      not null default true,

    constraint uq_adjustment_reason_code unique (code)
);

create table stock_adjustments (
    id                uuid         primary key,
    version           bigint       not null default 0,
    created_at        timestamptz  not null,
    created_by        uuid,
    updated_at        timestamptz  not null,
    updated_by        uuid,

    adjustment_number varchar(20)  not null,         -- ADJ-000123
    warehouse_id      uuid         not null,
    reason_id         uuid         not null,
    status            varchar(20)  not null,         -- PENDING_APPROVAL | POSTED | REJECTED | CANCELLED
    notes             varchar(500),
    requested_by      uuid         not null,         -- user id; the requester cannot approve
    decided_by        uuid,                          -- approver or rejecter (null when posted without approval)
    decided_at        timestamptz,
    rejection_reason  varchar(500),

    constraint uq_adjustment_number unique (adjustment_number),
    constraint fk_adjustment_reason foreign key (reason_id) references adjustment_reasons (id)
);
create index ix_adjustment_wh_status on stock_adjustments (warehouse_id, status);
create index ix_adjustment_requester on stock_adjustments (warehouse_id, requested_by);

create table stock_adjustment_lines (
    id             uuid          primary key,
    version        bigint        not null default 0,
    created_at     timestamptz   not null,
    created_by     uuid,
    updated_at     timestamptz   not null,
    updated_by     uuid,

    adjustment_id  uuid          not null,
    line_no        integer       not null,
    location_id    uuid          not null,
    item_id        uuid          not null,
    lot_number     varchar(64),                      -- normalized; the Lot is resolved when posting
    expiry_date    date,                             -- only used if the lot has to be created
    serial_number  varchar(64),
    quantity_delta numeric(19,4) not null,           -- signed, non-zero, base UOM

    constraint uq_adjustment_line_no         unique (adjustment_id, line_no),
    constraint fk_adjustment_line_adjustment foreign key (adjustment_id) references stock_adjustments (id),
    constraint ck_adjustment_line_delta      check (quantity_delta <> 0)
);
create index ix_adjustment_line_item     on stock_adjustment_lines (item_id);
create index ix_adjustment_line_location on stock_adjustment_lines (location_id);

-- ============================================================ transfers
create table stock_transfers (
    id                       uuid         primary key,
    version                  bigint       not null default 0,
    created_at               timestamptz  not null,
    created_by               uuid,
    updated_at               timestamptz  not null,
    updated_by               uuid,

    transfer_number          varchar(20)  not null,  -- TRF-000123
    type                     varchar(20)  not null,  -- BIN | INTER_WAREHOUSE
    status                   varchar(20)  not null,  -- PENDING | IN_TRANSIT | COMPLETED | CANCELLED
    source_warehouse_id      uuid         not null,
    destination_warehouse_id uuid         not null,  -- equals the source for BIN transfers
    shipped_at               timestamptz,
    completed_at             timestamptz,            -- also set when cancelled (terminal time)
    cancel_reason            varchar(500),
    notes                    varchar(500),

    constraint uq_transfer_number unique (transfer_number),
    constraint ck_transfer_bin_same_warehouse
        check (type <> 'BIN' or source_warehouse_id = destination_warehouse_id),
    constraint ck_transfer_inter_other_warehouse
        check (type <> 'INTER_WAREHOUSE' or source_warehouse_id <> destination_warehouse_id)
);
create index ix_transfer_source on stock_transfers (source_warehouse_id, status);
create index ix_transfer_dest   on stock_transfers (destination_warehouse_id, status);

create table stock_transfer_lines (
    id               uuid          primary key,
    version          bigint        not null default 0,
    created_at       timestamptz   not null,
    created_by       uuid,
    updated_at       timestamptz   not null,
    updated_by       uuid,

    transfer_id      uuid          not null,
    line_no          integer       not null,
    item_id          uuid          not null,
    lot_id           uuid,
    serial_number    varchar(64),
    from_location_id uuid          not null,
    to_location_id   uuid,                           -- null until receiving for INTER_WAREHOUSE
    quantity         numeric(19,4) not null,         -- planned, base UOM
    shipped_qty      numeric(19,4) not null default 0,
    received_qty     numeric(19,4) not null default 0,   -- in transit = shipped_qty - received_qty (while IN_TRANSIT)
    short_reason     varchar(500),                   -- line closed short (pick, bin move, or close-short on receive)

    constraint uq_transfer_line_no       unique (transfer_id, line_no),
    constraint fk_transfer_line_transfer foreign key (transfer_id) references stock_transfers (id),
    constraint fk_transfer_line_lot      foreign key (lot_id)      references lots (id),
    constraint ck_transfer_line_quantity check (quantity > 0),
    constraint ck_transfer_line_shipped  check (shipped_qty >= 0 and shipped_qty <= quantity),
    constraint ck_transfer_line_received check (received_qty >= 0 and received_qty <= shipped_qty)
);
create index ix_transfer_line_item on stock_transfer_lines (item_id);
create index ix_transfer_line_from on stock_transfer_lines (from_location_id);
create index ix_transfer_line_to   on stock_transfer_lines (to_location_id);

-- ============================================================ cycle counts
create table stock_counts (
    id               uuid         primary key,
    version          bigint       not null default 0,
    created_at       timestamptz  not null,
    created_by       uuid,
    updated_at       timestamptz  not null,
    updated_by       uuid,

    count_number     varchar(20)  not null,          -- CNT-000123
    warehouse_id     uuid         not null,
    zone_location_id uuid,                           -- optional scope: a ZONE of the warehouse
    status           varchar(20)  not null,          -- DRAFT | IN_PROGRESS | REVIEW | COMPLETED | CANCELLED
    started_at       timestamptz,
    completed_at     timestamptz,                    -- also set when cancelled (terminal time)
    approved_by      uuid,
    cancel_reason    varchar(500),
    notes            varchar(500),

    constraint uq_count_number unique (count_number)
);
create index ix_count_wh_status on stock_counts (warehouse_id, status);

create table stock_count_lines (
    id            uuid          primary key,
    version       bigint        not null default 0,
    created_at    timestamptz   not null,
    created_by    uuid,
    updated_at    timestamptz   not null,
    updated_by    uuid,

    count_id      uuid          not null,
    line_no       integer       not null,
    location_id   uuid          not null,
    item_id       uuid          not null,
    lot_id        uuid,
    serial_number varchar(64),
    expected_qty  numeric(19,4) not null,            -- system quantity at the moment the line was counted
    counted_qty   numeric(19,4),                     -- null until counted
    status        varchar(16)   not null,            -- PENDING | COUNTED
    counted_by    uuid,
    counted_at    timestamptz,
    task_id       uuid,                              -- the COUNT task carrying this line (null for lines added during the count)
    task_line_id  uuid,

    constraint uq_count_line_no       unique (count_id, line_no),
    constraint fk_count_line_count    foreign key (count_id) references stock_counts (id),
    constraint fk_count_line_lot      foreign key (lot_id)   references lots (id),
    constraint ck_count_line_expected check (expected_qty >= 0),
    constraint ck_count_line_counted  check (counted_qty is null or counted_qty >= 0),
    -- a line is COUNTED exactly when it has a counted quantity
    constraint ck_count_line_status check (
           (status = 'COUNTED' and counted_qty is not null)
        or (status = 'PENDING' and counted_qty is null))
);

-- The same bin/item/lot/serial cannot appear twice in one count (addLine relies on this to reject duplicates).
create unique index uq_count_line_key on stock_count_lines
    (count_id, location_id, item_id,
     coalesce(lot_id, '00000000-0000-0000-0000-000000000000'::uuid),
     coalesce(serial_number, ''));

create index ix_count_line_status   on stock_count_lines (count_id, status);
create index ix_count_line_item     on stock_count_lines (item_id);
create index ix_count_line_location on stock_count_lines (location_id);

-- ============================================================ reorder policies
create table reorder_policies (
    id           uuid          primary key,
    version      bigint        not null default 0,
    created_at   timestamptz   not null,
    created_by   uuid,
    updated_at   timestamptz   not null,
    updated_by   uuid,

    warehouse_id uuid          not null,
    item_id      uuid          not null,
    min_qty      numeric(19,4) not null,             -- reorder point
    reorder_qty  numeric(19,4) not null,             -- suggested order size
    max_qty      numeric(19,4),
    active       boolean       not null default true,

    constraint uq_reorder_wh_item unique (warehouse_id, item_id),
    constraint ck_reorder_min     check (min_qty >= 0),
    constraint ck_reorder_qty     check (reorder_qty > 0),
    constraint ck_reorder_max     check (max_qty is null or max_qty > min_qty)
);

-- ============================================================ seed data: default adjustment reasons
insert into adjustment_reasons (id, version, created_at, updated_at, code, name, direction, requires_approval, active)
values
    (gen_random_uuid(), 0, now(), now(), 'DAMAGED',    'Damaged goods',    'DECREASE', true, true),
    (gen_random_uuid(), 0, now(), now(), 'EXPIRED',    'Expired goods',    'DECREASE', true, true),
    (gen_random_uuid(), 0, now(), now(), 'LOST',       'Lost or missing',  'DECREASE', true, true),
    (gen_random_uuid(), 0, now(), now(), 'FOUND',      'Found stock',      'INCREASE', true, true),
    (gen_random_uuid(), 0, now(), now(), 'CORRECTION', 'Stock correction', 'BOTH',     true, true);
