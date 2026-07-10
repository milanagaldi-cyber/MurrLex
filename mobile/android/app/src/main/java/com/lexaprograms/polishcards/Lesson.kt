package com.lexaprograms.polishcards

import kotlinx.serialization.Serializable

@Serializable
data class Lesson(
    val id: String,
    val title: String,
    val lessonInfo: String = "",
    val sourceLanguage: String = "",
    val targetLanguage: String = "",
    val cards: List<Flashcard>,
    val createdAt: String = "",
    val updatedAt: String = "",
    val timesCompleted: Int = 0,
    val editable: Boolean = true,
    val hidden: Boolean = false
)
