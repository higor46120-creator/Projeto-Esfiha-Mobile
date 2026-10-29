package br.com.fiec.helptec.data

import br.com.fiec.helptec.data.model.Chamado
import com.suaempresa.app.network.GoogleLoginRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    @POST("auth/google")
    suspend fun autenticarGoogle(@Body request: AuthRequest): Response<AuthResponse>

    @POST("chamados")
    suspend fun criarChamado(@Body request: Chamado): Response<Chamado>

    @GET("chamados")
    suspend fun listarChamados(): Response<List<Chamado>>

    @POST("api/v1/auth/google")
    suspend fun authenticateWithGoogle(@Body request: GoogleLoginRequest): Response<AuthServerResponse>

    // Exemplo de endpoint protegido (O AuthInterceptor enviará o Bearer Token automaticamente aqui)
    @GET("api/v1/perfil")
    suspend fun getPerfilUsuario(): Response<AuthResponse>
}