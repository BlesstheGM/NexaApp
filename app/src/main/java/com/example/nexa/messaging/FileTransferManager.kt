package com.example.nexa.messaging

import com.example.nexa.security.CryptoManager
import java.io.File
import java.io.FileInputStream
import java.net.Socket
import javax.crypto.SecretKey

class FileTransferManager {
    fun sendFile(peerIp: String, file: File, key: SecretKey) {
        val socket = Socket(peerIp, 8888)
        val output = socket.getOutputStream()
        val input = FileInputStream(file)
        val buffer = ByteArray(1024)

        var read: Int
        while (input.read(buffer).also { read = it } != -1) {
            val encrypted = CryptoManager.encrypt(buffer.copyOf(read), key)
            output.write(encrypted)
        }

        input.close()
        output.close()
        socket.close()
    }
}
