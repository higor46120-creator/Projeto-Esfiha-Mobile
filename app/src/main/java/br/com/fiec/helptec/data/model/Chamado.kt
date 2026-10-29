package br.com.fiec.helptec.data.model

data class Chamado(
    val id: Long? = null,
    val titulo: String,
    val sala: String,
    val maquina: String,
    val categoria: String,
    val prioridade: String,
    val descricao: String,
    val devolutiva: String,
    val status: String
)