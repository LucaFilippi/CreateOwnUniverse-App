package com.lucafilippi.universebible.data.local.entity

import androidx.room.*

@Entity(
    tableName = "appearances",
    primaryKeys = ["characterId", "productionId"],
    foreignKeys = [
        ForeignKey(entity = CharacterEntity::class, parentColumns = ["universeId", "id"], childColumns = ["universeId", "characterId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ProductionEntity::class, parentColumns = ["universeId", "id"], childColumns = ["universeId", "productionId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("universeId", "characterId"), Index("universeId", "productionId")],
)
data class Appearance(val universeId: String, val characterId: String, val productionId: String, val role: String)
