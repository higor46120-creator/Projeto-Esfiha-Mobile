package br.com.fiec.helptec.data.model

import br.com.fiec.helptec.data.ChamadoDTO

/**
 * Modelo usado SÓ nas telas (lista e adapter).
 * O que vai/vem do backend é o ChamadoDTO; a conversão fica em [Chamado.fromDto].
 */
data class Chamado(
    val id: Long? = null,
    val titulo: String = "",
    val sala: String = "",
    val maquina: String = "",
    val categoria: String = "",
    val prioridade: String? = null,
    val descricao: String = "",
    val devolutiva: String? = null,
    val status: String? = null
) {
    companion object {

        // O backend não tem campos de título/categoria. O app grava na descrição assim:
        //   "[Categoria] Título\nTexto da descrição"
        // e separa de volta na hora de exibir.
        private val CABECALHO = Regex("""^\[(.+?)]\s*(.*)$""")

        fun montarDescricao(categoria: String, titulo: String, descricao: String): String =
            "[$categoria] $titulo\n$descricao"

        fun fromDto(
            dto: ChamadoDTO,
            nomesSalas: Map<Long, String> = emptyMap(),
            nomesEquipamentos: Map<Long, String> = emptyMap()
        ): Chamado {
            val textoCompleto = dto.descricao.orEmpty()
            val primeiraLinha = textoCompleto.substringBefore('\n')
            val resto = if (textoCompleto.contains('\n')) textoCompleto.substringAfter('\n') else ""

            val match = CABECALHO.find(primeiraLinha)
            val categoria = match?.groupValues?.get(1).orEmpty()
            val titulo = match?.groupValues?.get(2)?.ifBlank { null }
                ?: primeiraLinha.take(60)
            val descricao = if (match != null) resto else textoCompleto

            return Chamado(
                id = dto.idChamado,
                titulo = titulo,
                sala = dto.idSala?.let { nomesSalas[it] ?: "Sala #$it" }.orEmpty(),
                maquina = dto.idEquipamento?.let { nomesEquipamentos[it] ?: "#$it" }.orEmpty(),
                categoria = categoria.ifBlank { "-" },
                prioridade = dto.prioridade,
                descricao = descricao,
                devolutiva = dto.resolucao,
                status = dto.status
            )
        }
    }
}
