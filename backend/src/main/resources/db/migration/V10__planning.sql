-- V10 · Planning tables (plans, trips, stops, deferrals, deferral choices)
-- See docs/data-model.md "Planning (V10–V19)". References core tables only.

CREATE TABLE plans (
    id                  VARCHAR(40)  PRIMARY KEY,
    run_date            DATE         NOT NULL,
    depot               VARCHAR(20)  NOT NULL,
    version             INTEGER      NOT NULL,
    status              VARCHAR(20)  NOT NULL CHECK (status IN ('DRAFT', 'PUBLISHED', 'SUPERSEDED')),
    summary             JSONB        NOT NULL DEFAULT '{}'::jsonb,
    parent_plan_id      VARCHAR(40)  REFERENCES plans (id),
    revise_reason       TEXT,
    created_at          TIMESTAMPTZ  NOT NULL,
    created_by          VARCHAR(40)  REFERENCES users (id),
    published_at        TIMESTAMPTZ,
    published_by        VARCHAR(40)  REFERENCES users (id),
    version_lock        INTEGER      NOT NULL DEFAULT 0,
    UNIQUE (run_date, depot, version)
);

CREATE TABLE trips (
    id                  VARCHAR(40)  PRIMARY KEY,
    plan_id             VARCHAR(40)  NOT NULL REFERENCES plans (id),
    vehicle_id          VARCHAR(10)  NOT NULL REFERENCES vehicles (id),
    trip_no             SMALLINT     NOT NULL CHECK (trip_no IN (1, 2)),
    brand               VARCHAR(10)  NOT NULL CHECK (brand IN ('Fresh', 'Style', 'Tech')),
    district            VARCHAR(40)  NOT NULL,
    window_type         VARCHAR(10)  NOT NULL CHECK (window_type IN ('FRESH', 'DAYTIME')),
    depart_at           TIMESTAMPTZ  NOT NULL,
    minutes             INTEGER      NOT NULL,
    weight_kg           NUMERIC(10,2) NOT NULL,
    volume_m3           NUMERIC(10,3) NOT NULL,
    km                  NUMERIC(8,2) NOT NULL,
    UNIQUE (plan_id, vehicle_id, trip_no)
);
CREATE INDEX idx_trips_plan ON trips (plan_id);

CREATE TABLE stops (
    id                  VARCHAR(40)  PRIMARY KEY,
    trip_id             VARCHAR(40)  NOT NULL REFERENCES trips (id),
    order_id            VARCHAR(40)  NOT NULL REFERENCES orders (id),
    seq                 SMALLINT     NOT NULL,                  -- delivery order
    load_seq            SMALLINT     NOT NULL,                  -- loading order (reverse of delivery)
    arrive_from         TIMESTAMPTZ  NOT NULL,
    arrive_to           TIMESTAMPTZ  NOT NULL,
    late_risk           NUMERIC(4,3) NOT NULL CHECK (late_risk >= 0 AND late_risk <= 1),
    UNIQUE (trip_id, seq)
);
CREATE INDEX idx_stops_order ON stops (order_id);

CREATE TABLE deferrals (
    id                  VARCHAR(40)  PRIMARY KEY,
    plan_id             VARCHAR(40)  NOT NULL REFERENCES plans (id),
    order_id            VARCHAR(40)  NOT NULL REFERENCES orders (id),
    kind                VARCHAR(20)  NOT NULL CHECK (kind IN ('UNAVOIDABLE', 'CHOSEN')),
    rule                VARCHAR(30)  NOT NULL CHECK (rule IN (
                            'BRAND_DISTRICT', 'FRIDGE_REQUIRED', 'VAN_ONLY', 'WRONG_DEPOT', 'OVER_WEIGHT',
                            'OVER_VOLUME', 'MAX_TRIPS', 'TIME_BUDGET', 'FUEL_QUOTA', 'WINDOW',
                            'NO_VEHICLE_FITS', 'FRIDGE_CAPACITY')),
    reason              TEXT         NOT NULL,
    priority_score      INTEGER      NOT NULL,
    days_waited         INTEGER      NOT NULL,
    new_date            DATE         NOT NULL,
    needs_decision      BOOLEAN      NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_deferrals_plan ON deferrals (plan_id);
CREATE INDEX idx_deferrals_order ON deferrals (order_id);

CREATE TABLE deferral_choices (
    id                  VARCHAR(40)  PRIMARY KEY,
    deferral_id         VARCHAR(40)  NOT NULL REFERENCES deferrals (id),
    choice              VARCHAR(10)  NOT NULL CHECK (choice IN ('KEEP', 'REDUCE', 'CANCEL', 'SPLIT')),
    units               INTEGER,
    chosen_by           VARCHAR(40)  NOT NULL REFERENCES users (id),
    chosen_at           TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_deferral_choices_deferral ON deferral_choices (deferral_id);
