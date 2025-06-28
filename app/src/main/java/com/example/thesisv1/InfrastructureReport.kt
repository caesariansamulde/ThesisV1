package com.example.thesisv1.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "infrastructure_reports")
data class InfrastructureReport(
    @PrimaryKey val reportId: String,
    val disasterProfileId: String,
    val facilityType: Int,
    val damage: Int,
    val remarks: String?,
    val latitude: Double,
    val longitude: Double,
    val photo1: String? = null,
    val photo2: String? = null,
    val photo3: String? = null,

    val timestamp: Date = Date()
)

