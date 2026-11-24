package com.lucafilippi.universebible.domain.model

data class Universe(
    val id: String,
    val name: String,
    val description: String,
    val studioName: String,
    val coverImagePath: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
