package br.com.fiec.helptec.auth

/**
 * Regras de acesso por papel (role) do usuário, vindo de GET /usuarios/me.
 * Os nomes são os do enum UserRole do backend.
 *
 *   USER      -> só abre chamado
 *   APROVADOR -> abre chamado + aprova/reprova (tela de gestão)
 *   SUPORTE   -> gestão: resolve chamados
 *   GERENTE   -> gestão: só visualiza (triagem é feita no web)
 *   ADMIN     -> tudo
 */
object Perfis {

    /** Papéis que podem abrir a tela de gestão de chamados (AdminActivity). */
    val GESTAO = setOf("ADMIN", "SUPORTE", "GERENTE", "APROVADOR")

    /** Papéis que começam na tela de gestão logo após o login. */
    private val INICIAM_NA_GESTAO = setOf("ADMIN", "SUPORTE", "GERENTE")

    fun podeAcessarGestao(role: String?): Boolean =
        role?.uppercase() in GESTAO

    /** O aprovador começa em "Abrir chamado" e entra na gestão pelo botão do cabeçalho. */
    fun iniciaNaGestao(role: String?): Boolean =
        role?.uppercase() in INICIAM_NA_GESTAO
}
