package br.com.fiec.helptec.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiec.helptec.data.ApiService
import br.com.fiec.helptec.data.ChamadoDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response

/**
 * Executa a ação (aprovar, reprovar, resolver) fora da Activity.
 * Se o usuário girar a tela durante o envio, a requisição continua
 * e a tela nova recebe o resultado, sem enviar a ação duas vezes.
 */
class AtualizarChamadoViewModel : ViewModel() {

    sealed interface Estado {
        object Ocioso : Estado
        object Enviando : Estado
        object Sucesso : Estado
        object SessaoExpirada : Estado
        data class Erro(val mensagem: String) : Estado
    }

    companion object {
        const val ACAO_APROVAR = "Aprovar"
        const val ACAO_REPROVAR = "Reprovar"
        const val ACAO_RESOLVER = "Resolver"
    }

    private val _estado = MutableStateFlow<Estado>(Estado.Ocioso)
    val estado: StateFlow<Estado> = _estado.asStateFlow()

    fun executar(api: ApiService, idChamado: Long, acao: String, devolutiva: String) {
        if (_estado.value == Estado.Enviando) return
        _estado.value = Estado.Enviando

        viewModelScope.launch {
            _estado.value = try {
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
                    response == null -> Estado.Erro("Ação inválida")
                    response.isSuccessful -> Estado.Sucesso
                    response.code() == 401 -> Estado.SessaoExpirada
                    response.code() == 403 -> Estado.Erro("Seu perfil não tem permissão para esta ação")
                    else -> {
                        val erro = withContext(Dispatchers.IO) { response.errorBody()?.string() }
                        Log.e("AtualizarChamado", "Erro ${response.code()}: $erro")
                        Estado.Erro("Não foi possível: o status do chamado pode ter mudado (${response.code()})")
                    }
                }
            } catch (e: Exception) {
                Log.e("AtualizarChamado", "Erro ao atualizar chamado", e)
                Estado.Erro("Falha de conexão com o servidor")
            }
        }
    }

    /** Chamado pela tela depois de mostrar um erro, para não repetir o aviso ao girar. */
    fun erroExibido() {
        if (_estado.value is Estado.Erro) _estado.value = Estado.Ocioso
    }
}
