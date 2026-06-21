package com.lexaprograms.polishcards

import kotlinx.serialization.Serializable

@Serializable
data class Lesson(
    val id: String,
    val title: String,
    val cards: List<Flashcard>,
    val timesCompleted: Int = 0,
    val editable: Boolean = true,
    val hidden: Boolean = false
)
