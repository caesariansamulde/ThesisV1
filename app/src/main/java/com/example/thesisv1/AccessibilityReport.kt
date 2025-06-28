package com.example.thesisv1.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "accessibility_reports")
data class AccessibilityReport(
    @PrimaryKey val reportId: String,
    val disasterProfileId: String,
    val facilityType: Int,            // changed from String to Int
    val status: Int,                  // changed from String to Int
    val obstructions: List<Int>,      // changed from String to List<Int>
    val otherObstruction: String?,    // optional other details
    val alternativeRouteAvailable: Boolean,
    val alternativeRouteDescription: String?,
    val remarks: String?,
    val assessedBy: String?,
    val latitude: Double,
    val longitude: Double,
    val photo1: String?,
    val photo2: String?,
    val photo3: String?,
    val timestamp: Date = Date()
)
