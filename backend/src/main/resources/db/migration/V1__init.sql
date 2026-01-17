CREATE TABLE tags
(
    id   INTEGER PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    type TEXT
);

CREATE TABLE images
(
    id         INTEGER PRIMARY KEY,
    title      TEXT NOT NULL,
    file_name  TEXT,
    extension  TEXT,
    size       BIGINT,
    width      INTEGER,
    height     INTEGER,
    hash       TEXT UNIQUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE image_tags
(
    image_id INTEGER NOT NULL,
    tag_id   INTEGER NOT NULL,
    PRIMARY KEY (image_id, tag_id),
    FOREIGN KEY (image_id) REFERENCES images (id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags (id) ON DELETE CASCADE
);

CREATE TABLE tagger_settings
(
    setting_key   TEXT PRIMARY KEY,
    setting_value TEXT
);

CREATE TABLE system_settings
(
    setting_key   TEXT PRIMARY KEY,
    setting_value TEXT
);

CREATE INDEX idx_tags_name ON tags (name);
CREATE INDEX idx_images_hash ON images (hash);
CREATE INDEX idx_images_created_at ON images (created_at);