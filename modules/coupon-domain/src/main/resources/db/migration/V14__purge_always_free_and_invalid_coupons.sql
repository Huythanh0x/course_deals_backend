-- Migration V14: Archive and purge Always-Free courses and invalid 2030-expiry coupons
-- Rejects rows where uses_remaining <= 0, coupon_code is null/blank, or expired_date >= 2030-01-01

-- Step 1: Archive invalid/Always-Free rows into expired_course_data
INSERT IGNORE INTO expired_course_data (coupon_url, course_id, title, created_at, updated_at)
SELECT
    coupon_url,
    course_id,
    title,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM coupon_course_data
WHERE coupon_url IS NOT NULL
  AND coupon_url <> ''
  AND (
      uses_remaining IS NULL
      OR uses_remaining <= 0
      OR coupon_code IS NULL
      OR coupon_code = ''
      OR expired_date >= '2030-01-01 00:00:00'
  );

-- Step 2: Delete invalid/Always-Free rows from coupon_course_data
DELETE FROM coupon_course_data
WHERE uses_remaining IS NULL
   OR uses_remaining <= 0
   OR coupon_code IS NULL
   OR coupon_code = ''
   OR expired_date >= '2030-01-01 00:00:00';

-- Step 3: Clean up any trailing query fragments (&...) on coupon_code in remaining rows
UPDATE coupon_course_data
SET coupon_code = SUBSTRING_INDEX(coupon_code, '&', 1)
WHERE coupon_code LIKE '%&%';
