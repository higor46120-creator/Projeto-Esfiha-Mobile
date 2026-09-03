package br.com.fiec.helptec

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        TemaUtil.aplicarTemaSalvo(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Busca dinâmica de IDs para evitar o erro 'Unresolved reference' do compilador
        val idEmail = resources.getIdentifier("etEmail", "id", packageName)
        val idSenha = resources.getIdentifier("etSenha", "id", packageName)
        val idBtnLogin = resources.getIdentifier("btnLogin", "id", packageName)
        val idBtnTema = resources.getIdentifier("btnAlternarTema", "id", packageName)

        // Mapeamento seguro dos componentes
        val etEmail = if (idEmail != 0) findViewById<TextInputEditText>(idEmail) else null
        val etSenha = if (idSenha != 0) findViewById<TextInputEditText>(idSenha) else null
        val btnLogin = if (idBtnLogin != 0) findViewById<Button>(idBtnLogin) else null
        val btnAlternarTema = if (idBtnTema != 0) findViewById<ImageButton>(idBtnTema) else null

        // Listener para alternar tema
        btnAlternarTema?.setOnClickListener {
            TemaUtil.alternarTema(this)
            recreate()
        }

        // Ação de Login com validação try-catch
        btnLogin?.setOnClickListener {
            val email = etEmail?.text?.toString()?.trim() ?: ""
            val senha = etSenha?.text?.toString()?.trim() ?: ""

            try {
                if (email.isBlank() || senha.isBlank()) {
                    throw IllegalArgumentException("Preencha e-mail e senha.")
                }

                if (email == "admin@fiec.com" && senha == "admin123") {
                    val intent = Intent(this, AdminActivity::class.java)
                    startActivity(intent)
                    finish()
                } else if (email == "user@fiec.com" && senha == "123456") {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    throw SecurityException("Usuário ou senha incorreto")
                }

            } catch (e: SecurityException) {
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            } catch (e: IllegalArgumentException) {
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Erro no login: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}