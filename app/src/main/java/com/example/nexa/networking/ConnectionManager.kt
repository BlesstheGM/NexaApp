package com.example.nexa.networking

import java.io.IOException
import java.net.Socket

class ConnectionManager {
    private val sockets = mutableMapOf<String, Socket>()

    fun connect(ip: String, port: Int): Boolean {
        return try {
            val socket = Socket(ip, port)
            sockets[ip] = socket
            true
        } catch (e: IOException) {
            false
        }
    }

    fun send(ip: String, data: ByteArray) {
        sockets[ip]?.getOutputStream()?.write(data)
    }

    fun disconnect(ip: String) {
        sockets[ip]?.close()
        sockets.remove(ip)
    }
}
