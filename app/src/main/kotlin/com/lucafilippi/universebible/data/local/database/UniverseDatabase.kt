package com.lucafilippi.universebible.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.lucafilippi.universebible.data.local.dao.UniverseDao
import com.lucafilippi.universebible.data.local.entity.UniverseEntity

@Database(entities = [UniverseEntity::class], version = 1, exportSchema = true)
abstract class UniverseDatabase : RoomDatabase() {
    abstract fun universeDao(): UniverseDao
}
