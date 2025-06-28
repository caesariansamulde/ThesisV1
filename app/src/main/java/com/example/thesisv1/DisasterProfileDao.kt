package com.example.thesisv1.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete

import com.example.thesisv1.data.DisasterProfile

@Dao
interface DisasterProfileDao {

    @Insert
    suspend fun insertProfile(profile: DisasterProfile)

    @Query("SELECT * FROM disaster_profiles")
    suspend fun getAllProfiles(): List<DisasterProfile>

    @Query("SELECT * FROM disaster_profiles WHERE profileId = :id LIMIT 1")
    suspend fun getProfileById(id: String): DisasterProfile?

    @Delete
    suspend fun deleteProfile(profile: DisasterProfile)

}
