---
name: scan-related-code
description: Find the files and modules relevant to a ticket's requirements and record them into its workspace, before any implementation starts. Use after read-requirements, standalone or as part of ticket-flow.
---

# Scan related code

1. Read the ticket workspace's Requirements section — it must already exist and be
   filled in (run `read-requirements` first if not).
2. Narrow scope fast using `AGENTS.md`'s module map — identify which of the 9 backend
   modules (e.g. `coupon-domain`, `coupon-api-service`, `coupon-crawler-service`, `course-scraper`)
   are touched by the requirements.
3. Record findings in the workspace's Code scan section as a short list — file path +
   one-line "why this matters", not a full exploration transcript:
   ```markdown
   - `modules/coupon-domain/.../CouponCourseRepository.kt` — query needed for filtering
   - `modules/coupon-api-service/.../CouponCourseController.kt` — REST endpoint mapping
   ```
4. Note any existing architecture patterns to adhere to (e.g., separation of discovery vs.
   processing, JobRunr enqueueing, Flyway migrations for entity alterations).
5. Append one line to Progress log: `<date> scanned code, N files found relevant`.

## Output

Report the code-scan list — this is what `confirm-edge-cases` and `implement-change`
build on next.
