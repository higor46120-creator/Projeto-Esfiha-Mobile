package br.com.fiec.helptec

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView

class CategoriaAdapter(
    context: Context,
    private val itens: List<OpcaoItem>
) : ArrayAdapter<OpcaoItem>(context, 0, itens) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return criarViewCustomizada(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return criarViewCustomizada(position, convertView, parent)
    }

    private fun criarViewCustomizada(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_dropdown_icon, parent, false)

        val item = getItem(position)
        val imgIcone = view.findViewById<ImageView>(R.id.imgIconeOpcao)
        val tvTexto = view.findViewById<TextView>(R.id.tvTextoOpcao)

        if (item != null) {
            imgIcone.setImageResource(item.iconeResId)
            tvTexto.text = item.texto

            if (item.corHex != null) {
                imgIcone.setColorFilter(Color.parseColor(item.corHex))
            } else {
                imgIcone.setColorFilter(Color.parseColor("#1E88E5"))
            }
        }

        return view
    }
}