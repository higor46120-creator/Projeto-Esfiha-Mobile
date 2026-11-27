package br.com.fiec.helptec.data

// =====================================================================
// Modelos que espelham EXATAMENTE os DTOs do backend (Spring).
// Todos os campos têm valor padrão para o Gson conseguir criar o objeto
// mesmo quando algum campo vier ausente/nulo no JSON.
// =====================================================================

/** Resposta paginada do backend: PageResponseDTO<T> */
data class PageResponse<T>(
    val itens: List<T>? = null,
    val totalItens: Long = 0,
    val paginaAtual: Int = 0,
    val totalPaginas: Int = 0
)

/** ChamadoDTO do backend */
data class ChamadoDTO(
    val idChamado: Long? = null,
    val idUsuario: String? = null,
    val area: String? = null,
    val idSuporte: String? = null,
    val descricao: String? = null,
    val status: String? = null,
    val prioridade: String? = null,
    val criticidade: String? = null,
    val dataAbertura: String? = null,
    val dataFinal: String? = null,
    val resolucao: String? = null,
    val idEquipamento: Long? = null,
    val idSala: Long? = null,
    val salaInformada: String? = null,        // sala digitada (sem cadastro)
    val equipamentoInformado: String? = null  // máquina digitada (sem cadastro)
)

/**
 * Corpo do POST /api/chamados.
 * O solicitante (idUsuario) NÃO é enviado: o backend pega do JWT.
 * Sala/máquina do cadastro vão pelo id. Se o usuário digitou uma que não existe,
 * o id fica nulo e o texto vai em salaInformada/equipamentoInformado.
 */
data class NovoChamadoRequest(
    val descricao: String,
    val prioridade: String,
    val criticidade: String,
    val idSala: Long?,
    val idEquipamento: Long?,
    val salaInformada: String? = null,
    val equipamentoInformado: String? = null
)

/** SalasDTO do backend */
data class SalaDTO(
    val idSala: Long? = null,
    val nome: String? = null
) {
    override fun toString(): String = nome ?: "Sala #$idSala"
}

/** EquipamentoDTO do backend */
data class EquipamentoDTO(
    val id: Long? = null,
    val nome: String? = null,
    val patrimonio: String? = null,
    val status: String? = null,
    val tipo: String? = null,
    val preco: Double? = null
) {
    override fun toString(): String = when {
        !patrimonio.isNullOrBlank() -> "${nome ?: "Equipamento"} ($patrimonio)"
        else -> nome ?: "Equipamento #$id"
    }
}

/** MeResponseDTO do backend (GET /usuarios/me) */
data class MeResponse(
    val idUsuario: String? = null,
    val nome: String? = null,
    val email: String? = null,
    val role: String? = null,
    val area: String? = null
)
