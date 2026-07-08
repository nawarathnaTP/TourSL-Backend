-- Rename PENDING to PENDING_PAYMENT in booking status
UPDATE booking SET status = 'PENDING_PAYMENT' WHERE status = 'PENDING';
