-- =====================================================================================
-- wms-operations: tasks, task lines and task history
--
-- Tables:    task, task_line, task_event
-- Sequence:  task_number_seq (TSK-000123, formatted in the service by TaskNumbers)
--
-- ---------------------------------------------------------------------------
-- Sequence for human-readable task numbers
-- Read with: SELECT nextval('task_number_seq')  (gaps are acceptable)
-- ---------------------------------------------------------------------------
CREATE SEQUENCE task_number_seq START WITH 1 INCREMENT BY 1 NO CYCLE;

-- ---------------------------------------------------------------------------
-- task: header of a unit of work
-- ---------------------------------------------------------------------------
CREATE TABLE task (
    id                uuid         PRIMARY KEY,
    version           bigint       NOT NULL DEFAULT 0,
    created_at        timestamptz  NOT NULL,
    created_by        uuid,
    updated_at        timestamptz  NOT NULL,
    updated_by        uuid,

    task_number       varchar(30)  NOT NULL,
    type              varchar(20)  NOT NULL,     -- TaskType
    status            varchar(20)  NOT NULL,     -- TaskStatus
    priority          varchar(20)  NOT NULL,     -- TaskPriority

    warehouse_id      uuid         NOT NULL,     -- wms-foundation (no FK)
    zone_location_id  uuid,                      -- wms-foundation ZONE (no FK), optional

    reference_type    varchar(40)  NOT NULL,     -- owning module's constant, e.g. PURCHASE_ORDER
    reference_id      uuid         NOT NULL,
    reference_number  varchar(40),               -- human label for the board

    assignee_id       uuid,                      -- wms-auth user (no FK)
    assigned_at       timestamptz,
    started_at        timestamptz,
    completed_at      timestamptz,
    due_at            timestamptz,

    cancel_reason     varchar(255),
    notes             varchar(500),

    CONSTRAINT uq_task_number UNIQUE (task_number)
);

-- Task board: warehouse + status + type (+ priority)
CREATE INDEX ix_task_board     ON task (warehouse_id, status, type, priority);
-- "My tasks"
CREATE INDEX ix_task_assignee  ON task (assignee_id, status);
-- findByReference / cancelByReference
CREATE INDEX ix_task_reference ON task (reference_type, reference_id);

-- claimNext (FOR UPDATE SKIP LOCKED): small index over open tasks only
CREATE INDEX ix_task_open_claim ON task (warehouse_id, type) WHERE status = 'OPEN';
-- productivity: completed tasks by completion date
CREATE INDEX ix_task_completed  ON task (warehouse_id, completed_at) WHERE status = 'COMPLETED';

-- ---------------------------------------------------------------------------
-- task_line: one instruction inside a task
-- ---------------------------------------------------------------------------
CREATE TABLE task_line (
    id                 uuid           PRIMARY KEY,
    version            bigint         NOT NULL DEFAULT 0,
    created_at         timestamptz    NOT NULL,
    created_by         uuid,
    updated_at         timestamptz    NOT NULL,
    updated_by         uuid,

    task_id            uuid           NOT NULL,
    line_no            integer        NOT NULL,

    item_id            uuid           NOT NULL,  -- wms-foundation (no FK)
    lot_number         varchar(60),              -- normalized with Codes.normalize
    serial_number      varchar(60),              -- normalized with Codes.normalize

    from_location_id   uuid,                     -- wms-foundation BIN (no FK)
    to_location_id     uuid,                     -- wms-foundation BIN (no FK)

    planned_qty        numeric(19,4)  NOT NULL,  -- always in the item's base UOM
    done_qty           numeric(19,4)  NOT NULL DEFAULT 0,

    status             varchar(20)    NOT NULL,  -- TaskLineStatus
    reference_line_id  uuid,                     -- PO line, SO line, ... (no FK)
    short_reason       varchar(255),

    CONSTRAINT fk_task_line_task      FOREIGN KEY (task_id) REFERENCES task (id),
    CONSTRAINT uq_task_line_no        UNIQUE (task_id, line_no),
    -- business invariants (also validated in the service)
    CONSTRAINT ck_task_line_planned   CHECK (planned_qty > 0),
    CONSTRAINT ck_task_line_done      CHECK (done_qty >= 0 AND done_qty <= planned_qty)
);

-- uq_task_line_no already serves lookups by task_id

-- ---------------------------------------------------------------------------
-- task_event: append-only history of a task
-- ---------------------------------------------------------------------------
CREATE TABLE task_event (
    id              uuid         PRIMARY KEY,
    version         bigint       NOT NULL DEFAULT 0,
    created_at      timestamptz  NOT NULL,
    created_by      uuid,
    updated_at      timestamptz  NOT NULL,
    updated_by      uuid,

    task_id         uuid         NOT NULL,
    event_type      varchar(30)  NOT NULL,       -- TaskEventType
    from_status     varchar(20),                 -- TaskStatus
    to_status       varchar(20),                 -- TaskStatus
    actor_user_id   uuid,                        -- null when done by a job (no FK)
    assignee_id     uuid,                        -- snapshot at the time of the event (no FK)
    occurred_at     timestamptz  NOT NULL,
    note            varchar(255),

    CONSTRAINT fk_task_event_task FOREIGN KEY (task_id) REFERENCES task (id)
);

CREATE INDEX ix_task_event_task ON task_event (task_id, occurred_at);
