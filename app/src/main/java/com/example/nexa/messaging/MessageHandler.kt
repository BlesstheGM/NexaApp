package com.example.nexa.messaging

import com.example.nexa.networking.ConnectionManager
import com.example.nexa.security.CryptoManager
import javax.crypto.SecretKey

class MessageHandler(private val connectionManager: ConnectionManager) {
    fun sendMessage(peerIp: String, message: String, key: SecretKey) {
        val encrypted = CryptoManager.encrypt(message.toByteArray(), key)
        connectionManager.send(peerIp, encrypted)
    }
}
