package com.steefwel28.consumoenergetico.ui

import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.steefwel28.consumoenergetico.core.di.AppContainer
import com.steefwel28.consumoenergetico.ui.screens.calculate.CalculateViewModel
import com.steefwel28.consumoenergetico.ui.screens.duel.AcDuelViewModel
import com.steefwel28.consumoenergetico.ui.screens.myhome.MyHomeViewModel
import com.steefwel28.consumoenergetico.ui.screens.recommend.RecommendViewModel
import com.steefwel28.consumoenergetico.ui.screens.settings.SettingsViewModel

/** Fábricas mínimas; el grafo vive en [AppContainer]. */
object AppViewModelProvider {

    fun calculate(container: AppContainer) = viewModelFactory {
        initializer { CalculateViewModel(createSavedStateHandle(), container) }
    }

    fun duel(container: AppContainer) = viewModelFactory {
        initializer { AcDuelViewModel(createSavedStateHandle(), container) }
    }

    fun recommend(container: AppContainer) = viewModelFactory {
        initializer { RecommendViewModel(container) }
    }

    fun myHome(container: AppContainer) = viewModelFactory {
        initializer { MyHomeViewModel(container) }
    }

    fun settings(container: AppContainer) = viewModelFactory {
        initializer { SettingsViewModel(container) }
    }
}
