package br.com.fiec.helptec.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import br.com.fiec.helptec.R
import br.com.fiec.helptec.admin.AdminActivity
import br.com.fiec.helptec.data.RetrofitClient
import br.com.fiec.helptec.data.repository.HelpTecRepository
import br.com.fiec.helptec.main.MainActivity
import br.com.fiec.helptec.network.TokenManager
import br.com.fiec.helptec.utils.TemaUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var tokenManager: TokenManager
    private val repository by lazy { HelpTecRepository(applicationContext) }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        handleSignInResult(task)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        tokenManager = TokenManager(this)

        val btnAlternarTema = findViewById<ImageButton>(R.id.btnAlternarTema)
        btnAlternarTema?.setOnClickListener {
            TemaUtil.alternarTema(this)
        }

        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etSenha = findViewById<EditText>(R.id.etSenha)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        btnLogin?.setOnClickListener {
            val email = etEmail?.text?.toString()?.trim().orEmpty()
            val senha = etSenha?.text?.toString()?.trim().orEmpty()

            if (email.isEmpty() || senha.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            executarLoginApi(email, senha, btnLogin, progressBar)
        }

        val webClientId = "801876311719-o76pql4jcc9uc5m58qtvp7m7dvq8g6ap.apps.googleusercontent.com"
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val btnLoginGoogle = findViewById<SignInButton>(R.id.btnLoginGoogle)
        btnLoginGoogle?.setOnClickListener {
            signInGoogle()
        }
    }

    private fun executarLoginApi(
        email: String,
        senha: String,
        btnLogin: Button,
        progressBar: ProgressBar?
    ) {
        progressBar?.visibility = View.VISIBLE
        btnLogin.isEnabled = false

        lifecycleScope.launch {
            val result = repository.login(email, senha)

            progressBar?.visibility = View.GONE
            btnLogin.isEnabled = true

            if (result.isSuccess) {
                val token = result.getOrNull()

                if (!token.isNullOrEmpty()) {
                    tokenManager.saveToken(token)
                    Toast.makeText(this@LoginActivity, "Login realizado com sucesso!", Toast.LENGTH_SHORT).show()
                    irParaMain()
                } else {
                    Toast.makeText(this@LoginActivity, "Erro: Token JWT não retornado pelo servidor", Toast.LENGTH_LONG).show()
                }
            } else {
                val error = result.exceptionOrNull()
                Toast.makeText(this@LoginActivity, "Erro: ${error?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun signInGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            val googleIdToken = account.idToken

            if (googleIdToken.isNullOrEmpty()) {
                Toast.makeText(this, "Não foi possível obter o ID Token do Google", Toast.LENGTH_SHORT).show()
                return
            }

            autenticarGoogleNoBackend(googleIdToken, account.displayName ?: "Usuário")

        } catch (e: ApiException) {
            Log.e("HelpTecLogin", "Erro no login Google. Código: ${e.statusCode}")
            Toast.makeText(this, "Erro ao autenticar via Google. Código: ${e.statusCode}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("HelpTecLogin", "Erro ao processar login Google", e)
            Toast.makeText(this, "Erro ao processar login: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun autenticarGoogleNoBackend(googleIdToken: String, displayName: String) {
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        progressBar?.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = repository.loginComGoogle(googleIdToken)
            progressBar?.visibility = View.GONE

            if (result.isSuccess) {
                val appJwtToken = result.getOrNull()

                if (!appJwtToken.isNullOrEmpty()) {
                    tokenManager.saveToken(appJwtToken)
                    Toast.makeText(this@LoginActivity, "Bem-vindo, $displayName!", Toast.LENGTH_SHORT).show()
                    irParaMain()
                } else {
                    Toast.makeText(this@LoginActivity, "Erro: Servidor não retornou um Token JWT válido.", Toast.LENGTH_LONG).show()
                }
            } else {
                val error = result.exceptionOrNull()
                Log.e("HelpTecLogin", "Erro de autenticação no backend com token do Google", error)
                Toast.makeText(this@LoginActivity, "Falha ao autenticar no servidor: ${error?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Depois do login, pergunta ao backend quem é o usuário (GET /usuarios/me)
     * e abre a tela certa: gestão de chamados para ADMIN/SUPORTE/GERENTE
     * (regra em [Perfis]), abertura de chamado para os demais.
     */
    private fun irParaMain() {
        lifecycleScope.launch {
            val role = try {
                val resp = withContext(Dispatchers.IO) {
                    RetrofitClient.getInstance(applicationContext).me()
                }
                if (!resp.isSuccessful) {
                    val erro = withContext(Dispatchers.IO) { resp.errorBody()?.string() }
                    Log.e("HelpTecPerfil", "GET /usuarios/me falhou: HTTP ${resp.code()} $erro")
                }
                resp.body()?.role
            } catch (e: Exception) {
                Log.w("HelpTecLogin", "Não foi possível consultar o perfil; abrindo tela padrão", e)
                null
            }

            Log.d("HelpTecLogin", "Papel do usuário logado: $role")

            val destino = if (Perfis.iniciaNaGestao(role)) {
                AdminActivity::class.java
            } else {
                MainActivity::class.java
            }

            startActivity(Intent(this@LoginActivity, destino))
            finish()
        }
    }
}