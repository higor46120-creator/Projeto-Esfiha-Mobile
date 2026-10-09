package br.com.fiec.helptec

import android.app.Application

class HelpTecApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        TemaUtil.aplicarTemaSalvo(this)
    }
}