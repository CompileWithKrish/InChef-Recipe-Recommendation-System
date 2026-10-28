-- Migration: V2 Add Recipe Image Retrieval Metadata Columns and Indexes
-- Target Database: MySQL 8 (inchef_db)

ALTER TABLE recipes 
    ADD COLUMN image_source_url VARCHAR(500) NULL,
    ADD COLUMN image_source_type VARCHAR(50) NULL,
    ADD COLUMN image_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN image_last_checked_at DATETIME(6) NULL,
    ADD COLUMN image_content_hash VARCHAR(100) NULL;

CREATE INDEX idx_recipe_image_status ON recipes(image_status);

-- Mark already populated image URLs as RESOLVED from WEBPAGE_METADATA
UPDATE recipes 
SET image_status = 'RESOLVED',
    image_source_type = 'WEBPAGE_METADATA',
    image_source_url = source_url,
    image_last_checked_at = NOW()
WHERE image_url IS NOT NULL 
  AND TRIM(image_url) != '' 
  AND image_url NOT LIKE '%neutral_recipe_placeholder%'
  AND (image_status IS NULL OR image_status = 'PENDING');
