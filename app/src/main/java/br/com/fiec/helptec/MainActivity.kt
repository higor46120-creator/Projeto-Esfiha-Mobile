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

    private lateinit var adapter: ChamadoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Mapeamento dos campos do formulário
        val etTitulo = findViewById<TextInputEditText>(R.id.etTitulo)
        val etSala = findViewById<TextInputEditText>(R.id.etSala)
        val etMaquina = findViewById<TextInputEditText>(R.id.etMaquina)
        val etDescricao = findViewById<TextInputEditText>(R.id.etDescricao)
        val spCategoria = findViewById<AutoCompleteTextView>(R.id.spCategoria)
        val spPrioridade = findViewById<AutoCompleteTextView>(R.id.spPrioridade)
        val btnAbrirChamado = findViewById<Button>(R.id.btnAbrirChamado)
        val btnSair = findViewById<Button>(R.id.btnSair)
        val rvChamados = findViewById<RecyclerView>(R.id.rvChamados)

        // Traz o botão de sair para a camada superior prevenindo bloqueios no clique
        btnSair.bringToFront()

        // Opções dos Menus Dropdown (AutoCompleteTextView)
        val categorias = arrayOf("Hardware", "Software", "Rede", "Acessos")
        val prioridades = arrayOf("Baixa", "Média", "Alta")

        spCategoria.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, categorias))
        spPrioridade.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, prioridades))

        // Configuração do RecyclerView para o Usuário Comum
        adapter = ChamadoAdapter(ChamadoRepository.listaChamados)
        rvChamados.layoutManager = LinearLayoutManager(this)
        rvChamados.adapter = adapter

        // Ação de envio do chamado
        btnAbrirChamado.setOnClickListener {
            val titulo = etTitulo.text?.toString() ?: ""
            val sala = etSala.text?.toString() ?: ""
            val maquina = etMaquina.text?.toString() ?: ""
            val descricao = etDescricao.text?.toString() ?: ""
            val categoria = spCategoria.text?.toString() ?: ""
            val prioridade = spPrioridade.text?.toString() ?: ""

            // Confere se nenhum campo do formulário ficou vazio
            if (titulo.isNotBlank() && sala.isNotBlank() && maquina.isNotBlank() &&
                descricao.isNotBlank() && categoria.isNotBlank() && prioridade.isNotBlank()) {

                // Cria o novo objeto Chamado
                val novoChamado = Chamado(
                    id = ChamadoRepository.gerarId(),
                    titulo = titulo,
                    sala = sala,
                    maquina = maquina,
                    categoria = categoria,
                    prioridade = prioridade,
                    descricao = descricao
                )

                // Salva no repositório e notifica o Adapter para atualizar a lista imediatamente
                ChamadoRepository.adicionarChamado(novoChamado)
                adapter.notifyItemInserted(0)
                rvChamados.scrollToPosition(0)

                // Limpa os campos do formulário após o envio
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

        // Lógica para o Botão Sair com limpeza de pilha de atividades
        btnSair.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            // Remove todas as telas anteriores da pilha do Android para impedir voltar no botão do celular
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    // Atualiza o histórico sempre que a tela voltar ao foco
    override fun onResume() {
        super.onResume()
        adapter.notifyDataSetChanged()
    }
}