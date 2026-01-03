-- Create Tags table
CREATE TABLE tags
(
    id   INTEGER PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    type TEXT
);

-- Create Images table
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

-- Create Image-Tags join table
CREATE TABLE image_tags
(
    image_id INTEGER NOT NULL,
    tag_id   INTEGER NOT NULL,
    PRIMARY KEY (image_id, tag_id),
    FOREIGN KEY (image_id) REFERENCES images (id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags (id) ON DELETE CASCADE
);

-- Create Tagger Server Configs table
CREATE TABLE tagger_server_configs
(
    id     INTEGER PRIMARY KEY,
    name   TEXT NOT NULL,
    url    TEXT NOT NULL,
    active BOOLEAN DEFAULT 1
);

-- Create System Settings table
CREATE TABLE system_settings
(
    setting_key   TEXT PRIMARY KEY,
    setting_value TEXT
);

-- Create indexes for better search performance
CREATE INDEX idx_tags_name ON tags (name);
CREATE INDEX idx_images_hash ON images (hash);
CREATE INDEX idx_images_created_at ON images (created_at);
