package com.lexaprograms.polishcards

import kotlinx.serialization.Serializable

@Serializable
data class CatDialog(
    val id: String,
    val title: String,
    val basicLanguage: String,
    val targetLanguage: String,
    val messages: List<CatDialogMessage> = emptyList(),
    val createdAt: String = "",
    val updatedAt: String = "",
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
    val featured: Boolean = false,
    val hidden: Boolean = false
)

@Serializable
data class CatDialogMessage(
    val id: String,
    val text: String,
    val fromCat: Boolean,
    val createdAt: String = "",
    val createdAtMillis: Long = 0L,
    val analysis: String = "",
    val featuredSelection: String = ""
)
