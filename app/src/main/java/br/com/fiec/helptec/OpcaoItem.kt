package br.com.fiec.helptec

data class OpcaoItem(
    val texto: String,
    val iconeResId: Int,
    val corHex: String? = null
) {
    override fun toString(): String = texto
}