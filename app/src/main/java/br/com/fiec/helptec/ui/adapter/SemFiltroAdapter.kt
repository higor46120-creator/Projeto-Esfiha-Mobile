package br.com.fiec.helptec.ui.adapter

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter

/**
 * ArrayAdapter que NÃO filtra as opções pelo texto digitado.
 *
 * Nos dropdowns do Material (MaterialAutoCompleteTextView), o Android filtra a lista pelo
 * texto do campo. Ao girar a tela o texto selecionado é restaurado e a lista passava a
 * mostrar só aquela opção. Com este adapter todas as opções continuam aparecendo.
 */
open class SemFiltroAdapter<T : Any>(
    context: Context,
    private val opcoes: List<T>,
    layout: Int = android.R.layout.simple_dropdown_item_1line
) : ArrayAdapter<T>(context, layout, opcoes) {

    private val semFiltro = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults =
            FilterResults().apply {
                values = opcoes
                count = opcoes.size
            }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            notifyDataSetChanged()
        }

        override fun convertResultToString(resultValue: Any?): CharSequence =
            resultValue?.toString().orEmpty()
    }

    override fun getFilter(): Filter = semFiltro
}
