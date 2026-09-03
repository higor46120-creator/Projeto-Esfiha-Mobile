package br.com.fiec.helptec

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AdminActivity : AppCompatActivity() {

    private lateinit var adapter: ChamadoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        TemaUtil.aplicarTemaSalvo(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        val rvChamadosAdmin = findViewById<RecyclerView>(R.id.rvChamadosAdmin)
        val btnSairAdmin = findViewById<Button>(R.id.btnSairAdmin)
        val btnAlternarTemaAdmin = findViewById<ImageButton>(R.id.btnAlternarTemaAdmin)

        btnSairAdmin.bringToFront()
        btnAlternarTemaAdmin.setOnClickListener {
            TemaUtil.alternarTema(this)
        }

        adapter = ChamadoAdapter(ChamadoRepository.listaChamados) { chamado, posicao ->
            mostrarOpcoesStatus(chamado, posicao)
        }

        rvChamadosAdmin.layoutManager = LinearLayoutManager(this)
        rvChamadosAdmin.adapter = adapter

        btnSairAdmin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun mostrarOpcoesStatus(chamado: Chamado, posicao: Int) {
        val opcoes = arrayOf("Aberto", "Em Andamento", "Concluído")

        AlertDialog.Builder(this)
            .setTitle("Atualizar Status do Chamado #${chamado.id}")
            .setItems(opcoes) { _, which ->
                val novoStatus = opcoes[which]

                if (novoStatus == "Concluído") {
                    solicitarDevolutiva(chamado, posicao)
                } else {
                    chamado.status = novoStatus
                    adapter.notifyItemChanged(posicao)
                    Toast.makeText(this, "Status alterado para: ${chamado.status}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun solicitarDevolutiva(chamado: Chamado, posicao: Int) {
        val etDevolutiva = EditText(this).apply {
            hint = "Descreva a solução aplicada..."
            setText(chamado.devolutiva)
        }

        AlertDialog.Builder(this)
            .setTitle("Devolutiva do Atendimento")
            .setMessage("Informe a solução do chamado antes de concluir:")
            .setView(etDevolutiva)
            .setPositiveButton("Salvar e Concluir") { _, _ ->
                val textoDevolutiva = etDevolutiva.text.toString().trim()

                if (textoDevolutiva.isNotBlank()) {
                    chamado.status = "Concluído"
                    chamado.devolutiva = textoDevolutiva
                    adapter.notifyItemChanged(posicao)
                    Toast.makeText(this, "Chamado concluído com sucesso!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "É necessário preencher a devolutiva para concluir.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        adapter.notifyDataSetChanged()
    }
}