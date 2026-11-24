package com.lucafilippi.universebible.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lucafilippi.universebible.R
import com.lucafilippi.universebible.ui.screens.universes.UniverseDeleteDialog
import com.lucafilippi.universebible.ui.screens.universes.UniverseDetailScreen
import com.lucafilippi.universebible.ui.screens.universes.UniverseEditorDialog
import com.lucafilippi.universebible.ui.screens.universes.UniverseListScreen
import com.lucafilippi.universebible.ui.viewmodel.UniverseMutation
import com.lucafilippi.universebible.ui.viewmodel.UniversesState
import com.lucafilippi.universebible.ui.viewmodel.UniversesViewModel
import kotlinx.coroutines.launch

private const val UNIVERSES = "universes"
private const val UNIVERSE = "universe/{id}"

private enum class UniverseDialog { None, Create, Edit, Delete }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniverseBibleScreen(viewModel: UniversesViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val mutation by viewModel.mutation.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val selectedId = entry?.arguments?.getString("id")
    val selected = (state as? UniversesState.Ready)?.universes?.firstOrNull { it.id == selectedId }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var dialog by rememberSaveable { mutableStateOf(UniverseDialog.None) }
    val savedMessage = stringResource(R.string.universe_saved)
    val deletedMessage = stringResource(R.string.universe_deleted)

    fun dismissDialog() {
        dialog = UniverseDialog.None
        viewModel.dismissMutation()
    }

    LaunchedEffect(mutation) {
        when (mutation) {
            UniverseMutation.Saved -> {
                dismissDialog()
                scope.launch { snackbar.showSnackbar(savedMessage) }
            }
            UniverseMutation.Deleted -> {
                dismissDialog()
                if (navController.currentDestination?.route == UNIVERSE) navController.popBackStack()
                scope.launch { snackbar.showSnackbar(deletedMessage) }
            }
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                navigationIcon = {
                    if (selectedId != null) {
                        TextButton(onClick = { navController.popBackStack() }) {
                            Text(stringResource(R.string.back))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        NavHost(navController, startDestination = UNIVERSES, modifier = Modifier.padding(padding)) {
            composable(UNIVERSES) {
                UniverseListScreen(
                    state = state,
                    onCreate = { dialog = UniverseDialog.Create },
                    onOpen = { navController.navigate("universe/$it") { launchSingleTop = true } },
                    onRetry = viewModel::reload,
                )
            }
            composable(UNIVERSE, arguments = listOf(navArgument("id") { type = NavType.StringType })) { detailEntry ->
                val id = detailEntry.arguments?.getString("id")
                val universe = (state as? UniversesState.Ready)?.universes?.find { it.id == id }
                if (universe != null) com.lucafilippi.universebible.ui.studio.StudioWorkspace(
                    universe = universe,
                    editUniverse = { dialog = UniverseDialog.Edit },
                    deleteUniverse = { dialog = UniverseDialog.Delete },
                    switchUniverse = { navController.popBackStack() },
                )
            }
        }
    }

    when (dialog) {
        UniverseDialog.Create, UniverseDialog.Edit -> {
            if (dialog == UniverseDialog.Create || selected != null) {
                UniverseEditorDialog(
                    initial = selected,
                    busy = mutation == UniverseMutation.Saving,
                    failed = mutation == UniverseMutation.Failed,
                    onDismiss = ::dismissDialog,
                    onSave = { name, description, studio, cover ->
                        viewModel.save(selected?.id, name, description, studio, cover)
                    },
                )
            }
        }
        UniverseDialog.Delete -> selected?.let { universe ->
            UniverseDeleteDialog(
                name = universe.name,
                busy = mutation == UniverseMutation.Deleting,
                failed = mutation == UniverseMutation.Failed,
                onDismiss = ::dismissDialog,
                onDelete = { viewModel.delete(universe.id) },
            )
        }
        UniverseDialog.None -> Unit
    }
}
