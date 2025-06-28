package com.example.thesisv1.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.thesisv1.data.DisasterProfile
import com.example.thesisv1.data.DisasterProfileDao

@Database(
    entities = [
        DisasterProfile::class,
        CasualtyReport::class,
        InfrastructureReport::class,
        AccessibilityReport::class,
        ResourceNeedsReport::class,
        UtilityDisruptionReport::class
    ],
    version = 1
)
@TypeConverters(Converters::class)

abstract class DisasterDatabase : RoomDatabase() {
    abstract fun disasterProfileDao(): DisasterProfileDao
    abstract fun casualtyReportDao(): CasualtyReportDao
    abstract fun infrastructureReportDao(): InfrastructureReportDao
    abstract fun accessibilityReportDao(): AccessibilityReportDao
    abstract fun resourceNeedsReportDao(): ResourceNeedsReportDao
    abstract fun utilityDisruptionReportDao(): UtilityDisruptionReportDao




    companion object {
        @Volatile
        private var INSTANCE: DisasterDatabase? = null

        fun getDatabase(context: Context): DisasterDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DisasterDatabase::class.java,
                    "disaster_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
