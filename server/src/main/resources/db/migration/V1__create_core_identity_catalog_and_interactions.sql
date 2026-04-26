CREATE EXTENSION IF NOT EXISTS citext;

CREATE SEQUENCE IF NOT EXISTS users_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS items_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS user_item_interactions_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS ingestion_runs_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS ingestion_errors_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE users (
    id BIGINT PRIMARY KEY DEFAULT nextval('users_seq'),
    uuid UUID NOT NULL,
    email CITEXT,
    username VARCHAR(64),
    anilist_username VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    CONSTRAINT uk_users_uuid UNIQUE (uuid),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT chk_users_status CHECK (status IN ('active', 'disabled', 'pending'))
);

CREATE INDEX idx_users_anilist_username ON users (anilist_username);

CREATE TABLE items (
    id BIGINT PRIMARY KEY DEFAULT nextval('items_seq'),
    external_source VARCHAR(32) NOT NULL DEFAULT 'anilist',
    external_id VARCHAR(64) NOT NULL,
    media_type VARCHAR(16) NOT NULL,
    canonical_title TEXT NOT NULL,
    title_english TEXT,
    title_romaji TEXT,
    title_native TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_items_external_source_external_id UNIQUE (external_source, external_id),
    CONSTRAINT chk_items_media_type CHECK (media_type IN ('anime', 'manga'))
);

CREATE INDEX idx_items_media_type_is_active ON items (media_type, is_active);

CREATE TABLE item_metadata (
    item_id BIGINT PRIMARY KEY,
    genres TEXT[] NOT NULL DEFAULT '{}',
    tags TEXT[] NOT NULL DEFAULT '{}',
    studios TEXT[] NOT NULL DEFAULT '{}',
    authors TEXT[] NOT NULL DEFAULT '{}',
    format VARCHAR(32),
    status VARCHAR(32),
    episodes_or_chapters INTEGER,
    year_start INTEGER,
    synopsis TEXT,
    metadata_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    metadata_version VARCHAR(64) NOT NULL DEFAULT 'v1',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_item_metadata_item FOREIGN KEY (item_id) REFERENCES items (id) ON DELETE CASCADE
);

CREATE INDEX idx_item_metadata_genres ON item_metadata USING GIN (genres);
CREATE INDEX idx_item_metadata_tags ON item_metadata USING GIN (tags);
CREATE INDEX idx_item_metadata_metadata_json ON item_metadata USING GIN (metadata_json);

CREATE TABLE user_item_interactions (
    id BIGINT PRIMARY KEY DEFAULT nextval('user_item_interactions_seq'),
    user_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    progress NUMERIC(7, 2),
    rating NUMERIC(4, 2),
    interaction_timestamp TIMESTAMPTZ NOT NULL,
    source VARCHAR(32) NOT NULL,
    source_event_id VARCHAR(128),
    source_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    inserted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_user_item_interactions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_item_interactions_item FOREIGN KEY (item_id) REFERENCES items (id) ON DELETE CASCADE,
    CONSTRAINT chk_user_item_interactions_status CHECK (status IN ('planned', 'in_progress', 'completed', 'dropped', 'paused')),
    CONSTRAINT chk_user_item_interactions_source CHECK (source IN ('anilist_import', 'explicit_feedback', 'system_backfill')),
    CONSTRAINT chk_user_item_interactions_rating CHECK (rating IS NULL OR (rating >= 0 AND rating <= 10)),
    CONSTRAINT uk_user_item_interactions_source_event UNIQUE (source, source_event_id)
);

CREATE UNIQUE INDEX idx_user_item_interactions_fallback_dedupe
    ON user_item_interactions (user_id, item_id, status, interaction_timestamp, source)
    WHERE source_event_id IS NULL;

CREATE INDEX idx_user_item_interactions_user_timestamp
    ON user_item_interactions (user_id, interaction_timestamp DESC);
CREATE INDEX idx_user_item_interactions_item_timestamp
    ON user_item_interactions (item_id, interaction_timestamp DESC);
CREATE INDEX idx_user_item_interactions_status
    ON user_item_interactions (status);

CREATE TABLE ingestion_runs (
    id BIGINT PRIMARY KEY DEFAULT nextval('ingestion_runs_seq'),
    run_id UUID NOT NULL,
    user_id BIGINT,
    source VARCHAR(32) NOT NULL DEFAULT 'anilist',
    status VARCHAR(32) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    summary_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    error_count INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_ingestion_runs_run_id UNIQUE (run_id),
    CONSTRAINT fk_ingestion_runs_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
);

CREATE TABLE ingestion_errors (
    id BIGINT PRIMARY KEY DEFAULT nextval('ingestion_errors_seq'),
    ingestion_run_id BIGINT NOT NULL,
    error_code VARCHAR(64) NOT NULL,
    error_message TEXT NOT NULL,
    payload_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_ingestion_errors_ingestion_run
        FOREIGN KEY (ingestion_run_id) REFERENCES ingestion_runs (id) ON DELETE CASCADE
);

CREATE INDEX idx_ingestion_errors_ingestion_run_id ON ingestion_errors (ingestion_run_id);
