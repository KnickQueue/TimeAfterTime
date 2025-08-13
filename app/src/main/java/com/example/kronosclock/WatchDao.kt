package com.example.kronosclock.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchDao {
    @Insert
    suspend fun insert(watch: Watch): Long

    @Update
    suspend fun update(watch: Watch)

    @Query("SELECT * FROM watches WHERE id = :id")
    suspend fun get(id: Long): Watch?

    @Query("SELECT * FROM watches ORDER BY make, model")
    fun getAll(): Flow<List<Watch>>
}