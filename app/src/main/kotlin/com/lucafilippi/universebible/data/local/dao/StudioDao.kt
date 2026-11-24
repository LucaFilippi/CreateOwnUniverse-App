package com.lucafilippi.universebible.data.local.dao

import androidx.room.*
import com.lucafilippi.universebible.data.local.entity.*
import kotlinx.coroutines.flow.Flow

data class UniverseCounts(val projects: Int, val characters: Int, val phases: Int)

@Dao
abstract class StudioDao {
    @Query("""
        SELECT coverImagePath FROM universes WHERE coverImagePath IS NOT NULL
        UNION SELECT posterImagePath FROM productions WHERE posterImagePath IS NOT NULL
        UNION SELECT portraitImagePath FROM characters WHERE portraitImagePath IS NOT NULL
        UNION SELECT coverImagePath FROM phases WHERE coverImagePath IS NOT NULL
    """)
    abstract suspend fun referencedImages(): List<String>

    @Query("SELECT (SELECT count(*) FROM productions WHERE universeId=:id) AS projects, (SELECT count(*) FROM characters WHERE universeId=:id) AS characters, (SELECT count(*) FROM phases WHERE universeId=:id) AS phases")
    abstract fun counts(id: String): Flow<UniverseCounts>

    @Query("SELECT * FROM productions WHERE universeId = :universe ORDER BY createdAt DESC, id")
    abstract fun productions(universe: String): Flow<List<ProductionEntity>>

    @Upsert
    protected abstract suspend fun store(row: ProductionEntity)

    @Query("SELECT universeId FROM productions WHERE id = :id")
    protected abstract suspend fun ownerOfProduction(id: String): String?

    @Transaction
    open suspend fun save(row: ProductionEntity) {
        require(row.title.isNotBlank())
        require(ownerOfProduction(row.id)?.let { it == row.universeId } != false)
        require(row.phaseId == null || ownerOfPhase(row.phaseId) == row.universeId)
        require(row.plannedSeasonCount == null || row.plannedSeasonCount > 0)
        store(row)
        touch(row.universeId)
    }

    @Query("DELETE FROM productions WHERE id = :id AND universeId = :universe")
    protected abstract suspend fun removeProduction(universe: String, id: String)

    @Transaction
    open suspend fun deleteProduction(universe: String, id: String) {
        removeProduction(universe, id)
        touch(universe)
    }

    @Query("SELECT * FROM characters WHERE universeId = :universe ORDER BY createdAt DESC, id")
    abstract fun characters(universe: String): Flow<List<CharacterEntity>>

    @Upsert
    protected abstract suspend fun store(row: CharacterEntity)

    @Query("SELECT universeId FROM characters WHERE id = :id")
    protected abstract suspend fun ownerOfCharacter(id: String): String?

    @Transaction
    open suspend fun save(row: CharacterEntity) {
        require(row.name.isNotBlank())
        require(ownerOfCharacter(row.id)?.let { it == row.universeId } != false)
        store(row)
        touch(row.universeId)
    }

    @Query("DELETE FROM characters WHERE id = :id AND universeId = :universe")
    protected abstract suspend fun removeCharacter(universe: String, id: String)

    @Transaction
    open suspend fun deleteCharacter(universe: String, id: String) {
        removeCharacter(universe, id)
        touch(universe)
    }

    @Query("SELECT * FROM phases WHERE universeId = :universe ORDER BY displayOrder ASC, id")
    abstract fun phases(universe: String): Flow<List<PhaseEntity>>

    @Upsert
    protected abstract suspend fun store(row: PhaseEntity)

    @Query("SELECT universeId FROM phases WHERE id = :id")
    protected abstract suspend fun ownerOfPhase(id: String): String?

    @Transaction
    open suspend fun save(row: PhaseEntity) {
        require(row.name.isNotBlank())
        require(ownerOfPhase(row.id)?.let { it == row.universeId } != false)
        store(row)
        touch(row.universeId)
    }

    @Query("DELETE FROM phases WHERE id = :id AND universeId = :universe")
    protected abstract suspend fun removePhase(universe: String, id: String)

    @Transaction
    open suspend fun deletePhase(universe: String, id: String) {
        removePhase(universe, id)
        touch(universe)
    }

    @Query("UPDATE universes SET updatedAt = :now WHERE id = :universe")
    abstract suspend fun touch(universe: String, now: Long = System.currentTimeMillis())

    @Query("SELECT * FROM appearances WHERE universeId = :universe")
    abstract fun appearances(universe: String): Flow<List<Appearance>>

    @Upsert
    protected abstract suspend fun store(row: Appearance)

    @Transaction
    open suspend fun save(row: Appearance) {
        store(row)
        touch(row.universeId)
    }

    @Query("DELETE FROM appearances WHERE universeId = :universe AND characterId = :character AND productionId = :production")
    protected abstract suspend fun unlink(universe: String, character: String, production: String)

    @Transaction
    open suspend fun remove(row: Appearance) {
        unlink(row.universeId, row.characterId, row.productionId)
        touch(row.universeId)
    }
}
