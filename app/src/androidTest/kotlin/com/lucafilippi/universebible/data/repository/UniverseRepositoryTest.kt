package com.lucafilippi.universebible.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lucafilippi.universebible.data.local.database.UniverseDatabase
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UniverseRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun createsEditsAndDeletesOnlyTheSelectedUniverse() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, UniverseDatabase::class.java).build()
        try {
            val repository = UniverseRepository(db.universeDao())
            val editedId = repository.create("  Night Sky ", " Opening slate ", " Studio A ")
            val otherId = repository.create("Daybreak", "", "Studio B")

            val created = repository.observeUniverses().first().single { it.id == editedId }
            assertEquals(editedId, UUID.fromString(editedId).toString())
            assertNotEquals(otherId, editedId)
            assertEquals("Night Sky", created.name)
            assertEquals("Opening slate", created.description)
            assertEquals("Studio A", created.studioName)
            assertNull(created.coverImagePath)
            assertEquals(created.createdAt, created.updatedAt)

            repository.edit(editedId, " Night Sky Stories ", " Second slate ", " Studio C ")
            val edited = repository.observeUniverses().first().single { it.id == editedId }
            assertEquals("Night Sky Stories", edited.name)
            assertEquals("Second slate", edited.description)
            assertEquals("Studio C", edited.studioName)
            assertEquals(created.createdAt, edited.createdAt)
            assertTrue(edited.updatedAt >= edited.createdAt)

            repository.delete(editedId)
            val remaining = repository.observeUniverses().first().single()
            assertEquals(otherId, remaining.id)
            assertEquals("Daybreak", remaining.name)
        } finally {
            db.close()
        }
    }

    @Test
    fun rejectsBlankNamesForCreateAndEdit() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, UniverseDatabase::class.java).build()
        try {
            val repository = UniverseRepository(db.universeDao())

            val createError = runCatching { repository.create("   ", "description", "studio") }
            assertTrue(createError.exceptionOrNull() is IllegalArgumentException)
            assertTrue(repository.observeUniverses().first().isEmpty())

            val id = repository.create("Daybreak", "", "")
            val editError = runCatching { repository.edit(id, "   ", "description", "studio") }
            assertTrue(editError.exceptionOrNull() is IllegalArgumentException)
            assertEquals("Daybreak", repository.observeUniverses().first().single().name)
        } finally {
            db.close()
        }
    }

    @Test
    fun universeSurvivesDatabaseBeingClosedAndReopened() = runBlocking {
        val filename = "universe-test-${UUID.randomUUID()}.db"
        fun openDatabase() = Room.databaseBuilder(context, UniverseDatabase::class.java, filename).build()

        try {
            val original = openDatabase()
            val id = try {
                UniverseRepository(original.universeDao()).create("Archive", "Persisted on disk", "Studio")
            } finally {
                original.close()
            }
            val reopened = openDatabase()
            try {
                val universe = UniverseRepository(reopened.universeDao()).observeUniverses().first().single()
                assertEquals(id, universe.id)
                assertEquals("Persisted on disk", universe.description)
            } finally {
                reopened.close()
            }
        } finally {
            context.deleteDatabase(filename)
        }
        Unit
    }
}
