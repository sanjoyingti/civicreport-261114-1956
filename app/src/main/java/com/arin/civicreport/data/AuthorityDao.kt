package com.arin.civicreport.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AuthorityDao {
    @Query("SELECT * FROM authorities ORDER BY \"order\" ASC")
    fun getAll(): Flow<List<Authority>>

    @Query("SELECT * FROM authorities WHERE id = :id")
    suspend fun getById(id: String): Authority?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(authority: Authority)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(authorities: List<Authority>)

    @Update
    suspend fun update(authority: Authority)

    @Query("DELETE FROM authorities WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM authorities " +
           "WHERE (activity_tags = 'all' OR activity_tags LIKE '%' || :tag || '%') " +
           "AND (:state = '' OR :state IS NULL OR applicable_states LIKE '%' || :state || '%') " +
           "AND (:city = '' OR :city IS NULL OR applicable_cities LIKE '%' || :city || '%') " +
           "ORDER BY \"order\" ASC")
    suspend fun findByTag(tag: String, state: String, city: String): List<Authority>
}
