package br.com.fiec.helptec.data

data class ChamadoRequest(
    val titulo: String,
    val sala: String,
    val maquina: String,
    val categoria: String,
    val prioridade: String,
    val descricao: String,
    val latitude: Double?,
    val longitude: Double?
)

data class ChamadoResponse(
    val id: Long?,
    val titulo: String,
    val sala: String,
    val maquina: String,
    val categoria: String,
    val prioridade: String,
    val descricao: String,
    val status: String? = "ABERTO",
    val latitude: Double?,
    val longitude: Double?
)