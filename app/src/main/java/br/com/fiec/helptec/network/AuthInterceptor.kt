package br.com.fiec.helptec.network

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(context: Context) : Interceptor {
    private val tokenManager = TokenManager(context) // Sua classe que salva o token no SharedPref/DataStore

    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()
        val token = tokenManager.getToken()

        // Adiciona o token no cabeçalho se ele existir
        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }
}