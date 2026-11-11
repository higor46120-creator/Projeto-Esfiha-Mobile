package br.com.fiec.helptec.data

import android.content.Context
import br.com.fiec.helptec.network.ApiConfig
import br.com.fiec.helptec.network.TokenManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // Endereço do backend: altere em network/ApiConfig.kt
    private const val BASE_URL = ApiConfig.BASE_URL

    fun getInstance(context: Context): ApiService {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS // BODY expõe o ID token do Google no Logcat
            redactHeader("Authorization") // não mostra o JWT no Logcat
        }

        val authInterceptor = Interceptor { chain ->
            val tokenManager = TokenManager(context)
            val token = tokenManager.getToken()

            val requestBuilder = chain.request().newBuilder()
            // Não envia token nas rotas de login/cadastro: um JWT antigo/expirado
            // ali faria o backend recusar o próprio login.
            val rotaDeLogin = chain.request().url.encodedPath.contains("/api/v1/auth/")
            if (!token.isNullOrEmpty() && !rotaDeLogin) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }

            chain.proceed(requestBuilder.build())
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}