-- Add title and tour_type to tour table
ALTER TABLE tour ADD COLUMN title VARCHAR(255);
ALTER TABLE tour ADD COLUMN tour_type VARCHAR(50) NOT NULL DEFAULT 'TOURIST';

-- Create guide_tour_package table
CREATE TABLE guide_tour_package (
    package_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tour_id         UUID NOT NULL UNIQUE REFERENCES tour(tour_id),
    description     TEXT,
    cover_image_url VARCHAR(512),
    max_slots       INT,
    available_slots INT,
    price_per_slot  DECIMAL,
    is_published    BOOLEAN NOT NULL DEFAULT FALSE,
    status          VARCHAR(50) NOT NULL DEFAULT 'DRAFT'
);
CREATE INDEX idx_guide_tour_package_tour_id ON guide_tour_package(tour_id);
CREATE INDEX idx_guide_tour_package_status ON guide_tour_package(status);
CREATE INDEX idx_guide_tour_package_published ON guide_tour_package(is_published);
