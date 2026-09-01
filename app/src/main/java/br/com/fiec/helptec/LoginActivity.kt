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

        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etSenha = findViewById<TextInputEditText>(R.id.etSenha)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val email = etEmail.text?.toString() ?: ""
            val senha = etSenha.text?.toString() ?: ""

            if (email.isNotBlank() && senha.isNotBlank()) {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Preencha e-mail e senha.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}