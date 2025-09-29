package com.example.nexa.security

import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

object CryptoManager {
    fun generateAESKey(): SecretKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

    fun encrypt(data: ByteArray, key: SecretKey): ByteArray =
        Cipher.getInstance("AES").apply { init(Cipher.ENCRYPT_MODE, key) }.doFinal(data)

    fun decrypt(data: ByteArray, key: SecretKey): ByteArray =
        Cipher.getInstance("AES").apply { init(Cipher.DECRYPT_MODE, key) }.doFinal(data)
}
