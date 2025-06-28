package com.example.thesisv1.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface InfrastructureReportDao {
    @Insert
    suspend fun insertReport(report: InfrastructureReport)

    @Query("SELECT * FROM infrastructure_reports WHERE disasterProfileId = :profileId")
    suspend fun getReportsByProfileId(profileId: String): List<InfrastructureReport>

    @Query("SELECT * FROM infrastructure_reports WHERE reportId = :reportId")
    suspend fun getReportById(reportId: String): InfrastructureReport?

}
