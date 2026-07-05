-- Hibernate maps Java Double → float(53) / DOUBLE PRECISION.
-- NUMERIC(4,3) in the original migration causes schema-validation failure.
ALTER TABLE comments
    ALTER COLUMN spam_score TYPE DOUBLE PRECISION USING spam_score::DOUBLE PRECISION;
