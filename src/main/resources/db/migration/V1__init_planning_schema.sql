-- 1. tourist
CREATE TABLE tourist (
    user_id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email       VARCHAR(255) NOT NULL UNIQUE,
    first_name  VARCHAR(255) NOT NULL,
    last_name   VARCHAR(255) NOT NULL,
    picture_url VARCHAR(512),
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);

-- 2. accounts
CREATE TABLE accounts (
    acc_id           UUID PRIMARY KEY DEFAULT gen_random_uuid() ,
    user_id          UUID NOT NULL REFERENCES tourist(user_id),
    provider         VARCHAR(50) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    password         VARCHAR(255),
    created_at       TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);
CREATE INDEX idx_accounts_user_id ON accounts(user_id);

-- 3. transport_options
CREATE TABLE transport_options (
    transport_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type         VARCHAR(100) NOT NULL,
    label        VARCHAR(255) NOT NULL
);

-- 4. tour
CREATE TABLE tour (
    tour_id    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES tourist(user_id),
    start_day  DATE NOT NULL,
    end_day    DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);
CREATE INDEX idx_tour_user_id ON tour(user_id);

-- 5. days
CREATE TABLE days (
    day_id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tour_id    UUID NOT NULL REFERENCES tour(tour_id),
    day_no     INT NOT NULL,
    date       DATE NOT NULL,
    lodging_id UUID
);
CREATE INDEX idx_days_tour_id ON days(tour_id);

-- 6. locations
CREATE TABLE locations (
    location_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_id VARCHAR(255),
    place_name  VARCHAR(255) NOT NULL,
    latitude    DECIMAL NOT NULL,
    longitude   DECIMAL NOT NULL,
    image_url   VARCHAR(512)
);

-- 7. stops
CREATE TABLE stops (
    stop_id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    day_id      UUID NOT NULL REFERENCES days(day_id),
    location_id UUID NOT NULL REFERENCES locations(location_id),
    stop_order  INT NOT NULL,
    duration    INT NOT NULL,
    CONSTRAINT uq_stops_day_order UNIQUE (day_id, stop_order)
);
CREATE INDEX idx_stops_day_id ON stops(day_id);
CREATE INDEX idx_stops_location_id ON stops(location_id);

-- 8. activities
CREATE TABLE activities (
    activity_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    stop_id     UUID NOT NULL REFERENCES stops(stop_id),
    duration    INT NOT NULL,
    description TEXT NOT NULL,
    booking_id  UUID
);
CREATE INDEX idx_activities_stop_id ON activities(stop_id);

-- 9. route
CREATE TABLE route (
    route_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    start_stop_id UUID NOT NULL REFERENCES stops(stop_id),
    end_stop_id   UUID NOT NULL REFERENCES stops(stop_id),
    CONSTRAINT uq_route_start_end UNIQUE (start_stop_id, end_stop_id)
);
CREATE INDEX idx_route_start_stop_id ON route(start_stop_id);
CREATE INDEX idx_route_end_stop_id ON route(end_stop_id);

-- 10. route_options
CREATE TABLE route_options (
    option_id    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    route_id     UUID NOT NULL REFERENCES route(route_id),
    transport_id UUID NOT NULL REFERENCES transport_options(transport_id),
    distance     DECIMAL NOT NULL,
    time         INT NOT NULL,
    cost         DECIMAL NOT NULL,
    is_selected  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    selected_at  TIMESTAMP WITH TIME ZONE
);
CREATE INDEX idx_route_options_route_id ON route_options(route_id);
CREATE INDEX idx_route_options_transport_id ON route_options(transport_id);
