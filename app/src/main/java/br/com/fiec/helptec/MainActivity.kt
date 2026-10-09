package br.com.fiec.helptec

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import br.com.fiec.helptec.data.ApiService
import br.com.fiec.helptec.databinding.ActivityMainBinding
import br.com.fiec.helptec.network.RetrofitClient
import br.com.fiec.helptec.network.TokenManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.suaempresa.app.network.GoogleLoginRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var tokenManager: TokenManager
    private lateinit var apiService: ApiService

    companion object {
        private const val TAG = "GoogleAuth"

        // IMPORTANTE: Use o WEB CLIENT ID gerado no Google Cloud Console
        // (O mesmo inserido no application.yml do Spring Boot)
        private const val WEB_CLIENT_ID = "SEU_WEB_CLIENT_ID_AQUI.apps.googleusercontent.com"
    }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        handleSignInResult(task)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Inicializa o gerenciador de token e o cliente Retrofit
        tokenManager = TokenManager(this)
        apiService = RetrofitClient.create(tokenManager)

        // 2. Configura o Google Sign-In solicitando o ID Token com o Web Client ID
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID) // Habilita a geração do idToken para o backend
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // 3. Listener do botão de login
        binding.btnLoginGoogle.setOnClickListener {
            signIn()
        }

        // Listener para botão de logout
        binding.btnSair?.setOnClickListener {
            signOut()
        }
    }

    override fun onStart() {
        super.onStart()
        // Verifica se já possui um token do servidor armazenado
        val tokenServidor = tokenManager.getToken()
        if (!tokenServidor.isNullOrEmpty()) {
            Log.d(TAG, "Sessão do servidor ativa: $tokenServidor")
            updateUI("Sessão Ativa", "Sessão já iniciada no backend.")
        } else {
            // Tenta recuperar conta salva no Google caso precise reautenticar
            val account = GoogleSignIn.getLastSignedInAccount(this)
            account?.idToken?.let { idToken ->
                enviarIdTokenParaBackend(idToken)
            }
        }
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            val idToken = account.idToken

            if (idToken != null) {
                // Envia o idToken obtido para validação no Spring Boot
                enviarIdTokenParaBackend(idToken)
            } else {
                Toast.makeText(this, "Não foi possível obter o idToken do Google.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            Log.e(TAG, "Erro no Google Sign-In. Código: ${e.statusCode}", e)
            Toast.makeText(this, "Falha no Login Google: ${e.statusCode}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Envia o idToken do Google para o Spring Boot e armazena o token de retorno do servidor.
     */
    private fun enviarIdTokenParaBackend(idToken: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.authenticateWithGoogle(GoogleLoginRequest(idToken))

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val authData = response.body()!!

                        // SUCESSO: Armazena o token do SERVIDOR no TokenManager
                        tokenManager.saveToken(authData.token)

                        Log.d(TAG, "Token do backend salvo com sucesso!")
                        Toast.makeText(this@MainActivity, "Autenticado com sucesso no backend!", Toast.LENGTH_SHORT).show()

                        updateUI(authData.name, authData.email)
                    } else if (response.code() == 404) {
                        Toast.makeText(this@MainActivity, "Usuário não cadastrado no sistema.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@MainActivity, "Erro na validação do servidor: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e(TAG, "Falha ao se comunicar com o backend", e)
                    Toast.makeText(this@MainActivity, "Erro de rede ao conectar no backend.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun signOut() {
        googleSignInClient.signOut().addOnCompleteListener(this) {
            tokenManager.clearToken() // Limpa o JWT armazenado
            Toast.makeText(this, "Sessão encerrada com sucesso.", Toast.LENGTH_SHORT).show()
            updateUI("Deslogado", "")
        }
    }

    private fun updateUI(name: String, email: String) {
        binding.tvTitle.text = "Bem-vindo(a), $name\n$email"
    }
}