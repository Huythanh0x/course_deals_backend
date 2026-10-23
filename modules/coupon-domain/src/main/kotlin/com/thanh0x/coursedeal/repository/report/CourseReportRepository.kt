package com.thanh0x.coursedeal.repository.report

import com.thanh0x.coursedeal.model.report.CourseReport
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CourseReportRepository : JpaRepository<CourseReport, Long> {
    fun findByCourseId(courseId: Int): List<CourseReport>

    fun findByStatus(status: String): List<CourseReport>
}
