package br.com.fiec.helptec.main

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel : ViewModel() {

    private val _descricao = MutableStateFlow("")
    val descricao: StateFlow<String> = _descricao.asStateFlow()

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
}
