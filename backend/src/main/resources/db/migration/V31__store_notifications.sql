CREATE TABLE store_notification_settings (
    user_id VARCHAR(40) PRIMARY KEY REFERENCES users(id),
    deliveries BOOLEAN NOT NULL DEFAULT TRUE,
    orders BOOLEAN NOT NULL DEFAULT TRUE,
    issues BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE store_notification_reads (
    user_id VARCHAR(40) NOT NULL REFERENCES users(id),
    notification_id VARCHAR(160) NOT NULL,
    PRIMARY KEY (user_id, notification_id)
);
