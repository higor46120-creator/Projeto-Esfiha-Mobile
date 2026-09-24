package br.com.fiec.helptec

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task

class LoginActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient

    // Callback para capturar o resultado do Google Sign-In
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        handleSignInResult(task)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Aplica o tema salvo ANTES de inflar o layout
        TemaUtil.aplicarTemaSalvo(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // 2. Configuração da troca de tema no botão superior
        val btnAlternarTema = findViewById<ImageButton>(R.id.btnAlternarTema)
        btnAlternarTema?.setOnClickListener {
            TemaUtil.alternarTema(this)
            recreate() // Recria a activity para aplicar as novas cores
        }

        // 3. Configuração do Google Sign-In com o ID do Cliente WEB
        val webClientId = "801876311719-o76pql4jcc9uc5m58qtvp7m7dvq8g6ap.apps.googleusercontent.com"

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // 4. Configuração do clique no botão do Google
        val btnLoginGoogle = findViewById<SignInButton>(R.id.btnLoginGoogle)
        btnLoginGoogle.setOnClickListener {
            signIn()
        }
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)

            val email = account.email
            val displayName = account.displayName
            Toast.makeText(this, "Bem-vindo, $displayName!", Toast.LENGTH_SHORT).show()

            // Navega para a MainActivity do HelpTec
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()

        } catch (e: ApiException) {
            Log.e("HelpTecLogin", "Erro no login Google. Código: ${e.statusCode}")
            Toast.makeText(this, "Erro ao autenticar. Código: ${e.statusCode}", Toast.LENGTH_SHORT).show()
        }
    }
}