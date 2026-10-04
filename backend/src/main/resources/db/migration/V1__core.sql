-- V1 · Core tables
-- Reference data is loaded at startup by the seed loader from ./data (never committed).
-- Conventions: ids are strings, enums are UPPER_CASE text with CHECK constraints,
-- times are timestamptz (UTC), outlet windows are local wall-clock TIME.

-- ───────────────────────── Reference data ─────────────────────────

CREATE TABLE outlets (
    id                  VARCHAR(10)  PRIMARY KEY,               -- OUT001
    brand               VARCHAR(10)  NOT NULL CHECK (brand IN ('Fresh', 'Style', 'Tech')),
    district            VARCHAR(40)  NOT NULL,
    depot               VARCHAR(20)  NOT NULL,                  -- Peliyagoda | Kandy
    dock_type           VARCHAR(20)  NOT NULL CHECK (dock_type IN ('rear_dock', 'street', 'mall_bay')),
    parking_constraint  VARCHAR(20)  NOT NULL CHECK (parking_constraint IN ('normal', 'van_only', 'mall_dock')),
    mall_window_open    TIME,
    mall_window_close   TIME,
    window_open         TIME         NOT NULL,
    window_close        TIME         NOT NULL
);
CREATE INDEX idx_outlets_depot ON outlets (depot);

CREATE TABLE vehicles (
    id                  VARCHAR(10)  PRIMARY KEY,               -- VEH036
    type                VARCHAR(10)  NOT NULL CHECK (type IN ('truck', 'van')),
    temp                VARCHAR(10)  NOT NULL CHECK (temp IN ('reefer', 'ambient')),
    weight_cap_kg       NUMERIC(10,2) NOT NULL,
    volume_cap_m3       NUMERIC(10,3) NOT NULL,
    fuel_type           VARCHAR(20)  NOT NULL,
    km_per_l            NUMERIC(6,2) NOT NULL,
    weekly_fuel_quota_l NUMERIC(10,2) NOT NULL,
    depot               VARCHAR(20)  NOT NULL
);
CREATE INDEX idx_vehicles_depot ON vehicles (depot);

CREATE TABLE district_travel (
    district            VARCHAR(40)  NOT NULL,
    depot               VARCHAR(20)  NOT NULL,
    road_class          VARCHAR(20)  NOT NULL,
    free_flow_kmh       NUMERIC(6,2) NOT NULL,
    depot_to_district_km NUMERIC(8,2) NOT NULL,
    outbound_min        INTEGER      NOT NULL,                  -- depot_to_district_freeflow_min
    inter_stop_km       NUMERIC(8,2) NOT NULL,
    inter_stop_min      INTEGER      NOT NULL,                  -- inter_stop_freeflow_min
    PRIMARY KEY (district, depot)
);

CREATE TABLE service_allowance (
    brand               VARCHAR(10)  NOT NULL,
    dock_type           VARCHAR(20)  NOT NULL,
    minutes             INTEGER      NOT NULL,
    PRIMARY KEY (brand, dock_type)
);

-- ───────────────────────── People ─────────────────────────

