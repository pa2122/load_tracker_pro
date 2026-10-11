package com.loadtracker.pro

import android.content.Context
import android.util.Base64
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class GitHubTokenResult(
    val token: String,
    val error: String? = null
)

object GitHubTokenStore {
    private const val KEY_ALIAS = "load_tracker_github_token"
    private const val PREFS_NAME = "dev_prefs"
    private const val ENCRYPTED_TOKEN_KEY = "gh_token_encrypted"
    private const val LEGACY_TOKEN_KEY = "gh_token"
    private const val IV_SIZE_BYTES = 12

    fun load(context: Context): GitHubTokenResult {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return try {
            val encrypted = prefs.getString(ENCRYPTED_TOKEN_KEY, null)
            if (!encrypted.isNullOrBlank()) {
                val token = decrypt(encrypted)
                if (prefs.contains(LEGACY_TOKEN_KEY)) {
                    prefs.edit().remove(LEGACY_TOKEN_KEY).apply()
                }
                GitHubTokenResult(token)
            } else {
                val legacyToken = prefs.getString(LEGACY_TOKEN_KEY, "").orEmpty()
                if (legacyToken.isBlank()) {
                    GitHubTokenResult("")
                } else {
                    save(context, legacyToken)
                }
            }
        } catch (e: Exception) {
            GitHubTokenResult("", "Unable to read encrypted GitHub credentials: ${e.localizedMessage}")
        }
    }

    fun save(context: Context, token: String): GitHubTokenResult {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return try {
            val editor = prefs.edit()
            if (token.isBlank()) {
                editor.remove(ENCRYPTED_TOKEN_KEY)
                    .remove(LEGACY_TOKEN_KEY)
            } else {
                editor.putString(ENCRYPTED_TOKEN_KEY, encrypt(token.trim()))
                    .remove(LEGACY_TOKEN_KEY)
            }
            if (!editor.commit()) {
                GitHubTokenResult("", "Unable to save GitHub credentials to device storage.")
            } else {
                GitHubTokenResult(token.trim())
            }
        } catch (e: Exception) {
            GitHubTokenResult("", "Unable to encrypt GitHub credentials: ${e.localizedMessage}")
        }
    }

    private fun encrypt(token: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val ciphertext = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        val encoded = ByteBuffer.allocate(cipher.iv.size + ciphertext.size)
            .put(cipher.iv)
            .put(ciphertext)
            .array()
        return Base64.encodeToString(encoded, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): String {
        val payload = Base64.decode(encoded, Base64.NO_WRAP)
        require(payload.size > IV_SIZE_BYTES) { "Stored token payload is invalid." }
        val iv = payload.copyOfRange(0, IV_SIZE_BYTES)
        val ciphertext = payload.copyOfRange(IV_SIZE_BYTES, payload.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existingKey != null) return existingKey

        val generator = KeyGenerator.getInstance("AES", "AndroidKeyStore")
        val spec = android.security.keystore.KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                    android.security.keystore.KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }
}
