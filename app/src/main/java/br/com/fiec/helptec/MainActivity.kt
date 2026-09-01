package br.com.fiec.helptec

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {

    private val listaChamados = mutableListOf<Chamado>()
    private lateinit var adapter: ChamadoAdapter
    private var contadorId = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etTitulo = findViewById<TextInputEditText>(R.id.etTitulo)
        val etSala = findViewById<TextInputEditText>(R.id.etSala)
        val etMaquina = findViewById<TextInputEditText>(R.id.etMaquina)
        val etDescricao = findViewById<TextInputEditText>(R.id.etDescricao)
        val spCategoria = findViewById<AutoCompleteTextView>(R.id.spCategoria)
        val spPrioridade = findViewById<AutoCompleteTextView>(R.id.spPrioridade)
        val btnAbrirChamado = findViewById<Button>(R.id.btnAbrirChamado)
        val btnSair = findViewById<Button>(R.id.btnSair)
        val rvChamados = findViewById<RecyclerView>(R.id.rvChamados)

        val categorias = arrayOf("Hardware", "Software", "Rede", "Acessos")
        val prioridades = arrayOf("Baixa", "Média", "Alta")

        spCategoria.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, categorias))
        spPrioridade.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, prioridades))

        adapter = ChamadoAdapter(listaChamados)
        rvChamados.layoutManager = LinearLayoutManager(this)
        rvChamados.adapter = adapter

        btnAbrirChamado.setOnClickListener {
            val titulo = etTitulo.text?.toString() ?: ""
            val sala = etSala.text?.toString() ?: ""
            val maquina = etMaquina.text?.toString() ?: ""
            val descricao = etDescricao.text?.toString() ?: ""
            val categoria = spCategoria.text?.toString() ?: ""
            val prioridade = spPrioridade.text?.toString() ?: ""

            if (titulo.isNotBlank() && sala.isNotBlank() && maquina.isNotBlank() &&
                descricao.isNotBlank() && categoria.isNotBlank() && prioridade.isNotBlank()) {

                val novoChamado = Chamado(contadorId++, titulo, sala, maquina, categoria, prioridade, descricao)
                listaChamados.add(0, novoChamado)
                adapter.notifyItemInserted(0)

                etTitulo.text?.clear()
                etSala.text?.clear()
                etMaquina.text?.clear()
                etDescricao.text?.clear()
                spCategoria.text?.clear()
                spPrioridade.text?.clear()
                Toast.makeText(this, "Chamado enviado com sucesso!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Preencha todos os campos do formulário.", Toast.LENGTH_SHORT).show()
            }
        }

        btnSair.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}