package com.example.lock.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface StateDao {
    @Query("SELECT * FROM device_state WHERE id = 1")
    suspend fun get(): StateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: StateEntity)
}

@Dao
interface CommandDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: CommandLogEntity): Long

    @Query("SELECT * FROM command_log WHERE commandId = :commandId LIMIT 1")
    suspend fun find(commandId: Long): CommandLogEntity?

    @Query("SELECT MAX(sequence) FROM command_log")
    suspend fun maxSequence(): Long?

    @Query("SELECT * FROM command_log ORDER BY appliedAt DESC LIMIT 20")
    suspend fun recent(): List<CommandLogEntity>
}
