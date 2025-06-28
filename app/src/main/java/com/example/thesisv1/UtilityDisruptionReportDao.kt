package com.example.thesisv1.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UtilityDisruptionReportDao {
    @Insert
    suspend fun insertReport(report: UtilityDisruptionReport)

    @Query("SELECT * FROM utility_disruption_reports WHERE disasterProfileId = :profileId")
    suspend fun getReportsByProfileId(profileId: String): List<UtilityDisruptionReport>

    @Query("SELECT * FROM utility_disruption_reports WHERE reportId = :reportId LIMIT 1")
    suspend fun getReportById(reportId: String): UtilityDisruptionReport?
}
