package com.lucafilippi.universebible.ui.screens.universes

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lucafilippi.universebible.R
import com.lucafilippi.universebible.domain.model.Universe
import com.lucafilippi.universebible.ui.viewmodel.UniversesState

@Composable
fun UniverseListScreen(
    state: UniversesState,
    onCreate: () -> Unit,
    onOpen: (String) -> Unit,
    onRetry: () -> Unit,
) {
    when (state) {
        UniversesState.Loading -> LoadingIndicator()
        UniversesState.LoadFailed -> LoadFailed(onRetry)
        is UniversesState.Ready -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.universes_title), style = MaterialTheme.typography.headlineLarge)
                    Button(onClick = onCreate) { Text(stringResource(R.string.new_universe)) }
                }
            }
            if (state.universes.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.universes_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(state.universes, key = { it.id }) { universe ->
                Card(onClick = { onOpen(universe.id) }, modifier = Modifier.fillMaxWidth()) {
                    UniverseBanner(universe, expanded = false)
                    Text(
                        text = stringResource(R.string.last_updated, formattedDate(universe.updatedAt)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun UniverseDetailScreen(
    state: UniversesState,
    id: String?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRetry: () -> Unit,
) {
    when (state) {
        UniversesState.Loading -> LoadingIndicator()
        UniversesState.LoadFailed -> LoadFailed(onRetry)
        is UniversesState.Ready -> {
            val universe = state.universes.firstOrNull { it.id == id }
            if (universe == null) {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.universe_missing))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        Card(Modifier.fillMaxWidth()) { UniverseBanner(universe, expanded = true) }
                    }
                    if (universe.description.isNotBlank()) {
                        item { Text(universe.description, style = MaterialTheme.typography.bodyLarge) }
                    }
                    item {
                        Text(
                            text = stringResource(R.string.last_updated, formattedDate(universe.updatedAt)),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = onEdit) { Text(stringResource(R.string.edit_universe)) }
                            OutlinedButton(onClick = onDelete) { Text(stringResource(R.string.delete_universe)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UniverseBanner(universe: Universe, expanded: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.surface),
                ),
            )
            .padding(horizontal = 20.dp, vertical = if (expanded) 40.dp else 24.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = universe.name,
            style = if (expanded) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineSmall,
            maxLines = if (expanded) Int.MAX_VALUE else 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (universe.studioName.isNotBlank()) {
            Text(
                text = universe.studioName,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
private fun LoadFailed(onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.universes_load_failed))
            Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
    }
}

@Composable
private fun formattedDate(timestamp: Long): String =
    DateUtils.formatDateTime(
        LocalContext.current,
        timestamp,
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR,
    )
