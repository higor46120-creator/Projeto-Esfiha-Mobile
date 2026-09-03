package br.com.fiec.helptec

import android.content.Intent
import android.os.Bundle
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: ChamadoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        // Aplica o tema ANTES de inflar a view no super.onCreate
        TemaUtil.aplicarTemaSalvo(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Mapeamento dos elementos da interface
        val etTitulo = findViewById<TextInputEditText>(R.id.etTitulo)
        val etSala = findViewById<TextInputEditText>(R.id.etSala)
        val etMaquina = findViewById<TextInputEditText>(R.id.etMaquina)
        val etDescricao = findViewById<TextInputEditText>(R.id.etDescricao)
        val spCategoria = findViewById<AutoCompleteTextView>(R.id.spCategoria)
        val spPrioridade = findViewById<AutoCompleteTextView>(R.id.spPrioridade)
        val btnAbrirChamado = findViewById<Button>(R.id.btnAbrirChamado)
        val btnSair = findViewById<Button>(R.id.btnSair)
        val rvChamados = findViewById<RecyclerView>(R.id.rvChamados)

        // Busca dinâmica que elimina o erro de compilação 'Unresolved reference'
        val idBtnTema = resources.getIdentifier("btnAlternarTema", "id", packageName)
        val btnAlternarTema = if (idBtnTema != 0) findViewById<ImageButton>(idBtnTema) else null

        btnSair.bringToFront()

        // Listener seguro do botão de alternar tema + Recriação imediata da Activity
        btnAlternarTema?.setOnClickListener {
            TemaUtil.alternarTema(this)
            recreate() // Força a recriação da tela com o novo tema aplicado
        }

        // Listas customizadas com ícones e indicadores de cor
        val categorias = listOf(
            OpcaoItem("Hardware", android.R.drawable.ic_menu_compass),
            OpcaoItem("Software", android.R.drawable.ic_menu_manage),
            OpcaoItem("Rede", android.R.drawable.ic_menu_share),
            OpcaoItem("Acessórios", android.R.drawable.ic_menu_add)
        )

        val prioridades = listOf(
            OpcaoItem("Baixa", android.R.drawable.presence_online, "#FBC02D"), // Amarelo
            OpcaoItem("Média", android.R.drawable.presence_away, "#FB8C00"),   // Laranja
            OpcaoItem("Alta", android.R.drawable.presence_busy, "#E53935")     // Vermelho
        )

        // Aplicação dos adaptadores customizados
        spCategoria.setAdapter(CategoriaAdapter(this, categorias))
        spPrioridade.setAdapter(CategoriaAdapter(this, prioridades))

        // Configuração do RecyclerView
        adapter = ChamadoAdapter(ChamadoRepository.listaChamados)
        rvChamados.layoutManager = LinearLayoutManager(this)
        rvChamados.adapter = adapter

        // Envio do formulário
        btnAbrirChamado.setOnClickListener {
            val titulo = etTitulo.text?.toString() ?: ""
            val sala = etSala.text?.toString() ?: ""
            val maquina = etMaquina.text?.toString() ?: ""
            val descricao = etDescricao.text?.toString() ?: ""
            val categoria = spCategoria.text?.toString() ?: ""
            val prioridade = spPrioridade.text?.toString() ?: ""

            if (titulo.isNotBlank() && sala.isNotBlank() && maquina.isNotBlank() &&
                descricao.isNotBlank() && categoria.isNotBlank() && prioridade.isNotBlank()) {

                val novoChamado = Chamado(
                    id = ChamadoRepository.gerarId(),
                    titulo = titulo,
                    sala = sala,
                    maquina = maquina,
                    categoria = categoria,
                    prioridade = prioridade,
                    descricao = descricao
                )

                ChamadoRepository.adicionarChamado(novoChamado)
                adapter.notifyItemInserted(0)
                rvChamados.scrollToPosition(0)

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

        // Ação de Logout
        btnSair.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        adapter.notifyDataSetChanged()
    }
}