package com.lucafilippi.universebible.ui.studio

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lucafilippi.universebible.data.local.entity.Appearance
import com.lucafilippi.universebible.data.repository.*
import java.time.LocalDate

@Composable
internal fun Editor(kind: Kind, existing: StudioItem?, phases: List<StudioItem>, busy: Boolean, close: () -> Unit, save: (Map<String,String>) -> Unit) {
    val initial = remember(existing?.id, kind) {
        existing?.fields ?: kind.fields.associateWith { key -> when(key) {
            "characterStatus","alignment" -> "Unknown"
            "displayOrder" -> ((phases.maxOfOrNull { it["displayOrder"].toIntOrNull() ?: 0 } ?: 0) + 1).toString()
            else -> choices[key]?.firstOrNull().orEmpty()
        } }
    }
    var fields by rememberSaveable { mutableStateOf<Map<String,String>>(initial.toMap()) }
    var invalid by rememberSaveable { mutableStateOf(false) }
    var discard by rememberSaveable { mutableStateOf(false) }
    fun change(key: String, value: String) { fields = HashMap(fields).apply { put(key, value) } }
    fun dismiss() { if (!busy) { if (fields == initial) close() else discard = true } }
    AlertDialog(onDismissRequest = ::dismiss, title = { Text(label(if(existing == null) "New" else "Edit") + " · " + label(kind.singular)) },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (key in kind.fields) {
                val value = fields[key].orEmpty()
                when {
                    key == "plannedSeasonCount" && fields["productionType"] !in listOf("Animated series","Television series") -> Unit
                    key == kind.imageField -> ImageField(value) { change(key,it) }
                    key == "phaseId" -> Select(label(key), value, listOf("" to label("Unassigned")) + phases.map { it.id to it.title }) { change(key,it) }
                    key in choices -> Select(label(key), value, choices.getValue(key).map { it to label(it) }) { change(key,it) }
                    else -> OutlinedTextField(value, { change(key,it) }, label = { Text(label(key)) }, modifier = Modifier.fillMaxWidth())
                }
            }
            if (invalid) Text(label("Check the required name, numbers and date."), color = MaterialTheme.colorScheme.error)
        } },
        confirmButton = { TextButton(enabled = !busy, onClick = {
            val date = fields["releaseDate"].orEmpty()
            val seasons = fields["plannedSeasonCount"].orEmpty()
            invalid = fields[kind.fields.first()].isNullOrBlank() ||
                listOf("displayOrder","releaseOrder").any { !fields[it].isNullOrBlank() && fields[it]?.toIntOrNull() == null } ||
                (fields["productionType"] in listOf("Animated series","Television series") && seasons.isNotBlank() && (seasons.toIntOrNull() ?: 0) < 1) ||
                (date.isNotBlank() && runCatching { LocalDate.parse(date) }.isFailure)
            if (!invalid) save(fields)
        }) { Text(label("Save")) } },
        dismissButton = { TextButton(onClick = ::dismiss, enabled = !busy) { Text(label("Cancel")) } })
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text(label("Discard changes?")) },
        confirmButton = { TextButton(onClick = close) { Text(label("Discard")) } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text(label("Keep editing")) } })
}

@Composable
internal fun Detail(item: StudioItem, slate: Slate, edit: () -> Unit, delete: () -> Unit, open: (StudioItem) -> Unit,
    link: (String,String,String) -> Unit, unlink: (Appearance) -> Unit, assign: (StudioItem,String) -> Unit) {
    key(item.id) {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Artwork(item[item.kind.imageField], Modifier.fillMaxWidth().height(230.dp))
            Text(item.title, style = MaterialTheme.typography.headlineLarge)
            Row { TextButton(onClick = edit) { Text(label("Edit")) }; TextButton(onClick = delete) { Text(label("Delete")) } }
            for (field in item.kind.fields.drop(1)) {
                val raw = item[field]
                if (raw.isBlank() || field == item.kind.imageField) continue
                val value = when { field == "phaseId" -> slate.phases.find { it.id == raw }?.title.orEmpty()
                    field in choices -> label(raw)
                    else -> raw }
                Text(label(field), color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelMedium)
                Text(value)
            }
            if (item.kind == Kind.PHASE) {
                val lineup = slate.productions.filter { it["phaseId"] == item.id }.sortedBy { it["releaseOrder"].toIntOrNull() ?: 0 }
                Text(label("Projects") + " · " + lineup.size, style = MaterialTheme.typography.titleLarge)
                for (project in lineup) {
                    Card(onClick = { open(project) }) {
                        Artwork(project["posterImagePath"], Modifier.fillMaxWidth().height(128.dp))
                        Text(project.title); Text(label(project["productionType"]) + "  " + project["releaseDate"])
                    }
                    TextButton(onClick = { assign(project,"") }) { Text(label("Remove")) }
                }
                val available = slate.productions.filter { it["phaseId"] != item.id }
                if (available.isNotEmpty()) Select(label("Assign projects"), "", available.map { it.id to it.title }) { id -> assign(available.first { it.id == id }, item.id) }
            } else {
                val project = item.kind == Kind.PRODUCTION
                val candidates = if (project) slate.characters else slate.productions
                val links = slate.appearances.filter { if(project) it.productionId == item.id else it.characterId == item.id }
                Text(label("Appearances"), style = MaterialTheme.typography.titleLarge)
                if (links.isEmpty()) Text(label("No appearances yet"))
                for (appearance in links) {
                    val other = candidates.find { it.id == if(project) appearance.characterId else appearance.productionId } ?: continue
                    TextButton(onClick = { open(other) }) { Text(other.title) }
                    Select(label("role"), appearance.role, listOf("" to "—") + choices.getValue("role").map { it to label(it) }) { link(appearance.characterId,appearance.productionId,it) }
                    TextButton(onClick = { unlink(appearance) }) { Text(label("Remove")) }
                }
                val available = candidates.filter { other -> links.none { if(project) it.characterId == other.id else it.productionId == other.id } }
                if (available.isNotEmpty()) Select(label("Add appearance"), "", available.map { it.id to it.title }) { id ->
                    if (project) link(id,item.id,"") else link(item.id,id,"")
                }
            }
        }
    }
}
