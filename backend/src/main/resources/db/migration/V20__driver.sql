-- Driver module (V20–V29). Schema per docs/data-model.md, "Driver (V20–V29)".
--
-- Driver writes arrive from a phone that may have been offline for hours, so every
-- action carries the clientId the phone generated. client_id is UNIQUE on each of
-- the tables a driver can create, and is the primary key of sync_log: applying the
-- same action twice has no effect. See docs/api.md §9.

-- The driver's side of a trip. One row once the driver accepts the load (R0).
CREATE TABLE trip_runs (
    trip_id       VARCHAR(40)  PRIMARY KEY REFERENCES trips (id),
    driver_id     VARCHAR(40)  NOT NULL REFERENCES users (id),
    accepted_at   TIMESTAMPTZ,
    started_at    TIMESTAMPTZ,
    ended_at      TIMESTAMPTZ,
    -- Bumped on every successful sync; the live board shows it (D6).
    last_sync_at  TIMESTAMPTZ
);

CREATE INDEX idx_trip_runs_driver ON trip_runs (driver_id);

CREATE TABLE deliveries (
    id                  VARCHAR(40)  PRIMARY KEY,
    stop_id             VARCHAR(40)  NOT NULL REFERENCES stops (id),
    order_id            VARCHAR(40)  NOT NULL REFERENCES orders (id),
    vehicle_id          VARCHAR(10)  NOT NULL REFERENCES vehicles (id),
    outcome             VARCHAR(20)  NOT NULL CHECK (outcome IN ('DELIVERED', 'PARTIAL', 'FAILED')),
    units               INTEGER      NOT NULL CHECK (units >= 0),
    -- Why a delivery was short or did not happen; the record is the proof (R4c, R4d).
    reason              VARCHAR(30)  CHECK (reason IS NULL
                                            OR reason IN ('STORE_CLOSED', 'NO_ACCESS', 'REFUSED', 'DAMAGED')),
    received_by         VARCHAR(100),
    photo_file_id       VARCHAR(40)  REFERENCES files (id),
    signature_file_id   VARCHAR(40)  REFERENCES files (id),
    arrived_at          TIMESTAMPTZ  NOT NULL,
    completed_at        TIMESTAMPTZ  NOT NULL,
    recorded_by         VARCHAR(40)  REFERENCES users (id),
    client_id           VARCHAR(40)  NOT NULL UNIQUE,
    -- Set by the 10-second undo (R4b), not by a confirm dialog.
    undone_at           TIMESTAMPTZ,
    -- The dispatcher's call on a failed delivery (D6f).
    decision            VARCHAR(20)  CHECK (decision IS NULL
                                            OR decision IN ('REPLAN_TOMORROW', 'TRY_LATER_TODAY', 'CANCEL')),
    decided_by          VARCHAR(40)  REFERENCES users (id),
    decided_at          TIMESTAMPTZ,
    store_choice        VARCHAR(20)  CHECK (store_choice IS NULL
                                            OR store_choice IN ('REPLAN_TOMORROW', 'TRY_LATER_TODAY', 'CANCEL')),
    store_choice_at     TIMESTAMPTZ,
    -- The driver records this and the dispatcher decides on it: two writers.
    version             INTEGER      NOT NULL DEFAULT 0
);

-- A stop has one live delivery. Undoing one frees the stop for another.
CREATE UNIQUE INDEX uq_deliveries_live_stop ON deliveries (stop_id) WHERE undone_at IS NULL;
CREATE INDEX idx_deliveries_order ON deliveries (order_id);
CREATE INDEX idx_deliveries_vehicle ON deliveries (vehicle_id);

-- Time spent waiting for a store to open (R4w).
CREATE TABLE store_waits (
    id          VARCHAR(40)  PRIMARY KEY,
    stop_id     VARCHAR(40)  NOT NULL REFERENCES stops (id),
    started_at  TIMESTAMPTZ  NOT NULL,
    ended_at    TIMESTAMPTZ,
    minutes     INTEGER,
    note        TEXT,
    client_id   VARCHAR(40)  NOT NULL UNIQUE
);

CREATE INDEX idx_store_waits_stop ON store_waits (stop_id);

CREATE TABLE vehicle_problems (
    id              VARCHAR(40)  PRIMARY KEY,
    vehicle_id      VARCHAR(10)  NOT NULL REFERENCES vehicles (id),
    trip_id         VARCHAR(40)  REFERENCES trips (id),
    kind            VARCHAR(20)  NOT NULL CHECK (kind IN
                            ('BREAKDOWN', 'FRIDGE_FAULT', 'ACCIDENT', 'TYRE', 'OTHER')),
    can_drive       BOOLEAN      NOT NULL,
    fridge_temp_c   NUMERIC(4,1),
    note            TEXT,
    status          VARCHAR(20)  NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'RESOLVED')),
    -- The dispatcher's reply, shown on the same screen (R9ok).
    reply           TEXT,
    reported_by     VARCHAR(40)  REFERENCES users (id),
    reported_at     TIMESTAMPTZ  NOT NULL,
    replied_by      VARCHAR(40)  REFERENCES users (id),
    replied_at      TIMESTAMPTZ,
    client_id       VARCHAR(40)  NOT NULL UNIQUE
);

CREATE INDEX idx_vehicle_problems_vehicle ON vehicle_problems (vehicle_id);
CREATE INDEX idx_vehicle_problems_status ON vehicle_problems (status);

-- One row per action the phone has ever sent us. The primary key is what makes a
-- replay harmless: a second INSERT with the same client_id is simply ignored.
CREATE TABLE sync_log (
    client_id    VARCHAR(40)  PRIMARY KEY,
    user_id      VARCHAR(40)  REFERENCES users (id),
    type         VARCHAR(30)  NOT NULL,
    result       VARCHAR(20)  NOT NULL CHECK (result IN ('APPLIED', 'DUPLICATE', 'CONFLICT')),
    entity_id    VARCHAR(40),
    received_at  TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_sync_log_received ON sync_log (received_at DESC);

-- A delivery made in the field that clashes with a dispatcher's change. The rule is
-- that physical facts win: the delivery stands and the dispatcher decides (D8).
CREATE TABLE conflicts (
    id            VARCHAR(40)  PRIMARY KEY,
    stop_id       VARCHAR(40)  REFERENCES stops (id),
    order_id      VARCHAR(40)  REFERENCES orders (id),
    delivery_id   VARCHAR(40)  REFERENCES deliveries (id),
    details       JSONB        NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'RESOLVED')),
    resolution    VARCHAR(20)  CHECK (resolution IS NULL OR resolution IN ('KEEP_FIELD', 'OVERRIDE')),
    resolved_by   VARCHAR(40)  REFERENCES users (id),
    resolved_at   TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_conflicts_status ON conflicts (status, created_at DESC);

-- Goods handed back at the depot at the end of a trip (R8r).
CREATE TABLE returns (
    id            VARCHAR(40)  PRIMARY KEY,
    trip_id       VARCHAR(40)  NOT NULL REFERENCES trips (id),
    order_id      VARCHAR(40)  NOT NULL REFERENCES orders (id),
    units         INTEGER      NOT NULL CHECK (units > 0),
    reason        VARCHAR(30)  NOT NULL CHECK (reason IN
                        ('STORE_CLOSED', 'NO_ACCESS', 'REFUSED', 'DAMAGED')),
    recorded_by   VARCHAR(40)  REFERENCES users (id),
    recorded_at   TIMESTAMPTZ  NOT NULL,
    client_id     VARCHAR(40)  NOT NULL UNIQUE
);

CREATE INDEX idx_returns_trip ON returns (trip_id);