package app.secondbrain.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.secondbrain.ui.screens.CaptureDraft
import app.secondbrain.ui.screens.CaptureSheet
import app.secondbrain.ui.screens.CollectionScreen
import app.secondbrain.ui.screens.DetailScreen
import app.secondbrain.ui.screens.LibraryScreen
import app.secondbrain.ui.screens.RecentScreen
import app.secondbrain.ui.screens.SearchScreen
import app.secondbrain.ui.screens.SettingsScreen
import kotlin.reflect.KClass

private data class Tab(val route: Any, val routeClass: KClass<*>, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(RecentRoute, RecentRoute::class, "Recent", Icons.Rounded.Schedule),
    Tab(LibraryRoute, LibraryRoute::class, "Library", Icons.Rounded.Category),
    Tab(SearchRoute, SearchRoute::class, "Search", Icons.Rounded.Search),
)

@Composable
fun AppRoot(viewModel: AppViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    var capturing by rememberSaveable { mutableStateOf(false) }
    val draft = remember { CaptureDraft() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(it)
        }
    }

    val entry by navController.currentBackStackEntryAsState()
    val destination = entry?.destination
    val topLevel = tabs.any { destination?.hasRoute(it.routeClass) == true }
    val showFab = destination?.hasRoute(SearchRoute::class) != true

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (topLevel) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = destination?.hasRoute(tab.routeClass) == true,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            ),
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (topLevel && showFab) {
                FloatingActionButton(
                    onClick = { capturing = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                ) { Icon(Icons.Rounded.Add, contentDescription = "Add something") }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = RecentRoute,
            modifier = Modifier.padding(padding),
            enterTransition = { fadeIn(tween(220)) },
            exitTransition = { fadeOut(tween(120)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(120)) },
        ) {
            val open: (Long) -> Unit = { navController.navigate(DetailRoute(it)) }
            composable<RecentRoute> {
                RecentScreen(
                    state = state,
                    onOpen = open,
                    onOpenInbox = { navController.navigate(CollectionRoute(null)) },
                    onOpenSettings = { navController.navigate(SettingsRoute) },
                )
            }
            composable<LibraryRoute> {
                LibraryScreen(
                    state = state,
                    onOpenCollection = { navController.navigate(CollectionRoute(it)) },
                    onAddCategory = viewModel::addCategory,
                )
            }
            composable<SearchRoute> { SearchScreen(state = state, onOpen = open) }
            composable<CollectionRoute> {
                CollectionScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onOpen = open,
                    onRename = viewModel::renameCategory,
                    onDelete = viewModel::deleteCategory,
                )
            }
            composable<DetailRoute> { DetailScreen(state = state, onBack = { navController.popBackStack() }) }
            composable<SettingsRoute> { SettingsScreen(state, viewModel, onBack = { navController.popBackStack() }) }
        }
    }

    if (capturing) {
        CaptureSheet(
            draft = draft,
            newCaptureTarget = viewModel::newCaptureTarget,
            onSave = viewModel::save,
            onDismiss = { capturing = false },
        )
    }
}
