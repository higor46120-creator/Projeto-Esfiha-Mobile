package br.com.fiec.helptec.network

import br.com.fiec.helptec.data.ApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // Endereço do backend: altere em network/ApiConfig.kt
    private const val BASE_URL = ApiConfig.BASE_URL

    fun create(tokenManager: TokenManager): ApiService {
        // Autenticação: lê o token salvo no login a CADA requisição,
        // então sempre usa o JWT mais recente (e nada fica fixo no código).
        val authInterceptor = okhttp3.Interceptor { chain ->
            val builder = chain.request().newBuilder()
                .header("Accept", "application/json")

            val token = tokenManager.getToken()
            // Não envia token nas rotas de login/cadastro: um JWT antigo/expirado
            // ali faria o backend recusar o próprio login.
            val rotaDeLogin = chain.request().url.encodedPath.contains("/api/v1/auth/")
            if (!token.isNullOrEmpty() && !rotaDeLogin) {
                builder.header("Authorization", "Bearer $token")
            }

            chain.proceed(builder.build())
        }

        // Log detalhado no Logcat. Fica DEPOIS do authInterceptor para mostrar
        // o header enviado; o valor do Authorization é ocultado no log.
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS // BODY expõe o ID token do Google no Logcat
            redactHeader("Authorization")
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
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