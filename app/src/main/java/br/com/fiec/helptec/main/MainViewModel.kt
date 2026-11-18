package br.com.fiec.helptec.main

import androidx.lifecycle.ViewModel
import br.com.fiec.helptec.data.EquipamentoDTO
import br.com.fiec.helptec.data.SalaDTO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Estado da tela "Abrir chamado". O ViewModel sobrevive à rotação da tela,
 * então nada aqui é perdido nem buscado de novo no backend ao girar o aparelho.
 */
class MainViewModel : ViewModel() {

    private val _descricao = MutableStateFlow("")
    val descricao: StateFlow<String> = _descricao.asStateFlow()

    /** Listas dos dropdowns (null = ainda não carregadas do backend). */
    var salas: List<SalaDTO>? = null
    var equipamentos: List<EquipamentoDTO>? = null

    /** Itens escolhidos nos dropdowns. */
    var salaSelecionada: SalaDTO? = null
    var equipamentoSelecionado: EquipamentoDTO? = null

    /** Resultado do /usuarios/me para o botão "Gestão" (null = ainda não consultado). */
    var podeAcessarGestao: Boolean? = null

    /** Digitação: SUBSTITUI o texto pelo conteúdo atual do campo. */
    fun setDescricao(texto: String) {
        _descricao.value = texto
    }

    /** Reconhecimento de voz: ACRESCENTA o trecho falado ao que já foi escrito. */
    fun acrescentarDescricao(trecho: String) {
        if (trecho.isBlank()) return
        val atual = _descricao.value
        _descricao.value = if (atual.isBlank()) trecho else "$atual $trecho"
    }

    fun limparSelecoes() {
        salaSelecionada = null
        equipamentoSelecionado = null
        _descricao.value = ""
    }
}
