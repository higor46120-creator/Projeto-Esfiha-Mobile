package br.com.fiec.helptec.ui

import android.content.res.ColorStateList
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import br.com.fiec.helptec.R

/**
 * Rótulos e cores de status/prioridade dos chamados, iguais ao TicketList do front-end.
 * Usado pela lista (ChamadoAdapter) e pela tela de atualização.
 */
object ChamadoVisual {

    data class Cores(@ColorRes val fundo: Int, @ColorRes val texto: Int)

    private val cinza = Cores(R.color.ht_badge_gray_bg, R.color.ht_badge_gray_fg)
    private val azul = Cores(R.color.ht_badge_blue_bg, R.color.ht_badge_blue_fg)
    private val amarelo = Cores(R.color.ht_badge_yellow_bg, R.color.ht_badge_yellow_fg)
    private val roxo = Cores(R.color.ht_badge_purple_bg, R.color.ht_badge_purple_fg)
    private val vermelho = Cores(R.color.ht_badge_red_bg, R.color.ht_badge_red_fg)
    private val verde = Cores(R.color.ht_badge_green_bg, R.color.ht_badge_green_fg)

    fun rotuloStatus(status: String?): String {
        val s = status?.uppercase().orEmpty()
        return when (s) {
            "AGUARDANDO_APROVACAO" -> "Aguardando aprovação"
            "APROVADO" -> "Aprovado"
            "REPROVADO" -> "Reprovado"
            "EM_ATENDIMENTO" -> "Em atendimento"
            "RESOLVIDO" -> "Resolvido"
            "CANCELADO" -> "Cancelado"
            "" -> "Sem status"
            else -> s.lowercase().replaceFirstChar { it.uppercase() }.replace('_', ' ')
        }
    }

    fun coresStatus(status: String?): Cores = when (status?.uppercase()) {
        "AGUARDANDO_APROVACAO" -> amarelo
        "APROVADO" -> azul
        "EM_ATENDIMENTO" -> roxo
        "RESOLVIDO" -> verde
        "REPROVADO" -> vermelho
        else -> cinza
    }

    fun rotuloPrioridade(prioridade: String?): String = when (prioridade?.uppercase()) {
        "ALTA" -> "Alta"
        "MEDIA", "MÉDIA" -> "Média"
        "BAIXA" -> "Baixa"
        "CRITICA", "CRÍTICA" -> "Crítica"
        null, "" -> "—"
        else -> prioridade.orEmpty()
    }

    // Igual ao PRIORITY_COLOR do web: Baixa=cinza, Média=azul, Alta=amarelo, Crítica=vermelho
    fun coresPrioridade(prioridade: String?): Cores = when (prioridade?.uppercase()) {
        "MEDIA", "MÉDIA" -> azul
        "ALTA" -> amarelo
        "CRITICA", "CRÍTICA" -> vermelho
        else -> cinza
    }

    /** Aplica fundo (pílula) e cor do texto de um badge. */
    fun pintar(view: TextView, cores: Cores) {
        val ctx = view.context
        view.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, cores.fundo))
        view.setTextColor(ContextCompat.getColor(ctx, cores.texto))
    }

    /** Pinta uma barra/indicador com a cor forte do status. */
    fun pintarBarra(view: View, cores: Cores) {
        view.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(view.context, cores.texto))
    }
}
