-- Migration: Add fulltext index on coupon_course_data (title, heading, description)
-- Enables efficient fulltext searching in MySQL while fallback/Specification remains H2 compatible.

CREATE FULLTEXT INDEX idx_coupon_course_fulltext ON coupon_course_data(title, heading, description);
