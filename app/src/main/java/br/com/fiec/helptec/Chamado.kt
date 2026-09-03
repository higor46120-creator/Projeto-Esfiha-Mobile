package br.com.fiec.helptec

import java.io.Serializable

data class Chamado(
    val id: Int,
    val titulo: String,
    val sala: String,
    val maquina: String,
    val categoria: String,
    val prioridade: String,
    val descricao: String,
    var status: String = "Aberto",
    var devolutiva: String = ""
) : Serializable