package com.lucafilippi.universebible.ui.screens.universes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lucafilippi.universebible.R
import com.lucafilippi.universebible.domain.model.Universe

@Composable
fun UniverseEditorDialog(
    initial: Universe?,
    busy: Boolean,
    failed: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String, studio: String) -> Unit,
) {
    var name by rememberSaveable(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var studio by rememberSaveable(initial?.id) { mutableStateOf(initial?.studioName.orEmpty()) }
    var description by rememberSaveable(initial?.id) { mutableStateOf(initial?.description.orEmpty()) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    val nameMissing = submitted && name.isBlank()

    fun requestDismiss() {
        if (busy) return
        val hasChanges = name != initial?.name.orEmpty() ||
            studio != initial?.studioName.orEmpty() || description != initial?.description.orEmpty()
        if (hasChanges) confirmDiscard = true else onDismiss()
    }

    AlertDialog(
        onDismissRequest = ::requestDismiss,
        title = { Text(stringResource(if (initial == null) R.string.new_universe else R.string.edit_universe)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.universe_name)) },
                    singleLine = true,
                    enabled = !busy,
                    isError = nameMissing,
                    supportingText = if (nameMissing) {
                        { Text(stringResource(R.string.universe_name_required)) }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = studio,
                    onValueChange = { studio = it },
                    label = { Text(stringResource(R.string.studio_name)) },
                    singleLine = true,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.universe_description)) },
                    minLines = 3,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (failed) {
                    Text(stringResource(R.string.universe_save_failed), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy,
                onClick = {
                    submitted = true
                    if (name.isNotBlank()) onSave(name, description, studio)
                },
            ) {
                Text(stringResource(if (busy) R.string.saving else R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = ::requestDismiss, enabled = !busy) { Text(stringResource(R.string.cancel)) }
        },
    )

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.discard_changes)) },
            text = { Text(stringResource(R.string.discard_changes_message)) },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.discard)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) { Text(stringResource(R.string.keep_editing)) }
            },
        )
    }
}

@Composable
fun UniverseDeleteDialog(
    name: String,
    busy: Boolean,
    failed: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(stringResource(R.string.delete_universe)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.delete_universe_message, name))
                if (failed) {
                    Text(stringResource(R.string.universe_delete_failed), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDelete, enabled = !busy) {
                Text(
                    stringResource(if (busy) R.string.deleting else R.string.delete),
                    color = if (busy) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text(stringResource(R.string.cancel)) }
        },
    )
}
