package br.com.fiec.helptec.main

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
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
import br.com.fiec.helptec.admin.AdminActivity
import br.com.fiec.helptec.auth.Perfis
import br.com.fiec.helptec.auth.LoginActivity
import br.com.fiec.helptec.data.ApiService
import br.com.fiec.helptec.data.EquipamentoDTO
import br.com.fiec.helptec.data.NovoChamadoRequest
import br.com.fiec.helptec.data.SalaDTO
import br.com.fiec.helptec.data.model.Chamado
import br.com.fiec.helptec.maps.MapsActivity
import br.com.fiec.helptec.network.RetrofitClient
import br.com.fiec.helptec.network.TokenManager
import br.com.fiec.helptec.ui.adapter.SemFiltroAdapter
import android.widget.ArrayAdapter
import androidx.core.widget.doAfterTextChanged
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
    private var etSala: AutoCompleteTextView? = null
    private var etMaquina: AutoCompleteTextView? = null

    private var etDescricao: TextInputEditText? = null
    private var tilDescricao: TextInputLayout? = null
    private var spPrioridade: AutoCompleteTextView? = null
    private var spCategoria: AutoCompleteTextView? = null
    private var btnAbrirChamado: MaterialButton? = null
    private var btnAbrirMapa: MaterialButton? = null
    private var btnSair: MaterialButton? = null
    private var btnGestao: MaterialButton? = null

    // ViewModel: guarda descrição, listas e seleções; sobrevive à rotação da tela
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

            viewModel.acrescentarDescricao(recognizedText)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializa as Views pelo ID
        inicializarViews()

        // Inicializa serviços de rede (o token é lido pelo interceptor a cada requisição)
        tokenManager = TokenManager(this)
        apiService = RetrofitClient.create(tokenManager)

        // Configura a tela
        setupDropdowns()
        setupListeners()
        configurarCamposLivres()
        setupObservers()
        carregarSalasEEquipamentos()
        mostrarBotaoGestaoSePermitido()
    }

    /** Aprovador, suporte, gerente e admin veem o botão "Gestão" no cabeçalho. */
    private fun mostrarBotaoGestaoSePermitido() {
        // Já consultado antes de girar a tela: só reaplica
        viewModel.podeAcessarGestao?.let { pode ->
            btnGestao?.visibility = if (pode) View.VISIBLE else View.GONE
            return
        }
        lifecycleScope.launch {
            val role = try {
                val resp = withContext(Dispatchers.IO) { apiService.me() }
                if (resp.isSuccessful) {
                    Log.d("HelpTecPerfil", "GET /usuarios/me -> ${resp.code()} papel=${resp.body()?.role}")
                    resp.body()?.role
                } else {
                    val erro = withContext(Dispatchers.IO) { resp.errorBody()?.string() }
                    Log.e("HelpTecPerfil", "GET /usuarios/me falhou: HTTP ${resp.code()} $erro")
                    Toast.makeText(
                        this@MainActivity,
                        "Não foi possível verificar seu perfil (HTTP ${resp.code()})",
                        Toast.LENGTH_LONG
                    ).show()
                    null
                }
            } catch (e: Exception) {
                Log.e("HelpTecPerfil", "GET /usuarios/me: erro de rede/JSON", e)
                null
            }
            val pode = Perfis.podeAcessarGestao(role)
            if (role != null) viewModel.podeAcessarGestao = pode
            btnGestao?.visibility = if (pode) View.VISIBLE else View.GONE
        }
    }

    /** Busca salas e equipamentos cadastrados no backend para os dropdowns. */
    private fun carregarSalasEEquipamentos() {
        // Depois de girar a tela as listas já estão no ViewModel: não busca de novo
        val salasSalvas = viewModel.salas
        val equipSalvos = viewModel.equipamentos
        if (salasSalvas != null && equipSalvos != null) {
            preencherSalasEEquipamentos(salasSalvas, equipSalvos)
            return
        }

        lifecycleScope.launch {
            try {
                val respSalas = withContext(Dispatchers.IO) { apiService.listarSalas() }
                val respEquip = withContext(Dispatchers.IO) { apiService.listarEquipamentos() }

                if (respSalas.code() == 401 || respEquip.code() == 401) {
                    sessaoExpirada()
                    return@launch
                }

                val salas = respSalas.body()?.itens.orEmpty()
                val equipamentos = respEquip.body()?.itens.orEmpty()

                if (respSalas.isSuccessful && respEquip.isSuccessful) {
                    viewModel.salas = salas
                    viewModel.equipamentos = equipamentos
                }
                preencherSalasEEquipamentos(salas, equipamentos)

                if (salas.isEmpty() || equipamentos.isEmpty()) {
                    Toast.makeText(
                        this@MainActivity,
                        "Nenhuma sala ou equipamento cadastrado: digite a sala e a máquina",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Erro ao carregar salas/equipamentos", e)
                Toast.makeText(
                    this@MainActivity,
                    "Não foi possível carregar a lista de salas e máquinas; você pode digitá-las",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Sala e máquina: o usuário escolhe uma sugestão do cadastro OU digita livremente.
     * Aqui o adapter FILTRA as sugestões conforme o texto digitado.
     */
    private fun preencherSalasEEquipamentos(salas: List<SalaDTO>, equipamentos: List<EquipamentoDTO>) {
        etSala?.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, salas))
        etSala?.setOnItemClickListener { parent, _, position, _ ->
            viewModel.salaSelecionada = parent.getItemAtPosition(position) as SalaDTO
        }

        etMaquina?.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, equipamentos))
        etMaquina?.setOnItemClickListener { parent, _, position, _ ->
            viewModel.equipamentoSelecionado = parent.getItemAtPosition(position) as EquipamentoDTO
        }
    }

    /** Se o usuário editar o texto depois de escolher uma sugestão, a escolha deixa de valer. */
    private fun configurarCamposLivres() {
        etSala?.doAfterTextChanged { texto ->
            if (viewModel.salaSelecionada?.toString() != texto?.toString()) viewModel.salaSelecionada = null
        }
        etMaquina?.doAfterTextChanged { texto ->
            if (viewModel.equipamentoSelecionado?.toString() != texto?.toString()) viewModel.equipamentoSelecionado = null
        }
    }

    /** Sala do cadastro que corresponde ao texto (escolhida na lista ou digitada igual). */
    private fun salaDoCadastro(texto: String): SalaDTO? =
        viewModel.salaSelecionada?.takeIf { it.toString() == texto }
            ?: viewModel.salas?.firstOrNull {
                it.nome.equals(texto, ignoreCase = true) || it.toString().equals(texto, ignoreCase = true)
            }

    /** Equipamento do cadastro que corresponde ao texto (nome, patrimônio ou "nome (patrimônio)"). */
    private fun equipamentoDoCadastro(texto: String): EquipamentoDTO? =
        viewModel.equipamentoSelecionado?.takeIf { it.toString() == texto }
            ?: viewModel.equipamentos?.firstOrNull {
                it.toString().equals(texto, ignoreCase = true) ||
                    it.nome.equals(texto, ignoreCase = true) ||
                    it.patrimonio.equals(texto, ignoreCase = true)
            }

    private fun sessaoExpirada() {
        Toast.makeText(this, "Sessão expirada. Faça login novamente.", Toast.LENGTH_LONG).show()
        tokenManager.clearToken()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
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
        btnGestao = findViewById(R.id.btnGestao)
    }

    private fun setupDropdowns() {
        // --- 1. PRIORIDADES (Alta, Média, Baixa) ---
        val prioridades = arrayOf("Alta", "Média", "Baixa")
        val adapterPrioridade = SemFiltroAdapter(this, prioridades.toList())
        spPrioridade?.setAdapter(adapterPrioridade)

        // --- 2. CATEGORIAS (com ícone) ---
        val categorias = listOf(
            CategoriaItem("Hardware", android.R.drawable.ic_menu_manage),
            CategoriaItem("Software", android.R.drawable.ic_menu_edit),
            CategoriaItem("Rede", android.R.drawable.ic_menu_share),
            CategoriaItem("Outros", android.R.drawable.ic_menu_help)
        )
        val adapterCategoria = CategoriaAdapter(categorias)
        spCategoria?.setAdapter(adapterCategoria)
        spCategoria?.setOnItemClickListener { _, _, position, _ ->
            spCategoria?.setText(categorias[position].nome, false)
        }
    }

    private fun setupListeners() {
        // Ícone de microfone no campo de descrição
        tilDescricao?.setEndIconOnClickListener { iniciarReconhecimentoDeVoz() }

        // Sincroniza ViewModel ao digitar
        etDescricao?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val texto = s?.toString().orEmpty()
                if (texto != viewModel.descricao.value) {
                    viewModel.setDescricao(texto)
                }
            }
        })

        btnAbrirChamado?.setOnClickListener { abrirChamado() }

        btnGestao?.setOnClickListener {
            startActivity(Intent(this, AdminActivity::class.java))
        }

        btnAbrirMapa?.setOnClickListener {
            startActivity(Intent(this, MapsActivity::class.java))
        }

        btnSair?.setOnClickListener {
            tokenManager.clearToken()
            startActivity(Intent(this, LoginActivity::class.java))
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
        val descricaoInput = etDescricao?.text?.toString()?.trim().orEmpty()
        val prioridadeInput = spPrioridade?.text?.toString()?.trim().orEmpty().uppercase()
        val categoria = spCategoria?.text?.toString()?.trim().orEmpty()
        val salaTexto = etSala?.text?.toString()?.trim().orEmpty()
        val maquinaTexto = etMaquina?.text?.toString()?.trim().orEmpty()

        // Validação básica dos campos da UI
        if (titulo.isEmpty() || descricaoInput.isEmpty() || prioridadeInput.isEmpty() || categoria.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            return
        }
        if (salaTexto.isEmpty() || maquinaTexto.isEmpty()) {
            Toast.makeText(this, "Informe a sala e a máquina (escolha da lista ou digite)", Toast.LENGTH_SHORT).show()
            return
        }

        // Existe no cadastro? Usa o id. Não existe? Vai o texto digitado na descrição.
        val sala = salaDoCadastro(salaTexto)
        val equipamento = equipamentoDoCadastro(maquinaTexto)

        // Normaliza prioridade para o backend (ALTA, MEDIA, BAIXA)
        val prioridadeFormatada = when {
            prioridadeInput.contains("ALT") -> "ALTA"
            prioridadeInput.contains("MÉD") || prioridadeInput.contains("MED") -> "MEDIA"
            else -> "BAIXA"
        }

        // O solicitante não vai no corpo: o backend identifica pelo JWT
        val request = NovoChamadoRequest(
            descricao = Chamado.montarDescricao(categoria, titulo, descricaoInput),
            prioridade = prioridadeFormatada,
            criticidade = prioridadeFormatada,
            idSala = sala?.idSala,
            idEquipamento = equipamento?.id,
            // Não existe no cadastro? Vai o texto digitado (campos próprios no backend)
            salaInformada = if (sala == null) salaTexto else null,
            equipamentoInformado = if (equipamento == null) maquinaTexto else null
        )

        btnAbrirChamado?.isEnabled = false

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.criarChamado(request)
                }

                when {
                    response.isSuccessful -> {
                        val numero = response.body()?.idChamado
                        Toast.makeText(
                            this@MainActivity,
                            if (numero != null) "Chamado #$numero aberto com sucesso!" else "Chamado aberto com sucesso!",
                            Toast.LENGTH_SHORT
                        ).show()
                        limparCampos()
                    }
                    response.code() == 401 -> sessaoExpirada()
                    else -> {
                        val errorMsg = response.errorBody()?.string()
                        Log.e("MainActivity", "Erro ${response.code()}: $errorMsg")
                        Toast.makeText(
                            this@MainActivity,
                            "Erro ao abrir chamado (${response.code()})",
                            Toast.LENGTH_LONG
                        ).show()
                    }
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
        etSala?.setText("", false)
        etMaquina?.setText("", false)
        viewModel.limparSelecoes()
        spPrioridade?.setText("", false)
        spCategoria?.setText("", false)
    }

    /**
     * Adapter customizado interno para ícone + nome sem layout XML externo
     */
    private inner class CategoriaAdapter(
        private val itens: List<CategoriaItem>
    ) : SemFiltroAdapter<CategoriaItem>(this@MainActivity, itens) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
            criarLinha(position)

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
            criarLinha(position)

        private fun criarLinha(position: Int): View {
            val item = itens[position]
            val scale = context.resources.displayMetrics.density

            val icone = ImageView(context).apply {
                setImageResource(item.iconeResId)
                layoutParams = LinearLayout.LayoutParams(
                    (24 * scale).toInt(),
                    (24 * scale).toInt()
                )
            }

            val texto = TextView(context).apply {
                text = item.nome
                textSize = 16f
                setPadding((12 * scale).toInt(), 0, 0, 0)
            }

            return LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(
                    (16 * scale).toInt(),
                    (12 * scale).toInt(),
                    (16 * scale).toInt(),
                    (12 * scale).toInt()
                )
                addView(icone)
                addView(texto)
            }
        }

        override fun getItem(position: Int): CategoriaItem = itens[position]
    }
}