package br.com.fiec.helptec.network

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "auth_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_SERVER_TOKEN = "server_jwt_token"
    }

    /**
     * Salva o token gerado pelo Spring Boot.
     */
    fun saveToken(token: String) {
        prefs.edit().putString(KEY_SERVER_TOKEN, token).apply()
    }

    /**
     * Recupera o token do servidor salvo.
     */
    fun getToken(): String? {
        return prefs.getString(KEY_SERVER_TOKEN, null)
    }

    /**
     * Apaga o token ao realizar Logout.
     */
    fun clearToken() {
        prefs.edit().remove(KEY_SERVER_TOKEN).apply()
    }
}