package com.thanh0x.coursedeal.model.report

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

/**
 * Entity representing a user-submitted report about an invalid or expired course deal.
 */
@Entity
@Table(name = "course_reports")
@Suppress("LongParameterList")
class CourseReport(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "course_id", nullable = false)
    var courseId: Int = 0,
    @Column(nullable = false, length = 50)
    var reason: String = "",
    @Column(columnDefinition = "TEXT")
    var details: String? = null,
    @Column(name = "reporter_ip", length = 50)
    var reporterIp: String? = null,
    @Column(nullable = false, length = 30)
    var status: String = "PENDING",
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    var createdAt: LocalDateTime? = null,
)
