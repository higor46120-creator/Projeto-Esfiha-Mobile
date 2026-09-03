package br.com.fiec.helptec

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Mapeamento dos componentes da interface
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etSenha = findViewById<TextInputEditText>(R.id.etSenha)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val email = etEmail.text?.toString()?.trim() ?: ""
            val senha = etSenha.text?.toString()?.trim() ?: ""

            // Bloco de tratamento de erros no processo de Login
            try {
                // Validação 1: Impede tentativa de envio com campos vazios
                if (email.isBlank() || senha.isBlank()) {
                    throw IllegalArgumentException("Preencha e-mail e senha.")
                }

                // Validação 2: Direcionamento por Perfil de Acesso
                if (email == "admin@fiec.com" && senha == "admin123") {
                    // Credencial de Administrador -> Direciona para o Painel de Suporte
                    val intent = Intent(this, AdminActivity::class.java)
                    startActivity(intent)
                    finish() // Fecha a tela de Login da pilha
                } else if (email == "user@fiec.com" && senha == "123456") {
                    // Credencial de Usuário Comum -> Direciona para Formulário de Chamados
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish() // Fecha a tela de Login da pilha
                } else {
                    // Lança exceção se nenhuma das credenciais bater
                    throw SecurityException("Usuário ou senha incorreto")
                }

            } catch (e: SecurityException) {
                // Captura a falha de credenciais incorretas e exibe o Toast correspondente
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            } catch (e: IllegalArgumentException) {
                // Captura erro de preenchimento incompleto
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                // Captura genérica para prevenir crashes imprevistos
                Toast.makeText(this, "Erro no login: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}