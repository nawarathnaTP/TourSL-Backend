CREATE TABLE booking (
    booking_id  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    package_id  UUID NOT NULL REFERENCES guide_tour_package(package_id),
    tourist_id  UUID NOT NULL REFERENCES users(user_id),
    slots_booked INT NOT NULL,
    total_price DECIMAL NOT NULL,
    status      VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    booked_at   TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);
CREATE INDEX idx_booking_package_id ON booking(package_id);
CREATE INDEX idx_booking_tourist_id ON booking(tourist_id);
CREATE INDEX idx_booking_status ON booking(status);
