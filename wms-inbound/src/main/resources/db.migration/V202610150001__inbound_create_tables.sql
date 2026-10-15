-- wms-inbound: purchase orders, goods receipts, customer returns (RMA).
-- Standards: audit block identical to BaseEntity; no foreign keys to other modules
-- (supplier_id, customer_id, warehouse_id, item_id, location_id, task ids are plain uuids);
-- enum columns are varchar without check constraints.

create sequence po_number_seq start with 1 increment by 1; -- PO-000123
create sequence receipt_number_seq start with 1 increment by 1; -- GRN-000123
create sequence return_number_seq start with 1 increment by 1; -- RMA-000123

-- ---------------------------------------------------------------- purchase orders

create table purchase_orders
(
    id                 uuid primary key,
    version            bigint      not null default 0,
    created_at         timestamptz not null,
    created_by         uuid,
    updated_at         timestamptz not null,
    updated_by         uuid,

    po_number          varchar(30) not null,
    supplier_id        uuid        not null,
    warehouse_id       uuid        not null,
    status             varchar(30) not null,
    order_date         date        not null,
    expected_date      date,
    supplier_reference varchar(100),
    currency           varchar(3)  not null default 'RWF',
    notes              varchar(1000),
    confirmed_at       timestamptz,
    confirmed_by       uuid,
    closed_at          timestamptz,
    closed_by          uuid,
    cancelled_at       timestamptz,
    cancel_reason      varchar(500),

    constraint uq_purchase_orders_number unique (po_number),
    constraint ck_purchase_orders_dates check (expected_date is null or expected_date >= order_date),
    constraint ck_purchase_orders_currency check (currency ~ '^[A-Z]{3}$'
)
    );

create index ix_purchase_orders_board on purchase_orders (warehouse_id, status, order_date desc);
create index ix_purchase_orders_supplier on purchase_orders (supplier_id, status);

create table purchase_order_lines
(
    id                uuid primary key,
    version           bigint         not null default 0,
    created_at        timestamptz    not null,
    created_by        uuid,
    updated_at        timestamptz    not null,
    updated_by        uuid,

    purchase_order_id uuid           not null,
    line_no           integer        not null,
    item_id           uuid           not null,
    ordered_qty       numeric(19, 4) not null,
    accepted_qty      numeric(19, 4) not null default 0,
    rejected_qty      numeric(19, 4) not null default 0,
    unit_price        numeric(19, 4),
    tolerance_percent numeric(5, 2)  not null default 0,
    notes             varchar(500),

    constraint fk_purchase_order_lines_order
        foreign key (purchase_order_id) references purchase_orders (id),
    constraint uq_purchase_order_lines_line_no unique (purchase_order_id, line_no),
    constraint ck_purchase_order_lines_ordered check (ordered_qty > 0),
    constraint ck_purchase_order_lines_accepted check (accepted_qty >= 0),
    constraint ck_purchase_order_lines_rejected check (rejected_qty >= 0),
    constraint ck_purchase_order_lines_price check (unit_price is null or unit_price >= 0),
    constraint ck_purchase_order_lines_tolerance check (tolerance_percent >= 0 and tolerance_percent <= 100)
);

create index ix_purchase_order_lines_item on purchase_order_lines (item_id);

-- ---------------------------------------------------------------- goods receipts

create table goods_receipts
(
    id                   uuid primary key,
    version              bigint      not null default 0,
    created_at           timestamptz not null,
    created_by           uuid,
    updated_at           timestamptz not null,
    updated_by           uuid,

    receipt_number       varchar(30) not null,
    purchase_order_id    uuid        not null,
    warehouse_id         uuid        not null,
    mode                 varchar(20) not null,
    received_at          timestamptz not null,
    received_by          uuid        not null,
    delivery_note_number varchar(100),
    notes                varchar(1000),
    task_id              uuid,

    constraint uq_goods_receipts_number unique (receipt_number),
    constraint fk_goods_receipts_order
        foreign key (purchase_order_id) references purchase_orders (id)
);

create index ix_goods_receipts_order on goods_receipts (purchase_order_id);
create index ix_goods_receipts_warehouse on goods_receipts (warehouse_id, received_at desc);

