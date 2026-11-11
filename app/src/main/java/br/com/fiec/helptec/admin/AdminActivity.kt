package br.com.fiec.helptec.admin

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import br.com.fiec.helptec.R
import br.com.fiec.helptec.auth.LoginActivity
import br.com.fiec.helptec.auth.Perfis
import br.com.fiec.helptec.data.ApiService
import br.com.fiec.helptec.data.RetrofitClient
import br.com.fiec.helptec.data.model.Chamado
import br.com.fiec.helptec.databinding.ActivityAdminBinding
import br.com.fiec.helptec.main.MainActivity
import br.com.fiec.helptec.network.TokenManager
import br.com.fiec.helptec.ui.adapter.ChamadoAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Tela de gestão de chamados: filtros + lista.
 * Acesso: ADMIN, SUPORTE, GERENTE e APROVADOR (ver [Perfis]).
 * Tocar em um chamado abre a [AtualizarChamadoActivity]; ao concluir a ação, a lista é recarregada.
 */
class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding
    private lateinit var chamadoAdapter: ChamadoAdapter
    private lateinit var api: ApiService

    private var roleAtual: String? = null

    /** Todos os chamados vindos do backend; os filtros trabalham em cima desta lista. */
    private var todosChamados: List<Chamado> = emptyList()

    /** Abre a tela de atualização e recarrega a lista quando ela devolver RESULT_OK. */
    private val atualizarChamadoLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
            if (resultado.resultCode == Activity.RESULT_OK) {
                carregarChamados()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        api = RetrofitClient.getInstance(applicationContext)

        configurarRecyclerView()
        configurarFiltros()
        configurarListeners()

        verificarAcessoECarregar()
    }

    /**
     * Confere no backend o papel do usuário logado antes de mostrar qualquer dado.
     * Quem não tiver acesso à gestão volta para a tela de abrir chamado.
     */
    private fun verificarAcessoECarregar() {
        lifecycleScope.launch {
            val resposta = try {
                withContext(Dispatchers.IO) { api.me() }
            } catch (e: Exception) {
                Log.e("AdminActivity", "Erro ao consultar perfil", e)
                Toast.makeText(this@AdminActivity, "Falha de conexão com o servidor", Toast.LENGTH_LONG).show()
                irParaLogin()
                return@launch
            }

            if (resposta.code() == 401) {
                sessaoExpirada()
                return@launch
            }

            val role = resposta.body()?.role?.uppercase()
            if (!Perfis.podeAcessarGestao(role)) {
                Toast.makeText(
                    this@AdminActivity,
                    "Acesso restrito a aprovadores, suporte, gerentes e administradores",
                    Toast.LENGTH_LONG
                ).show()
                startActivity(Intent(this@AdminActivity, MainActivity::class.java))
                finish()
                return@launch
            }

            roleAtual = role
            selecionarFiltroPadrao(role)
            carregarChamados()
        }
    }

    // ---------------- Lista ----------------

    private fun configurarRecyclerView() {
        chamadoAdapter = ChamadoAdapter(onItemClick = { chamado, _ -> abrirAtualizacao(chamado) })

        binding.rvChamadosAdmin.apply {
            layoutManager = LinearLayoutManager(this@AdminActivity)
            adapter = chamadoAdapter
        }
    }

    private fun abrirAtualizacao(chamado: Chamado) {
        if (chamado.id == null) {
            Toast.makeText(this, "Chamado sem número; recarregue a lista", Toast.LENGTH_SHORT).show()
            return
        }
        atualizarChamadoLauncher.launch(AtualizarChamadoActivity.criarIntent(this, chamado, roleAtual))
    }

    private fun carregarChamados() {
        binding.progressChamados.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val (respChamados, nomesSalas, nomesEquip) = withContext(Dispatchers.IO) {
                    val chamados = api.listarChamados()
                    val salas = runCatching { api.listarSalas().body()?.itens.orEmpty() }.getOrDefault(emptyList())
                    val equipamentos = runCatching { api.listarEquipamentos().body()?.itens.orEmpty() }.getOrDefault(emptyList())
                    Triple(
                        chamados,
                        salas.mapNotNull { s -> s.idSala?.let { it to (s.nome ?: "Sala #$it") } }.toMap(),
                        equipamentos.mapNotNull { e -> e.id?.let { it to e.toString() } }.toMap()
                    )
                }

                when {
                    respChamados.isSuccessful -> {
                        val pagina = respChamados.body()
                        // Mais recentes primeiro
                        todosChamados = pagina?.itens.orEmpty()
                            .sortedByDescending { it.idChamado ?: 0L }
                            .map { Chamado.fromDto(it, nomesSalas, nomesEquip) }
                        Log.d(
                            "AdminActivity",
                            "Chamados recebidos: ${todosChamados.size} de ${pagina?.totalItens ?: 0} no backend"
                        )
                        aplicarFiltros()
                    }
                    respChamados.code() == 401 -> sessaoExpirada()
                    else -> {
                        Log.e("AdminActivity", "Erro ao listar chamados: ${respChamados.code()}")
                        Toast.makeText(this@AdminActivity, "Erro ao carregar chamados", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("AdminActivity", "Erro ao carregar chamados", e)
                Toast.makeText(this@AdminActivity, "Falha ao carregar chamados", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressChamados.visibility = View.GONE
            }
        }
    }

    // ---------------- Filtros ----------------

    private fun configurarFiltros() {
        binding.chipGroupStatus.setOnCheckedStateChangeListener { _, _ -> aplicarFiltros() }
        binding.etBuscaChamado.doAfterTextChanged { aplicarFiltros() }
    }

    /** Já abre no filtro que interessa a cada papel. */
    private fun selecionarFiltroPadrao(role: String?) {
        val chip = when (role) {
            "APROVADOR" -> R.id.chipAguardando
            "SUPORTE" -> R.id.chipAtendimento
            "GERENTE" -> R.id.chipAprovado     // aprovados aguardam a triagem do gerente
            else -> R.id.chipTodos
        }
        binding.chipGroupStatus.check(chip)
    }

    /** Status do backend correspondente ao chip marcado (null = todos). */
    private fun statusSelecionado(): String? = when (binding.chipGroupStatus.checkedChipId) {
        R.id.chipAguardando -> "AGUARDANDO_APROVACAO"
        R.id.chipAprovado -> "APROVADO"
        R.id.chipAtendimento -> "EM_ATENDIMENTO"
        R.id.chipResolvido -> "RESOLVIDO"
        R.id.chipReprovado -> "REPROVADO"
        else -> null
    }

    private fun aplicarFiltros() {
        val status = statusSelecionado()
        val busca = binding.etBuscaChamado.text?.toString()?.trim()?.lowercase().orEmpty()

        val filtrados = todosChamados.filter { c ->
            val statusOk = status == null || c.status.equals(status, ignoreCase = true)
            val buscaOk = busca.isEmpty() || listOf(
                c.id?.toString().orEmpty(),
                "#${c.id}",
                c.titulo,
                c.descricao,
                c.sala,
                c.maquina,
                c.categoria
            ).any { it.lowercase().contains(busca) }
            statusOk && buscaOk
        }

        chamadoAdapter.atualizarLista(filtrados)

        binding.tvTotalChamados.text = when {
            todosChamados.isEmpty() -> "nenhum"
            filtrados.size == todosChamados.size -> "${todosChamados.size} chamado(s)"
            else -> "${filtrados.size} de ${todosChamados.size}"
        }
        binding.tvListaVazia.visibility = if (filtrados.isEmpty()) View.VISIBLE else View.GONE
        binding.tvListaVazia.text = if (todosChamados.isEmpty()) {
            "Nenhum chamado cadastrado."
        } else {
            "Nenhum chamado encontrado com esse filtro."
        }
    }

    // ---------------- Cabeçalho ----------------

    private fun configurarListeners() {
        binding.btnAlternarTemaAdmin.setOnClickListener { alternarTema() }
        binding.btnSairAdmin.setOnClickListener { realizarLogout() }
        binding.btnNovoChamadoAdmin.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    private fun alternarTema() {
        val modoAtual = AppCompatDelegate.getDefaultNightMode()
        if (modoAtual == AppCompatDelegate.MODE_NIGHT_YES) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }

    private fun sessaoExpirada() {
        Toast.makeText(this, "Sessão expirada. Faça login novamente.", Toast.LENGTH_LONG).show()
        irParaLogin()
    }

    private fun realizarLogout() {
        Toast.makeText(this, "Sessão encerrada", Toast.LENGTH_SHORT).show()
        irParaLogin()
    }

    private fun irParaLogin() {
        TokenManager(this).clearToken()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
