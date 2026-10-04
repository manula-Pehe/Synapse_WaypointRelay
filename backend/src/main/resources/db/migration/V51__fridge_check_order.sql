-- A loader can re-check within the same demo-clock second; insertion order decides the latest reading.
ALTER TABLE fridge_checks ADD COLUMN recorded_seq BIGINT GENERATED ALWAYS AS IDENTITY;
DROP INDEX idx_fridge_checks_trip;
CREATE INDEX idx_fridge_checks_latest ON fridge_checks(trip_id, recorded_seq DESC);
