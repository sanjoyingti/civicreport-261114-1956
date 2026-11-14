package com.arin.civicreport.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Report::class, Authority::class], version = 1, exportSchema = false)
abstract class CivicDatabase : RoomDatabase() {
    abstract fun reportDao(): ReportDao
    abstract fun authorityDao(): AuthorityDao

    companion object {
        @Volatile private var INSTANCE: CivicDatabase? = null

        fun getInstance(context: Context): CivicDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CivicDatabase::class.java,
                    "civicreport.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
