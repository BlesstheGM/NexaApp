package com.example.nexa.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "message_queue")
data class QueuedItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val peerNodeId: String,
    val isFile: Boolean,
    val content: String,        // text message or file absolute path
    val createdAt: Long = System.currentTimeMillis()
)
