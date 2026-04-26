UPDATE users
SET status = UPPER(status)
WHERE status IS NOT NULL;

ALTER TABLE users
    ALTER COLUMN status SET DEFAULT 'ACTIVE';

ALTER TABLE users
    DROP CONSTRAINT IF EXISTS chk_users_status;

ALTER TABLE users
    ADD CONSTRAINT chk_users_status
        CHECK (status IN ('ACTIVE', 'DISABLED', 'PENDING'));

UPDATE items
SET media_type = UPPER(media_type)
WHERE media_type IS NOT NULL;

ALTER TABLE items
    DROP CONSTRAINT IF EXISTS chk_items_media_type;

ALTER TABLE items
    ADD CONSTRAINT chk_items_media_type
        CHECK (media_type IN ('ANIME', 'MANGA'));

UPDATE user_item_interactions
SET status = UPPER(status)
WHERE status IS NOT NULL;

UPDATE user_item_interactions
SET source = UPPER(source)
WHERE source IS NOT NULL;

ALTER TABLE user_item_interactions
    DROP CONSTRAINT IF EXISTS chk_user_item_interactions_status;

ALTER TABLE user_item_interactions
    ADD CONSTRAINT chk_user_item_interactions_status
        CHECK (status IN ('PLANNED', 'IN_PROGRESS', 'COMPLETED', 'DROPPED', 'PAUSED'));

ALTER TABLE user_item_interactions
    DROP CONSTRAINT IF EXISTS chk_user_item_interactions_source;

ALTER TABLE user_item_interactions
    ADD CONSTRAINT chk_user_item_interactions_source
        CHECK (source IN ('ANILIST_IMPORT', 'EXPLICIT_FEEDBACK', 'SYSTEM_BACKFILL'));

UPDATE ingestion_runs
SET source = UPPER(source)
WHERE source IS NOT NULL;

UPDATE ingestion_runs
SET status = UPPER(status)
WHERE status IS NOT NULL;

ALTER TABLE ingestion_runs
    ALTER COLUMN source SET DEFAULT 'ANILIST';
