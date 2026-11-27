package br.com.fiec.helptec.admin

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import br.com.fiec.helptec.auth.LoginActivity
import br.com.fiec.helptec.data.ApiService
import br.com.fiec.helptec.data.ChamadoDTO
import br.com.fiec.helptec.data.RetrofitClient
import br.com.fiec.helptec.data.model.Chamado
import br.com.fiec.helptec.databinding.ActivityAtualizarChamadoBinding
import br.com.fiec.helptec.network.TokenManager
import br.com.fiec.helptec.ui.ChamadoVisual
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response

/**
 * Tela para atualizar UM chamado (aberta pela gestão).
 * Mostra os detalhes e só oferece as ações que fazem sentido para o papel do usuário
 * E para o status atual do chamado (as mesmas regras do backend):
 *
 *   AGUARDANDO_APROVACAO -> Aprovar / Reprovar   (APROVADOR, ADMIN)
 *   EM_ATENDIMENTO       -> Resolver             (SUPORTE, ADMIN)
 *   APROVADO             -> triagem é feita no sistema web (GERENTE)
 *
 * Ao concluir, devolve RESULT_OK e fecha: a gestão recarrega a lista.
 */
class AtualizarChamadoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAtualizarChamadoBinding
    private lateinit var api: ApiService

    private var idChamado: Long = -1L
    private var status: String? = null
    private var role: String? = null

    companion object {
        private const val EXTRA_ID = "extra_id"
        private const val EXTRA_TITULO = "extra_titulo"
        private const val EXTRA_DESCRICAO = "extra_descricao"
        private const val EXTRA_SALA = "extra_sala"
        private const val EXTRA_MAQUINA = "extra_maquina"
        private const val EXTRA_CATEGORIA = "extra_categoria"
        private const val EXTRA_PRIORIDADE = "extra_prioridade"
        private const val EXTRA_STATUS = "extra_status"
        private const val EXTRA_DEVOLUTIVA = "extra_devolutiva"
        private const val EXTRA_ROLE = "extra_role"

        private const val ACAO_APROVAR = "Aprovar"
        private const val ACAO_REPROVAR = "Reprovar"
        private const val ACAO_RESOLVER = "Resolver"

        /** Monta o Intent com os dados do chamado selecionado na lista. */
        fun criarIntent(context: Context, chamado: Chamado, role: String?): Intent =
            Intent(context, AtualizarChamadoActivity::class.java).apply {
                putExtra(EXTRA_ID, chamado.id ?: -1L)
                putExtra(EXTRA_TITULO, chamado.titulo)
                putExtra(EXTRA_DESCRICAO, chamado.descricao)
                putExtra(EXTRA_SALA, chamado.sala)
                putExtra(EXTRA_MAQUINA, chamado.maquina)
                putExtra(EXTRA_CATEGORIA, chamado.categoria)
                putExtra(EXTRA_PRIORIDADE, chamado.prioridade)
                putExtra(EXTRA_STATUS, chamado.status)
                putExtra(EXTRA_DEVOLUTIVA, chamado.devolutiva)
                putExtra(EXTRA_ROLE, role)
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAtualizarChamadoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        api = RetrofitClient.getInstance(applicationContext)

        idChamado = intent.getLongExtra(EXTRA_ID, -1L)
        status = intent.getStringExtra(EXTRA_STATUS)?.uppercase()
        role = intent.getStringExtra(EXTRA_ROLE)?.uppercase()

        if (idChamado <= 0) {
            Toast.makeText(this, "Chamado inválido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        preencherDetalhes()
        configurarAcoes()

        binding.btnVoltar.setOnClickListener { finish() }
        binding.btnExecutarAcao.setOnClickListener { executarAcao() }
    }

    private fun preencherDetalhes() {
        binding.tvNumeroChamado.text = "Chamado #$idChamado"
        binding.tvTituloDetalhe.text = intent.getStringExtra(EXTRA_TITULO).orEmpty().ifBlank { "Sem título" }
        binding.tvDescricaoDetalhe.text = intent.getStringExtra(EXTRA_DESCRICAO).orEmpty().ifBlank { "-" }

        val local = listOf(
            intent.getStringExtra(EXTRA_SALA).orEmpty(),
            intent.getStringExtra(EXTRA_MAQUINA).orEmpty()
        ).filter { it.isNotBlank() }.joinToString(" · ")
        binding.tvLocalDetalhe.text = local.ifBlank { "Local não informado" }
        binding.tvCategoriaDetalhe.text = intent.getStringExtra(EXTRA_CATEGORIA).orEmpty().ifBlank { "-" }

        binding.tvStatusDetalhe.text = ChamadoVisual.rotuloStatus(status)
        ChamadoVisual.pintar(binding.tvStatusDetalhe, ChamadoVisual.coresStatus(status))

        val prioridade = intent.getStringExtra(EXTRA_PRIORIDADE)
        binding.tvPrioridadeDetalhe.text = ChamadoVisual.rotuloPrioridade(prioridade)
        ChamadoVisual.pintar(binding.tvPrioridadeDetalhe, ChamadoVisual.coresPrioridade(prioridade))

        val devolutiva = intent.getStringExtra(EXTRA_DEVOLUTIVA)
        if (!devolutiva.isNullOrBlank()) {
            binding.tvDevolutivaDetalhe.visibility = View.VISIBLE
            binding.tvDevolutivaDetalhe.text = "Devolutiva: $devolutiva"
            binding.etDevolutiva.setText(devolutiva)
        }
    }

    /** Ações possíveis = permitidas pelo papel ∩ válidas para o status atual. */
    private fun acoesDisponiveis(): List<String> {
        val podeAprovar = role in setOf("ADMIN", "APROVADOR")
        val podeResolver = role in setOf("ADMIN", "SUPORTE")

        return when (status) {
            "AGUARDANDO_APROVACAO" -> if (podeAprovar) listOf(ACAO_APROVAR, ACAO_REPROVAR) else emptyList()
            "EM_ATENDIMENTO" -> if (podeResolver) listOf(ACAO_RESOLVER) else emptyList()
            else -> emptyList()
        }
    }

    private fun motivoSemAcao(): String = when (status) {
        "AGUARDANDO_APROVACAO" -> "Somente o aprovador ou um administrador pode aprovar este chamado."
        "APROVADO" -> "Chamado aprovado: aguardando a triagem do gerente, feita pelo sistema web."
        "EM_ATENDIMENTO" -> "Somente o suporte ou um administrador pode resolver este chamado."
        "RESOLVIDO" -> "Este chamado já foi resolvido."
        "REPROVADO" -> "Este chamado foi reprovado."
        "CANCELADO" -> "Este chamado foi cancelado."
        else -> "Nenhuma ação disponível para este chamado."
    }

    private fun configurarAcoes() {
        val acoes = acoesDisponiveis()

        if (acoes.isEmpty()) {
            binding.tvSemAcao.visibility = View.VISIBLE
            binding.tvSemAcao.text = motivoSemAcao()
            binding.tilAcao.visibility = View.GONE
            binding.tilDevolutiva.visibility = View.GONE
            binding.btnExecutarAcao.visibility = View.GONE
            return
        }

        binding.spAcao.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, acoes))
        // Já deixa selecionada a ação mais provável
        binding.spAcao.setText(acoes.first(), false)

        // A devolutiva só é usada ao resolver
        val resolve = acoes.contains(ACAO_RESOLVER)
        binding.tilDevolutiva.visibility = if (resolve) View.VISIBLE else View.GONE
    }

    private fun executarAcao() {
        val acao = binding.spAcao.text.toString().trim()
        val devolutiva = binding.etDevolutiva.text?.toString()?.trim().orEmpty()

        if (acao.isEmpty()) {
            Toast.makeText(this, "Escolha uma ação", Toast.LENGTH_SHORT).show()
            return
        }
        if (acao == ACAO_RESOLVER && devolutiva.isEmpty()) {
            binding.tilDevolutiva.error = "Escreva a devolutiva para resolver o chamado"
            return
        }
        binding.tilDevolutiva.error = null

        carregando(true)

        lifecycleScope.launch {
            try {
                val response: Response<ChamadoDTO>? = withContext(Dispatchers.IO) {
                    when (acao) {
                        ACAO_APROVAR -> api.aprovarChamado(idChamado)
                        ACAO_REPROVAR -> api.reprovarChamado(idChamado)
                        ACAO_RESOLVER -> api.resolverChamado(
                            idChamado,
                            devolutiva.toRequestBody("text/plain; charset=utf-8".toMediaType())
                        )
                        else -> null
                    }
                }

                when {
                    response == null -> Toast.makeText(
                        this@AtualizarChamadoActivity, "Ação inválida", Toast.LENGTH_SHORT
                    ).show()

                    response.isSuccessful -> {
                        Toast.makeText(
                            this@AtualizarChamadoActivity,
                            "Chamado #$idChamado atualizado!",
                            Toast.LENGTH_SHORT
                        ).show()
                        setResult(Activity.RESULT_OK)
                        finish()   // volta para a lista, que recarrega
                    }

                    response.code() == 401 -> sessaoExpirada()

                    response.code() == 403 -> Toast.makeText(
                        this@AtualizarChamadoActivity,
                        "Seu perfil não tem permissão para esta ação",
                        Toast.LENGTH_LONG
                    ).show()

                    else -> {
                        val erro = response.errorBody()?.string()
                        Log.e("AtualizarChamado", "Erro ${response.code()}: $erro")
                        Toast.makeText(
                            this@AtualizarChamadoActivity,
                            "Não foi possível: o status do chamado pode ter mudado (${response.code()})",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("AtualizarChamado", "Erro ao atualizar chamado", e)
                Toast.makeText(
                    this@AtualizarChamadoActivity,
                    "Falha de conexão com o servidor",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                if (!isFinishing) carregando(false)
            }
        }
    }

    private fun carregando(ativo: Boolean) {
        binding.progressAcao.visibility = if (ativo) View.VISIBLE else View.GONE
        binding.btnExecutarAcao.isEnabled = !ativo
    }

    private fun sessaoExpirada() {
        Toast.makeText(this, "Sessão expirada. Faça login novamente.", Toast.LENGTH_LONG).show()
        TokenManager(this).clearToken()
        startActivity(
            Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }
}
