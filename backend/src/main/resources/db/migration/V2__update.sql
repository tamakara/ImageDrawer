DROP TABLE IF EXISTS tagger_settings;
CREATE INDEX idx_image_tags_tag_id ON image_tags (tag_id, image_id);