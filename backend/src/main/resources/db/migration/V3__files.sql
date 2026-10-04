-- Proof photos, signatures and issue photos, kept in the database so the backend stays stateless.
-- Other modules go through FileService.

CREATE TABLE files (
    id            VARCHAR(40)  PRIMARY KEY,                        -- f-<uuid>, never guessable
    kind          VARCHAR(20)  NOT NULL CHECK (kind IN ('PHOTO', 'SIGNATURE')),
    content_type  VARCHAR(60)  NOT NULL CHECK (content_type IN ('image/jpeg', 'image/png', 'image/webp')),
    size_bytes    INTEGER      NOT NULL CHECK (size_bytes > 0 AND size_bytes <= 10485760),
    data          BYTEA        NOT NULL,
    client_id     VARCHAR(40)  UNIQUE,                             -- set by offline uploads; a retry returns the same file
    uploaded_by   VARCHAR(40)  REFERENCES users (id),              -- null = system
    created_at    TIMESTAMPTZ  NOT NULL                            -- demo clock time
);
