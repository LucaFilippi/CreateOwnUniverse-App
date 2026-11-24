package com.lucafilippi.universebible.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "universes")
data class UniverseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val studioName: String,
    val coverImagePath: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
