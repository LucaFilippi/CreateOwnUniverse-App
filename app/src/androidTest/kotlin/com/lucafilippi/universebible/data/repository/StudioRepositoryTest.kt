package com.lucafilippi.universebible.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lucafilippi.universebible.data.local.database.UniverseDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StudioRepositoryTest {
    @Test fun catalogSearchFiltersAndOrdering() {
        val early = StudioItem(Kind.PRODUCTION, "1", 100, mapOf("title" to "Zenith", "releaseOrder" to "2", "productionStatus" to "Planned"))
        val late = StudioItem(Kind.PRODUCTION, "2", 200, mapOf("title" to "Arc", "releaseOrder" to "10", "productionStatus" to "Concept"))
        val slate = Slate(listOf(late, early), emptyList(), emptyList(), emptyList())
        assertEquals(listOf(early, late), slate.catalog(Kind.PRODUCTION, "", emptyMap(), "releaseOrder"))
        assertEquals(listOf(late, early), slate.catalog(Kind.PRODUCTION, "", emptyMap(), "title"))
        assertEquals(listOf(late, early), slate.catalog(Kind.PRODUCTION, "", emptyMap(), "Created"))
        assertEquals(listOf(early), slate.catalog(Kind.PRODUCTION, "ENI", mapOf("productionStatus" to "Planned"), "title"))
    }

    @Test fun upgradesExistingUniverseDatabase() = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        val filename = "migration-" + java.util.UUID.randomUUID() + ".db"
        try {
            context.openOrCreateDatabase(filename, Context.MODE_PRIVATE, null).use { legacy ->
                legacy.execSQL("CREATE TABLE universes (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, description TEXT NOT NULL, studioName TEXT NOT NULL, coverImagePath TEXT, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
                legacy.execSQL("INSERT INTO universes VALUES ('old','Legacy','','',NULL,1,1)")
                legacy.version = 1
            }
            val upgraded = Room.databaseBuilder(context, UniverseDatabase::class.java, filename).build()
            try {
                assertEquals("Legacy", UniverseRepository(upgraded.universeDao()).observeUniverses().first().single().name)
                StudioRepository(upgraded.studioDao(), "old").save(Kind.CHARACTER, null, mapOf("name" to "New cast"))
                assertEquals("New cast", upgraded.studioDao().characters("old").first().single().name)
            } finally { upgraded.close() }
        } finally { context.deleteDatabase(filename) }
    }

    @Test fun catalogRelationshipsAndIsolation() = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        val db = Room.inMemoryDatabaseBuilder(context, UniverseDatabase::class.java).build()
        try {
            val universes = UniverseRepository(db.universeDao())
            fun repo(id: String) = StudioRepository(db.studioDao(), id)
            val local = repo(universes.create("Local", "", ""))
            val foreign = repo(universes.create("Other", "", ""))
            local.save(Kind.PHASE, null, mapOf("name" to "Opening", "displayOrder" to "1"))
            val phase = local.slate.first().phases.single()
            local.save(Kind.PRODUCTION, null, mapOf("title" to "First", "phaseId" to phase.id))
            local.save(Kind.CHARACTER, null, mapOf("name" to "Aster"))
            foreign.save(Kind.CHARACTER, null, mapOf("name" to "Foreign"))
            val project = local.slate.first().productions.single()
            val character = local.slate.first().characters.single()
            val other = foreign.slate.first().characters.single()
            assertTrue(runCatching { foreign.save(Kind.PRODUCTION, null, mapOf("title" to "Invalid", "phaseId" to phase.id)) }.isFailure)
            assertTrue(runCatching { local.link(other.id, project.id, "Cameo") }.isFailure)
            assertTrue(runCatching { local.save(Kind.CHARACTER, null, mapOf("name" to " ")) }.isFailure)
            local.link(character.id, project.id, "Cameo")
            local.link(character.id, project.id, "Protagonist")
            val appearance = local.slate.first().appearances.single()
            assertEquals("Protagonist", appearance.role)
            local.unlink(appearance)
            assertTrue(local.slate.first().appearances.isEmpty())
            local.link(character.id, project.id, "")
            local.save(Kind.CHARACTER, character, character.fields + ("alias" to "Star"))
            local.save(Kind.PRODUCTION, project, project.fields + ("title" to "Revised"))
            local.save(Kind.PHASE, phase, phase.fields + ("displayOrder" to "2"))
            assertEquals("Star", local.slate.first().characters.single()["alias"])
            assertEquals("2", local.slate.first().phases.single()["displayOrder"])
            local.delete(phase)
            assertEquals("", local.slate.first().productions.single()["phaseId"])
            assertEquals("Revised", local.slate.first().productions.single().title)
            local.delete(character)
            assertTrue(local.slate.first().appearances.isEmpty())
            local.delete(project)
            assertTrue(local.slate.first().productions.isEmpty())
            universes.delete(foreign.universe)
            assertTrue(foreign.slate.first().characters.isEmpty())
        } finally { db.close() }
    }
}
