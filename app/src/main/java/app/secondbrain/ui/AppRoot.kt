package app.secondbrain.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.secondbrain.R
import app.secondbrain.ui.components.dotGrid
import app.secondbrain.ui.screens.AddScreen
import app.secondbrain.ui.screens.BrainScreen
import app.secondbrain.ui.screens.DetailScreen
import app.secondbrain.ui.screens.SearchScreen
import app.secondbrain.ui.screens.SettingsScreen
import app.secondbrain.ui.screens.TagRuleScreen
import app.secondbrain.ui.screens.TagRulesScreen
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.merge

private class Tab(
    val route: Any,
    val type: KClass<*>,
    val label: String,
    @DrawableRes val icon: Int,
    @DrawableRes val selectedIcon: Int,
)

private val tabs = listOf(
    Tab(AddRoute, AddRoute::class, "Add", R.drawable.ic_tab_add, R.drawable.ic_tab_add_filled),
    Tab(SearchRoute, SearchRoute::class, "Library", R.drawable.ic_tab_search, R.drawable.ic_tab_search_filled),
    Tab(BrainRoute, BrainRoute::class, "Brain", R.drawable.ic_tab_brain, R.drawable.ic_tab_brain_filled),
)

@Composable
private fun TabIcon(tab: Tab, selected: Boolean) {
    // The label beside it names the tab, so the icon stays silent.
    Icon(painterResource(if (selected) tab.selectedIcon else tab.icon), contentDescription = null, Modifier.size(24.dp))
}

@Composable
fun AppRoot(viewModel: AppViewModel, addViewModel: AddViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Held here, outside the navigation graph, so each tab keeps its state while you are elsewhere.
    val searchViewModel: SearchViewModel = hiltViewModel()
    val brainViewModel: BrainViewModel = hiltViewModel()
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }

    val entry by navController.currentBackStackEntryAsState()
    val destination = entry?.destination
    val current = tabs.firstOrNull { destination?.hasRoute(it.type) == true }

    fun switchTo(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(Unit) {
        merge(viewModel.messages, addViewModel.messages).collect { message ->
            snackbar.currentSnackbarData?.dismiss()
            val saved = message.startsWith("Saved") && message.endsWith("brain")
            val result = snackbar.showSnackbar(message, actionLabel = "View".takeIf { saved }, duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) {
                brainViewModel.select(null)
                switchTo(BrainRoute)
            }
        }
    }
    LaunchedEffect(Unit) {
        addViewModel.shared.collect {
            // Drop anything stacked on the tabs, then bring Add forward.
            val ready = navController.currentDestination != null
            if (ready && !navController.popBackStack(AddRoute, inclusive = false)) switchTo(AddRoute)
        }
    }

    // Tabs sit along the bottom on phones and down the side on wider screens.
    val wide = LocalConfiguration.current.screenWidthDp >= 600

    Scaffold(
        modifier = Modifier.dotGrid(
            ground = MaterialTheme.colorScheme.background,
            dot = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.09f),
        ),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (current != null && !wide) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = tab === current,
                            onClick = { if (tab !== current) switchTo(tab.route) },
                            icon = { TabIcon(tab, selected = tab === current) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Row(Modifier.padding(padding).consumeWindowInsets(padding)) {
            if (current != null && wide) {
                NavigationRail(Modifier.fillMaxHeight(), containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                    tabs.forEach { tab ->
                        NavigationRailItem(
                            selected = tab === current,
                            onClick = { if (tab !== current) switchTo(tab.route) },
                            icon = { TabIcon(tab, selected = tab === current) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
            NavHost(
                navController = navController,
                startDestination = AddRoute,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                enterTransition = { fadeIn(tween(220)) },
                exitTransition = { fadeOut(tween(120)) },
                popEnterTransition = { fadeIn(tween(220)) },
                popExitTransition = { fadeOut(tween(120)) },
            ) {
                val openSettings: () -> Unit = { navController.navigate(SettingsRoute) { launchSingleTop = true } }
                val open: (Long) -> Unit = { id -> navController.navigate(DetailRoute(id)) { launchSingleTop = true } }
                val back: () -> Unit = { navController.popBackStack() }
                val searchTag: (String) -> Unit = { tag ->
                    searchViewModel.showTag(tag)
                    switchTo(SearchRoute)
                }
                composable<AddRoute> { AddScreen(addViewModel, onOpenSettings = openSettings) }
                composable<SearchRoute> {
                    SearchScreen(
                        viewModel = searchViewModel,
                        onOpen = open,
                        onComplete = viewModel::setCompleted,
                        onOpenSettings = openSettings,
                    )
                }
                composable<BrainRoute> {
                    BrainScreen(
                        viewModel = brainViewModel,
                        onOpen = open,
                        onComplete = viewModel::setCompleted,
                        onSearchTag = searchTag,
                        onAdd = { switchTo(AddRoute) },
                        onOpenSettings = openSettings,
                    )
                }
                composable<DetailRoute> {
                    DetailScreen(
                        onBack = back,
                        onOpenTag = { tag ->
                            // Leave the item first so it isn't restored on top of its tab later.
                            navController.popBackStack()
                            searchTag(tag)
                        },
                        onOpenItem = { id ->
                            navController.popBackStack()
                            open(id)
                        },
                    )
                }
                composable<SettingsRoute> {
                    SettingsScreen(
                        state, viewModel,
                        onBack = back,
                        onOpenTagRules = { navController.navigate(TagRulesRoute) { launchSingleTop = true } },
                    )
                }
                composable<TagRulesRoute> {
                    TagRulesScreen(
                        onBack = back,
                        onOpen = { name -> navController.navigate(TagRuleRoute(name)) { launchSingleTop = true } },
                        onExport = viewModel::exportRules,
                        onImport = viewModel::importRules,
                    )
                }
                composable<TagRuleRoute> { TagRuleScreen(onBack = back, onApply = viewModel::retagAll) }
            }
        }
    }
}
