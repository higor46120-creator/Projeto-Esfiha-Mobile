package br.com.fiec.helptec.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object TemaUtil {

    private const val PREFS_NAME = "helptec_prefs"
    private const val KEY_DARK_MODE = "dark_mode"

    fun aplicarTemaSalvo(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean(KEY_DARK_MODE, false)

        val mode = if (isDarkMode) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun alternarTema(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean(KEY_DARK_MODE, false)
        val novoModo = !isDarkMode

        prefs.edit().putBoolean(KEY_DARK_MODE, novoModo).apply()

        val mode = if (novoModo) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}