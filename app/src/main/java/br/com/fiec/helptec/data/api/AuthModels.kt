package br.com.fiec.helptec.data

// Corpo enviado ao Spring em POST api/v1/auth/google
// O nome do campo ("idToken") precisa ser igual ao do DTO no backend.
data class GoogleLoginRequest(
    val idToken: String
)

// Corpo enviado ao Spring em POST api/v1/auth/login
// O backend (LoginRequest record) usa os campos "email" e "password".
data class LoginRequest(
    val email: String,
    val password: String
)

// Resposta do backend com o JWT da aplicação.
// Se o Spring devolve outro nome (ex.: "accessToken"), troque aqui
// ou use @SerializedName("accessToken").
data class AuthServerResponse(
    val token: String? = null
)