package com.lucafilippi.universebible.data.repository

import com.lucafilippi.universebible.data.local.dao.UniverseDao
import com.lucafilippi.universebible.data.local.entity.UniverseEntity
import com.lucafilippi.universebible.domain.model.Universe
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UniverseRepository(private val dao: UniverseDao) {
    fun observeUniverses(): Flow<List<Universe>> =
        dao.observeAll().map { entities -> entities.map { it.toUniverse() } }

    suspend fun create(name: String, description: String, studioName: String): String {
        val trimmedName = name.trim()
        require(trimmedName.isNotEmpty()) { "Universe name is required." }
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        dao.insert(
            UniverseEntity(
                id = id,
                name = trimmedName,
                description = description.trim(),
                studioName = studioName.trim(),
                coverImagePath = null,
                createdAt = now,
                updatedAt = now,
            ),
        )
        return id
    }

    suspend fun edit(id: String, name: String, description: String, studioName: String) {
        val trimmedName = name.trim()
        require(trimmedName.isNotEmpty()) { "Universe name is required." }
        check(
            dao.update(id, trimmedName, description.trim(), studioName.trim(), System.currentTimeMillis()) == 1,
        ) { "Universe no longer exists." }
    }

    suspend fun delete(id: String) {
        dao.delete(id)
    }
}

private fun UniverseEntity.toUniverse() = Universe(
    id = id,
    name = name,
    description = description,
    studioName = studioName,
    coverImagePath = coverImagePath,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
