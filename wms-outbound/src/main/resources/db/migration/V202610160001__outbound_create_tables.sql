-- wms-outbound: sales orders, pick lists, shipments, supplier returns (RTV)
-- Cross-module ids (customer_id, supplier_id, warehouse_id, item_id, location_id, task_id, task_line_id) are plain uuid columns.

-- ---------------------------------------------------------------- sequences
create sequence so_number_seq       start with 1 increment by 1;   -- SO-000123
create sequence pick_number_seq     start with 1 increment by 1;   -- PCK-000123
create sequence shipment_number_seq start with 1 increment by 1;   -- DN-000123
create sequence rtv_number_seq      start with 1 increment by 1;   -- RTV-000123

-- ------------------------------------------------------------- sales_orders
create table sales_orders (
                              id                 uuid        primary key,
                              version            bigint      not null default 0,
                              created_at         timestamptz not null,
                              created_by         uuid,
                              updated_at         timestamptz not null,
                              updated_by         uuid,
                              order_number       varchar(20) not null,
                              customer_id        uuid        not null,
                              warehouse_id       uuid        not null,
                              status             varchar(30) not null,
                              order_date         date        not null,
                              required_date      date,
                              currency           varchar(3)  not null,
                              customer_reference varchar(60),
                              notes              varchar(1000),
                              cancel_reason      varchar(500),
                              confirmed_at       timestamptz,
                              closed_at          timestamptz,
                              constraint uq_sales_orders_number   unique (order_number),
                              constraint ck_sales_orders_currency check (currency ~ '^[A-Z]{3}$'),
    constraint ck_sales_orders_dates    check (required_date is null or required_date >= order_date)
);

create index ix_sales_orders_board    on sales_orders (warehouse_id, status, order_date desc);
create index ix_sales_orders_customer on sales_orders (customer_id, status);

-- -------------------------------------------------------- sales_order_lines
create table sales_order_lines (
                                   id             uuid           primary key,
                                   version        bigint         not null default 0,
                                   created_at     timestamptz    not null,
                                   created_by     uuid,
                                   updated_at     timestamptz    not null,
                                   updated_by     uuid,
                                   sales_order_id uuid           not null,
                                   line_no        integer        not null,
                                   item_id        uuid           not null,
                                   ordered_qty    numeric(19,4)  not null,
                                   released_qty   numeric(19,4)  not null default 0,
                                   picked_qty     numeric(19,4)  not null default 0,
                                   shipped_qty    numeric(19,4)  not null default 0,
                                   unit_price     numeric(19,4),
                                   note           varchar(255),
                                   constraint fk_sales_order_lines_order foreign key (sales_order_id) references sales_orders (id),
                                   constraint uq_sales_order_lines_no    unique (sales_order_id, line_no),
                                   constraint uq_sales_order_lines_item  unique (sales_order_id, item_id),       -- one line per item (SO_LINE_DUPLICATED)
                                   constraint ck_sales_order_lines_qty   check (ordered_qty > 0 and released_qty >= 0 and picked_qty >= 0 and shipped_qty >= 0),
    -- picked <= released <= ordered, and what is shipped was picked
                                   constraint ck_sales_order_lines_flow  check (picked_qty <= released_qty and released_qty <= ordered_qty and shipped_qty <= picked_qty),
                                   constraint ck_sales_order_lines_price check (unit_price is null or unit_price >= 0)
);

create index ix_sales_order_lines_item on sales_order_lines (item_id);              -- ItemOutboundGuard

-- --------------------------------------------------------------- pick_lists
create table pick_lists (
                            id             uuid        primary key,
                            version        bigint      not null default 0,
                            created_at     timestamptz not null,
                            created_by     uuid,
                            updated_at     timestamptz not null,
                            updated_by     uuid,
                            pick_number    varchar(20) not null,
                            sales_order_id uuid        not null,
                            warehouse_id   uuid        not null,
                            status         varchar(20) not null,
                            task_id        uuid,                                                            -- PICK task in wms-operations
                            released_at    timestamptz not null,
                            completed_at   timestamptz,
                            cancel_reason  varchar(500),
                            constraint fk_pick_lists_order  foreign key (sales_order_id) references sales_orders (id),
                            constraint uq_pick_lists_number unique (pick_number),
                            constraint uq_pick_lists_task   unique (task_id)                                -- nulls are distinct: a plain unique is enough
);

create index ix_pick_lists_order on pick_lists (sales_order_id, status);
create index ix_pick_lists_board on pick_lists (warehouse_id, status, released_at desc);

-- --------------------------------------------------------------- pick_lines
create table pick_lines (
                            id                   uuid          primary key,
                            version              bigint        not null default 0,
                            created_at           timestamptz   not null,
                            created_by           uuid,
                            updated_at           timestamptz   not null,
                            updated_by           uuid,
                            pick_list_id         uuid          not null,
                            line_no              integer       not null,
                            sales_order_line_id  uuid          not null,
                            item_id              uuid          not null,
                            planned_qty          numeric(19,4) not null,
                            picked_qty           numeric(19,4) not null default 0,
                            status               varchar(20)   not null,
                            suggested_location_id uuid,
                            suggested_lot_number varchar(60),
                            task_line_id         uuid,
                            short_reason         varchar(500),
                            constraint fk_pick_lines_list       foreign key (pick_list_id) references pick_lists (id),
                            constraint fk_pick_lines_order_line foreign key (sales_order_line_id) references sales_order_lines (id),
                            constraint uq_pick_lines_no         unique (pick_list_id, line_no),
                            constraint uq_pick_lines_task_line  unique (task_line_id),
                            constraint ck_pick_lines_qty        check (planned_qty > 0 and picked_qty >= 0 and picked_qty <= planned_qty),
    -- a line closed below plan always has a reason (also cancelled lines, which carry the cancel reason)
                            constraint ck_pick_lines_short_reason check (picked_qty >= planned_qty or short_reason is not null)
);

