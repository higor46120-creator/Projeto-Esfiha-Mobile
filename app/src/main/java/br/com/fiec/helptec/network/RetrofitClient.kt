package br.com.fiec.helptec.network

import br.com.fiec.helptec.data.ApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL = "http://10.0.2.2:8080/" // IP padrao do emulador Android

    fun create(tokenManager: TokenManager): ApiService {
        // Log das requisições no Logcat
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // Interceptor que anexa o token Bearer
        val authInterceptor = AuthInterceptor(tokenManager)

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor) // Depuração HTTP
            .addInterceptor(authInterceptor)    // Injeção do JWT
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}