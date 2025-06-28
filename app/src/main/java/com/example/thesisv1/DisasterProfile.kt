package com.example.thesisv1.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "disaster_profiles")
data class DisasterProfile(
    @PrimaryKey val profileId: String,
    val type: String,
    val name: String,
    val dateTime: String,
    val region: Int,
    val province: String,
    val municipality: String,
    val barangay: String,
    val latitude: Double,
    val longitude: Double
)
