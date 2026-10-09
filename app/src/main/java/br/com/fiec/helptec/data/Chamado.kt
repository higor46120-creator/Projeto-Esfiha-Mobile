package br.com.fiec.helptec.data

data class Chamado(
    val id: Long? = null,
    val titulo: String,
    val sala: String,
    val maquina: String,
    val categoria: String,
    val prioridade: String,
    val descricao: String,
    val status: String = "ABERTO",
    val devolutiva: String? = "",
    val latitude: Double? = null,
    val longitude: Double? = null
)