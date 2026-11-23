package com.steefwel28.consumoenergetico

import android.app.Application
import com.steefwel28.consumoenergetico.core.di.AppContainer
import com.steefwel28.consumoenergetico.core.signature.CraftedBy

@CraftedBy("steefwel28")
class ConsumoEnergeticoApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
