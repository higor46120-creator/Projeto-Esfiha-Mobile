package br.com.fiec.helptec

// Objeto Singleton para armazenar os dados centralizados na memória do app.
// Garante que o Usuário Comum e o Administrador acessem a mesma lista de chamados.
object ChamadoRepository {
    // Lista em memória compartilhada por todas as telas da aplicação
    val listaChamados = mutableListOf<Chamado>()
    private var contadorId = 1

    // Adiciona o novo chamado sempre na primeira posição (índice 0) para exibição do mais recente
    fun adicionarChamado(chamado: Chamado) {
        listaChamados.add(0, chamado)
    }

    // Auto-incremento manual para gerar IDs numéricos únicos (#1, #2, #3...)
    fun gerarId(): Int = contadorId++
}