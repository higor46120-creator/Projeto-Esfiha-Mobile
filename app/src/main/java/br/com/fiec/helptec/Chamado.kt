package br.com.fiec.helptec

data class Chamado(
    val id: Int,
    val titulo: String,
    val sala: String,
    val maquina: String,
    val categoria: String,
    val prioridade: String,
    val descricao: String,
    val status: String = "Aberto"
)