package br.com.fiec.helptec.admin

import android.R
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import br.com.fiec.helptec.ui.adapter.ChamadoAdapter
import br.com.fiec.helptec.auth.LoginActivity
import br.com.fiec.helptec.data.model.Chamado
import br.com.fiec.helptec.data.RetrofitClient
import br.com.fiec.helptec.databinding.ActivityAdminBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding
    private lateinit var chamadoAdapter: ChamadoAdapter

    // Declarado como var para permitir reatribuição ao selecionar um chamado
    private var chamadoSelecionado: Chamado? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarDropdowns()
        configurarRecyclerView()
        configurarListeners()

        carregarChamados()
    }

    private fun configurarDropdowns() {
        val statusList = listOf("ABERTO", "EM_ANDAMENTO", "CONCLUIDO", "CANCELADO")
        val adapterStatus = ArrayAdapter(this, R.layout.simple_dropdown_item_1line, statusList)
        binding.spStatus.setAdapter(adapterStatus)
    }

    private fun configurarRecyclerView() {
        chamadoAdapter = ChamadoAdapter(onItemClick = { chamado, _ ->
            chamadoSelecionado = chamado
            preencherCamposEdicao(chamado)
        })

        binding.rvChamadosAdmin.apply {
            layoutManager = LinearLayoutManager(this@AdminActivity)
            adapter = chamadoAdapter
        }
    }

    private fun preencherCamposEdicao(chamado: Chamado) {
        binding.tvChamadoSelecionado.text = "Editando Chamado #${chamado.id ?: ""}: ${chamado.titulo}"
        binding.etDevolutiva.setText(chamado.devolutiva ?: "")

        // Uso de setText(..., false) para evitar reatribuição 'val'
        binding.spStatus.setText(chamado.status, false)
    }

    private fun configurarListeners() {
        binding.btnSalvarAtualizacao.setOnClickListener {
            salvarAtualizacao()
        }

        binding.btnAlternarTemaAdmin.setOnClickListener {
            alternarTema()
        }

        binding.btnSairAdmin.setOnClickListener {
            realizarLogout()
        }
    }

    private fun salvarAtualizacao() {
        val chamado = chamadoSelecionado
        if (chamado == null || chamado.id == null) {
            Toast.makeText(this, "Selecione um chamado na lista primeiro", Toast.LENGTH_SHORT).show()
            return
        }

        val novoStatus = binding.spStatus.text.toString().trim()
        val novaDevolutiva = binding.etDevolutiva.text.toString().trim()

        val chamadoAtualizado = chamado.copy(
            status = novoStatus,
            devolutiva = novaDevolutiva
        )

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.getInstance(applicationContext).criarChamado(chamadoAtualizado)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@AdminActivity,
                            "Chamado atualizado com sucesso!",
                            Toast.LENGTH_SHORT
                        ).show()
                        limparCampos()
                        carregarChamados()
                    } else {
                        Toast.makeText(
                            this@AdminActivity,
                            "Erro ao atualizar: ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("AdminActivity", "Erro ao atualizar chamado", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@AdminActivity,
                        "Falha de conexão com o servidor",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun carregarChamados() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.getInstance(applicationContext).listarChamados()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        chamadoAdapter.atualizarLista(response.body()!!)
                    }
                }
            } catch (e: Exception) {
                Log.e("AdminActivity", "Erro ao carregar chamados", e)
            }
        }
    }

    private fun alternarTema() {
        val modoAtual = AppCompatDelegate.getDefaultNightMode()
        if (modoAtual == AppCompatDelegate.MODE_NIGHT_YES) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }

    private fun realizarLogout() {
        getSharedPreferences("auth_prefs", MODE_PRIVATE).edit().clear().apply()
        Toast.makeText(this, "Sessão encerrada", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun limparCampos() {
        chamadoSelecionado = null
        binding.tvChamadoSelecionado.text = "Nenhum chamado selecionado"
        binding.etDevolutiva.text?.clear()

        // Uso de setText("", false) sem tentar reatribuir 'val'
        binding.spStatus.setText("", false)
    }
}