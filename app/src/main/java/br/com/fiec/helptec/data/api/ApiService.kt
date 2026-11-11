package br.com.fiec.helptec.data

import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // ---------------- Autenticação ----------------

    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthServerResponse>

    @POST("api/v1/auth/google")
    suspend fun authenticateWithGoogle(
        @Body request: GoogleLoginRequest
    ): Response<AuthServerResponse>

    /** Dados do usuário logado (inclui o papel: USER, APROVADOR, GERENTE, SUPORTE, ADMIN) */
    @GET("usuarios/me")
    suspend fun me(): Response<MeResponse>

    // ---------------- Chamados ----------------

    /** O backend exige page e size; sem size ele não devolve nada. */
    @GET("api/chamados")
    suspend fun listarChamados(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 100
    ): Response<PageResponse<ChamadoDTO>>

    @POST("api/chamados")
    suspend fun criarChamado(
        @Body request: NovoChamadoRequest
    ): Response<ChamadoDTO>

    /** Papel APROVADOR ou ADMIN */
    @PATCH("api/chamados/{id}/aprovar")
    suspend fun aprovarChamado(@Path("id") id: Long): Response<ChamadoDTO>

    /** Papel APROVADOR ou ADMIN */
    @PATCH("api/chamados/{id}/reprovar")
    suspend fun reprovarChamado(@Path("id") id: Long): Response<ChamadoDTO>

    /**
     * Papel SUPORTE ou ADMIN.
     * O backend recebe a resolução como texto puro (@RequestBody String),
     * por isso o corpo é text/plain e não JSON.
     */
    @PATCH("api/chamados/{id}/resolver")
    suspend fun resolverChamado(
        @Path("id") id: Long,
        @Body resolucao: RequestBody
    ): Response<ChamadoDTO>

    // ---------------- Cadastros usados nos formulários ----------------

    @GET("api/salas")
    suspend fun listarSalas(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 200
    ): Response<PageResponse<SalaDTO>>

    @GET("api/equipamentos")
    suspend fun listarEquipamentos(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 200
    ): Response<PageResponse<EquipamentoDTO>>
}
