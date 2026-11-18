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

        // Versão antiga: sala/máquina digitadas iam no fim da descrição. Hoje vão nos campos
        // salaInformada/equipamentoInformado; a leitura abaixo continua para chamados antigos.
        // Formato antigo, em linhas no fim da descrição:
        //   "Sala informada: Lab 7"
        //   "Máquina informada: PC do professor"
        private const val SALA_INFORMADA = "Sala informada: "
        private const val MAQUINA_INFORMADA = "Máquina informada: "

        fun montarDescricao(
            categoria: String,
            titulo: String,
            descricao: String,
            salaInformada: String? = null,
            maquinaInformada: String? = null
        ): String {
            val extras = listOfNotNull(
                salaInformada?.takeIf { it.isNotBlank() }?.let { SALA_INFORMADA + it },
                maquinaInformada?.takeIf { it.isNotBlank() }?.let { MAQUINA_INFORMADA + it }
            )
            val base = "[$categoria] $titulo\n$descricao"
            return if (extras.isEmpty()) base else base + "\n\n" + extras.joinToString("\n")
        }

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
            val descricaoBruta = if (match != null) resto else textoCompleto

            // Separa as linhas de sala/máquina digitadas do texto do problema
            val linhas = descricaoBruta.lines()
            val salaInformada = linhas.firstOrNull { it.startsWith(SALA_INFORMADA) }
                ?.removePrefix(SALA_INFORMADA)?.trim()
            val maquinaInformada = linhas.firstOrNull { it.startsWith(MAQUINA_INFORMADA) }
                ?.removePrefix(MAQUINA_INFORMADA)?.trim()
            val descricao = linhas
                .filterNot { it.startsWith(SALA_INFORMADA) || it.startsWith(MAQUINA_INFORMADA) }
                .joinToString("\n")
                .trimEnd()

            return Chamado(
                id = dto.idChamado,
                titulo = titulo,
                // Cadastro > campo do backend > linha antiga na descrição (chamados mais velhos)
                sala = dto.idSala?.let { nomesSalas[it] ?: "Sala #$it" }
                    ?: dto.salaInformada ?: salaInformada.orEmpty(),
                maquina = dto.idEquipamento?.let { nomesEquipamentos[it] ?: "#$it" }
                    ?: dto.equipamentoInformado ?: maquinaInformada.orEmpty(),
                categoria = categoria.ifBlank { "-" },
                prioridade = dto.prioridade,
                descricao = descricao,
                devolutiva = dto.resolucao,
                status = dto.status
            )
        }
    }
}
