package com.lucafilippi.universebible.ui.studio

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lucafilippi.universebible.ui.viewmodel.StudioViewModel
import com.lucafilippi.universebible.ui.viewmodel.StudioState
import com.lucafilippi.universebible.UniverseBibleApplication
import com.lucafilippi.universebible.data.repository.*
import com.lucafilippi.universebible.domain.model.Universe
import kotlinx.coroutines.launch
import java.util.Locale

internal val Kind.singular get() = when(this) { Kind.PRODUCTION -> "Production"; Kind.CHARACTER -> "Character"; Kind.PHASE -> "Phase" }
private val tabs = listOf("Studio", "Projects", "Characters", "Phases", "More")
private val kinds = mapOf("Projects" to Kind.PRODUCTION, "Characters" to Kind.CHARACTER, "Phases" to Kind.PHASE)

@Composable
fun StudioWorkspace(universe: Universe, editUniverse: () -> Unit, deleteUniverse: () -> Unit, switchUniverse: () -> Unit) {
    val app = LocalContext.current.applicationContext as UniverseBibleApplication
    val model = viewModel<StudioViewModel>(key = universe.id) {
        StudioViewModel(StudioRepository(app.database.studioDao(), universe.id))
    }
    val repo = model.repository
    val state by model.state.collectAsStateWithLifecycle()
    val slate = when (val current = state) {
        StudioState.Loading -> { CircularProgressIndicator(); return }
        StudioState.ReadFailed -> {
            TextButton(onClick = model::reload) { Text(label("Could not complete the operation.") + " " + label("Retry")) }
            return
        }
        is StudioState.Ready -> current.slate
    }
    var tab by rememberSaveable(universe.id) { mutableStateOf("Studio") }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    var editing by rememberSaveable { mutableStateOf<Kind?>(null) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    var failure by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val saved = label("Saved")
    val detail = (slate.productions + slate.characters + slate.phases).find { it.id == detailId }
    fun operate(action: suspend () -> Unit) {
        if (busy) return
        busy = true
        scope.launch {
            try { action(); launch { snackbar.showSnackbar(saved) } }
            catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { failure = true }
            finally { busy = false }
        }
    }
    BackHandler(enabled = detailId != null && editing == null) { detailId = null }
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, bottomBar = {
        NavigationBar {
            tabs.forEachIndexed { i, name ->
                NavigationBarItem(selected = tab == name, onClick = { tab = name; detailId = null },
                    icon = { Text(listOf("◉","▣","♙","≋","•••")[i]) }, label = { Text(label(name), maxLines = 1) })
            }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            if (detail != null) {
                Detail(detail, slate, edit = { editing = detail.kind }, delete = { deleting = true }, open = { detailId = it.id },
                    link = { character, production, role -> operate { repo.link(character, production, role) } },
                    unlink = { operate { repo.unlink(it) } },
                    assign = { project, phase -> operate { repo.save(project.kind, project, project.fields + ("phaseId" to phase)) } })
            } else if (tab == "Studio") {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item { Artwork(universe.coverImagePath, Modifier.fillMaxWidth().height(150.dp)) }
                    item { Text(universe.name, style = MaterialTheme.typography.headlineLarge); Text(universe.studioName); Text(universe.description) }
                    val series = slate.productions.count { it["productionType"] in listOf("Television series","Animated series") }
                    val movies = slate.productions.count { it["productionType"] in listOf("Movie","Animated movie") }
                    item { Text(listOf("Projects" to slate.productions.size,"Movies" to movies,"Series" to series,"Characters" to slate.characters.size,"Phases" to slate.phases.size).joinToStringLocalized()) }
                    item { Text(label("Quick actions"), style = MaterialTheme.typography.titleMedium) }
                    Kind.entries.forEach { k -> item { OutlinedButton(onClick = { editing = k }) { Text(label("New") + " · " + label(k.singular)) } } }
                    listOf("Recent projects" to slate.productions.take(3), "Recent characters" to slate.characters.take(3), "Phases" to slate.phases).forEach { (title, entries) ->
                        item { Text(label(title), style = MaterialTheme.typography.titleLarge) }
                        if (entries.isEmpty()) item { Text(label("No entries yet")) }
                        entries.forEach { item { TextButton(onClick = { detailId = it.id }) { Text(it.title) } } }
                    }
                }
            } else if (tab == "More") {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(label("Universe settings"), style = MaterialTheme.typography.headlineSmall)
                    TextButton(onClick = editUniverse) { Text(label("Edit") + " · " + universe.name) }
                    TextButton(onClick = deleteUniverse) { Text(label("Delete") + " · " + universe.name) }
                    TextButton(onClick = switchUniverse) { Text(label("Switch universe")) }
                    LanguageSelector()
                    Text(label("About"), style = MaterialTheme.typography.titleLarge)
                    Text("UniverseBible 0.1.0"); Text(label("Offline cinematic universe planner"))
                }
            } else key(tab) {
                val kind = kinds.getValue(tab)
                Catalog(kind, slate, { editing = kind }, { detailId = it.id })
            }
        }
    }
    editing?.let { kind ->
        Editor(kind, detail, slate.phases, busy, { editing = null }) { fields ->
            operate { repo.save(kind, detail, fields); editing = null }
        }
    }
    if (deleting && detail != null) AlertDialog(onDismissRequest = { deleting = false }, title = { Text(label("Delete permanently?")) },
        text = { Text(detail.title + "\n" + label(if (detail.kind == Kind.PHASE) "Productions in this phase will become unassigned." else "This also removes its appearances.")) },
        confirmButton = { TextButton(enabled = !busy, onClick = { operate { repo.delete(detail); deleting = false; detailId = null } }) { Text(label("Delete")) } },
        dismissButton = { TextButton(onClick = { deleting = false }) { Text(label("Cancel")) } })
    if (failure) AlertDialog(onDismissRequest = { failure = false }, text = { Text(label("Could not complete the operation.")) },
        confirmButton = { TextButton(onClick = { failure = false }) { Text(label("Back")) } })
}

