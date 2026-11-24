package com.lucafilippi.universebible.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.lucafilippi.universebible.data.local.entity.UniverseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UniverseDao {
    @Query("SELECT * FROM universes ORDER BY updatedAt DESC, id ASC")
    fun observeAll(): Flow<List<UniverseEntity>>

    @Insert
    suspend fun insert(universe: UniverseEntity)

    @Query(
        """
        UPDATE universes
        SET name = :name, description = :description, studioName = :studioName, coverImagePath = :cover, updatedAt = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun update(
        id: String,
        name: String,
        description: String,
        studioName: String,
        cover: String?,
        updatedAt: Long,
    ): Int

    @Query("DELETE FROM universes WHERE id = :id")
    suspend fun delete(id: String): Int
}
