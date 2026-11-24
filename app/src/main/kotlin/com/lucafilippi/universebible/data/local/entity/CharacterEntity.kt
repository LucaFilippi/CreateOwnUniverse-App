package com.lucafilippi.universebible.data.local.entity

import androidx.room.*
import java.util.UUID

@Entity(
    tableName = "characters",
    foreignKeys = [
        ForeignKey(entity = UniverseEntity::class, parentColumns = ["id"], childColumns = ["universeId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("universeId"), Index(value = ["universeId", "id"], unique = true)],
)
data class CharacterEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val universeId: String,
    val name: String = "",
    val alias: String = "",
    val realName: String = "",
    val portraitImagePath: String? = null,
    val biography: String = "",
    val origin: String = "",
    val powers: String = "",
    val abilities: String = "",
    val weaknesses: String = "",
    val personality: String = "",
    val alignment: String = "Unknown",
    val status: String = "Unknown",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
)
