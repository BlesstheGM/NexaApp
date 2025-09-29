package com.example.nexa.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface QueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(item: QueuedItem): Long

    @Query("DELETE FROM message_queue WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM message_queue WHERE peerNodeId = :peer ORDER BY createdAt ASC")
    fun getQueueForPeer(peer: String): Flow<List<QueuedItem>>

    @Query("SELECT * FROM message_queue ORDER BY createdAt ASC")
    suspend fun getAll(): List<QueuedItem>
}
