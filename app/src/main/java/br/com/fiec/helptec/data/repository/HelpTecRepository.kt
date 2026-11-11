package br.com.fiec.helptec.data.repository

import android.content.Context
import br.com.fiec.helptec.data.ApiService
import br.com.fiec.helptec.data.AuthServerResponse
import br.com.fiec.helptec.data.GoogleLoginRequest
import br.com.fiec.helptec.data.LoginRequest
import br.com.fiec.helptec.data.RetrofitClient
import retrofit2.Response

class HelpTecRepository(context: Context) {

    private val api: ApiService = RetrofitClient.getInstance(context.applicationContext)

    // Retorna o JWT da aplicação
    suspend fun login(email: String, pass: String): Result<String> =
        chamarAuth { api.login(LoginRequest(email, pass)) }

    // Envia o ID Token do Google ao backend e retorna o JWT da aplicação
    suspend fun loginComGoogle(googleIdToken: String): Result<String> =
        chamarAuth { api.authenticateWithGoogle(GoogleLoginRequest(googleIdToken)) }

    private suspend fun chamarAuth(
        request: suspend () -> Response<AuthServerResponse>
    ): Result<String> {
        return try {
            val response = request()
            val token = response.body()?.token

            when {
                response.isSuccessful && !token.isNullOrEmpty() ->
                    Result.success(token)

                response.isSuccessful ->
                    Result.failure(Exception("Servidor respondeu OK, mas sem token no corpo"))

                else -> {
                    val corpo = response.errorBody()?.string().orEmpty()
                    Result.failure(Exception("HTTP ${response.code()} $corpo".trim()))
                }
            }
        } catch (e: Exception) {
            // Sem rede, timeout, host errado, JSON inválido etc.
            Result.failure(e)
        }
    }
}