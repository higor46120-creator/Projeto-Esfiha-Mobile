package br.com.fiec.helptec

import android.app.Application
import br.com.fiec.helptec.utils.TemaUtil

class HelpTecApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        TemaUtil.aplicarTemaSalvo(this)
    }
}