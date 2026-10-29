package br.com.fiec.helptec.data

data class AuthRequest(
    val idToken: String
)

data class AuthResponse(
    val token: String
)

// Request enviado para o Spring Boot
data class GoogleLoginRequest(
    val idToken: String
)

// Response retornado do Spring Boot após validação
data class AuthServerResponse(
    val token: String, // JWT do seu servidor
    val email: String,
    val name: String
)
