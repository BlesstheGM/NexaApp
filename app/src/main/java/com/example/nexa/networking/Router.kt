package com.example.nexa.networking

class Router {
    private val routes = mutableMapOf<String, String>() // peerId -> nextHopIp

    fun update(peerId: String, nextHopIp: String) {
        routes[peerId] = nextHopIp
    }

    fun getRoute(peerId: String): String? = routes[peerId]
}
