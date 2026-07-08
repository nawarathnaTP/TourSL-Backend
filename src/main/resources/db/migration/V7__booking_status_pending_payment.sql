-- Rename PENDING to PENDING_PAYMENT in booking status
UPDATE booking SET status = 'PENDING_PAYMENT' WHERE status = 'PENDING';

-- Add payment deadline column
ALTER TABLE booking ADD COLUMN payment_deadline TIMESTAMP WITH TIME ZONE;

-- Backfill existing rows with 15 minutes from booked_at
UPDATE booking SET payment_deadline = booked_at + INTERVAL '15 minutes';

ALTER TABLE booking ALTER COLUMN payment_deadline SET NOT NULL;
