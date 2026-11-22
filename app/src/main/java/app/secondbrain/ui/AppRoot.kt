package app.secondbrain.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.secondbrain.ui.screens.CaptureDraft
import app.secondbrain.ui.screens.CaptureSheet
import app.secondbrain.ui.screens.CollectionScreen
import app.secondbrain.ui.screens.DetailScreen
import app.secondbrain.ui.screens.HomeScreen
import app.secondbrain.ui.screens.RecentScreen
import app.secondbrain.ui.screens.SearchScreen
import app.secondbrain.ui.screens.SettingsScreen

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
    // Capture is available wherever you browse; not while reading one item, searching, or in settings.
    val showFab = listOf(HomeRoute::class, RecentRoute::class, CollectionRoute::class)
        .any { destination?.hasRoute(it) == true }

    // No bottom bar: the folder shelf is home, and everything else is one tap away from it.
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (showFab) {
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
            startDestination = HomeRoute,
            modifier = Modifier.padding(padding),
            enterTransition = { fadeIn(tween(220)) },
            exitTransition = { fadeOut(tween(120)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(120)) },
        ) {
            val open: (Long) -> Unit = { navController.navigate(DetailRoute(it)) }
            val back: () -> Unit = { navController.popBackStack() }
            composable<HomeRoute> {
                HomeScreen(
                    state = state,
                    onOpenFolder = { navController.navigate(CollectionRoute(it)) },
                    onOpenRecent = { navController.navigate(RecentRoute) },
                    onOpenSearch = { navController.navigate(SearchRoute()) },
                    onOpenSettings = { navController.navigate(SettingsRoute) },
                    onAddFolder = { viewModel.addCategory(it) },
                )
            }
            composable<RecentRoute> { RecentScreen(state = state, onBack = back, onOpen = open) }
            composable<SearchRoute> { backStackEntry ->
                SearchScreen(
                    state = state,
                    onBack = back,
                    onOpen = open,
                    initialQuery = backStackEntry.toRoute<SearchRoute>().query,
                )
            }
            composable<CollectionRoute> {
                CollectionScreen(
                    state = state,
                    onBack = back,
                    onOpen = open,
                    onOpenFolder = { navController.navigate(CollectionRoute(it)) },
                    onAddSubfolder = { name, parent -> viewModel.addCategory(name, parent) },
                    onRename = viewModel::renameCategory,
                    onDelete = viewModel::deleteCategory,
                )
            }
            composable<DetailRoute> {
                DetailScreen(
                    state = state,
                    onBack = back,
                    onOpenTag = { navController.navigate(SearchRoute(it)) },
                )
            }
            composable<SettingsRoute> { SettingsScreen(state, viewModel, onBack = back) }
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
