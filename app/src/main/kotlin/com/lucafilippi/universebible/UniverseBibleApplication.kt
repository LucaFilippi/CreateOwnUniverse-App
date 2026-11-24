package com.lucafilippi.universebible

import android.app.Application
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.room.Room
import com.lucafilippi.universebible.data.local.database.UniverseDatabase
import com.lucafilippi.universebible.data.repository.UniverseRepository

class UniverseBibleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Keep recent imports available to restored editor drafts.
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val cutoff = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
                val candidates = File(filesDir, "artwork").listFiles().orEmpty()
                    .filter { it.isFile && it.extension == "jpg" && it.lastModified() < cutoff }
                if (candidates.isNotEmpty()) {
                    val referenced = database.studioDao().referencedImages().toSet()
                    candidates.filter { it.absolutePath !in referenced }.forEach { it.delete() }
                }
            }.onFailure { android.util.Log.w("UniverseBible", "Artwork cleanup deferred", it) }
        }
    }

    val database by lazy {
        Room.databaseBuilder(this, UniverseDatabase::class.java, "universebible.db")
            .build()
    }

    val universeRepository by lazy {
        UniverseRepository(database.universeDao())
    }
}
