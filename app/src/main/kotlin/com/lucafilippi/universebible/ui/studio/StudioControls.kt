package com.lucafilippi.universebible.ui.studio

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.lucafilippi.universebible.data.storage.importImage
import kotlinx.coroutines.launch
import java.io.File

@Composable
internal fun Artwork(path: String?, modifier: Modifier) {
    if (path.isNullOrBlank()) Placeholder(modifier)
    else SubcomposeAsyncImage(model = File(path), contentDescription = null, modifier = modifier,
        contentScale = ContentScale.Crop, error = { Placeholder(Modifier.fillMaxSize()) })
}

@Composable
private fun Placeholder(modifier: Modifier) {
    Canvas(modifier) {
        drawRect(Color(0xFF202331))
        val center = Offset(size.width * .67f, size.height * .55f)
        drawCircle(Color(0xFF4B376C), size.minDimension * .37f, center)
        drawCircle(Color(0xFFA78BFA), size.minDimension * .46f, center, style = Stroke(2.dp.toPx()))
        for (index in 1..15) {
            drawCircle(Color(0xFFC6B9E9), (1 + index % 2).toFloat(), Offset(size.width * ((index * 37) % 97) / 97, size.height * ((index * 53) % 89) / 89))
        }
    }
}

@Composable
internal fun ImageField(path: String, onChange: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var failed by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            runCatching { importImage(context, uri) }.onSuccess(onChange).onFailure { failed = true }
        }
    }
    Artwork(path, Modifier.fillMaxWidth().height(130.dp))
    Row {
        TextButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text(label("Pick image")) }
        if (path.isNotBlank()) TextButton(onClick = { onChange("") }) { Text(label("Remove")) }
    }
    if (failed) Text(label("Could not complete the operation."), color = MaterialTheme.colorScheme.error)
}

@Composable
internal fun Select(labelText: String, selected: String, options: List<Pair<String,String>>, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }) { Text(labelText + " · " + options.firstOrNull { it.first == selected }?.second.orEmpty()) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (key, text) -> DropdownMenuItem(text = { Text(text) }, onClick = { open = false; onSelect(key) }) }
        }
    }
}
