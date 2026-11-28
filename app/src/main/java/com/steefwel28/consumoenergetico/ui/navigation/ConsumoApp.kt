package com.steefwel28.consumoenergetico.ui.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsRemote
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.steefwel28.consumoenergetico.R
import com.steefwel28.consumoenergetico.core.di.AppContainer
import com.steefwel28.consumoenergetico.domain.settings.AppearancePreferences
import com.steefwel28.consumoenergetico.ui.AppViewModelProvider
import com.steefwel28.consumoenergetico.ui.components.AppFormat
import com.steefwel28.consumoenergetico.ui.components.LiquidGlassSurface
import com.steefwel28.consumoenergetico.ui.components.LocalHazeState
import com.steefwel28.consumoenergetico.ui.components.LocalPixelOverlayState
import com.steefwel28.consumoenergetico.ui.components.PixelDialog
import com.steefwel28.consumoenergetico.ui.components.PixelOverlayState
import com.steefwel28.consumoenergetico.ui.components.PixelSpringExpressive
import com.steefwel28.consumoenergetico.ui.components.pixelChargeHaptics
import com.steefwel28.consumoenergetico.ui.components.rememberPixelHaptics
import com.steefwel28.consumoenergetico.ui.screens.calculate.CalculateRoute
import com.steefwel28.consumoenergetico.ui.screens.calculate.CalculateUiState
import com.steefwel28.consumoenergetico.ui.screens.calculate.CalculateViewModel
import com.steefwel28.consumoenergetico.ui.screens.calculate.CapsulesSheet
import com.steefwel28.consumoenergetico.ui.screens.calculate.RemoteSimulatorSheet
import com.steefwel28.consumoenergetico.ui.screens.duel.AcDuelRoute
import com.steefwel28.consumoenergetico.ui.screens.myhome.MyHomeRoute
import com.steefwel28.consumoenergetico.ui.screens.recommend.RecommendRoute
import com.steefwel28.consumoenergetico.ui.screens.settings.SettingsRoute
import com.steefwel28.consumoenergetico.ui.screens.splash.ExpressiveSplashScreen
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ConsumoApp(
    container: AppContainer,
    appearance: AppearancePreferences = AppearancePreferences(),
) {
    val navController = rememberNavController()
    val overlay = remember { PixelOverlayState() }
    val pendingUpdate by container.releaseChecker.hasPendingUpdate.collectAsStateWithLifecycle()
    val pendingRelease by container.releaseChecker.pendingRelease.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    var splashDone by rememberSaveable { mutableStateOf(false) }
    val calculateViewModel: CalculateViewModel = viewModel(
        factory = AppViewModelProvider.calculate(container),
    )

    // Precarga Room y verificación de releases en paralelo durante el splash.
    LaunchedEffect(Unit) {
        launch {
            runCatching {
                withContext(Dispatchers.IO) { container.database.propertyGroupDao().count() }
            }
        }
        launch { container.releaseChecker.checkForUpdate() }
    }

    CompositionLocalProvider(LocalPixelOverlayState provides overlay) {
        Crossfade(targetState = splashDone, animationSpec = tween(420), label = "splash") { done ->
            if (!done) {
                ExpressiveSplashScreen(onFinished = { splashDone = true })
            } else {
                MainScaffold(
                    container = container,
                    navController = navController,
                    overlay = overlay,
                    pendingUpdate = pendingUpdate,
                    calculateViewModel = calculateViewModel,
                )

                // Aviso inicial de actualización: se muestra una vez por versión en la
                // sesión y recuerda en DataStore las versiones omitidas.
                var dismissedVersion by rememberSaveable { mutableStateOf<String?>(null) }
                val release = pendingRelease?.takeIf { it.version != dismissedVersion }
                if (release != null) {
                    val defaultBody = stringResource(R.string.update_body)
                    PixelDialog(
                        title = stringResource(R.string.update_title, release.version),
                        text = release.notes.take(280).ifBlank { defaultBody },
                        confirmLabel = stringResource(R.string.set_update),
                        dismissLabel = stringResource(R.string.set_skip),
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
    }
}

@Composable
private fun MainScaffold(
    container: AppContainer,
    navController: androidx.navigation.NavHostController,
    overlay: PixelOverlayState,
    pendingUpdate: Boolean,
    calculateViewModel: CalculateViewModel,
) {
    val calculateState by calculateViewModel.state.collectAsStateWithLifecycle()
    var showSimulator by rememberSaveable { mutableStateOf(false) }
    var showCapsules by rememberSaveable { mutableStateOf(false) }
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    val hazeState = rememberHazeState()
    val currentRoute by navController.currentBackStackEntryAsState()
    val route = currentRoute?.destination?.route

    CompositionLocalProvider(LocalHazeState provides hazeState) {
        // Fondo sólido del esquema Día/Noche/OLED; el desenfoque Haze vive en
        // la barra inferior, el menú y las hojas modales.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                // Fuente del desenfoque: todo lo que pase por detrás de la
                // barra inferior y del menú.
                .hazeSource(hazeState),
        ) {
            Scaffold(
                modifier = if (menuOpen) Modifier.blur(24.dp) else Modifier,
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
                ),
                topBar = {
                    TopMenuBar(
                        pendingUpdate = pendingUpdate,
                        expanded = menuOpen,
                        onExpandedChange = { menuOpen = it },
                        simulatorSubtitle = calculateState.simulatorSubtitle(),
                        onDuel = {
                            navController.navigate(AppDestination.DUEL.route) {
                                launchSingleTop = true
                            }
                        },
                        onSimulator = { showSimulator = true },
                        onCapsules = { showCapsules = true },
                        onSettings = {
                            navController.navigate(AppDestination.SETTINGS.route) {
                                launchSingleTop = true
                            }
                        },
                    )
                },
                bottomBar = {
                    // Duelo y Ajustes son pantallas de detalle: sin barra de pestañas.
                    if (route == null || route !in AppDestination.overlayRoutes) {
                        PixelNavigationBar(
                            currentRoute = route,
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
                    }
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
                        enterTransition = { androidx.compose.animation.fadeIn(tween(220)) },
                        exitTransition = { androidx.compose.animation.fadeOut(tween(160)) },
                    ) {
                        composable(AppDestination.CALCULATE.route) {
                            CalculateRoute(container, viewModel = calculateViewModel)
                        }
                        composable(AppDestination.RECOMMEND.route) {
                            RecommendRoute(container)
                        }
                        composable(AppDestination.HOME.route) {
                            MyHomeRoute(container)
                        }
                        composable(AppDestination.DUEL.route) {
                            AcDuelRoute(
                                container = container,
                                onBack = { navController.popBackStack() },
                            )
                        }
                        composable(AppDestination.SETTINGS.route) {
                            SettingsRoute(
                                container = container,
                                onBack = { navController.popBackStack() },
                            )
                        }
                    }
                }
            }
            // Velo del menú `☰`: oscurece todo el fondo para que el vidrio flotante
            // se lea nítido. Tocar fuera cierra el menú.
            if (menuOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.26f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { menuOpen = false },
                )
            }
        }
    }

    if (showSimulator) {
        RemoteSimulatorSheet(
            state = calculateState.simulatorSeed(),
            onUpdate = calculateViewModel::update,
            onDismiss = { showSimulator = false },
        )
    }
    if (showCapsules) {
        CapsulesSheet(onDismiss = { showCapsules = false })
    }
}

/** Si las casillas siguen vacías, el simulador arranca con 12,000 BTU · SEER 21. */
private fun CalculateUiState.simulatorSeed(): CalculateUiState =
    if (spec != null) this else copy(btuInput = "12000", wattsInput = "", seerInput = "21")

private fun CalculateUiState.simulatorSubtitle(): String {
    val seeded = simulatorSeed()
    val btu = seeded.btu ?: 12_000.0
    val seer = seeded.seer ?: 21.0
    return "${AppFormat.btuNumber(btu)} BTU · SEER ${AppFormat.decimal(seer)}"
}

/** Menú superior `☰`: Duelo AC vs. AC, simulador, cápsulas y Ajustes. */
@Composable
private fun TopMenuBar(
    pendingUpdate: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    simulatorSubtitle: String,
    onDuel: () -> Unit,
    onSimulator: () -> Unit,
    onCapsules: () -> Unit,
    onSettings: () -> Unit,
) {
    val haptics = rememberPixelHaptics()
    val scale by animateFloatAsState(
        targetValue = if (expanded) 1f else 0.88f,
        animationSpec = PixelSpringExpressive,
        label = "menu-scale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(140),
        label = "menu-alpha",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(48.dp)
            .padding(end = 10.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Box {
            val menuIcon: @Composable () -> Unit = {
                Icon(
                    imageVector = Icons.Rounded.Menu,
                    contentDescription = stringResource(R.string.menu_open_cd),
                )
            }
            if (pendingUpdate) {
                BadgedBox(
                    badge = {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ) { Text("A") }
                    },
                ) {
                    IconButton(
                        onClick = {
                            haptics.tap()
                            onExpandedChange(true)
                        },
                        modifier = Modifier.pixelChargeHaptics(),
                    ) { menuIcon() }
                }
            } else {
                IconButton(
                    onClick = {
                        haptics.tap()
                        onExpandedChange(true)
                    },
                    modifier = Modifier.pixelChargeHaptics(),
                ) { menuIcon() }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .alpha(if (expanded) 1f else 0.6f),
                shape = MaterialTheme.shapes.large,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 0.dp,
                shadowElevation = 3.dp,
                border = null,
            ) {
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = stringResource(R.string.menu_duel),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = stringResource(R.string.menu_duel_subtitle),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Rounded.CompareArrows, contentDescription = null)
                    },
                    onClick = {
                        haptics.tap()
                        onExpandedChange(false)
                        onDuel()
                    },
                )
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = stringResource(R.string.menu_simulator),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = simulatorSubtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    leadingIcon = {
                        Icon(Icons.Rounded.SettingsRemote, contentDescription = null)
                    },
                    onClick = {
                        haptics.tap()
                        onExpandedChange(false)
                        onSimulator()
                    },
                )
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = stringResource(R.string.menu_capsules),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = stringResource(R.string.menu_capsules_subtitle),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    leadingIcon = {
                        Icon(Icons.Rounded.AutoStories, contentDescription = null)
                    },
                    onClick = {
                        haptics.tap()
                        onExpandedChange(false)
                        onCapsules()
                    },
                )
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = stringResource(R.string.menu_settings),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = stringResource(R.string.menu_settings_subtitle),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    leadingIcon = {
                        Icon(Icons.Rounded.Settings, contentDescription = null)
                    },
                    onClick = {
                        haptics.tap()
                        onExpandedChange(false)
                        onSettings()
                    },
                )
            }
        }
    }
}

@Composable
private fun PixelNavigationBar(
    currentRoute: String?,
    onNavigate: (AppDestination) -> Unit,
) {
    val haptics = rememberPixelHaptics()
    LiquidGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
    NavigationBar(
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
    ) {
        AppDestination.tabs.forEach { destination ->
            val selected = currentRoute == destination.route
            val label = stringResource(destination.labelRes)
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
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = null,
                        modifier = Modifier.graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        },
                    )
                },
                label = { Text(label) },
                modifier = Modifier
                    .pixelChargeHaptics()
                    .semantics { contentDescription = label },
            )
        }
    }
    }
}
