-- wms-foundation: warehouses, locations, item categories, items, item UOMs, suppliers, customers
-- Standards: see backbone.md section 2 (audit block identical to BaseEntity, uq_/ck_/fk_/ix_ names,
-- quantities numeric(19,4), enums stored as varchar without check constraints except where a business
-- invariant needs one, no foreign keys across modules).
-- No seed rows: foundation has no reference data.

-- ---------------------------------------------------------------------------
-- warehouses
-- ---------------------------------------------------------------------------
create table warehouses (
    id            uuid         primary key,
    version       bigint       not null default 0,
    created_at    timestamptz  not null,
    created_by    uuid,
    updated_at    timestamptz  not null,
    updated_by    uuid,

    code          varchar(20)  not null,               -- normalized (trim + uppercase) by the service
    name          varchar(120) not null,
    address_line  varchar(255),
    city          varchar(100),
    country       varchar(100),
    active        boolean      not null default true,

    constraint uq_warehouses_code unique (code)
);

-- ---------------------------------------------------------------------------
-- locations (ZONE > AISLE > RACK > BIN; only BIN holds stock)
-- ---------------------------------------------------------------------------
create table locations (
    id            uuid          primary key,
    version       bigint        not null default 0,
    created_at    timestamptz   not null,
    created_by    uuid,
    updated_at    timestamptz   not null,
    updated_by    uuid,

    warehouse_id  uuid          not null,
    parent_id     uuid,
    code          varchar(40)   not null,              -- normalized by the service
    name          varchar(120),
    type          varchar(30)   not null,              -- LocationType
    usage         varchar(30)   not null default 'STORAGE',   -- LocationUsage
    barcode       varchar(60),
    max_capacity  numeric(19,4),
    active        boolean       not null default true,

    constraint uq_locations_warehouse_code unique (warehouse_id, code),
    constraint uq_locations_barcode unique (barcode),   -- several NULLs are allowed
    constraint fk_locations_warehouse foreign key (warehouse_id) references warehouses (id),
    constraint fk_locations_parent foreign key (parent_id) references locations (id),
    constraint ck_locations_parent_not_self check (parent_id is null or parent_id <> id),
    constraint ck_locations_capacity check (max_capacity is null or max_capacity >= 0),
    -- business invariant: a non-STORAGE usage is only allowed on BIN locations
    constraint ck_locations_usage_bin check (usage = 'STORAGE' or type = 'BIN')
);

create index ix_locations_parent          on locations (parent_id);
create index ix_locations_warehouse_usage on locations (warehouse_id, type, usage);

-- ---------------------------------------------------------------------------
-- item_categories
-- ---------------------------------------------------------------------------
create table item_categories (
    id           uuid         primary key,
    version      bigint       not null default 0,
    created_at   timestamptz  not null,
    created_by   uuid,
    updated_at   timestamptz  not null,
    updated_by   uuid,

    name         varchar(100) not null,
    description  varchar(255),
    active       boolean      not null default true
);

-- case-insensitive uniqueness, matches existsByNameIgnoreCase
create unique index uq_item_categories_name on item_categories (lower(name));

-- ---------------------------------------------------------------------------
-- items
-- sku, base_uom, tracking_type and expiry_tracked are immutable after creation (enforced in the service)
-- ---------------------------------------------------------------------------
create table items (
    id              uuid          primary key,
    version         bigint        not null default 0,
    created_at      timestamptz   not null,
    created_by      uuid,
    updated_at      timestamptz   not null,
    updated_by      uuid,

    sku             varchar(40)   not null,            -- normalized by the service
    name            varchar(200)  not null,
    description     text,
    barcode         varchar(60),
    category_id     uuid,
    base_uom        varchar(10)   not null,
    tracking_type   varchar(30)   not null default 'NONE',   -- TrackingType: NONE, LOT, SERIAL
    expiry_tracked  boolean       not null default false,
    unit_cost       numeric(19,4),
    weight_kg       numeric(19,4),
    active          boolean       not null default true,

    constraint uq_items_sku unique (sku),
    constraint uq_items_barcode unique (barcode),       -- several NULLs are allowed
    constraint fk_items_category foreign key (category_id) references item_categories (id),
    constraint ck_items_unit_cost check (unit_cost is null or unit_cost >= 0),
    constraint ck_items_weight check (weight_kg is null or weight_kg >= 0),
    -- business invariant: expiry tracking requires LOT tracking
    constraint ck_items_expiry_requires_lot check (not expiry_tracked or tracking_type = 'LOT')
);

create index ix_items_category on items (category_id);

-- ---------------------------------------------------------------------------
-- item_uoms (units of measure other than the base unit, e.g. BOX = 12 EA)
-- Item barcodes and box barcodes must be unique ACROSS both tables: the service checks that,
-- the database only guarantees uniqueness inside each table.
-- ---------------------------------------------------------------------------
create table item_uoms (
    id              uuid          primary key,
    version         bigint        not null default 0,
    created_at      timestamptz   not null,
    created_by      uuid,
    updated_at      timestamptz   not null,
    updated_by      uuid,

    item_id         uuid          not null,
    uom             varchar(10)   not null,
    factor_to_base  numeric(19,4) not null,
    barcode         varchar(60),

    constraint uq_item_uoms_item_uom unique (item_id, uom),
    constraint uq_item_uoms_barcode unique (barcode),   -- several NULLs are allowed
    constraint fk_item_uoms_item foreign key (item_id) references items (id) on delete cascade,
    constraint ck_item_uoms_factor check (factor_to_base > 0)
);

-- ---------------------------------------------------------------------------
-- suppliers
-- ---------------------------------------------------------------------------
create table suppliers (
    id              uuid         primary key,
    version         bigint       not null default 0,
    created_at      timestamptz  not null,
    created_by      uuid,
    updated_at      timestamptz  not null,
    updated_by      uuid,

    code            varchar(30)  not null,              -- normalized by the service
    name            varchar(150) not null,
    contact_person  varchar(150),
    phone           varchar(50),
    email           varchar(255),
    address_line    varchar(255),
    city            varchar(100),
    country         varchar(100),
    active          boolean      not null default true,

    constraint uq_suppliers_code unique (code)
);

-- ---------------------------------------------------------------------------
-- customers
-- ---------------------------------------------------------------------------
create table customers (
    id              uuid         primary key,
    version         bigint       not null default 0,
    created_at      timestamptz  not null,
    created_by      uuid,
    updated_at      timestamptz  not null,
    updated_by      uuid,

    code            varchar(30)  not null,              -- normalized by the service
    name            varchar(150) not null,
    contact_person  varchar(150),
    phone           varchar(50),
    email           varchar(255),
    address_line    varchar(255),
    city            varchar(100),
    country         varchar(100),
    active          boolean      not null default true,

    constraint uq_customers_code unique (code)
);