@Composable
internal fun List<Pair<String,Int>>.joinToStringLocalized(): String {
    val values = mutableListOf<String>()
    for ((key, value) in this) values.add(label(key) + " " + value)
    return values.joinToString("   ·   ")
}

@Composable
private fun Catalog(kind: Kind, slate: Slate, create: () -> Unit, open: (StudioItem) -> Unit) {
    var search by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(if (kind == Kind.PHASE) "displayOrder" else "Created") }
    var filters by rememberSaveable { mutableStateOf<Map<String,String>>(emptyMap()) }
    val criteria = when(kind) { Kind.PRODUCTION -> listOf("productionType","productionStatus","phaseId"); Kind.CHARACTER -> listOf("alignment","characterStatus"); else -> emptyList() }
    val items = slate.catalog(kind, search, filters, sort)
    LazyVerticalGrid(columns = GridCells.Adaptive(145.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Button(onClick = create) { Text(label("New") + " · " + label(kind.singular)) }
                OutlinedTextField(search, { search = it }, singleLine = true, label = { Text(label("Search")) })
                for (key in criteria) {
                    val options = if (key == "phaseId") listOf("" to label("Unassigned")) + slate.phases.map { it.id to it.title }
                        else choices.getValue(key).map { it to label(it) }
                    Select(label(key), filters[key] ?: "*", listOf("*" to label("All")) + options) { filters = filters + (key to it) }
                }
                val sorts = listOf("title","Created") + when(kind) { Kind.PRODUCTION -> listOf("releaseOrder"); Kind.PHASE -> listOf("displayOrder"); else -> emptyList() }
                Select(label("Sort"), sort, sorts.map { it to label(it) }) { sort = it }
                if (items.isEmpty()) Text(label(if(slate.items(kind).isEmpty()) "No entries yet" else "No matches"))
            }
        }
        items(items, key = { it.id }) { item ->
            Card(onClick = { open(item) }) {
                Artwork(item[item.kind.imageField], Modifier.fillMaxWidth().aspectRatio(.72f))
                Text(item.title, Modifier.padding(8.dp), style = MaterialTheme.typography.titleMedium)
                if (kind == Kind.PHASE) Text(label("Projects") + " " + slate.productions.count { it["phaseId"] == item.id }, Modifier.padding(8.dp))
            }
        }
    }
}
