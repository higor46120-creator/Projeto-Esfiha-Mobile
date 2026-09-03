package br.com.fiec.helptec

import java.io.Serializable

// Declaração do modelo de dados do chamado.
// Implementa Serializable para permitir o envio do objeto entre telas caso necessário.
data class Chamado(
    val id: Int,              // Identificador único gerado sequencialmente
    val titulo: String,          // Título resumido do problema
    val sala: String,            // Sala/Laboratório onde está o problema
    val maquina: String,         // Número ou identificador da máquina
    val categoria: String,       // Categoria do problema (Hardware, Rede, etc.)
    val prioridade: String,      // Grau de urgência (Baixa, Média, Alta)
    val descricao: String,       // Explicação detalhada do problema
    var status: String = "Aberto",    // Status mutável (Aberto, Em Andamento, Concluído)
    var devolutiva: String = ""       // Solução técnica inserida pelo Administrador
) : Serializable