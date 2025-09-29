package com.example.nexa.storage

import com.example.nexa.networking.ConnectionManager

class OfflineQueue {
    private val queue = mutableListOf<Pair<String, ByteArray>>() // peerIp -> message

    fun enqueue(peerIp: String, message: ByteArray) {
        queue.add(peerIp to message)
    }

    fun deliver(connectionManager: ConnectionManager) {
        val iterator = queue.iterator()
        while (iterator.hasNext()) {
            val (ip, msg) = iterator.next()
            if (connectionManager.connect(ip, 8888)) {
                connectionManager.send(ip, msg)
                iterator.remove()
            }
        }
    }
}
