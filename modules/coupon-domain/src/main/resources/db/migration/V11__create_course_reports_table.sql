-- Migration: Create course_reports table
CREATE TABLE IF NOT EXISTS course_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id INT NOT NULL,
    reason VARCHAR(50) NOT NULL,
    details TEXT NULL,
    reporter_ip VARCHAR(50) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_course_reports_course_id (course_id),
    INDEX idx_course_reports_status (status)
);