create table goods_receipt_lines
(
    id                     uuid primary key,
    version                bigint         not null default 0,
    created_at             timestamptz    not null,
    created_by             uuid,
    updated_at             timestamptz    not null,
    updated_by             uuid,

    goods_receipt_id       uuid           not null,
    purchase_order_line_id uuid           not null,
    line_no                integer        not null,
    item_id                uuid           not null,
    lot_number             varchar(100),
    expiry_date            date,
    serial_number          varchar(100),
    accepted_qty           numeric(19, 4) not null default 0,
    rejected_qty           numeric(19, 4) not null default 0,
    reject_reason          varchar(500),
    receiving_location_id  uuid,
    reject_location_id     uuid,
    unit_cost              numeric(19, 4),
    putaway_status         varchar(20)    not null default 'NOT_REQUIRED',
    suggested_location_id  uuid,
    putaway_location_id    uuid,
    putaway_done_qty       numeric(19, 4),
    task_line_id           uuid,

    constraint fk_goods_receipt_lines_receipt
        foreign key (goods_receipt_id) references goods_receipts (id),
    constraint fk_goods_receipt_lines_order_line
        foreign key (purchase_order_line_id) references purchase_order_lines (id),
    constraint uq_goods_receipt_lines_line_no unique (goods_receipt_id, line_no),
    constraint ck_goods_receipt_lines_accepted check (accepted_qty >= 0),
    constraint ck_goods_receipt_lines_rejected check (rejected_qty >= 0),
    constraint ck_goods_receipt_lines_has_qty check (accepted_qty + rejected_qty > 0),
    constraint ck_goods_receipt_lines_reject_reason check (rejected_qty = 0 or reject_reason is not null),
    constraint ck_goods_receipt_lines_cost check (unit_cost is null or unit_cost >= 0),
    constraint ck_goods_receipt_lines_putaway_qty
        check (putaway_done_qty is null or (putaway_done_qty >= 0 and putaway_done_qty <= accepted_qty))
);

create index ix_goods_receipt_lines_order_line on goods_receipt_lines (purchase_order_line_id);
create index ix_goods_receipt_lines_putaway_pending
    on goods_receipt_lines (goods_receipt_id) where putaway_status = 'PENDING';

-- ---------------------------------------------------------------- customer returns (RMA)

create table customer_returns
(
    id                 uuid primary key,
    version            bigint      not null default 0,
    created_at         timestamptz not null,
    created_by         uuid,
    updated_at         timestamptz not null,
    updated_by         uuid,

    return_number      varchar(30) not null,
    warehouse_id       uuid        not null,
    customer_id        uuid,
    sales_order_id     uuid,
    sales_order_number varchar(50),
    status             varchar(20) not null,
    notes              varchar(1000),
    received_at        timestamptz,
    received_by        uuid,
    cancelled_at       timestamptz,
    cancel_reason      varchar(500),

    constraint uq_customer_returns_number unique (return_number)
);

create index ix_customer_returns_board on customer_returns (warehouse_id, status, created_at desc);
create index ix_customer_returns_sales_order on customer_returns (sales_order_id) where sales_order_id is not null;

create table customer_return_lines
(
    id                 uuid primary key,
    version            bigint         not null default 0,
    created_at         timestamptz    not null,
    created_by         uuid,
    updated_at         timestamptz    not null,
    updated_by         uuid,

    customer_return_id uuid           not null,
    line_no            integer        not null,
    item_id            uuid           not null,
    expected_qty       numeric(19, 4) not null,
    received_qty       numeric(19, 4),
    lot_number         varchar(100),
    expiry_date        date,
    serial_number      varchar(100),
    return_reason      varchar(500),
    disposition        varchar(20),
    location_id        uuid,

    constraint fk_customer_return_lines_return
        foreign key (customer_return_id) references customer_returns (id),
    constraint uq_customer_return_lines_line_no unique (customer_return_id, line_no),
    constraint ck_customer_return_lines_expected check (expected_qty > 0),
    constraint ck_customer_return_lines_received check (received_qty is null or received_qty >= 0)
);

create index ix_customer_return_lines_item on customer_return_lines (item_id);