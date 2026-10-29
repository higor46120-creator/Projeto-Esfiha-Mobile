package br.com.fiec.helptec.data.repository

import br.com.fiec.helptec.data.model.Chamado

object ChamadoRepository {
    val listaChamados = mutableListOf<Chamado>()
    private var contadorId = 1

    fun adicionarChamado(chamado: Chamado) {
        listaChamados.add(0, chamado)
    }

    fun gerarId(): Int = contadorId++
}