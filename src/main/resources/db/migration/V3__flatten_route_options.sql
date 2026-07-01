-- Remove route_options table and flatten selected route data into route table

DROP INDEX IF EXISTS idx_route_options_route_id;
DROP INDEX IF EXISTS idx_route_options_transport_id;
DROP TABLE IF EXISTS route_options;

ALTER TABLE route ADD COLUMN transport_id UUID REFERENCES transport_options(transport_id);
ALTER TABLE route ADD COLUMN distance DECIMAL;
ALTER TABLE route ADD COLUMN time INT;
ALTER TABLE route ADD COLUMN cost DECIMAL;
ALTER TABLE route ADD COLUMN created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL;
ALTER TABLE route ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL;

CREATE INDEX idx_route_transport_id ON route(transport_id);
