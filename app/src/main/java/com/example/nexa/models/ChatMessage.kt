package com.example.nexa.models

enum class MessageType { TEXT, FILE }

data class ChatMessage(
    val id: Long = 0,
    val fromMe: Boolean,
    val type: MessageType,
    val content: String,         // text or status string
    val timestamp: Long = System.currentTimeMillis()
)
