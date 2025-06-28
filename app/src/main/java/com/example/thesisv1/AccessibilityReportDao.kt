// File: AccessibilityReportDao.kt
package com.example.thesisv1.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface AccessibilityReportDao {
    @Insert
    suspend fun insertReport(report: AccessibilityReport)

    @Query("SELECT * FROM accessibility_reports WHERE disasterProfileId = :profileId")
    suspend fun getReportsByProfileId(profileId: String): List<AccessibilityReport>

    @Query("SELECT * FROM accessibility_reports WHERE reportId = :reportId")
    suspend fun getReportById(reportId: String): AccessibilityReport?
}
