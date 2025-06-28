package com.example.thesisv1.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "casualty_reports")
data class CasualtyReport(
    @PrimaryKey val reportId: String,
    val disasterProfileId: String,
    val affectedFamilies: Int,
    val affectedPersons: Int,
    val age0to2: Int,
    val age3to5: Int,
    val age6to12: Int,
    val age13to17: Int,
    val pwdCount: Int,
    val elderlyCount: Int,
    val missingMale: Int,
    val missingFemale: Int,
    val injuredMale: Int,
    val injuredFemale: Int,
    val deadMale: Int,
    val deadFemale: Int,
    val remarks: String?,
    val latitude: Double,
    val longitude: Double,
    val needResources: Boolean = false,
    val food: String? = null,
    val nonFood: String? = null,
    val medical: String? = null,
    val shelter: String? = null,
    val manpower: String? = null,
    val priority: String? = null,
    val timestamp: Date = Date()
)
