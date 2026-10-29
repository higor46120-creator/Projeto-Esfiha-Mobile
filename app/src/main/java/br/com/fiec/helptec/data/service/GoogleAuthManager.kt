package br.com.fiec.helptec.data.service

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.suaempresa.app.network.AuthApiService
import com.suaempresa.app.network.GoogleLoginRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleAuthManager(
    private val context: Context,
    private val authApiService: AuthApiService
) {

    private val credentialManager = CredentialManager.create(context)

    companion object {
        // ATENÇÃO: Coloque aqui o mesmo WEB Client ID configurado no application.yml do Spring Boot.
        // NÃO coloque o Android Client ID aqui.
        private const val WEB_CLIENT_ID = "SEU_WEB_CLIENT_ID.apps.googleusercontent.com"
    }

    /**
     * Executa o fluxo completo:
     * 1. Solicita o idToken ao Google via CredentialManager (usando WebClientId)
     * 2. Envia o idToken para o endpoint do Spring Boot
     * 3. Retorna o resultado com a sessão do servidor
     */
    suspend fun performGoogleLoginAndSyncWithBackend(): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 1. Configura a opção de login do Google
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false) // Mostra todas as contas logadas no dispositivo
                .setServerClientId(WEB_CLIENT_ID)     // <-- Chave Web para o Spring Boot aceitar a 'aud'
                .setAutoSelectEnabled(false)
                .build()

            // 2. Monta a requisição de credencial
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            // 3. Exibe o BottomSheet nativo do Google no Android
            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            // 4. Extrai o idToken retornado do Google
            val credential = result.credential
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val googleIdToken = googleIdTokenCredential.idToken

            // 5. Envia o idToken para validação no Backend Spring Boot
            val apiResponse = authApiService.authenticateWithGoogle(GoogleLoginRequest(googleIdToken))

            if (apiResponse.isSuccessful && apiResponse.body() != null) {
                val backendToken = apiResponse.body()!!.token
                // Sucesso: Retorna o token gerado pelo seu servidor
                Result.success(backendToken)
            } else if (apiResponse.code() == 404) {
                Result.failure(Exception("Usuário não cadastrado no sistema."))
            } else {
                Result.failure(Exception("Erro na autenticação com o servidor: ${apiResponse.code()}"))
            }

        } catch (e: GetCredentialException) {
            Result.failure(Exception("Cancelado ou falhou no Google Auth: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("Erro durante o processo de autenticação: ${e.message}"))
        }
    }
}