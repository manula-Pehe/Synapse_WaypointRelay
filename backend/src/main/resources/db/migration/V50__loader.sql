-- Loader records belong to published planning trips and stops. Planning rows stay immutable.
CREATE TABLE trip_loading (
    trip_id VARCHAR(40) PRIMARY KEY REFERENCES trips(id),
    status VARCHAR(20) NOT NULL CHECK (status IN ('WAITING', 'LOADING', 'READY', 'LOADED', 'DEPARTED')),
    loaded_by VARCHAR(40) REFERENCES users(id),
    loaded_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE fridge_checks (
    id VARCHAR(40) PRIMARY KEY,
    trip_id VARCHAR(40) NOT NULL REFERENCES trips(id),
    running BOOLEAN NOT NULL,
    temp_c NUMERIC(4,1) NOT NULL,
    doors_ok BOOLEAN NOT NULL,
    passed BOOLEAN NOT NULL,
    checked_by VARCHAR(40) NOT NULL REFERENCES users(id),
    checked_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_fridge_checks_trip ON fridge_checks(trip_id, checked_at DESC);

CREATE TABLE load_ticks (
    stop_id VARCHAR(40) PRIMARY KEY REFERENCES stops(id),
    ticked_by VARCHAR(40) NOT NULL REFERENCES users(id),
    ticked_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE shortfalls (
    id VARCHAR(40) PRIMARY KEY,
    stop_id VARCHAR(40) NOT NULL UNIQUE REFERENCES stops(id),
    order_id VARCHAR(40) NOT NULL REFERENCES orders(id),
    missing_units INTEGER NOT NULL CHECK (missing_units > 0),
    reason VARCHAR(20) NOT NULL CHECK (reason IN ('MISSING', 'DAMAGED', 'WRONG_ITEM')),
    note TEXT,
    remainder_order_id VARCHAR(40) NOT NULL REFERENCES orders(id),
    reported_by VARCHAR(40) NOT NULL REFERENCES users(id),
    reported_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_shortfalls_stop ON shortfalls(stop_id);
CREATE INDEX idx_shortfalls_order ON shortfalls(order_id);

CREATE TABLE handovers (
    id VARCHAR(40) PRIMARY KEY,
    trip_id VARCHAR(40) NOT NULL UNIQUE REFERENCES trips(id),
    driver_id VARCHAR(40) NOT NULL REFERENCES users(id),
    loader_id VARCHAR(40) NOT NULL REFERENCES users(id),
    cases INTEGER NOT NULL CHECK (cases >= 0),
    weight_kg NUMERIC(10,2) NOT NULL,
    at TIMESTAMPTZ NOT NULL
);
