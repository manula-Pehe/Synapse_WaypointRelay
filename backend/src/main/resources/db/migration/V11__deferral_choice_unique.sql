-- V11 · A deferral takes one store choice. Replaces the plain index; the service checks first and
-- this stops two simultaneous requests from both inserting.
DROP INDEX idx_deferral_choices_deferral;
CREATE UNIQUE INDEX uq_deferral_choices_deferral ON deferral_choices (deferral_id);
