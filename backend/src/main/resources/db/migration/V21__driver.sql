-- V21 - how much stock is still on the truck when the driver reports a problem (F10, US-10.1).
--
-- Chethiya's breakdown re-plan (D6b) moves the stops this vehicle cannot reach, so the number
-- dispatch re-plans against is the cases left on board. It was missing: a report said what broke
-- but not what was stranded. Nullable because a problem can be reported with no load - a breakdown
-- on the way back to the depot - and there is nothing to hand over in that case.

ALTER TABLE vehicle_problems
    ADD COLUMN units_on_board INTEGER
    CHECK (units_on_board IS NULL OR units_on_board >= 0);
