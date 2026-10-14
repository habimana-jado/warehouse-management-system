-- wms-auth: users, roles, warehouse access, refresh tokens

-- ---------------------------------------------------------------------------
-- users
-- ---------------------------------------------------------------------------
create table users (
    id                    uuid         primary key,
    version               bigint       not null default 0,
    created_at            timestamptz  not null,
    created_by            uuid,
    updated_at            timestamptz  not null,
    updated_by            uuid,

    username              varchar(60)  not null,              -- stored lowercase by the service
    password_hash         varchar(100) not null,              -- BCrypt
    full_name             varchar(120) not null,
    email                 varchar(255),
    active                boolean      not null default true,
    must_change_password  boolean      not null default false,
    failed_login_attempts integer      not null default 0,
    locked_until          timestamptz,
    last_login_at         timestamptz,

    constraint ck_users_failed_attempts check (failed_login_attempts >= 0)
);

-- case-insensitive uniqueness, matches findByUsernameIgnoreCase / existsByUsernameIgnoreCase
create unique index uq_users_username on users (lower(username));

-- ---------------------------------------------------------------------------
-- user_roles (@ElementCollection of RoleName stored as string; no check on the enum column)
-- ---------------------------------------------------------------------------
create table user_roles (
    user_id uuid        not null,
    role    varchar(30) not null,

    constraint pk_user_roles primary key (user_id, role),
    constraint fk_user_roles_user foreign key (user_id) references users (id) on delete cascade
);

-- ---------------------------------------------------------------------------
-- user_warehouses (@ElementCollection of warehouse ids)
-- warehouse_id is a plain uuid: no foreign key across modules (warehouses belong to wms-foundation)
-- ---------------------------------------------------------------------------
create table user_warehouses (
    user_id      uuid not null,
    warehouse_id uuid not null,

    constraint pk_user_warehouses primary key (user_id, warehouse_id),
    constraint fk_user_warehouses_user foreign key (user_id) references users (id) on delete cascade
);

create index ix_user_warehouses_warehouse on user_warehouses (warehouse_id);

-- ---------------------------------------------------------------------------
-- refresh_tokens
-- Only the SHA-256 hash of the opaque token is stored. family_id groups the rotation chain
-- so reuse of a revoked token can revoke the whole family.
-- ---------------------------------------------------------------------------
create table refresh_tokens (
    id          uuid         primary key,
    user_id     uuid         not null,
    token_hash  varchar(64)  not null,
    family_id   varchar(36)  not null,
    expires_at  timestamptz  not null,
    revoked_at  timestamptz,
    user_agent  varchar(255),
    ip_address  varchar(45),

    constraint uq_refresh_tokens_token_hash unique (token_hash),
    constraint fk_refresh_tokens_user foreign key (user_id) references users (id) on delete cascade
);

create index ix_refresh_tokens_user    on refresh_tokens (user_id);
create index ix_refresh_tokens_family  on refresh_tokens (family_id);
create index ix_refresh_tokens_expires on refresh_tokens (expires_at);   -- scheduled cleanup
