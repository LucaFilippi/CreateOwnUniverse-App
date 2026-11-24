package com.lucafilippi.universebible.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.lucafilippi.universebible.data.local.dao.UniverseDao
import com.lucafilippi.universebible.data.local.entity.*
import com.lucafilippi.universebible.data.local.dao.StudioDao
import androidx.room.AutoMigration

@Database(
    entities = [UniverseEntity::class, ProductionEntity::class, CharacterEntity::class, PhaseEntity::class, Appearance::class],
    version = 2,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
    exportSchema = true,
)
abstract class UniverseDatabase : RoomDatabase() {
    abstract fun studioDao(): StudioDao
    abstract fun universeDao(): UniverseDao
}
