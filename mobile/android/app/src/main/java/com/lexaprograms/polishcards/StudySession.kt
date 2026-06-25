package com.lexaprograms.polishcards

import kotlinx.serialization.Serializable

@Serializable
data class StudySession(
    val lessonId: String = "",
    val mode: String = StudyMode.ORIGINAL.name,
    val currentCardId: Int = 0,
    val currentIndex: Int = 0,
    val portionCardIds: List<Int> = emptyList(),
    val completedCardIds: List<Int> = emptyList(),
    val portionCompletionSaved: Boolean = false,
    val isBackVisible: Boolean = false,
    val answer: String = "",
    val answerFeedbackVisible: Boolean = false,
    val excludeMasteredCards: Boolean = false
)
