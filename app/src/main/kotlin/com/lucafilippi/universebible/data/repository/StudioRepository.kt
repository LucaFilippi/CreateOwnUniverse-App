package com.lucafilippi.universebible.data.repository

import com.lucafilippi.universebible.data.local.dao.StudioDao
import com.lucafilippi.universebible.data.local.entity.*
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

enum class Kind(val fields: List<String>, val imageField: String) {
    PRODUCTION(listOf("title", "originalTitle", "posterImagePath", "synopsis", "productionType", "genre", "director", "writer", "releaseDate", "releaseOrder", "phaseId", "plannedSeasonCount", "productionStatus", "notes"), "posterImagePath"),
    CHARACTER(listOf("name", "alias", "realName", "portraitImagePath", "biography", "origin", "powers", "abilities", "weaknesses", "personality", "alignment", "characterStatus", "notes"), "portraitImagePath"),
    PHASE(listOf("name", "displayOrder", "description", "coverImagePath", "narrativeObjective", "notes"), "coverImagePath")
}

data class StudioItem(val kind: Kind, val id: String, val created: Long, val fields: Map<String, String>) {
    val title get() = fields.getValue(kind.fields.first())
    operator fun get(key: String) = fields[key].orEmpty()
}

val choices = mapOf(
    "productionType" to listOf("Movie", "Television series", "Special presentation", "Short film", "Animated movie", "Animated series"),
    "productionStatus" to listOf("Concept", "Planned", "In development", "Completed", "Cancelled"),
    "alignment" to listOf("Hero", "Villain", "Antihero", "Neutral", "Unknown"),
    "characterStatus" to listOf("Alive", "Deceased", "Missing", "Unknown"),
    "role" to listOf("Protagonist", "Antagonist", "Supporting character", "Cameo", "Post-credits appearance", "Other"),
)

data class Slate(val productions: List<StudioItem>, val characters: List<StudioItem>, val phases: List<StudioItem>, val appearances: List<Appearance>) {
    fun items(kind: Kind) = when (kind) {
        Kind.PRODUCTION -> productions
        Kind.CHARACTER -> characters
        Kind.PHASE -> phases
    }
}

fun Slate.catalog(kind: Kind, search: String, filters: Map<String, String>, sort: String): List<StudioItem> {
    return items(kind).filter { item ->
        val matchesName = item.title.contains(search, true) || item["alias"].contains(search, true)
        matchesName && filters.all { (key, value) -> value == "*" || item[key] == value }
    }.sortedWith(when (sort) {
        "Created" -> compareByDescending { it.created }
        "releaseOrder", "displayOrder" -> compareBy { it[sort].toIntOrNull() ?: 0 }
        else -> compareBy { it.title.lowercase(java.util.Locale.ROOT) }
    })
}

class StudioRepository(private val dao: StudioDao, val universe: String) {
    val slate = combine(dao.productions(universe), dao.characters(universe), dao.phases(universe), dao.appearances(universe)) { projects, cast, phases, links ->
        Slate(projects.map { it.toItem() }, cast.map { it.toItem() }, phases.map { it.toItem() }, links)
    }

    suspend fun save(kind: Kind, existing: StudioItem?, fields: Map<String, String>) {
        fun value(key: String) = fields[key].orEmpty().trim()
        val id = existing?.id ?: UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val created = existing?.created ?: now
        when (kind) {
            Kind.PRODUCTION -> dao.save(ProductionEntity(
                id = id, universeId = universe, createdAt = created, updatedAt = now,
                title = value("title"),
                originalTitle = value("originalTitle"),
                posterImagePath = value("posterImagePath").ifBlank { null },
                synopsis = value("synopsis"),
                productionType = value("productionType"),
                genre = value("genre"),
                director = value("director"),
                writer = value("writer"),
                releaseDate = value("releaseDate").takeIf { it.isNotBlank() }?.let { LocalDate.parse(it).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() },
                releaseOrder = value("releaseOrder").ifBlank { "0" }.toInt(),
                phaseId = value("phaseId").ifBlank { null },
                plannedSeasonCount = value("plannedSeasonCount").takeIf { value("productionType") in listOf("Television series", "Animated series") && it.isNotBlank() }?.toInt(),
                status = value("productionStatus"),
                notes = value("notes"),
            ))
            Kind.CHARACTER -> dao.save(CharacterEntity(
                id = id, universeId = universe, createdAt = created, updatedAt = now,
                name = value("name"),
                alias = value("alias"),
                realName = value("realName"),
                portraitImagePath = value("portraitImagePath").ifBlank { null },
                biography = value("biography"),
                origin = value("origin"),
                powers = value("powers"),
                abilities = value("abilities"),
                weaknesses = value("weaknesses"),
                personality = value("personality"),
                alignment = value("alignment"),
                status = value("characterStatus"),
                notes = value("notes"),
            ))
            Kind.PHASE -> dao.save(PhaseEntity(
                id = id, universeId = universe, createdAt = created, updatedAt = now,
                name = value("name"),
                displayOrder = value("displayOrder").ifBlank { "0" }.toInt(),
                description = value("description"),
                coverImagePath = value("coverImagePath").ifBlank { null },
                narrativeObjective = value("narrativeObjective"),
                notes = value("notes"),
            ))
        }
    }

    suspend fun delete(item: StudioItem) = when (item.kind) {
        Kind.PRODUCTION -> dao.deleteProduction(universe, item.id)
        Kind.CHARACTER -> dao.deleteCharacter(universe, item.id)
        Kind.PHASE -> dao.deletePhase(universe, item.id)
    }

    suspend fun link(character: String, production: String, role: String) =
        dao.save(Appearance(universe, character, production, role))
    suspend fun unlink(link: Appearance) = dao.remove(link)
}

private fun ProductionEntity.toItem() = StudioItem(
    Kind.PRODUCTION, id, createdAt, mapOf(
        "title" to title,
        "originalTitle" to originalTitle,
        "posterImagePath" to posterImagePath .orEmpty(),
        "synopsis" to synopsis,
        "productionType" to productionType,
        "genre" to genre,
        "director" to director,
        "writer" to writer,
        "releaseDate" to releaseDate?.let { java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDate().toString() }.orEmpty(),
        "releaseOrder" to releaseOrder.toString(),
        "phaseId" to phaseId .orEmpty(),
        "plannedSeasonCount" to plannedSeasonCount?.toString().orEmpty(),
        "productionStatus" to status,
        "notes" to notes
    ),
)

private fun CharacterEntity.toItem() = StudioItem(
    Kind.CHARACTER, id, createdAt, mapOf(
        "name" to name,
        "alias" to alias,
        "realName" to realName,
        "portraitImagePath" to portraitImagePath .orEmpty(),
        "biography" to biography,
        "origin" to origin,
        "powers" to powers,
        "abilities" to abilities,
        "weaknesses" to weaknesses,
        "personality" to personality,
        "alignment" to alignment,
        "characterStatus" to status,
        "notes" to notes
    ),
)

private fun PhaseEntity.toItem() = StudioItem(
    Kind.PHASE, id, createdAt, mapOf(
        "name" to name,
        "displayOrder" to displayOrder.toString(),
        "description" to description,
        "coverImagePath" to coverImagePath .orEmpty(),
        "narrativeObjective" to narrativeObjective,
        "notes" to notes
    ),
)
