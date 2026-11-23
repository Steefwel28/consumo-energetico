package com.steefwel28.consumoenergetico.ui.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.steefwel28.consumoenergetico.core.di.AppContainer
import com.steefwel28.consumoenergetico.ui.components.LocalPixelOverlayState
import com.steefwel28.consumoenergetico.ui.components.PixelDialog
import com.steefwel28.consumoenergetico.ui.components.PixelOverlayState
import com.steefwel28.consumoenergetico.ui.components.PixelSpringExpressive
import com.steefwel28.consumoenergetico.ui.components.rememberPixelHaptics
import com.steefwel28.consumoenergetico.ui.screens.calculate.CalculateRoute
import com.steefwel28.consumoenergetico.ui.screens.myhome.MyHomeRoute
import com.steefwel28.consumoenergetico.ui.screens.recommend.RecommendRoute
import com.steefwel28.consumoenergetico.ui.screens.settings.SettingsRoute
import kotlinx.coroutines.launch

@Composable
fun ConsumoApp(container: AppContainer) {
    val navController = rememberNavController()
    val overlay = remember { PixelOverlayState() }
    val pendingUpdate by container.releaseChecker.hasPendingUpdate.collectAsStateWithLifecycle()
    val pendingRelease by container.releaseChecker.pendingRelease.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(Unit) {
        container.releaseChecker.checkForUpdate()
    }

    CompositionLocalProvider(LocalPixelOverlayState provides overlay) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing.only(
                WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
            ),
            bottomBar = {
                PixelNavigationBar(
                    currentRoute = navController.currentBackStackEntryAsState().value
                        ?.destination?.route,
                    pendingUpdate = pendingUpdate,
                    onNavigate = { destination ->
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .then(
                        if (overlay.blurRadius > 0.dp) {
                            Modifier.blur(overlay.blurRadius)
                        } else {
                            Modifier
                        },
                    ),
            ) {
                NavHost(
                    navController = navController,
                    startDestination = AppDestination.CALCULATE.route,
                    enterTransition = { fadeIn(tween(220)) },
                    exitTransition = { fadeOut(tween(160)) },
                ) {
                    composable(AppDestination.CALCULATE.route) {
                        CalculateRoute(container)
                    }
                    composable(AppDestination.RECOMMEND.route) {
                        RecommendRoute(container)
                    }
                    composable(AppDestination.HOME.route) {
                        MyHomeRoute(container)
                    }
                    composable(AppDestination.SETTINGS.route) {
                        SettingsRoute(container)
                    }
                }
            }
        }

        // Aviso inicial de actualización: se muestra una vez por versión en la
        // sesión y recuerda en DataStore las versiones omitidas.
        var dismissedVersion by rememberSaveable { mutableStateOf<String?>(null) }
        val release = pendingRelease?.takeIf { it.version != dismissedVersion }
        if (release != null) {
            PixelDialog(
                title = "Actualización ${release.version}",
                text = release.notes.take(280).ifBlank {
                    "Hay una versión nueva de Consumo Energético publicada en GitHub."
                },
                confirmLabel = "Actualizar",
                dismissLabel = "Omitir",
                onConfirm = {
                    dismissedVersion = release.version
                    runCatching {
                        uriHandler.openUri(release.apkDownloadUrl ?: release.releaseUrl)
                    }
                },
                onDismissRequest = {
                    dismissedVersion = release.version
                    scope.launch { container.releaseChecker.skipVersion(release.version) }
                },
            )
        }
    }
}

@Composable
private fun PixelNavigationBar(
    currentRoute: String?,
    pendingUpdate: Boolean,
    onNavigate: (AppDestination) -> Unit,
) {
    val haptics = rememberPixelHaptics()
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
    ) {
        AppDestination.entries.forEach { destination ->
            val selected = currentRoute == destination.route
            val hasBadge = destination == AppDestination.SETTINGS && pendingUpdate
            val scale by animateFloatAsState(
                targetValue = if (selected) 1f else 0.9f,
                animationSpec = PixelSpringExpressive,
                label = "tab-${destination.route}",
            )

            NavigationBarItem(
                selected = selected,
                onClick = {
                    haptics.tap()
                    onNavigate(destination)
                },
                icon = {
                    val icon: @Composable () -> Unit = {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = null,
                            modifier = Modifier.graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            },
                        )
                    }
                    if (hasBadge) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError,
                                ) { Text("A") }
                            },
                        ) { icon() }
                    } else {
                        icon()
                    }
                },
                label = { Text(destination.label) },
                modifier = if (hasBadge) {
                    Modifier.semantics {
                        contentDescription = "${destination.label}, actualización disponible"
                    }
                } else {
                    Modifier
                },
            )
        }
    }
}
