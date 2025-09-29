package com.example.nexa.security

import java.security.*
import javax.crypto.Cipher

object KeyExchangeManager {
    fun generateRSAKeyPair(): KeyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()

    fun encryptWithPublicKey(data: ByteArray, publicKey: PublicKey): ByteArray =
        Cipher.getInstance("RSA").apply { init(Cipher.ENCRYPT_MODE, publicKey) }.doFinal(data)

    fun decryptWithPrivateKey(data: ByteArray, privateKey: PrivateKey): ByteArray =
        Cipher.getInstance("RSA").apply { init(Cipher.DECRYPT_MODE, privateKey) }.doFinal(data)
}
