package br.com.fiec.helptec

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object TemaUtil {

    private const val PREFS_NAME = "helptec_prefs"
    private const val KEY_DARK_MODE = "modo_escuro"

    fun aplicarTemaSalvo(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean(KEY_DARK_MODE, true)

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    fun alternarTema(activity: Activity) {
        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isDarkModeAtual = prefs.getBoolean(KEY_DARK_MODE, true)
        val novoModo = !isDarkModeAtual

        prefs.edit().putBoolean(KEY_DARK_MODE, novoModo).apply()

        if (novoModo) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        // Força o recarregamento imediato da Activity atual
        activity.recreate()
    }
}