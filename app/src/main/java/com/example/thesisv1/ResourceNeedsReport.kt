package com.example.thesisv1.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "resource_needs_reports")
data class ResourceNeedsReport(
    @PrimaryKey val id: String,
    val disasterProfileId: String,
    val food: String?,
    val nonFood: String?,
    val medical: String?,
    val shelter: String?,
    val manpower: String?,
    val priority: String,
    val remarks: String?,
    val latitude: Double,
    val longitude: Double
)
