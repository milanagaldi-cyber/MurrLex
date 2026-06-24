package com.lexaprograms.polishcards

import android.content.Context
import android.net.Uri
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

class CardRepository(private val context: Context) {
    private val preferences = context.getSharedPreferences("polish_cards_store", Context.MODE_PRIVATE)
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }


    fun loadCardStartSide(): CardStartSide {
        val raw = preferences.getString(KEY_CARD_START_SIDE, CardStartSide.POLISH.name)
        return runCatching { CardStartSide.valueOf(raw ?: CardStartSide.POLISH.name) }
            .getOrDefault(CardStartSide.POLISH)
    }

    fun saveCardStartSide(side: CardStartSide) {
        preferences.edit().putString(KEY_CARD_START_SIDE, side.name).apply()
    }

    fun loadNotificationIntervalMinutes(): Int {
        return preferences.getInt(KEY_NOTIFICATION_INTERVAL_MINUTES, 30).coerceAtLeast(1)
    }

    fun saveNotificationIntervalMinutes(minutes: Int) {
        preferences.edit().putInt(KEY_NOTIFICATION_INTERVAL_MINUTES, minutes.coerceAtLeast(1)).apply()
    }

    fun loadMaxActiveNotifications(): Int {
        return preferences.getInt(KEY_MAX_ACTIVE_NOTIFICATIONS, 1).coerceAtLeast(1)
    }

    fun saveMaxActiveNotifications(maxNotifications: Int) {
        preferences.edit().putInt(KEY_MAX_ACTIVE_NOTIFICATIONS, maxNotifications.coerceAtLeast(1)).apply()
    }

    fun loadExcludeMasteredCards(): Boolean {
        return preferences.getBoolean(KEY_EXCLUDE_MASTERED_CARDS, false)
    }

    fun saveExcludeMasteredCards(exclude: Boolean) {
        preferences.edit().putBoolean(KEY_EXCLUDE_MASTERED_CARDS, exclude).apply()
    }

    fun loadShowCardLog(): Boolean {
        return preferences.getBoolean(KEY_SHOW_CARD_LOG, false)
    }

    fun saveShowCardLog(show: Boolean) {
        preferences.edit().putBoolean(KEY_SHOW_CARD_LOG, show).apply()
    }

    fun loadSoundEffectsEnabled(): Boolean {
        return preferences.getBoolean(KEY_SOUND_EFFECTS_ENABLED, true)
    }

    fun saveSoundEffectsEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_SOUND_EFFECTS_ENABLED, enabled).apply()
    }

    fun loadVibrationEnabled(): Boolean {
        return preferences.getBoolean(KEY_VIBRATION_ENABLED, true)
    }

    fun saveVibrationEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_VIBRATION_ENABLED, enabled).apply()
    }

    fun loadInterfaceLanguage(defaultLanguage: String): String {
        val raw = preferences.getString(KEY_INTERFACE_LANGUAGE, null) ?: defaultLanguage
        return raw.normalizedInterfaceLanguage()
    }

    fun saveInterfaceLanguage(language: String) {
        preferences.edit().putString(KEY_INTERFACE_LANGUAGE, language.normalizedInterfaceLanguage()).apply()
    }

    fun loadStudySession(lessonId: String): StudySession? {
        val raw = preferences.getString(studySessionKey(lessonId), null) ?: return null
        return runCatching { json.decodeFromString<StudySession>(raw) }
            .getOrNull()
            ?.takeIf { it.lessonId == lessonId }
    }

    fun saveStudySession(session: StudySession) {
        if (session.lessonId.isBlank()) return
        preferences.edit()
            .putString(studySessionKey(session.lessonId), json.encodeToString(session))
            .apply()
    }

    fun clearStudySession(lessonId: String) {
        preferences.edit().remove(studySessionKey(lessonId)).apply()
    }

    fun findRandomNotificationCard(): Pair<Lesson, Flashcard>? {
        val weightedCards = loadLessons(includeHidden = false)
            .flatMap { lesson -> lesson.cards.map { card -> lesson to card } }
            .flatMap { pair ->
                val weight = when (pair.second.starCount()) {
                    0 -> 4
                    1 -> 2
                    2 -> 1
                    else -> 0
                }
                List(weight) { pair }
            }
        return weightedCards.randomOrNull()
    }

    fun findLessonCard(lessonId: String, cardId: Int): Pair<Lesson, Flashcard>? {
        val lesson = loadLessons().firstOrNull { it.id == lessonId } ?: return null
        val card = lesson.cards.firstOrNull { it.id == cardId } ?: return null
        return lesson to card
    }

    fun appendCardLog(lessonId: String, cardId: Int, entry: String): Flashcard? {
        val lesson = loadLessons().firstOrNull { it.id == lessonId } ?: return null
        var updatedCard: Flashcard? = null
        val updatedLesson = lesson.copy(
            cards = lesson.cards.map { card ->
                if (card.id == cardId) {
                    card.copy(log = (card.log + entry).takeLast(100)).also { updatedCard = it }
                } else {
                    card
                }
            },
            editable = true
        )
        saveLesson(updatedLesson)
        return updatedCard
    }
    fun appendWrongAnswer(lessonId: String, cardId: Int, answer: String, date: String): Flashcard? {
        val lesson = loadLessons().firstOrNull { it.id == lessonId } ?: return null
        var updatedCard: Flashcard? = null
        val updatedLesson = lesson.copy(
            cards = lesson.cards.map { card ->
                if (card.id == cardId) {
                    card.copy(
                        mistake = answer,
                        madeAt = date,
                        wrongAnswers = (card.wrongAnswers + MistakeRecord(answer = answer, date = date)).takeLast(100),
                        log = (card.log + "$date - wrong: $answer").takeLast(100)
                    ).also { updatedCard = it }
                } else {
                    card
                }
            },
            editable = true
        )
        saveLesson(updatedLesson)
        return updatedCard
    }

    fun loadLessons(includeHidden: Boolean = false): List<Lesson> {
        val hiddenLessonIds = loadHiddenLessonIds()
        val builtInLessons = loadBuiltInLessons().map { it.copy(hidden = it.id in hiddenLessonIds) }
        val storedLessons = loadStoredLessons().map { it.copy(hidden = it.hidden || it.id in hiddenLessonIds) }
        val stats = loadStats()

        return (builtInLessons + storedLessons)
            .associateBy { it.id }
            .values
            .map { lesson ->
                lesson.copy(
                    title = lesson.title.cleanKindSuffix(),
                    timesCompleted = stats[lesson.id] ?: lesson.timesCompleted
                )
            }
            .filter { includeHidden || !it.hidden }
            .sortBySavedOrder(loadLessonOrder())
    }

    fun saveLesson(lesson: Lesson) {
        val hiddenLessonIds = if (lesson.hidden) {
            (loadHiddenLessonIds() + lesson.id).distinct()
        } else {
            loadHiddenLessonIds().filterNot { it == lesson.id }
        }
        val lessons = loadStoredLessons().filterNot { it.id == lesson.id } + lesson.copy(
            title = lesson.title.cleanKindSuffix(),
            editable = true
        )
        val lessonOrder = (loadLessonOrder() + lesson.id).distinct()
        preferences.edit()
            .putString(KEY_LESSONS, json.encodeToString(lessons))
            .putString(KEY_HIDDEN_LESSONS, json.encodeToString(hiddenLessonIds))
            .putString(KEY_LESSON_ORDER, json.encodeToString(lessonOrder))
            .apply()
    }

    fun saveLessonOrder(lessonIds: List<String>) {
        preferences.edit().putString(KEY_LESSON_ORDER, json.encodeToString(lessonIds)).apply()
    }

    fun deleteLesson(lessonId: String) {
        deleteLessons(setOf(lessonId))
    }

    fun setLessonHidden(lessonId: String, hidden: Boolean) {
        val storedLessons = loadStoredLessons().map { lesson ->
            if (lesson.id == lessonId) lesson.copy(hidden = hidden) else lesson
        }
        val hiddenLessonIds = if (hidden) {
            (loadHiddenLessonIds() + lessonId).distinct()
        } else {
            loadHiddenLessonIds().filterNot { it == lessonId }
        }
        preferences.edit()
            .putString(KEY_LESSONS, json.encodeToString(storedLessons))
            .putString(KEY_HIDDEN_LESSONS, json.encodeToString(hiddenLessonIds))
            .apply()
    }

    fun deleteLessons(lessonIds: Set<String>) {
        val lessons = loadStoredLessons().filterNot { it.id in lessonIds }
        val stats = loadStats().toMutableMap().also { map -> lessonIds.forEach { map.remove(it) } }
        val hiddenLessonIds = (loadHiddenLessonIds() + lessonIds).distinct()
        val lessonOrder = loadLessonOrder().filterNot { it in lessonIds }
        preferences.edit()
            .putString(KEY_LESSONS, json.encodeToString(lessons))
            .putString(KEY_STATS, json.encodeToString(stats))
            .putString(KEY_HIDDEN_LESSONS, json.encodeToString(hiddenLessonIds))
            .putString(KEY_LESSON_ORDER, json.encodeToString(lessonOrder))
            .also { editor -> lessonIds.forEach { editor.remove(studySessionKey(it)) } }
            .apply()
    }

    fun incrementCompletedCount(lessonId: String) {
        val stats = loadStats().toMutableMap()
        stats[lessonId] = (stats[lessonId] ?: 0) + 1
        preferences.edit().putString(KEY_STATS, json.encodeToString(stats)).apply()
    }

    fun importLesson(uri: Uri): Lesson {
        val content = context.contentResolver.openInputStream(uri)?.bufferedReader().use { reader ->
            requireNotNull(reader) { "Unable to open selected file" }.readText()
        }
        return parseLesson(content, fallbackTitle = uri.lastPathSegment ?: "Imported lesson")
    }

    private fun loadBuiltInLessons(): List<Lesson> {
        val files = context.assets.list("lessons").orEmpty().filter { it.endsWith(".json", ignoreCase = true) }
        return files.mapNotNull { fileName ->
            runCatching {
                val content = context.assets.open("lessons/$fileName").bufferedReader().use { it.readText() }
                parseLesson(content, fallbackTitle = fileName.substringBeforeLast('.')).copy(editable = false)
            }.getOrNull()
        }
    }

    private fun loadStoredLessons(): List<Lesson> {
        val raw = preferences.getString(KEY_LESSONS, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<Lesson>>(raw) }.getOrDefault(emptyList())
    }

    private fun loadStats(): Map<String, Int> {
        val raw = preferences.getString(KEY_STATS, null) ?: return emptyMap()
        return runCatching { json.decodeFromString<Map<String, Int>>(raw) }.getOrDefault(emptyMap())
    }

    private fun loadHiddenLessonIds(): List<String> {
        val raw = preferences.getString(KEY_HIDDEN_LESSONS, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<String>>(raw) }.getOrDefault(emptyList())
    }

    private fun loadLessonOrder(): List<String> {
        val raw = preferences.getString(KEY_LESSON_ORDER, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<String>>(raw) }.getOrDefault(emptyList())
    }

    private fun Collection<Lesson>.sortBySavedOrder(order: List<String>): List<Lesson> {
        val orderIndex = order.withIndex().associate { it.value to it.index }
        return sortedWith(
            compareBy<Lesson> { orderIndex[it.id] ?: Int.MAX_VALUE }
                .thenBy { it.title.lowercase() }
        )
    }

    private fun parseLesson(content: String, fallbackTitle: String): Lesson {
        val element = json.parseToJsonElement(content)
        return when (element) {
            is JsonArray -> {
                val cards = json.decodeFromJsonElement<List<Flashcard>>(element)
                Lesson(
                    id = newLessonId(),
                    title = fallbackTitle.cleanTitle().cleanKindSuffix(),
                    cards = cards.reindexCards()
                )
            }
            is JsonObject -> {
                val lesson = json.decodeFromJsonElement<Lesson>(element)
                val lessonInfo = lesson.lessonInfo.ifBlank {
                    element["info"]?.jsonPrimitive?.contentOrNull.orEmpty()
                }
                lesson.copy(
                    id = lesson.id.ifBlank { newLessonId() },
                    title = lesson.title.ifBlank { fallbackTitle.cleanTitle() }.cleanKindSuffix(),
                    lessonInfo = lessonInfo,
                    cards = lesson.cards.reindexCards()
                )
            }
            else -> throw SerializationException("Lesson JSON must be an object or an array of cards")
        }
    }

    private fun List<Flashcard>.reindexCards(): List<Flashcard> {
        return mapIndexed { index, card -> card.copy(id = index + 1) }
    }

    private fun String.cleanTitle(): String {
        return replace('_', ' ').replace('-', ' ').trim().ifBlank { "Untitled lesson" }
    }

    private fun String.cleanKindSuffix(): String {
        return replace(Regex("\\s*-\\s*(LN|MK)(\\b.*)?$", RegexOption.IGNORE_CASE), "").trim()
    }

    private fun String.normalizedInterfaceLanguage(): String {
        return when (lowercase()) {
            "de" -> "de"
            "by", "be" -> "be"
            "es" -> "es"
            "ua", "uk" -> "uk"
            "ru" -> "ru"
            "pl" -> "pl"
            else -> "en"
        }
    }

    private fun newLessonId(): String = "lesson_${UUID.randomUUID()}"

    private fun studySessionKey(lessonId: String): String = "$KEY_STUDY_SESSION_PREFIX$lessonId"

    companion object {
        private const val KEY_LESSONS = "lessons"
        private const val KEY_STATS = "stats"
        private const val KEY_HIDDEN_LESSONS = "hidden_lessons"
        private const val KEY_LESSON_ORDER = "lesson_order"
        private const val KEY_CARD_START_SIDE = "card_start_side"
        private const val KEY_NOTIFICATION_INTERVAL_MINUTES = "notification_interval_minutes"
        private const val KEY_MAX_ACTIVE_NOTIFICATIONS = "max_active_notifications"
        private const val KEY_EXCLUDE_MASTERED_CARDS = "exclude_mastered_cards"
        private const val KEY_SHOW_CARD_LOG = "show_card_log"
        private const val KEY_SOUND_EFFECTS_ENABLED = "sound_effects_enabled"
        private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        private const val KEY_INTERFACE_LANGUAGE = "interface_language"
        private const val KEY_STUDY_SESSION_PREFIX = "study_session_"
    }
}








