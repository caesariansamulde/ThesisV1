package com.example.thesisv1.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CasualtyReportDao {

    @Insert
    suspend fun insertReport(report: CasualtyReport)

    @Query("SELECT * FROM casualty_reports WHERE disasterProfileId = :profileId")
    suspend fun getReportsByProfileId(profileId: String): List<CasualtyReport>

    @Query("SELECT * FROM casualty_reports WHERE reportId = :reportId LIMIT 1")
    suspend fun getReportById(reportId: String): CasualtyReport?

    @Query("SELECT * FROM casualty_reports ORDER BY timestamp DESC")
    suspend fun getAllReports(): List<CasualtyReport>

}
