-- Fix: CHAR(7) maps to bpchar in Postgres but Hibernate expects varchar.
-- Convert both hex columns in categories to VARCHAR(7).

ALTER TABLE categories
    ALTER COLUMN color_hex TYPE VARCHAR(7),
    ALTER COLUMN bg_hex    TYPE VARCHAR(7);
