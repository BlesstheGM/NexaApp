package com.example.nexa.storage

import kotlinx.coroutines.flow.Flow

class QueueRepository(private val dao: QueueDao) {
    suspend fun enqueue(peerNodeId: String, isFile: Boolean, content: String) {
        dao.enqueue(QueuedItem(peerNodeId = peerNodeId, isFile = isFile, content = content))
    }
    fun queueForPeer(peerNodeId: String): Flow<List<QueuedItem>> = dao.getQueueForPeer(peerNodeId)
    suspend fun allQueued(): List<QueuedItem> = dao.getAll()
    suspend fun remove(id: Long) = dao.delete(id)
}
