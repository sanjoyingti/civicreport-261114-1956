package com.arin.civicreport.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {
    @Query("SELECT * FROM reports ORDER BY last_updated DESC")
    fun getAll(): Flow<List<Report>>

    @Query("SELECT * FROM reports WHERE id = :id")
    suspend fun getById(id: Long): Report?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: Report): Long

    @Update
    suspend fun update(report: Report)

    @Query("DELETE FROM reports WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE reports SET status = :status, last_updated = :now WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, now: Long)
}
