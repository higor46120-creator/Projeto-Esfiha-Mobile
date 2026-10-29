package br.com.fiec.helptec.main
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel : ViewModel() {

    private val _descricao = MutableStateFlow("")
    val descricao: StateFlow<String> = _descricao.asStateFlow()

    fun updateDescricao(novoTexto: String) {
        val textoAtual = _descricao.value
        _descricao.value = if (textoAtual.isBlank()) {
            novoTexto
        } else {
            "$textoAtual $novoTexto"
        }
    }
}