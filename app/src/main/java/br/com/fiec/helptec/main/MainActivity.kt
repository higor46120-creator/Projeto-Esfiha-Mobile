package br.com.fiec.helptec.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import br.com.fiec.helptec.data.ApiService
import br.com.fiec.helptec.data.ChamadoRequest
import br.com.fiec.helptec.data.model.Chamado
import br.com.fiec.helptec.databinding.ActivityMainBinding
import br.com.fiec.helptec.main.MainViewModel
import br.com.fiec.helptec.maps.MapsActivity
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
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var tokenManager: TokenManager
    private lateinit var apiService: ApiService

    // ViewModel para gerenciar o estado da descrição do chamado
    private val viewModel: MainViewModel by viewModels()

    companion object {
        private const val TAG = "GoogleAuth"

        // IMPORTANTE: Substitua pelo seu Web Client ID do Google Cloud Console
        private const val WEB_CLIENT_ID = "SEU_WEB_CLIENT_ID_AQUI.apps.googleusercontent.com"
    }

    // Launcher do Google Sign-In
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        handleSignInResult(task)
    }


    // Launcher para o Speech-to-Text (Reconhecimento de Voz)
    private val speechToTextLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val spokenTextResults =
                result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val recognizedText = spokenTextResults?.get(0) ?: ""

            // Atualiza o estado no ViewModel
            viewModel.updateDescricao(recognizedText)
        }
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Inicializa o gerenciador de token e o cliente Retrofit
        tokenManager = TokenManager(this)
        apiService = RetrofitClient.create(tokenManager)

        // 2. Configura o Google Sign-In solicitando o ID Token
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // 3. Configura os listeners e observadores
        setupListeners()
        setupObservers()
    }

    override fun onStart() {
        super.onStart()
        // Verifica se já possui um token do servidor armazenado
        val tokenServidor = tokenManager.getToken()
        if (!tokenServidor.isNullOrEmpty()) {
            Log.d(TAG, "Sessão do servidor ativa: $tokenServidor")
            updateUI("Sessão Ativa", "Sessão já iniciada no backend.")
        } else {
            val account = GoogleSignIn.getLastSignedInAccount(this)
            account?.idToken?.let { idToken ->
                enviarIdTokenParaBackend(idToken)
            }
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

    private fun setupListeners() {
        // Listener do botão de login com Google
        binding.btnLoginGoogle.setOnClickListener {
            signIn()
        }

        // Listener para botão de logout
        binding.btnSair?.setOnClickListener {
            signOut()
        }

        // Listener para abrir a tela do Google Maps
        binding.btnAbrirMapa.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
        }

        // Listener para o ícone de microfone embutido na Descrição
        binding.tilDescricao.setEndIconOnClickListener {
            iniciarReconhecimentoVoz()
        }

        // Listener para o botão de abrir chamado
        binding.btnAbrirChamado.setOnClickListener {
            enviarChamado()
        }
    }

    private fun setupObservers() {
        // Observa as alterações no Flow mantido pelo ViewModel
        lifecycleScope.launch {
            viewModel.descricao.collect { texto ->
                if (binding.etDescricao.text.toString() != texto) {
                    binding.etDescricao.setText(texto)
                    binding.etDescricao.setSelection(texto.length)
                }
            }
        }
    }

    private fun iniciarReconhecimentoVoz() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Fale a descrição do problema...")
        }

        try {
            speechToTextLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "O seu dispositivo não suporta reconhecimento de voz.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun enviarChamado() {
        val titulo = binding.etTitulo.text.toString().trim()
        val sala = binding.etSala.text.toString().trim()
        val maquina = binding.etMaquina.text.toString().trim()
        val categoria = binding.spCategoria.text.toString().trim()
        val prioridade = binding.spPrioridade.text.toString().trim()
        val descricao = binding.etDescricao.text.toString().trim()

        if (titulo.isEmpty() || descricao.isEmpty()) {
            Toast.makeText(this, "Preencha pelo menos o título e a descrição", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val chamadoRequest = Chamado(
            titulo = titulo,
            sala = sala,
            maquina = maquina,
            categoria = categoria,
            prioridade = prioridade,
            descricao = descricao,
            id = TODO(),
            devolutiva = TODO(),
            status = TODO()
        )

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.criarChamado(chamadoRequest)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@MainActivity,
                            "Chamado aberto com sucesso!",
                            Toast.LENGTH_LONG
                        ).show()
                        limparFormulario()
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Erro ao abrir chamado: ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("Chamado", "Erro de conexão", e)
                    Toast.makeText(
                        this@MainActivity,
                        "Erro de rede ao enviar chamado",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun limparFormulario() {
        binding.etTitulo.text?.clear()
        binding.etSala.text?.clear()
        binding.etMaquina.text?.clear()
        binding.spCategoria.setText("", false)
        binding.spPrioridade.setText("", false)
        viewModel.updateDescricao("")
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun signOut() {
        googleSignInClient.signOut().addOnCompleteListener(this) {
            tokenManager.clearToken()
            Toast.makeText(this, "Sessão encerrada com sucesso.", Toast.LENGTH_SHORT).show()
            updateUI("Deslogado", "")
        }
    }

    private fun updateUI(name: String, email: String) {
        binding.tvTitle.text = "Bem-vindo(a), $name\n$email"
    }
}