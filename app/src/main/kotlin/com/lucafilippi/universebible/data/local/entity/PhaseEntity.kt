package com.lucafilippi.universebible.data.local.entity

import androidx.room.*
import java.util.UUID

@Entity(
    tableName = "phases",
    foreignKeys = [
        ForeignKey(entity = UniverseEntity::class, parentColumns = ["id"], childColumns = ["universeId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("universeId"), Index(value = ["universeId", "id"], unique = true)],
)
data class PhaseEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val universeId: String,
    val name: String = "",
    val displayOrder: Int = 0,
    val description: String = "",
    val coverImagePath: String? = null,
    val narrativeObjective: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
)
