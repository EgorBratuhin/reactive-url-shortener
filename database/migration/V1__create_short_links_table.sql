CREATE TABLE short_links (
    id uuid PRIMARY KEY,
    short_code varchar(22) NOT NULL,
    original_url text NOT NULL,
    created_at timestamp with time zone NOT NULL,
    expires_at timestamp with time zone NULL
);

CREATE UNIQUE INDEX idx_short_links_code ON short_links (short_code DESC);

CREATE INDEX idx_short_links_expires_at ON short_links (expires_at);