create index ix_pick_lines_order_line on pick_lines (sales_order_line_id);

-- ---------------------------------------------------------- pick_allocations
create table pick_allocations (
                                  id              uuid          primary key,
                                  version         bigint        not null default 0,
                                  created_at      timestamptz   not null,
                                  created_by      uuid,
                                  updated_at      timestamptz   not null,
                                  updated_by      uuid,
                                  pick_line_id    uuid          not null,
                                  location_id     uuid          not null,
                                  lot_number      varchar(60),
                                  serial_number   varchar(60),
                                  quantity        numeric(19,4) not null,
                                  expiry_override boolean       not null default false,
                                  constraint fk_pick_allocations_line   foreign key (pick_line_id) references pick_lines (id),
                                  constraint ck_pick_allocations_qty    check (quantity > 0),
                                  constraint ck_pick_allocations_serial check (serial_number is null or quantity = 1)
);

-- one serial once per pick line; a partial index because a coalesce key would make every LOT allocation (no serial) collide
create unique index uq_pick_allocations_serial on pick_allocations (pick_line_id, serial_number) where serial_number is not null;
create index ix_pick_allocations_line on pick_allocations (pick_line_id);

-- ---------------------------------------------------------------- shipments
create table shipments (
                           id                 uuid        primary key,
                           version            bigint      not null default 0,
                           created_at         timestamptz not null,
                           created_by         uuid,
                           updated_at         timestamptz not null,
                           updated_by         uuid,
                           shipment_number    varchar(20) not null,
                           sales_order_id     uuid        not null,
                           customer_id        uuid        not null,
                           warehouse_id       uuid        not null,
                           status             varchar(20) not null,
                           shipped_at         timestamptz not null,
                           ship_to_address    varchar(500),
                           carrier            varchar(100),
                           tracking_reference varchar(100),
                           package_count      integer,
                           notes              varchar(1000),
                           delivered_at       timestamptz,
                           received_by_name   varchar(120),
                           pod_notes          varchar(500),
                           constraint fk_shipments_order     foreign key (sales_order_id) references sales_orders (id),
                           constraint uq_shipments_number    unique (shipment_number),
                           constraint ck_shipments_packages  check (package_count is null or package_count > 0),
                           constraint ck_shipments_delivery  check (delivered_at is null or delivered_at >= shipped_at)
);

create index ix_shipments_order on shipments (sales_order_id);
create index ix_shipments_board on shipments (warehouse_id, shipped_at desc);

-- ----------------------------------------------------------- shipment_lines
create table shipment_lines (
                                id                  uuid          primary key,
                                version             bigint        not null default 0,
                                created_at          timestamptz   not null,
                                created_by          uuid,
                                updated_at          timestamptz   not null,
                                updated_by          uuid,
                                shipment_id         uuid          not null,
                                line_no             integer       not null,
                                sales_order_line_id uuid          not null,
                                item_id             uuid          not null,
                                quantity            numeric(19,4) not null,
                                constraint fk_shipment_lines_shipment   foreign key (shipment_id) references shipments (id),
                                constraint fk_shipment_lines_order_line foreign key (sales_order_line_id) references sales_order_lines (id),
                                constraint uq_shipment_lines_no         unique (shipment_id, line_no),
                                constraint ck_shipment_lines_qty        check (quantity > 0)
);

create index ix_shipment_lines_order_line on shipment_lines (sales_order_line_id);

-- --------------------------------------------------------- supplier_returns
create table supplier_returns (
                                  id                 uuid        primary key,
                                  version            bigint      not null default 0,
                                  created_at         timestamptz not null,
                                  created_by         uuid,
                                  updated_at         timestamptz not null,
                                  updated_by         uuid,
                                  return_number      varchar(20) not null,
                                  supplier_id        uuid        not null,
                                  warehouse_id       uuid        not null,
                                  status             varchar(20) not null,
                                  return_date        date        not null,
                                  reason             varchar(500) not null,
                                  supplier_reference varchar(60),
                                  notes              varchar(1000),
                                  cancel_reason      varchar(500),
                                  posted_at          timestamptz,
                                  constraint uq_supplier_returns_number unique (return_number)
);

create index ix_supplier_returns_board on supplier_returns (warehouse_id, status, return_date desc);

-- ---------------------------------------------------- supplier_return_lines
create table supplier_return_lines (
                                       id                 uuid          primary key,
                                       version            bigint        not null default 0,
                                       created_at         timestamptz   not null,
                                       created_by         uuid,
                                       updated_at         timestamptz   not null,
                                       updated_by         uuid,
                                       supplier_return_id uuid          not null,
                                       line_no            integer       not null,
                                       item_id            uuid          not null,
                                       location_id        uuid          not null,
                                       lot_number         varchar(60),
                                       serial_number      varchar(60),
                                       quantity           numeric(19,4) not null,
                                       note               varchar(255),
                                       constraint fk_supplier_return_lines_return foreign key (supplier_return_id) references supplier_returns (id),
                                       constraint uq_supplier_return_lines_no     unique (supplier_return_id, line_no),
                                       constraint ck_supplier_return_lines_qty    check (quantity > 0),
                                       constraint ck_supplier_return_lines_serial check (serial_number is null or quantity = 1)
);

create index ix_supplier_return_lines_item on supplier_return_lines (item_id);      -- ItemOutboundGuard