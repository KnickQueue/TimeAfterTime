package com.example.kronosclock.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watches")
data class Watch(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val make: String,
    val model: String,
    val lastSyncedEpochMs: Long? = null,
    val lastOffsetMs: Long? = null
)
