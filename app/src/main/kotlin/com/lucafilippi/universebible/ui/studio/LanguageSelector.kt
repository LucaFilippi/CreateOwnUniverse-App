package com.lucafilippi.universebible.ui.studio

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Locale

private val Context.preferences by preferencesDataStore(name = "settings")
private val language = stringPreferencesKey("language")

fun localizedContext(base: Context): Context {
    val tag = runBlocking { base.preferences.data.first()[language] }.orEmpty()
    if (tag.isEmpty()) return base
    return base.createConfigurationContext(Configuration(base.resources.configuration).apply { setLocale(Locale.forLanguageTag(tag)) })
}

@Composable
internal fun LanguageSelector() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val stored by remember(context) { context.preferences.data.map { it[language].orEmpty() } }.collectAsState(initial = "")
    Select(label("Language"), stored, listOf("" to label("System default"), "en" to "English", "pt-BR" to "Português (Brasil)")) { tag ->
        scope.launch {
            context.preferences.edit { it[language] = tag }
            generateSequence(context) { (it as? ContextWrapper)?.baseContext }.filterIsInstance<Activity>().firstOrNull()?.recreate()
        }
    }
}
