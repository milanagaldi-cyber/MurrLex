package com.lexaprograms.polishcards

import kotlinx.serialization.Serializable

@Serializable
data class MistakeRecord(
    val answer: String = "",
    val date: String = ""
)

@Serializable
data class Flashcard(
    val id: Int = 0,
    val nativeValue: String = "",
    val correctValue: String = "",
    val wrongAnswers: List<MistakeRecord> = emptyList(),
    val hint: String = "",
    val madeAt: String = "",
    val where: String = "",
    val log: List<String> = emptyList(),
    val mistake: String = "",
    val value: String = "",
    val pl: String = "",
    val ru: String = "",
    val type: String = "card",
    val cardKind: String = "MK",
    val sourceLanguage: String = "",
    val targetLanguage: String = "",
    val stars: Int = 0
) {
    fun nativeText(): String = nativeValue.ifBlank { ru.ifBlank { value } }
    fun correctText(): String = correctValue.ifBlank { pl.ifBlank { value } }
    fun mistakeText(): String = mistake.ifBlank { wrongAnswers.lastOrNull()?.answer.orEmpty() }
    fun valueText(): String = correctText()
    fun hintText(): String = hint
    fun madeAtText(): String = madeAt.ifBlank { wrongAnswers.lastOrNull()?.date ?: "Not specified" }
    fun whereText(): String = where.ifBlank { "Not specified" }
    fun kindCode(): String = cardKind.ifBlank { "MK" }.uppercase()
    fun isLearningKind(): Boolean = kindCode() == "LN" || kindCode() == "TR"
    fun kindLabel(): String = when (kindCode()) {
        "LN" -> "Lesson"
        "TR" -> "Train"
        else -> "Mistakes"
    }
    fun frontLabel(): String = if (isLearningKind()) sourceLanguage.ifBlank { "Source" } else "Mistake made"
    fun backLabel(): String = if (isLearningKind()) targetLanguage.ifBlank { "Target" } else "Make it right"
    fun mistakeRecords(): List<MistakeRecord> {
        return if (wrongAnswers.isNotEmpty()) {
            wrongAnswers
        } else if (mistake.isNotBlank()) {
            listOf(MistakeRecord(answer = mistake, date = madeAt.ifBlank { "Not specified" }))
        } else {
            emptyList()
        }
    }
    fun starCount(): Int = Integer.bitCount(stars).coerceIn(0, 3)
}
