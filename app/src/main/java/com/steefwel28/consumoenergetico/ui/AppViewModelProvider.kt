package com.steefwel28.consumoenergetico.ui

import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.steefwel28.consumoenergetico.core.di.AppContainer
import com.steefwel28.consumoenergetico.ui.screens.calculate.CalculateViewModel
import com.steefwel28.consumoenergetico.ui.screens.recommend.RecommendViewModel

/** Fábricas mínimas; el grafo vive en [AppContainer]. */
object AppViewModelProvider {

    fun calculate(container: AppContainer) = viewModelFactory {
        initializer { CalculateViewModel(container) }
    }

    fun recommend(container: AppContainer) = viewModelFactory {
        initializer { RecommendViewModel(container) }
    }
}
