package br.com.fiec.helptec.admin

import androidx.lifecycle.ViewModel
import br.com.fiec.helptec.data.model.Chamado

/**
 * Estado da tela de gestão. Sobrevive à rotação da tela, então a lista
 * não é buscada de novo no backend só porque o aparelho girou.
 */
class AdminViewModel : ViewModel() {

    /** Papel do usuário logado, vindo do /usuarios/me. */
    var roleAtual: String? = null

    /** Todos os chamados vindos do backend; os filtros trabalham em cima desta lista. */
    var todosChamados: List<Chamado> = emptyList()

    /** Chip de status escolhido (View.NO_ID = nenhum ainda). */
    var chipSelecionado: Int = android.view.View.NO_ID

    /** true depois do primeiro carregamento com sucesso. */
    var carregado: Boolean = false
}
