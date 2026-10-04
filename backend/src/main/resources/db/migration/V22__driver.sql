-- V22 - the driver's signature on goods handed back at the depot (F11, US-11.2).
--
-- "Hand back returned goods with a signature, so that responsibility is clear": the reason and the
-- driver say what happened, the signature is who stood there and signed for it. Nullable because a
-- handback queued from a phone already out of reach of the depot cannot capture one, and losing the
-- handback record to a missing signature would be the worse outcome.

ALTER TABLE returns
    ADD COLUMN signature_file_id VARCHAR(40) REFERENCES files (id);
