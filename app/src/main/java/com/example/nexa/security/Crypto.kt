package com.example.nexa.security

import java.security.*
import java.security.spec.ECGenParameterSpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

object Crypto {
    fun generateEcKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec("secp256r1"))
        return kpg.generateKeyPair()
    }

    fun deriveSharedKey(privateKey: PrivateKey, peerPublicKey: PublicKey): ByteArray {
        val ka = KeyAgreement.getInstance("ECDH")
        ka.init(privateKey)
        ka.doPhase(peerPublicKey, true)
        return ka.generateSecret()
    }

    fun hkdfSha256(secret: ByteArray, salt: ByteArray = ByteArray(32) { 0 }, info: ByteArray = "NEXA-CHAT".toByteArray(), size: Int = 32): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(salt, "HmacSHA256"))
        val prk = mac.doFinal(secret)
        mac.init(SecretKeySpec(prk, "HmacSHA256"))
        mac.update(info)
        mac.update(0x01)
        return mac.doFinal().copyOf(size)
    }

    fun aesGcmEncrypt(aesKey: ByteArray, plaintext: ByteArray, aad: ByteArray? = null): Pair<ByteArray, ByteArray> {
        val iv = Random.Default.nextBytes(12)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(aesKey, "AES")
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
        if (aad != null) cipher.updateAAD(aad)
        val ciphertext = cipher.doFinal(plaintext)
        return iv to ciphertext
    }

    fun aesGcmDecrypt(aesKey: ByteArray, iv: ByteArray, ciphertext: ByteArray, aad: ByteArray? = null): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(aesKey, "AES")
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
        if (aad != null) cipher.updateAAD(aad)
        return cipher.doFinal(ciphertext)
    }
}