CREATE TABLE users (
    id                  VARCHAR(40)  PRIMARY KEY,               -- e.g. usr-dilani
    name                VARCHAR(100) NOT NULL,
    role                VARCHAR(20)  NOT NULL CHECK (role IN ('STORE_MANAGER', 'DISPATCHER', 'LOADER', 'DRIVER')),
    email               VARCHAR(120) UNIQUE,                    -- store manager, dispatcher
    staff_id            VARCHAR(20)  UNIQUE,                    -- driver (DRV-0036)
    secret_hash         VARCHAR(100) NOT NULL,                  -- BCrypt of password or PIN
    outlet_id           VARCHAR(10)  REFERENCES outlets (id),   -- store manager
    depot               VARCHAR(20),                            -- loader, dispatcher (null = all depots)
    vehicle_id          VARCHAR(10)  REFERENCES vehicles (id),  -- driver
    language            VARCHAR(2)   NOT NULL DEFAULT 'en' CHECK (language IN ('en', 'si', 'ta')),
    theme               VARCHAR(10)  NOT NULL DEFAULT 'system' CHECK (theme IN ('system', 'light', 'dark')),
    active              BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ───────────────────────── Fleet and runs ─────────────────────────

CREATE TABLE vehicle_availability (
    vehicle_id          VARCHAR(10)  NOT NULL REFERENCES vehicles (id),
    run_date            DATE         NOT NULL,
    status              VARCHAR(20)  NOT NULL CHECK (status IN ('AVAILABLE', 'IN_WORKSHOP', 'OFF_ROAD')),
    reason              VARCHAR(200),
    updated_by          VARCHAR(40)  REFERENCES users (id),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (vehicle_id, run_date)
);

CREATE TABLE fuel_usage (
    vehicle_id          VARCHAR(10)  NOT NULL REFERENCES vehicles (id),
    iso_year            INTEGER      NOT NULL,
    iso_week            INTEGER      NOT NULL,
    litres_used         NUMERIC(10,2) NOT NULL DEFAULT 0,
    PRIMARY KEY (vehicle_id, iso_year, iso_week)
);

-- One row per run date and depot: order cut-off and fleet confirmation.
CREATE TABLE order_runs (
    run_date            DATE         NOT NULL,
    depot               VARCHAR(20)  NOT NULL,
    orders_closed_at    TIMESTAMPTZ,
    orders_closed_by    VARCHAR(40)  REFERENCES users (id),
    fleet_confirmed_at  TIMESTAMPTZ,
    fleet_confirmed_by  VARCHAR(40)  REFERENCES users (id),
    PRIMARY KEY (run_date, depot)
);

-- ───────────────────────── Orders ─────────────────────────

CREATE TABLE orders (
    id                  VARCHAR(40)  PRIMARY KEY,               -- uuid string
    ref                 VARCHAR(40)  NOT NULL UNIQUE,           -- S1-001, ORD-1001-012C, S1-001-R
    outlet_id           VARCHAR(10)  NOT NULL REFERENCES outlets (id),
    brand               VARCHAR(10)  NOT NULL,
    temp_requirement    VARCHAR(10)  NOT NULL CHECK (temp_requirement IN ('CHILLED', 'AMBIENT')),
    units               INTEGER      NOT NULL CHECK (units >= 0),
    weight_kg           NUMERIC(10,2) NOT NULL,
    volume_m3           NUMERIC(10,3) NOT NULL,
    run_date            DATE         NOT NULL,                  -- delivery date (changes when MOVED)
    status              VARCHAR(20)  NOT NULL CHECK (status IN (
                            'PREPARED', 'CONFIRMED', 'PLANNED', 'LOADED', 'ON_THE_WAY',
                            'DELIVERED', 'PARTIAL', 'FAILED', 'MOVED', 'CANCELLED')),
    source              VARCHAR(20)  NOT NULL CHECK (source IN ('SEED', 'HISTORY', 'STORE', 'PHONE_IN', 'REMAINDER')),
    auto_confirm        BOOLEAN      NOT NULL DEFAULT FALSE,
    days_since_last_served INTEGER   NOT NULL DEFAULT 1,
    deferred_yesterday  BOOLEAN      NOT NULL DEFAULT FALSE,
    parent_order_id     VARCHAR(40)  REFERENCES orders (id),    -- remainder / split parent
    store_checked       BOOLEAN      NOT NULL DEFAULT TRUE,     -- FALSE for phone-in orders until the store checks (S2e)
    confirmed_at        TIMESTAMPTZ,
    confirmed_by        VARCHAR(40)  REFERENCES users (id),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version             INTEGER      NOT NULL DEFAULT 0         -- optimistic locking
);
CREATE INDEX idx_orders_run_status ON orders (run_date, status);
CREATE INDEX idx_orders_outlet ON orders (outlet_id, run_date);

CREATE TABLE order_events (
    id                  BIGSERIAL    PRIMARY KEY,
    order_id            VARCHAR(40)  NOT NULL REFERENCES orders (id),
    at                  TIMESTAMPTZ  NOT NULL,                  -- DemoClock time
    actor_user_id       VARCHAR(40)  REFERENCES users (id),     -- null = system / job
    type                VARCHAR(40)  NOT NULL,                  -- PREPARED, EDITED, CONFIRMED, CANCELLED, PLANNED, MOVED, LOADED, …
    from_status         VARCHAR(20),
    to_status           VARCHAR(20),
    details             JSONB        NOT NULL DEFAULT '{}'::jsonb
);
CREATE INDEX idx_order_events_order ON order_events (order_id, at);

-- ───────────────────────── Settings ─────────────────────────

CREATE TABLE app_settings (
    key                 VARCHAR(60)  PRIMARY KEY,               -- run_date, clock_offset_seconds, …
    value               VARCHAR(200) NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);
