package br.com.fiec.helptec.main

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import br.com.fiec.helptec.R
import br.com.fiec.helptec.data.ApiService
import br.com.fiec.helptec.data.model.Chamado
import br.com.fiec.helptec.maps.MapsActivity
import br.com.fiec.helptec.network.RetrofitClient
import br.com.fiec.helptec.network.TokenManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var tokenManager: TokenManager
    private lateinit var apiService: ApiService

    // Declaração dos elementos da interface
    private var etTitulo: TextInputEditText? = null
    private var etSala: TextInputEditText? = null
    private var etMaquina: TextInputEditText? = null
    private var etDescricao: TextInputEditText? = null
    private var tilDescricao: TextInputLayout? = null
    private var spPrioridade: AutoCompleteTextView? = null
    private var spCategoria: AutoCompleteTextView? = null
    private var btnAbrirChamado: MaterialButton? = null
    private var btnAbrirMapa: MaterialButton? = null
    private var btnSair: MaterialButton? = null

    // ViewModel para gerenciar o estado da descrição do chamado
    private val viewModel: MainViewModel by viewModels()

    // Model interno para as categorias
    private data class CategoriaItem(val nome: String, val iconeResId: Int) {
        override fun toString(): String = nome
    }

    // Launcher para o Reconhecimento de Voz
    private val speechToTextLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val spokenTextResults =
                result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val recognizedText = spokenTextResults?.get(0) ?: ""

            viewModel.updateDescricao(recognizedText)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializa as Views pelo ID
        inicializarViews()

        // Inicializa serviços de rede
        tokenManager = TokenManager(this)
        apiService = RetrofitClient.create(tokenManager)

        // Configura a tela
        setupDropdowns()
        setupListeners()
        setupObservers()
    }

    private fun inicializarViews() {
        etTitulo = findViewById(R.id.etTitulo)
        etSala = findViewById(R.id.etSala)
        etMaquina = findViewById(R.id.etMaquina)
        etDescricao = findViewById(R.id.etDescricao)
        tilDescricao = findViewById(R.id.tilDescricao)
        spPrioridade = findViewById(R.id.spPrioridade)
        spCategoria = findViewById(R.id.spCategoria)
        btnAbrirChamado = findViewById(R.id.btnAbrirChamado)
        btnAbrirMapa = findViewById(R.id.btnAbrirMapa)
        btnSair = findViewById(R.id.btnSair)
    }

    private fun setupDropdowns() {
        // --- 1. PRIORIDADES (Alta, Média, Baixa) ---
        val prioridades = arrayOf("Alta", "Média", "Baixa")
        val adapterPrioridade = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            prioridades
        )
        spPrioridade?.setAdapter(adapterPrioridade)

        // --- 2. CATEGORIAS (com ícone) ---
        // Troque os ícones pelos seus drawables (ex.: R.drawable.ic_hardware)
        val categorias = listOf(
            CategoriaItem("Hardware", android.R.drawable.ic_menu_manage),
            CategoriaItem("Software", android.R.drawable.ic_menu_edit),
            CategoriaItem("Rede", android.R.drawable.ic_menu_share),
            CategoriaItem("Outros", android.R.drawable.ic_menu_help)
        )
        val adapterCategoria = CategoriaAdapter(categorias)
        spCategoria?.setAdapter(adapterCategoria)
        spCategoria?.setOnItemClickListener { _, _, position, _ ->
            // Mantém apenas o nome no campo (o adapter já faz isso via toString do item)
            spCategoria?.setText(categorias[position].nome, false)
        }
    }

    private fun setupListeners() {
        // Ícone de microfone no campo de descrição
        tilDescricao?.setEndIconOnClickListener { iniciarReconhecimentoDeVoz() }

        // Mantém o ViewModel sincronizado quando o usuário digita
        etDescricao?.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val texto = s?.toString().orEmpty()
                if (texto != viewModel.descricao.value) {
                    viewModel.updateDescricao(texto)
                }
            }
        })

        btnAbrirChamado?.setOnClickListener { abrirChamado() }

        btnAbrirMapa?.setOnClickListener {
            startActivity(Intent(this, MapsActivity::class.java))
        }

        btnSair?.setOnClickListener {
            tokenManager.clearToken()
            finish()
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.descricao.collect { texto ->
                    if (etDescricao?.text?.toString() != texto) {
                        etDescricao?.setText(texto)
                        etDescricao?.setSelection(texto.length)
                    }
                }
            }
        }
    }

    private fun iniciarReconhecimentoDeVoz() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale("pt", "BR"))
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Descreva o problema...")
        }
        try {
            speechToTextLauncher.launch(intent)
        } catch (e: Exception) {
            Log.e("MainActivity", "Reconhecimento de voz indisponível", e)
            Toast.makeText(
                this,
                "Reconhecimento de voz não disponível neste dispositivo",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun abrirChamado() {
        val titulo = etTitulo?.text?.toString()?.trim().orEmpty()
        val sala = etSala?.text?.toString()?.trim().orEmpty()
        val maquina = etMaquina?.text?.toString()?.trim().orEmpty()
        val descricao = etDescricao?.text?.toString()?.trim().orEmpty()
        val prioridade = spPrioridade?.text?.toString()?.trim().orEmpty()
        val categoria = spCategoria?.text?.toString()?.trim().orEmpty()

        // Validação
        if (titulo.isEmpty() || sala.isEmpty() || maquina.isEmpty() ||
            descricao.isEmpty() || prioridade.isEmpty() || categoria.isEmpty()
        ) {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            return
        }

        // AJUSTE aqui conforme o construtor real da sua classe Chamado
        val chamado = Chamado(
            titulo = titulo,
            sala = sala,
            maquina = maquina,
            descricao = descricao,
            prioridade = prioridade,
            categoria = categoria,
            devolutiva = "",
            status = "Aberto"
        )

        btnAbrirChamado?.isEnabled = false

        lifecycleScope.launch {
            try {
                // AJUSTE o nome do método conforme o seu ApiService
                val response = withContext(Dispatchers.IO) {
                    apiService.criarChamado(chamado)
                }

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@MainActivity,
                        "Chamado aberto com sucesso!",
                        Toast.LENGTH_SHORT
                    ).show()
                    limparCampos()
                } else {
                    Log.e("MainActivity", "Erro ${response.code()}: ${response.message()}")
                    Toast.makeText(
                        this@MainActivity,
                        "Erro ao abrir chamado (${response.code()})",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Falha de rede", e)
                Toast.makeText(
                    this@MainActivity,
                    "Falha de conexão. Tente novamente.",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                btnAbrirChamado?.isEnabled = true
            }
        }
    }

    private fun limparCampos() {
        etTitulo?.text?.clear()
        etSala?.text?.clear()
        etMaquina?.text?.clear()
        viewModel.updateDescricao("")
        spPrioridade?.setText("", false)
        spCategoria?.setText("", false)
    }

    /**
     * Adapter com ícone + nome, criado por código (não precisa de layout XML extra).
     */
    private inner class CategoriaAdapter(
        private val itens: List<CategoriaItem>
    ) : ArrayAdapter<CategoriaItem>(
        this@MainActivity,
        android.R.layout.simple_dropdown_item_1line,
        itens
    ) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
            criarLinha(position)

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
            criarLinha(position)

        private fun criarLinha(position: Int): View {
            val item = itens[position]
            val dp = context.resources.displayMetrics.density

            val icone = ImageView(context).apply {
                setImageResource(item.iconeResId)
                layoutParams = LinearLayout.LayoutParams((24 * dp).toInt(), (24 * dp).toInt())
            }
            val texto = TextView(context).apply {
                text = item.nome
                textSize = 16f
                setPadding((12 * dp).toInt(), 0, 0, 0)
            }

            return LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding((16 * dp).toInt(), (12 * dp).toInt(), (16 * dp).toInt(), (12 * dp).toInt())
                addView(icone)
                addView(texto)
            }
        }

        // O AutoCompleteTextView usa o toString() para preencher o campo e filtrar
        override fun getItem(position: Int): CategoriaItem = itens[position]
    }
}