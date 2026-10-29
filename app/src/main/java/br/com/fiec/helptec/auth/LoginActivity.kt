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
import br.com.fiec.helptec.data.repository.HelpTecRepository
import br.com.fiec.helptec.main.MainActivity
import br.com.fiec.helptec.utils.TemaUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private val repository = HelpTecRepository()

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        handleSignInResult(task)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

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
                Toast.makeText(this@LoginActivity, "Login realizado com sucesso!", Toast.LENGTH_SHORT).show()
                irParaMain()
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
            val displayName = account.displayName
            Toast.makeText(this, "Bem-vindo, $displayName!", Toast.LENGTH_SHORT).show()
            irParaMain()
        } catch (e: ApiException) {
            Log.e("HelpTecLogin", "Erro no login Google. Código: ${e.statusCode}")
            Toast.makeText(this, "Erro ao autenticar via Google. Código: ${e.statusCode}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("HelpTecLogin", "Erro ao processar login Google", e)
            Toast.makeText(this, "Erro ao processar login: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun irParaMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}