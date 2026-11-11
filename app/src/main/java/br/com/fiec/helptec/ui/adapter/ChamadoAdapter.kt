package br.com.fiec.helptec.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.fiec.helptec.R
import br.com.fiec.helptec.data.model.Chamado
import br.com.fiec.helptec.ui.ChamadoVisual

class ChamadoAdapter(
    private var chamados: List<Chamado> = emptyList(),
    private val onItemClick: ((Chamado, Int) -> Unit)? = null
) : RecyclerView.Adapter<ChamadoAdapter.ChamadoViewHolder>() {

    class ChamadoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitulo: TextView = view.findViewById(R.id.tvTituloItem)
        val tvLocal: TextView = view.findViewById(R.id.tvLocalItem)
        val tvCategoria: TextView = view.findViewById(R.id.tvCategoriaItem)
        val tvPrioridade: TextView = view.findViewById(R.id.tvPrioridadeItem)
        val tvDescricao: TextView = view.findViewById(R.id.tvDescricaoItem)
        val tvStatus: TextView = view.findViewById(R.id.tvStatusItem)
        val tvDevolutiva: TextView = view.findViewById(R.id.tvDevolutivaItem)
        val vBarraStatus: View = view.findViewById(R.id.vBarraStatus)
    }

    fun atualizarLista(novaLista: List<Chamado>) {
        chamados = novaLista
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChamadoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chamado, parent, false)
        return ChamadoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChamadoViewHolder, position: Int) {
        val item = chamados[position]

        holder.tvTitulo.text = if (item.id != null) "#${item.id} · ${item.titulo}" else item.titulo
        holder.tvDescricao.text = item.descricao
        holder.tvDescricao.visibility = if (item.descricao.isBlank()) View.GONE else View.VISIBLE

        val local = listOf(item.sala, item.maquina).filter { it.isNotBlank() }.joinToString(" · ")
        holder.tvLocal.text = local.ifBlank { "Local não informado" }
        holder.tvCategoria.text = item.categoria.ifBlank { "-" }

        // Prioridade
        holder.tvPrioridade.text = ChamadoVisual.rotuloPrioridade(item.prioridade)
        ChamadoVisual.pintar(holder.tvPrioridade, ChamadoVisual.coresPrioridade(item.prioridade))

        // Status (badge + barra lateral)
        val coresStatus = ChamadoVisual.coresStatus(item.status)
        holder.tvStatus.text = ChamadoVisual.rotuloStatus(item.status)
        ChamadoVisual.pintar(holder.tvStatus, coresStatus)
        ChamadoVisual.pintarBarra(holder.vBarraStatus, coresStatus)

        if (!item.devolutiva.isNullOrBlank()) {
            holder.tvDevolutiva.visibility = View.VISIBLE
            holder.tvDevolutiva.text = "Devolutiva: ${item.devolutiva}"
        } else {
            holder.tvDevolutiva.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(item, position)
        }
    }

    override fun getItemCount(): Int = chamados.size
}
