package com.lexaprograms.polishcards

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Keeps MurrLex access/refresh session values out of plain shared preferences. */
object SecureSessionValue {
    private const val keyAlias = "murrlex_server_session_v1"
    private const val androidKeyStore = "AndroidKeyStore"

    fun encrypt(value: String): String {
        if (value.isBlank()) return ""
        return runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key())
            val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
            val packed = ByteBuffer.allocate(1 + cipher.iv.size + encrypted.size)
                .put(cipher.iv.size.toByte())
                .put(cipher.iv)
                .put(encrypted)
                .array()
            "v1:" + Base64.encodeToString(packed, Base64.NO_WRAP)
        }.getOrDefault("")
    }

    fun decrypt(value: String): String {
        if (!value.startsWith("v1:")) return ""
        return runCatching {
            val packed = Base64.decode(value.removePrefix("v1:"), Base64.NO_WRAP)
            val ivSize = packed.first().toInt() and 0xff
            require(ivSize in 12..32 && packed.size > ivSize + 1)
            val iv = packed.copyOfRange(1, 1 + ivSize)
            val encrypted = packed.copyOfRange(1 + ivSize, packed.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            cipher.doFinal(encrypted).toString(Charsets.UTF_8)
        }.getOrDefault("")
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(androidKeyStore).apply { load(null) }
        (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, androidKeyStore)
        generator.init(
            KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return generator.generateKey()
    }
}
