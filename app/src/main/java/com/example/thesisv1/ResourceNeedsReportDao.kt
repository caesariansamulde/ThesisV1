package com.example.thesisv1.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ResourceNeedsReportDao {
    @Insert
    suspend fun insertReport(report: ResourceNeedsReport)

    @Query("SELECT * FROM resource_needs_reports WHERE disasterProfileId = :profileId")
    suspend fun getReportsByProfileId(profileId: String): List<ResourceNeedsReport>
}
