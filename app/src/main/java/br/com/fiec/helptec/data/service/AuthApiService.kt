package com.suaempresa.app.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

// Request enviado para o Spring Boot
data class GoogleLoginRequest(
    val idToken: String
)

// Response retornado do Spring Boot após validação
data class AuthResponse(
    val token: String, // JWT do seu servidor
    val email: String,
    val name: String
)

interface AuthApiService {
    @POST("api/v1/auth/google")
    suspend fun authenticateWithGoogle(
        @Body request: GoogleLoginRequest
    ): Response<AuthResponse>
}