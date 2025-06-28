package com.example.thesisv1.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "utility_disruption_reports")
data class UtilityDisruptionReport(
    @PrimaryKey val reportId: String,
    val disasterProfileId: String,
    val utilityType: Int,             // ✅ use this
    val status: Int,                  // ✅ use this
    val disruptionDetails: String,    // Comma-separated numeric codes
    val otherDetail: String? = null,
    val remarks: String? = null,
    val latitude: Double,
    val longitude: Double,
    val photo1: String? = null,
    val photo2: String? = null,
    val photo3: String? = null,
    val timestamp: Date = Date()
)

