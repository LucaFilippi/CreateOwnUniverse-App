package com.lucafilippi.universebible

import android.app.Application
import androidx.room.Room
import com.lucafilippi.universebible.data.local.database.UniverseDatabase
import com.lucafilippi.universebible.data.repository.UniverseRepository

class UniverseBibleApplication : Application() {
    val universeRepository by lazy {
        val database = Room.databaseBuilder(this, UniverseDatabase::class.java, "universebible.db")
            .build()
        UniverseRepository(database.universeDao())
    }
}
