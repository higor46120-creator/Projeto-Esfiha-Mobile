package br.com.fiec.helptec

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChamadoAdapter(
    private val chamados: List<Chamado>,
    // Callback opcional ativado ao clicar em um item (usado pela AdminActivity)
    private val onItemClick: ((Chamado, Int) -> Unit)? = null
) : RecyclerView.Adapter<ChamadoAdapter.ChamadoViewHolder>() {

    // ViewHolder: Mapeia as Views de um item individual do card
    class ChamadoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitulo: TextView = view.findViewById(R.id.tvTituloItem)
        val tvLocal: TextView = view.findViewById(R.id.tvLocalItem)
        val tvCategoria: TextView = view.findViewById(R.id.tvCategoriaItem)
        val tvPrioridade: TextView = view.findViewById(R.id.tvPrioridadeItem)
        val tvDescricao: TextView = view.findViewById(R.id.tvDescricaoItem)
        val tvStatus: TextView = view.findViewById(R.id.tvStatusItem)
        val tvDevolutiva: TextView = view.findViewById(R.id.tvDevolutivaItem)
    }

    // Infla o layout do XML item_chamado para a lista
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChamadoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chamado, parent, false)
        return ChamadoViewHolder(view)
    }

    // Associa os dados do objeto Chamado aos componentes da interface
    override fun onBindViewHolder(holder: ChamadoViewHolder, position: Int) {
        val item = chamados[position]
        holder.tvTitulo.text = "#${item.id} - ${item.titulo}"
        holder.tvLocal.text = "Local: ${item.sala} (Máq. ${item.maquina})"
        holder.tvCategoria.text = "Categoria: ${item.categoria}"
        holder.tvPrioridade.text = item.prioridade.uppercase()
        holder.tvDescricao.text = item.descricao
        holder.tvStatus.text = "Status: ${item.status}"

        // Lógica visual: Se a devolutiva tiver texto, exibe a caixa verde; se vazia, oculta o elemento
        if (item.devolutiva.isNotBlank()) {
            holder.tvDevolutiva.visibility = View.VISIBLE
            holder.tvDevolutiva.text = "Devolutiva: ${item.devolutiva}"
        } else {
            holder.tvDevolutiva.visibility = View.GONE
        }

        // Configura a resposta ao clique do item da lista
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(item, position)
        }
    }

    // Retorna a quantidade total de itens a serem exibidos
    override fun getItemCount(): Int = chamados.size
}