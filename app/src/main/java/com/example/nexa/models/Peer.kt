package com.example.nexa.models

data class Peer(
    val name: String,
    val address: String,     // Wi‑Fi Direct deviceAddress (MAC-like)
    val isOnline: Boolean = true
)
