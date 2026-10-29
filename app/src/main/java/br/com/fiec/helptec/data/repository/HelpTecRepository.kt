package br.com.fiec.helptec.data.repository

class HelpTecRepository {

    suspend fun login(email: String, pass: String): Result<Boolean> {
        return try {
            // Insira aqui a lógica de chamada da sua API ou autenticação
            if (email.isNotEmpty() && pass.isNotEmpty()) {
                Result.success(true)
            } else {
                Result.failure(Exception("Dados inválidos"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}