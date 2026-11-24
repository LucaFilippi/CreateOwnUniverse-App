package com.lucafilippi.universebible.data.local.entity

import androidx.room.*
import java.util.UUID

@Entity(
    tableName = "productions",
    foreignKeys = [
        ForeignKey(entity = UniverseEntity::class, parentColumns = ["id"], childColumns = ["universeId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PhaseEntity::class, parentColumns = ["id"], childColumns = ["phaseId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("universeId"), Index(value = ["universeId", "id"], unique = true), Index("phaseId")],
)
data class ProductionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val universeId: String,
    val title: String = "",
    val originalTitle: String = "",
    val posterImagePath: String? = null,
    val synopsis: String = "",
    val productionType: String = "Movie",
    val genre: String = "",
    val director: String = "",
    val writer: String = "",
    val releaseDate: Long? = null,
    val releaseOrder: Int = 0,
    val phaseId: String? = null,
    val plannedSeasonCount: Int? = null,
    val status: String = "Concept",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
)
