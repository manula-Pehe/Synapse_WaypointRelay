CREATE TABLE receipts (
    id VARCHAR(40) PRIMARY KEY,
    order_id VARCHAR(40) NOT NULL UNIQUE REFERENCES orders(id),
    received_units INTEGER NOT NULL CHECK (received_units >= 0),
    note TEXT,
    received_by VARCHAR(40) REFERENCES users(id),
    received_at TIMESTAMPTZ NOT NULL,
    auto_closed BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE issues (
    id VARCHAR(40) PRIMARY KEY,
    ref VARCHAR(20) NOT NULL UNIQUE,
    outlet_id VARCHAR(10) NOT NULL REFERENCES outlets(id),
    order_id VARCHAR(40) REFERENCES orders(id),
    type VARCHAR(20) NOT NULL CHECK (type IN ('DAMAGED','MISSING','WRONG_ITEM','LATE','OTHER')),
    units INTEGER CHECK (units > 0),
    wants VARCHAR(20) NOT NULL CHECK (wants IN ('REPLACE','CREDIT','NOTHING')),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','ANSWERED','RESOLVED')),
    created_by VARCHAR(40) NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL,
    resolved_by VARCHAR(40) REFERENCES users(id),
    resolved_at TIMESTAMPTZ,
    version INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_issues_outlet_status ON issues(outlet_id, status);

CREATE TABLE issue_messages (
    id VARCHAR(40) PRIMARY KEY,
    issue_id VARCHAR(40) NOT NULL REFERENCES issues(id),
    author_id VARCHAR(40) NOT NULL REFERENCES users(id),
    text TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_issue_messages_issue ON issue_messages(issue_id, created_at);

CREATE TABLE issue_photos (
    id VARCHAR(40) PRIMARY KEY,
    issue_id VARCHAR(40) NOT NULL REFERENCES issues(id),
    content_type VARCHAR(30) NOT NULL,
    content BYTEA NOT NULL,
    uploaded_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE order_disputes (
    id VARCHAR(40) PRIMARY KEY,
    order_id VARCHAR(40) NOT NULL REFERENCES orders(id),
    outlet_id VARCHAR(10) NOT NULL REFERENCES outlets(id),
    message TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','RESOLVED')),
    created_by VARCHAR(40) NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ
);
