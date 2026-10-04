-- Notifications: one row per recipient, so read state is per user.

CREATE TABLE notifications (
    id          VARCHAR(40)  PRIMARY KEY,                          -- e.g. ntf-<uuid>
    user_id     VARCHAR(40)  NOT NULL REFERENCES users (id),       -- recipient
    severity    VARCHAR(10)  NOT NULL CHECK (severity IN ('CRITICAL', 'WARNING', 'INFO')),
    type        VARCHAR(40)  NOT NULL,                             -- e.g. DELIVERY_FAILED, CUTOFF_REMINDER
    title       VARCHAR(200) NOT NULL,
    body        TEXT         NOT NULL,
    link        VARCHAR(200),                                      -- frontend route
    created_at  TIMESTAMPTZ  NOT NULL,                             -- demo clock time
    read_at     TIMESTAMPTZ
);

CREATE INDEX idx_notifications_user_read_created ON notifications (user_id, read_at, created_at DESC);
