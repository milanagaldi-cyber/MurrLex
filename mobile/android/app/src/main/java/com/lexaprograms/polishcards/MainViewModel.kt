package com.lexaprograms.polishcards

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class AppScreen {
    CATALOG,
    STUDY,
    EDITOR,
    SETTINGS
}

enum class CardStartSide {
    POLISH,
    TRANSLATION
}

enum class DisplayTextSize {
    SMALL,
    MEDIUM,
    LARGE
}

enum class ControlSize {
    SMALL,
    MEDIUM
}

data class LessonDraft(
    val id: String = "",
    val title: String = "",
    val lessonInfo: String = "",
    val cards: List<Flashcard> = emptyList(),
    val timesCompleted: Int = 0,
    val editable: Boolean = true
)

data class CardDraft(
    val nativeValue: String = "",
    val correctValue: String = "",
    val hint: String = "",
    val madeAt: String = "",
    val where: String = "",
    val type: String = "card",
    val editingCardId: Int? = null
)

data class StudyUiState(
    val screen: AppScreen = AppScreen.CATALOG,
    val lessons: List<Lesson> = emptyList(),
    val selectedLessonIds: Set<String> = emptySet(),
    val showHiddenLessons: Boolean = false,
    val selectedLesson: Lesson? = null,
    val currentPortion: List<Flashcard> = emptyList(),
    val currentIndex: Int = 0,
    val cardTransitionDirection: Int = 1,
    val completedCardIds: Set<Int> = emptySet(),
    val portionCompletionSaved: Boolean = false,
    val mode: StudyMode = StudyMode.ORIGINAL,
    val cardStartSide: CardStartSide = CardStartSide.POLISH,
    val excludeMasteredCards: Boolean = false,
    val hideCompletedCards: Boolean = false,
    val showCardLog: Boolean = false,
    val displayTextSize: DisplayTextSize = DisplayTextSize.MEDIUM,
    val controlSize: ControlSize = ControlSize.MEDIUM,
    val interfaceLanguage: String = "en",
    val quickVocabularySourceLanguage: String = "Polish",
    val quickVocabularyTargetLanguage: String = "Russian",
    val useLocalTranslation: Boolean = false,
    val translationApiUrl: String = "",
    val translationApiToken: String = "",
    val soundEffectsEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val notificationIntervalMinutes: Int = 30,
    val notificationIntervalDraft: String = "30",
    val notificationMaxDraft: String = "1",
    val isBackVisible: Boolean = false,
    val answer: String = "",
    val answerFeedbackVisible: Boolean = false,
    val message: String? = null,
    val editorLesson: LessonDraft? = null,
    val cardDraft: CardDraft = CardDraft(),
    val returnToStudyAfterEdit: Boolean = false,
    val returnStudyCardId: Int? = null,
    val openedFromNotification: Boolean = false,
    val closeAfterNotificationAnswer: Boolean = false
) {
    val currentCard: Flashcard? = currentPortion.getOrNull(currentIndex.coerceIn(0, (currentPortion.size - 1).coerceAtLeast(0)))
    private val currentPortionIds: Set<Int> = currentPortion.map { it.id }.toSet()
    private val visibleCompletedIds: Set<Int> = completedCardIds.intersect(currentPortionIds)
    val portionSize: Int = currentPortion.size
    val completedCount: Int = visibleCompletedIds.size
    val remainingCount: Int = (currentPortion.size - visibleCompletedIds.size).coerceAtLeast(0)
    val isPortionFinished: Boolean = selectedLesson != null && currentPortion.isNotEmpty() && visibleCompletedIds.size == currentPortion.size
    val studyEmptyMessage: String? = when {
        selectedLesson == null || currentPortion.isNotEmpty() -> null
        selectedLesson.cards.isEmpty() -> "Nothing to show yet."
        excludeMasteredCards || hideCompletedCards -> "All done. Every visible card already has three stars."
        else -> "Nothing to show yet."
    }
    val currentPositionLabel: String = if (currentPortion.isEmpty()) "0 / 0" else "${currentIndex + 1} / ${currentPortion.size}"
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CardRepository(application.applicationContext)
    private val _uiState = MutableStateFlow(StudyUiState())
    val uiState: StateFlow<StudyUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = _uiState.value.copy(
            cardStartSide = repository.loadCardStartSide(),
            excludeMasteredCards = false,
            hideCompletedCards = false,
            showCardLog = repository.loadShowCardLog(),
            displayTextSize = repository.loadDisplayTextSize(),
            controlSize = repository.loadControlSize(),
            interfaceLanguage = repository.loadInterfaceLanguage(detectSystemInterfaceLanguage()),
            quickVocabularySourceLanguage = repository.loadQuickVocabularySourceLanguage(),
            quickVocabularyTargetLanguage = repository.loadQuickVocabularyTargetLanguage(),
            useLocalTranslation = repository.loadUseLocalTranslation(),
            translationApiUrl = repository.loadTranslationApiUrl(),
            translationApiToken = repository.loadTranslationApiToken(),
            soundEffectsEnabled = repository.loadSoundEffectsEnabled(),
            vibrationEnabled = repository.loadVibrationEnabled(),
            notificationIntervalMinutes = repository.loadNotificationIntervalMinutes(),
            notificationIntervalDraft = repository.loadNotificationIntervalMinutes().toString(),
            notificationMaxDraft = repository.loadMaxActiveNotifications().toString()
        )
        refreshLessons()
    }

    fun refreshLessons() {
        val lessons = repository.loadLessons(_uiState.value.showHiddenLessons)
        _uiState.value = _uiState.value.copy(
            lessons = lessons,
            selectedLessonIds = _uiState.value.selectedLessonIds.filter { id -> lessons.any { it.id == id } }.toSet()
        )
    }

    fun toggleShowHiddenLessons() {
        val next = !_uiState.value.showHiddenLessons
        val lessons = repository.loadLessons(next)
        _uiState.value = _uiState.value.copy(
            showHiddenLessons = next,
            lessons = lessons,
            selectedLessonIds = emptySet(),
            message = if (next) "Hidden lessons are visible" else "Hidden lessons are hidden"
        )
    }

    fun updateNotificationIntervalDraft(value: String) {
        _uiState.value = _uiState.value.copy(notificationIntervalDraft = value.filter { it.isDigit() })
    }

    fun updateNotificationMaxDraft(value: String) {
        _uiState.value = _uiState.value.copy(notificationMaxDraft = value.filter { it.isDigit() })
    }

    fun saveNotificationMax() {
        val maxNotifications = _uiState.value.notificationMaxDraft.toIntOrNull()?.coerceAtLeast(1) ?: 1
        repository.saveMaxActiveNotifications(maxNotifications)
        _uiState.value = _uiState.value.copy(
            notificationMaxDraft = maxNotifications.toString(),
            message = "Maximum active notifications: $maxNotifications"
        )
    }

    fun saveNotificationInterval(context: android.content.Context) {
        val minutes = _uiState.value.notificationIntervalDraft.toIntOrNull()?.coerceAtLeast(1) ?: 30
        repository.saveNotificationIntervalMinutes(minutes)
        CardNotificationWorker.schedule(context, minutes)
        _uiState.value = _uiState.value.copy(
            notificationIntervalMinutes = minutes,
            notificationIntervalDraft = minutes.toString(),
            message = "Notifications every $minutes minutes"
        )
    }

    fun openCardFromNotification(lessonId: String?, cardId: Int) {
        if (lessonId.isNullOrBlank() || cardId <= 0) return
        val (lesson, card) = repository.findLessonCard(lessonId, cardId) ?: return
        _uiState.value = _uiState.value.copy(
            lessons = repository.loadLessons(_uiState.value.showHiddenLessons),
            selectedLesson = lesson,
            currentPortion = listOf(card),
            currentIndex = 0,
            completedCardIds = emptySet(),
            portionCompletionSaved = false,
            isBackVisible = true,
            answer = "",
            screen = AppScreen.STUDY,
            message = null,
            openedFromNotification = true,
            closeAfterNotificationAnswer = false
        )
    }
    fun openSettings() {
        _uiState.value = _uiState.value.copy(screen = AppScreen.SETTINGS)
    }

    fun setCardStartSide(side: CardStartSide) {
        repository.saveCardStartSide(side)
        _uiState.value = _uiState.value.copy(
            cardStartSide = side,
            isBackVisible = side == CardStartSide.TRANSLATION,
            message = "Settings saved"
        )
    }

    fun setExcludeMasteredCards(exclude: Boolean) {
        repository.saveExcludeMasteredCards(exclude)
        val state = _uiState.value
        val lesson = state.selectedLesson
        val studyCards = if (lesson != null) {
            filterStudyCards(lesson.cards, exclude, state.hideCompletedCards, state.completedCardIds)
        } else {
            state.currentPortion
        }
        val rebuiltPortion = if (lesson != null) {
            orderStudyCards(studyCards, state.mode, shuffleRandom = false)
        } else {
            studyCards
        }
        val validIds = rebuiltPortion.map { it.id }.toSet()
        _uiState.value = state.copy(
            excludeMasteredCards = exclude,
            currentPortion = rebuiltPortion,
            currentIndex = state.currentIndex.coerceAtMost((rebuiltPortion.size - 1).coerceAtLeast(0)),
            completedCardIds = state.completedCardIds.intersect(validIds),
            message = if (exclude) "Three-star cards hidden" else "Showing all cards"
        )
        saveCurrentStudySession()
    }

    fun setShowAllCards(showAll: Boolean) {
        setExcludeMasteredCards(!showAll)
    }

    fun setHideCompletedCards(hide: Boolean) {
        val state = _uiState.value
        val lesson = state.selectedLesson
        val rebuiltPortion = if (lesson != null) {
            orderStudyCards(
                filterStudyCards(lesson.cards, state.excludeMasteredCards, hide, state.completedCardIds),
                state.mode,
                shuffleRandom = false
            )
        } else {
            state.currentPortion
        }
        _uiState.value = state.copy(
            hideCompletedCards = hide,
            currentPortion = rebuiltPortion,
            currentIndex = state.currentIndex.coerceAtMost((rebuiltPortion.size - 1).coerceAtLeast(0)),
            message = if (hide) "Done hidden" else "Done visible"
        )
        saveCurrentStudySession()
    }

    fun setDisplayTextSize(size: DisplayTextSize) {
        repository.saveDisplayTextSize(size)
        _uiState.value = _uiState.value.copy(
            displayTextSize = size,
            message = "Text size: ${size.name.lowercase().replaceFirstChar { it.titlecase(Locale.ROOT) }}"
        )
    }

    fun setControlSize(size: ControlSize) {
        repository.saveControlSize(size)
        _uiState.value = _uiState.value.copy(
            controlSize = size,
            message = "Controls: ${size.name.lowercase().replaceFirstChar { it.titlecase(Locale.ROOT) }}"
        )
    }

    fun setShowCardLog(show: Boolean) {
        repository.saveShowCardLog(show)
        _uiState.value = _uiState.value.copy(
            showCardLog = show,
            message = if (show) "Card log icon shown" else "Card log icon hidden"
        )
    }

    fun setInterfaceLanguage(language: String) {
        val normalized = normalizeInterfaceLanguage(language)
        repository.saveInterfaceLanguage(normalized)
        _uiState.value = _uiState.value.copy(
            interfaceLanguage = normalized,
            message = "Interface language: ${normalized.uppercase(Locale.ROOT)}"
        )
    }

    fun setQuickVocabularySourceLanguage(language: String) {
        val cleaned = language.trim().ifBlank { "Polish" }
        repository.saveQuickVocabularySourceLanguage(cleaned)
        _uiState.value = _uiState.value.copy(
            quickVocabularySourceLanguage = cleaned,
            message = "Quick vocabulary source: $cleaned"
        )
    }

    fun setQuickVocabularyTargetLanguage(language: String) {
        val cleaned = language.trim().ifBlank { "Russian" }
        repository.saveQuickVocabularyTargetLanguage(cleaned)
        _uiState.value = _uiState.value.copy(
            quickVocabularyTargetLanguage = cleaned,
            message = "Quick vocabulary target: $cleaned"
        )
    }



    fun setUseLocalTranslation(enabled: Boolean) {
        repository.saveUseLocalTranslation(enabled)
        _uiState.value = _uiState.value.copy(
            useLocalTranslation = enabled,
            message = if (enabled) "Local translation unavailable in this build" else "Server translation preferred"
        )
    }
    fun setTranslationApiUrl(url: String) {
        repository.saveTranslationApiUrl(url)
        _uiState.value = _uiState.value.copy(translationApiUrl = url.trim())
    }

    fun setTranslationApiToken(token: String) {
        repository.saveTranslationApiToken(token)
        _uiState.value = _uiState.value.copy(translationApiToken = token.trim())
    }
    fun setSoundEffectsEnabled(enabled: Boolean) {
        repository.saveSoundEffectsEnabled(enabled)
        _uiState.value = _uiState.value.copy(
            soundEffectsEnabled = enabled,
            message = if (enabled) "Sound effects enabled" else "Sound effects disabled"
        )
    }

    fun setVibrationEnabled(enabled: Boolean) {
        repository.saveVibrationEnabled(enabled)
        _uiState.value = _uiState.value.copy(
            vibrationEnabled = enabled,
            message = if (enabled) "Vibration enabled" else "Vibration disabled"
        )
    }
    private fun defaultBackVisible(): Boolean {
        return _uiState.value.cardStartSide == CardStartSide.TRANSLATION
    }

    private fun orderStudyCards(
        cards: List<Flashcard>,
        mode: StudyMode,
        shuffleRandom: Boolean
    ): List<Flashcard> {
        return when (mode) {
            StudyMode.ORIGINAL -> cards
            StudyMode.ALPHABETICAL -> cards.sortedBy { it.nativeText().lowercase(Locale.getDefault()) }
            StudyMode.RANDOM -> if (shuffleRandom) cards.shuffled() else cards
        }
    }

    private fun StudyMode.displayLabel(): String {
        return when (this) {
            StudyMode.ORIGINAL -> "Original"
            StudyMode.ALPHABETICAL -> "Alphabetical"
            StudyMode.RANDOM -> "Random"
        }
    }

    private fun restoreStudySession(lesson: Lesson): Boolean {
        val session = repository.loadStudySession(lesson.id) ?: return false
        val mode = runCatching { StudyMode.valueOf(session.mode) }
            .getOrDefault(StudyMode.ORIGINAL)
        val studyCards = filterStudyCards(lesson.cards, session.excludeMasteredCards, false, session.completedCardIds.toSet())
        val cardsById = studyCards.associateBy { it.id }
        val orderedIds = session.portionCardIds.toSet()
        val savedOrderCards = session.portionCardIds.mapNotNull { cardsById[it] }
        val newCards = studyCards.filterNot { it.id in orderedIds }
        val restoredPortion = (savedOrderCards + newCards).ifEmpty {
            orderStudyCards(studyCards, mode, shuffleRandom = false)
        }
        val validIds = restoredPortion.map { it.id }.toSet()
        val restoredIndex = session.currentCardId
            .takeIf { it > 0 }
            ?.let { cardId -> restoredPortion.indexOfFirst { it.id == cardId } }
            ?.takeIf { it >= 0 }
            ?: session.currentIndex

        _uiState.value = _uiState.value.copy(
            selectedLesson = lesson,
            selectedLessonIds = emptySet(),
            screen = AppScreen.STUDY,
            mode = mode,
            excludeMasteredCards = session.excludeMasteredCards,
            hideCompletedCards = false,
            currentPortion = restoredPortion,
            currentIndex = restoredIndex.coerceIn(0, (restoredPortion.size - 1).coerceAtLeast(0)),
            completedCardIds = session.completedCardIds.filter { it in validIds }.toSet(),
            portionCompletionSaved = session.portionCompletionSaved,
            isBackVisible = session.isBackVisible,
            answer = session.answer,
            answerFeedbackVisible = session.answerFeedbackVisible,
            message = null
        )
        return true
    }

    private fun saveCurrentStudySession() {
        val state = _uiState.value
        val lesson = state.selectedLesson ?: return
        if (state.openedFromNotification) return
        repository.saveStudySession(
            StudySession(
                lessonId = lesson.id,
                mode = state.mode.name,
                currentCardId = state.currentCard?.id ?: 0,
                currentIndex = state.currentIndex,
                portionCardIds = state.currentPortion.map { it.id },
                completedCardIds = state.completedCardIds.toList(),
                portionCompletionSaved = state.portionCompletionSaved,
                isBackVisible = state.isBackVisible,
                answer = state.answer,
                answerFeedbackVisible = state.answerFeedbackVisible,
                excludeMasteredCards = state.excludeMasteredCards
            )
        )
    }

    private fun detectSystemInterfaceLanguage(): String {
        val language = getApplication<Application>()
            .resources
            .configuration
            .locales[0]
            ?.language
            .orEmpty()
        return normalizeInterfaceLanguage(language)
    }

    private fun normalizeInterfaceLanguage(language: String): String {
        return when (language.lowercase(Locale.ROOT)) {
            "de" -> "de"
            "by", "be" -> "be"
            "es" -> "es"
            "ua", "uk" -> "uk"
            "ru" -> "ru"
            "pl" -> "pl"
            else -> "en"
        }
    }
    fun openCatalog() {
        saveCurrentStudySession()
        refreshLessons()
        _uiState.value = _uiState.value.copy(
            screen = AppScreen.CATALOG,
            selectedLesson = null,
            currentPortion = emptyList(),
            currentIndex = 0,
            completedCardIds = emptySet(),
            portionCompletionSaved = false,
            isBackVisible = false,
            answer = "",
            editorLesson = null,
            cardDraft = CardDraft()
        )
    }

    fun openLesson(lesson: Lesson) {
        _uiState.value = _uiState.value.copy(
            selectedLesson = lesson,
            selectedLessonIds = emptySet(),
            screen = AppScreen.STUDY
        )
        if (!restoreStudySession(lesson)) startNewPortion()
    }

    fun openNextVisibleLesson() {
        saveCurrentStudySession()
        val currentLessonId = _uiState.value.selectedLesson?.id ?: return
        val visibleLessons = repository.loadLessons(false).filterNot { it.hidden }
        val currentIndex = visibleLessons.indexOfFirst { it.id == currentLessonId }
        val nextLesson = visibleLessons.drop(currentIndex + 1).firstOrNull()
        if (currentIndex < 0 || nextLesson == null) {
            _uiState.value = _uiState.value.copy(message = "No next lesson")
            return
        }
        _uiState.value = _uiState.value.copy(
            lessons = repository.loadLessons(_uiState.value.showHiddenLessons),
            selectedLesson = nextLesson,
            selectedLessonIds = emptySet(),
            screen = AppScreen.STUDY,
            completedCardIds = emptySet(),
            portionCompletionSaved = false,
            cardTransitionDirection = 1,
            answer = "",
            message = null
        )
        if (!restoreStudySession(nextLesson)) startNewPortion()
    }

    fun toggleLessonSelection(lessonId: String) {
        val selected = _uiState.value.selectedLessonIds
        _uiState.value = _uiState.value.copy(
            selectedLessonIds = if (lessonId in selected) selected - lessonId else selected + lessonId
        )
    }

    fun clearLessonSelection() {
        _uiState.value = _uiState.value.copy(selectedLessonIds = emptySet())
    }

    fun deleteSelectedLessons() {
        val selectedIds = _uiState.value.selectedLessonIds
        if (selectedIds.isEmpty()) return
        repository.deleteLessons(selectedIds)
        _uiState.value = _uiState.value.copy(
            lessons = repository.loadLessons(_uiState.value.showHiddenLessons),
            selectedLessonIds = emptySet(),
            message = "Deleted ${selectedIds.size} lessons"
        )
    }

    fun setLessonHidden(lesson: Lesson, hidden: Boolean) {
        repository.setLessonHidden(lesson.id, hidden)
        _uiState.value = _uiState.value.copy(
            lessons = repository.loadLessons(_uiState.value.showHiddenLessons),
            selectedLessonIds = emptySet(),
            message = if (hidden) "Lesson hidden" else "Lesson visible"
        )
    }

    fun copyLesson(lesson: Lesson) {
        val copy = lesson.copy(
            id = newLessonId(),
            title = "${lesson.title} copy",
            timesCompleted = 0,
            editable = true
        )
        repository.saveLesson(copy)
        _uiState.value = _uiState.value.copy(
            lessons = repository.loadLessons(_uiState.value.showHiddenLessons),
            selectedLessonIds = emptySet(),
            message = "Lesson copied"
        )
    }

    fun moveLesson(draggedLessonId: String, targetLessonId: String) {
        if (draggedLessonId == targetLessonId) return
        val lessons = _uiState.value.lessons.toMutableList()
        val fromIndex = lessons.indexOfFirst { it.id == draggedLessonId }
        val toIndex = lessons.indexOfFirst { it.id == targetLessonId }
        if (fromIndex !in lessons.indices || toIndex !in lessons.indices) return

        val moved = lessons.removeAt(fromIndex)
        lessons.add(toIndex, moved)
        _uiState.value = _uiState.value.copy(lessons = lessons)
    }

    fun saveLessonOrder() {
        repository.saveLessonOrder(_uiState.value.lessons.map { it.id })
    }
    fun setMode(mode: StudyMode) {
        val state = _uiState.value
        val lesson = state.selectedLesson
        if (lesson == null) {
            _uiState.value = state.copy(mode = mode)
            return
        }

        val currentCardId = state.currentCard?.id
        val studyCards = filterStudyCards(lesson.cards, state.excludeMasteredCards, state.hideCompletedCards, state.completedCardIds)
        val cards = orderStudyCards(studyCards, mode, shuffleRandom = mode == StudyMode.RANDOM)
        val nextIndex = currentCardId
            ?.let { id -> cards.indexOfFirst { it.id == id } }
            ?.takeIf { it >= 0 }
            ?: 0

        _uiState.value = state.copy(
            mode = mode,
            currentPortion = cards,
            currentIndex = nextIndex.coerceAtMost((cards.size - 1).coerceAtLeast(0)),
            answer = "",
            answerFeedbackVisible = false,
            isBackVisible = defaultBackVisible(),
            message = mode.displayLabel()
        )
        saveCurrentStudySession()
    }

    fun startNewPortion() {
        val state = _uiState.value
        val lesson = state.selectedLesson ?: return
        repository.clearStudySession(lesson.id)
        val studyCards = filterStudyCards(lesson.cards, state.excludeMasteredCards, state.hideCompletedCards, state.completedCardIds)
        val cards = orderStudyCards(studyCards, state.mode, shuffleRandom = true)

        _uiState.value = state.copy(
            currentPortion = cards,
            currentIndex = 0,
            completedCardIds = emptySet(),
            portionCompletionSaved = false,
            isBackVisible = defaultBackVisible(),
            answer = "",
            answerFeedbackVisible = false,
            message = null
        )
    }


    fun previousCard() {
        val state = _uiState.value
        if (state.currentPortion.isEmpty()) return
        _uiState.value = state.copy(
            currentIndex = (state.currentIndex - 1).coerceAtLeast(0),
            cardTransitionDirection = -1,
            isBackVisible = defaultBackVisible(),
            answer = "",
            answerFeedbackVisible = false,
            message = null
        )
        saveCurrentStudySession()
    }

    fun nextCard() {
        val state = _uiState.value
        if (state.currentPortion.isEmpty()) return
        _uiState.value = state.copy(
            currentIndex = (state.currentIndex + 1).coerceAtMost(state.currentPortion.lastIndex),
            cardTransitionDirection = 1,
            isBackVisible = defaultBackVisible(),
            answer = "",
            answerFeedbackVisible = false,
            message = null
        )
        saveCurrentStudySession()
    }

    fun goToFirstCard() {
        val state = _uiState.value
        if (state.currentPortion.isEmpty()) return
        _uiState.value = state.copy(
            currentIndex = 0,
            cardTransitionDirection = -1,
            isBackVisible = defaultBackVisible(),
            answer = "",
            answerFeedbackVisible = false,
            message = null
        )
        saveCurrentStudySession()
    }

    fun goToFirstRemainingCard() {
        val state = _uiState.value
        if (state.currentPortion.isEmpty()) return
        val lastCompletedIndex = state.currentPortion.indexOfLast { card -> card.id in state.completedCardIds }
        val targetIndex = state.currentPortion
            .drop(lastCompletedIndex + 1)
            .indexOfFirst { card -> card.id !in state.completedCardIds }
            .takeIf { it >= 0 }
            ?.let { it + lastCompletedIndex + 1 }
            ?: state.currentPortion.indexOfFirst { card -> card.id !in state.completedCardIds }
        if (targetIndex < 0) return
        _uiState.value = state.copy(
            currentIndex = targetIndex,
            cardTransitionDirection = if (targetIndex >= state.currentIndex) 1 else -1,
            isBackVisible = defaultBackVisible(),
            answer = "",
            answerFeedbackVisible = false,
            message = null
        )
        saveCurrentStudySession()
    }

    fun goToLastCompletedCard() {
        val state = _uiState.value
        if (state.currentPortion.isEmpty()) return
        val targetIndex = state.currentPortion.indexOfLast { card -> card.id in state.completedCardIds }
        if (targetIndex < 0) return
        _uiState.value = state.copy(
            currentIndex = targetIndex,
            cardTransitionDirection = if (targetIndex >= state.currentIndex) 1 else -1,
            isBackVisible = defaultBackVisible(),
            answer = "",
            answerFeedbackVisible = false,
            message = null
        )
        saveCurrentStudySession()
    }

    fun toggleCard() {
        _uiState.value = _uiState.value.copy(
            isBackVisible = !_uiState.value.isBackVisible,
            message = null
        )
        saveCurrentStudySession()
    }

    fun updateAnswer(answer: String) {
        _uiState.value = _uiState.value.copy(answer = answer, answerFeedbackVisible = false, message = null)
        saveCurrentStudySession()
    }
    fun dismissAnswerFeedback() {
        val state = _uiState.value
        if (!state.answerFeedbackVisible) return
        _uiState.value = state.copy(answerFeedbackVisible = false)
        saveCurrentStudySession()
    }

    fun previewAnswer() {
        val state = _uiState.value
        val card = state.currentCard ?: return
        if (state.answer.isBlank()) {
            _uiState.value = state.copy(answerFeedbackVisible = false, message = "Enter answer")
        } else if (normalize(state.answer) == normalize(card.correctText())) {
            _uiState.value = state.copy(answerFeedbackVisible = false, message = "Correct")
        } else {
            _uiState.value = state.copy(
                answerFeedbackVisible = true,
                message = if (wrongLetterRatio(state.answer, card.correctText()) > 0.5) "Wrong" else null
            )
        }
        saveCurrentStudySession()
    }

    fun checkAnswer() {
        val state = _uiState.value
        val card = state.currentCard ?: return

        if (normalize(state.answer) == normalize(card.correctText())) {
            recordCardWork(card.id, "correct")
            addStarForCorrectTypedAnswer(card.id)
            completeCurrentCard(message = "Correct")
        } else {
            recordWrongAnswer(card.id, state.answer)
            _uiState.value = _uiState.value.copy(answerFeedbackVisible = true, message = "Try again")
            saveCurrentStudySession()
        }
    }

    fun acceptCurrentCard() {
        val state = _uiState.value
        if (state.answer.isBlank()) {
            state.currentCard?.let { recordCardWork(it.id, "correct without typed answer") }
            completeCurrentCard(message = null)
        } else {
            checkAnswer()
        }
    }

    fun skipCurrentCard() {
        _uiState.value.currentCard?.let { recordCardWork(it.id, "skipped") }
        completeCurrentCard(message = "Skipped")
    }

    private fun completeCurrentCard(message: String?) {
        val state = _uiState.value
        val currentCard = state.currentCard ?: return
        val newCompletion = currentCard.id !in state.completedCardIds
        val nextCompletedCardIds = state.completedCardIds + currentCard.id
        val nextIndex = if (state.currentIndex < state.currentPortion.lastIndex) {
            state.currentIndex + 1
        } else {
            state.currentIndex
        }
        val selectedLesson = state.selectedLesson
        val currentPortionIds = state.currentPortion.map { it.id }.toSet()
        val portionComplete = state.currentPortion.isNotEmpty() &&
            nextCompletedCardIds.intersect(currentPortionIds).size == state.currentPortion.size
        val shouldSavePortionCompletion = portionComplete && !state.portionCompletionSaved && selectedLesson != null
        val shouldCloseAfterNotificationAnswer = state.openedFromNotification &&
            newCompletion &&
            (message == null || message == "Correct")

        if (shouldSavePortionCompletion) {
            repository.incrementCompletedCount(selectedLesson.id)
            val lessons = repository.loadLessons(state.showHiddenLessons)
            _uiState.value = state.copy(
                lessons = lessons,
                selectedLesson = lessons.firstOrNull { it.id == selectedLesson.id } ?: selectedLesson,
                currentIndex = nextIndex,
                cardTransitionDirection = 1,
                completedCardIds = nextCompletedCardIds,
                portionCompletionSaved = true,
                isBackVisible = defaultBackVisible(),
                answer = "",
                answerFeedbackVisible = false,
                message = message,
                closeAfterNotificationAnswer = shouldCloseAfterNotificationAnswer
            )
        } else {
            _uiState.value = state.copy(
                currentIndex = nextIndex,
                cardTransitionDirection = 1,
                completedCardIds = nextCompletedCardIds,
                portionCompletionSaved = state.portionCompletionSaved || portionComplete,
                isBackVisible = defaultBackVisible(),
                answer = "",
                answerFeedbackVisible = false,
                message = if (newCompletion) message else message,
                closeAfterNotificationAnswer = shouldCloseAfterNotificationAnswer
            )
        }
        saveCurrentStudySession()
    }
    fun showMessage(message: String) {
        _uiState.value = _uiState.value.copy(message = message)
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun importLessons(uris: List<Uri>) {
        if (uris.isEmpty()) return
        runCatching {
            var existingIds = repository.loadLessons(true).map { it.id }.toSet()
            var importedCount = 0
            uris.forEach { uri ->
                val imported = repository.importLesson(uri)
                val lesson = if (imported.id in existingIds) imported.copy(id = newLessonId()) else imported
                repository.saveLesson(lesson.copy(editable = true))
                existingIds = existingIds + lesson.id
                importedCount += 1
            }
            importedCount to repository.loadLessons(_uiState.value.showHiddenLessons)
        }.onSuccess { (importedCount, lessons) ->
            _uiState.value = _uiState.value.copy(
                lessons = lessons,
                selectedLessonIds = emptySet(),
                screen = AppScreen.CATALOG,
                message = "Imported $importedCount lessons"
            )
        }.onFailure { error ->
            _uiState.value = _uiState.value.copy(message = error.message ?: "Import failed")
        }
    }

    fun createLesson() {
        _uiState.value = _uiState.value.copy(
            screen = AppScreen.EDITOR,
            selectedLessonIds = emptySet(),
            editorLesson = LessonDraft(
                id = newLessonId(),
                title = "New lesson",
                lessonInfo = "",
                cards = emptyList(),
                editable = true
            ),
            cardDraft = CardDraft()
        )
    }

    fun editLesson(lesson: Lesson) {
        _uiState.value = _uiState.value.copy(
            screen = AppScreen.EDITOR,
            selectedLessonIds = emptySet(),
            editorLesson = LessonDraft(
                id = lesson.id,
                title = lesson.title,
                lessonInfo = lesson.lessonInfo,
                cards = lesson.cards,
                timesCompleted = lesson.timesCompleted,
                editable = true
            ),
            cardDraft = CardDraft()
        )
    }

    fun editCurrentStudyCard() {
        val state = _uiState.value
        val card = state.currentCard ?: return
        _uiState.value = state.copy(
            cardDraft = CardDraft(
                nativeValue = card.editorFrontDraftText(),
                correctValue = card.editorCorrectDraftText(),
                hint = card.hint,
                madeAt = card.madeAt,
                where = card.where,
                type = card.type,
                editingCardId = card.id
            ),
            returnToStudyAfterEdit = false,
            returnStudyCardId = card.id
        )
    }

    fun updateLessonTitle(title: String) {
        val draft = _uiState.value.editorLesson ?: return
        _uiState.value = _uiState.value.copy(editorLesson = draft.copy(title = title))
    }

    fun updateLessonInfo(lessonInfo: String) {
        val draft = _uiState.value.editorLesson ?: return
        _uiState.value = _uiState.value.copy(editorLesson = draft.copy(lessonInfo = lessonInfo))
    }

    fun updateCardDraft(cardDraft: CardDraft) {
        _uiState.value = _uiState.value.copy(cardDraft = cardDraft)
    }

    fun addCardToLesson() {
        val state = _uiState.value
        val lesson = state.editorLesson ?: return
        val cardDraft = state.cardDraft
        if (cardDraft.nativeValue.isBlank() || cardDraft.correctValue.isBlank()) {
            _uiState.value = state.copy(message = "Native value and correct Polish are required")
            return
        }

        val cards = if (cardDraft.editingCardId != null) {
            lesson.cards.map { card ->
                if (card.id == cardDraft.editingCardId) {
                    val frontText = cardDraft.nativeValue.trim()
                    if (card.kindCode() == "MK") {
                        card.copy(
                            mistake = frontText,
                            correctValue = cardDraft.correctValue.trim(),
                            hint = cardDraft.hint.trim(),
                            madeAt = cardDraft.madeAt.trim(),
                            where = cardDraft.where.trim(),
                            type = cardDraft.type.trim().ifBlank { "card" }
                        )
                    } else {
                        card.copy(
                            nativeValue = frontText,
                            correctValue = cardDraft.correctValue.trim(),
                            hint = cardDraft.hint.trim(),
                            madeAt = cardDraft.madeAt.trim(),
                            where = cardDraft.where.trim(),
                            type = cardDraft.type.trim().ifBlank { "card" }
                        )
                    }
                } else {
                    card
                }
            }
        } else {
            lesson.cards + Flashcard(
                id = lesson.cards.size + 1,
                nativeValue = cardDraft.nativeValue.trim(),
                correctValue = cardDraft.correctValue.trim(),
                hint = cardDraft.hint.trim(),
                madeAt = cardDraft.madeAt.trim(),
                where = cardDraft.where.trim(),
                type = cardDraft.type.trim().ifBlank { "card" }
            )
        }

        _uiState.value = state.copy(
            editorLesson = lesson.copy(cards = cards.reindexCards()),
            cardDraft = CardDraft()
        )
    }

    fun editCardInLesson(card: Flashcard) {
        _uiState.value = _uiState.value.copy(
            cardDraft = CardDraft(
                nativeValue = card.editorFrontDraftText(),
                correctValue = card.editorCorrectDraftText(),
                hint = card.hint,
                madeAt = card.madeAt,
                where = card.where,
                type = card.type,
                editingCardId = card.id
            )
        )
    }

    fun cancelCardEditing() {
        _uiState.value = _uiState.value.copy(
            cardDraft = CardDraft(),
            returnToStudyAfterEdit = false,
            returnStudyCardId = null
        )
    }

    fun saveCurrentStudyCard() {
        val state = _uiState.value
        val lesson = state.selectedLesson ?: return
        val cardId = state.cardDraft.editingCardId ?: return
        val draft = state.cardDraft
        if (draft.nativeValue.isBlank() || draft.correctValue.isBlank()) {
            _uiState.value = state.copy(message = "Native value and correct value are required")
            return
        }
        val updatedCards = lesson.cards.map { card ->
            if (card.id == cardId) {
                val frontText = draft.nativeValue.trim()
                if (card.kindCode() == "MK") {
                    card.copy(
                        mistake = frontText,
                        correctValue = draft.correctValue.trim(),
                        hint = draft.hint.trim(),
                        madeAt = draft.madeAt.trim(),
                        where = draft.where.trim(),
                        type = draft.type.trim().ifBlank { "card" },
                        log = (card.log + "${timestamp()} - card edited").takeLast(100)
                    )
                } else {
                    card.copy(
                        nativeValue = frontText,
                        correctValue = draft.correctValue.trim(),
                        hint = draft.hint.trim(),
                        madeAt = draft.madeAt.trim(),
                        where = draft.where.trim(),
                        type = draft.type.trim().ifBlank { "card" },
                        log = (card.log + "${timestamp()} - card edited").takeLast(100)
                    )
                }
            } else {
                card
            }
        }
        val updatedLesson = lesson.copy(cards = updatedCards, editable = true)
        repository.saveLesson(updatedLesson)
        val lessons = repository.loadLessons(state.showHiddenLessons)
        val savedLesson = lessons.firstOrNull { it.id == updatedLesson.id } ?: updatedLesson
        val updatedPortion = state.currentPortion.map { portionCard ->
            savedLesson.cards.firstOrNull { it.id == portionCard.id } ?: portionCard
        }
        _uiState.value = state.copy(
            lessons = lessons,
            selectedLesson = savedLesson,
            currentPortion = updatedPortion,
            currentIndex = updatedPortion.indexOfFirst { it.id == cardId }
                .takeIf { it >= 0 } ?: state.currentIndex.coerceAtMost((updatedPortion.size - 1).coerceAtLeast(0)),
            cardDraft = CardDraft(),
            returnToStudyAfterEdit = false,
            returnStudyCardId = null,
            message = "Card saved"
        )
    }

    fun deleteCurrentStudyCard() {
        val state = _uiState.value
        val lesson = state.selectedLesson ?: return
        val cardId = state.cardDraft.editingCardId ?: state.currentCard?.id ?: return
        val oldIndex = state.currentIndex
        val updatedCards = lesson.cards.filterNot { it.id == cardId }.reindexCards()
        val updatedLesson = lesson.copy(cards = updatedCards, editable = true)
        repository.saveLesson(updatedLesson)
        val lessons = repository.loadLessons(state.showHiddenLessons)
        val savedLesson = lessons.firstOrNull { it.id == updatedLesson.id } ?: updatedLesson
        val studyCards = if (state.excludeMasteredCards) {
            savedLesson.cards.filterNot { it.starCount() == 3 }
        } else {
            savedLesson.cards
        }
        val updatedPortion = orderStudyCards(studyCards, state.mode, shuffleRandom = false)
        val nextIndex = oldIndex.coerceAtMost((updatedPortion.size - 1).coerceAtLeast(0))
        _uiState.value = state.copy(
            lessons = lessons,
            selectedLesson = savedLesson,
            currentPortion = updatedPortion,
            currentIndex = nextIndex,
            cardTransitionDirection = if (nextIndex < oldIndex) -1 else 1,
            completedCardIds = emptySet(),
            isBackVisible = defaultBackVisible(),
            answer = "",
            cardDraft = CardDraft(),
            returnToStudyAfterEdit = false,
            returnStudyCardId = null,
            message = "Card deleted"
        )
    }

    fun toggleCardStar(cardId: Int, starIndex: Int) {
        val state = _uiState.value
        state.editorLesson?.let { lesson ->
            val cards = lesson.cards.map { card ->
                if (card.id == cardId) card.copy(stars = nextSequentialStars(card.stars, starIndex)) else card
            }
            _uiState.value = state.copy(editorLesson = lesson.copy(cards = cards))
            return
        }

        val lesson = state.selectedLesson ?: return
        val updatedCards = lesson.cards.map { card ->
            if (card.id == cardId) card.copy(stars = nextSequentialStars(card.stars, starIndex)) else card
        }
        val changedCard = updatedCards.firstOrNull { it.id == cardId }
        val logEntry = changedCard?.let { "${timestamp()} - stars changed to ${it.starCount()}" }
        val loggedCards = if (logEntry != null) {
            updatedCards.map { card ->
                if (card.id == cardId) card.copy(log = (card.log + logEntry).takeLast(100)) else card
            }
        } else {
            updatedCards
        }
        val updatedLesson = lesson.copy(cards = loggedCards, editable = true)
        val updatedPortion = state.currentPortion.map { card ->
            loggedCards.firstOrNull { it.id == card.id } ?: card
        }.let { cards ->
            if (state.excludeMasteredCards) cards.filterNot { it.starCount() == 3 } else cards
        }
        repository.saveLesson(updatedLesson)
        _uiState.value = state.copy(
            selectedLesson = updatedLesson,
            currentPortion = updatedPortion,
            currentIndex = state.currentIndex.coerceAtMost((updatedPortion.size - 1).coerceAtLeast(0)),
            lessons = repository.loadLessons(state.showHiddenLessons)
        )
    }

    private fun addStarForCorrectTypedAnswer(cardId: Int) {
        val state = _uiState.value
        val lesson = state.selectedLesson ?: return
        val card = lesson.cards.firstOrNull { it.id == cardId } ?: return
        val nextStarCount = (card.starCount() + 1).coerceAtMost(3)
        if (nextStarCount == card.starCount()) return
        val updatedStars = starsForCount(nextStarCount)
        val logEntry = "${timestamp()} - stars changed to $nextStarCount after correct typed answer"
        val updatedCards = lesson.cards.map { existingCard ->
            if (existingCard.id == cardId) {
                existingCard.copy(
                    stars = updatedStars,
                    log = (existingCard.log + logEntry).takeLast(100)
                )
            } else {
                existingCard
            }
        }
        val updatedLesson = lesson.copy(cards = updatedCards, editable = true)
        val updatedPortion = state.currentPortion.map { portionCard ->
            updatedCards.firstOrNull { it.id == portionCard.id } ?: portionCard
        }

        repository.saveLesson(updatedLesson)
        _uiState.value = state.copy(
            selectedLesson = updatedLesson,
            currentPortion = updatedPortion,
            currentIndex = state.currentIndex.coerceAtMost((updatedPortion.size - 1).coerceAtLeast(0)),
            lessons = repository.loadLessons(state.showHiddenLessons)
        )
    }

    private fun nextSequentialStars(stars: Int, starIndex: Int): Int {
        val currentCount = stars.coerceSequentialStarCount()
        val requestedCount = (starIndex + 1).coerceIn(0, 3)
        val nextCount = if (currentCount == requestedCount) {
            starIndex.coerceIn(0, 3)
        } else {
            requestedCount
        }
        return starsForCount(nextCount)
    }

    private fun starsForCount(count: Int): Int {
        return when (count.coerceIn(0, 3)) {
            0 -> 0
            1 -> 0b001
            2 -> 0b011
            else -> 0b111
        }
    }

    private fun Int.coerceSequentialStarCount(): Int {
        return (0 until 3).count { index -> this and (1 shl index) != 0 }.coerceIn(0, 3)
    }

    fun moveCard(cardId: Int, direction: Int) {
        val state = _uiState.value
        val lesson = state.editorLesson ?: return
        val currentIndex = lesson.cards.indexOfFirst { it.id == cardId }
        val targetIndex = currentIndex + direction
        if (currentIndex !in lesson.cards.indices || targetIndex !in lesson.cards.indices) return

        val cards = lesson.cards.toMutableList()
        val moved = cards.removeAt(currentIndex)
        cards.add(targetIndex, moved)
        _uiState.value = state.copy(editorLesson = lesson.copy(cards = cards.reindexCards()))
    }

    fun deleteCardFromLesson(cardId: Int) {
        val state = _uiState.value
        val lesson = state.editorLesson ?: return
        val cards = lesson.cards
            .filterNot { it.id == cardId }
            .reindexCards()
        val updatedLesson = lesson.copy(cards = cards)
        val updatedLessonEntity = Lesson(
            id = updatedLesson.id,
            title = updatedLesson.title,
            lessonInfo = updatedLesson.lessonInfo,
            cards = updatedLesson.cards,
            timesCompleted = updatedLesson.timesCompleted,
            editable = true,
            hidden = state.lessons.firstOrNull { it.id == updatedLesson.id }?.hidden ?: false
        )
        if (updatedLesson.id.isNotBlank()) {
            repository.saveLesson(updatedLessonEntity)
        }
        val lessons = repository.loadLessons(state.showHiddenLessons)
        val selectedLesson = lessons.firstOrNull { it.id == updatedLesson.id }
            ?: if (updatedLesson.id.isNotBlank()) updatedLessonEntity else state.selectedLesson
        val studyCards = if (state.excludeMasteredCards) {
            (selectedLesson?.cards ?: updatedLesson.cards).filterNot { it.starCount() == 3 }
        } else {
            selectedLesson?.cards ?: updatedLesson.cards
        }
        val updatedPortion = orderStudyCards(studyCards, state.mode, shuffleRandom = false)
        val validCompletedIds = updatedPortion.map { it.id }.toSet()
        _uiState.value = state.copy(
            lessons = lessons,
            selectedLesson = selectedLesson,
            editorLesson = updatedLesson,
            currentPortion = updatedPortion,
            currentIndex = state.currentIndex.coerceAtMost((updatedPortion.size - 1).coerceAtLeast(0)),
            completedCardIds = state.completedCardIds.intersect(validCompletedIds),
            cardDraft = if (state.cardDraft.editingCardId == cardId) CardDraft() else state.cardDraft,
            returnToStudyAfterEdit = if (state.cardDraft.editingCardId == cardId) false else state.returnToStudyAfterEdit,
            returnStudyCardId = if (state.cardDraft.editingCardId == cardId) null else state.returnStudyCardId,
            message = "Card deleted"
        )
    }

    fun copyCardInLesson(cardId: Int) {
        val state = _uiState.value
        val lesson = state.editorLesson ?: return
        val source = lesson.cards.firstOrNull { it.id == cardId } ?: return
        val sourceIndex = lesson.cards.indexOfFirst { it.id == cardId }
        val cards = lesson.cards.toMutableList()
        cards.add(
            sourceIndex + 1,
            source.copy(
                id = lesson.cards.size + 1,
                log = (source.log + "${timestamp()} - card copied").takeLast(100)
            )
        )
        _uiState.value = state.copy(editorLesson = lesson.copy(cards = cards.reindexCards()))
    }

    fun saveEditedLesson() {
        val state = _uiState.value
        val draft = state.editorLesson ?: return
        if (draft.title.isBlank()) {
            _uiState.value = state.copy(message = "Lesson title is required")
            return
        }

        val lesson = Lesson(
            id = draft.id.ifBlank { newLessonId() },
            title = draft.title.trim(),
            lessonInfo = draft.lessonInfo.trim(),
            cards = draft.cards.reindexCards(),
            timesCompleted = draft.timesCompleted,
            editable = true,
            hidden = state.lessons.firstOrNull { it.id == draft.id }?.hidden ?: false
        )
        repository.saveLesson(lesson)
        if (state.returnToStudyAfterEdit) {
            val lessons = repository.loadLessons(state.showHiddenLessons)
            val updatedLesson = lessons.firstOrNull { it.id == lesson.id } ?: lesson
            val cardId = state.returnStudyCardId
            val updatedPortion = state.currentPortion.map { portionCard ->
                updatedLesson.cards.firstOrNull { it.id == portionCard.id } ?: portionCard
            }
            _uiState.value = state.copy(
                lessons = lessons,
                selectedLesson = updatedLesson,
                currentPortion = updatedPortion,
                currentIndex = updatedPortion.indexOfFirst { it.id == cardId }
                    .takeIf { it >= 0 } ?: state.currentIndex.coerceAtMost((updatedPortion.size - 1).coerceAtLeast(0)),
                screen = AppScreen.STUDY,
                editorLesson = null,
                cardDraft = CardDraft(),
                returnToStudyAfterEdit = false,
                returnStudyCardId = null,
                message = "Card saved"
            )
            return
        }
        _uiState.value = state.copy(
            lessons = repository.loadLessons(state.showHiddenLessons),
            screen = AppScreen.CATALOG,
            editorLesson = null,
            cardDraft = CardDraft(),
            returnToStudyAfterEdit = false,
            returnStudyCardId = null,
            message = "Lesson saved"
        )
    }

    fun deleteLesson(lessonId: String) {
        repository.deleteLesson(lessonId)
        _uiState.value = _uiState.value.copy(
            lessons = repository.loadLessons(_uiState.value.showHiddenLessons),
            selectedLessonIds = emptySet(),
            screen = AppScreen.CATALOG,
            message = "Lesson deleted"
        )
    }


    fun addQuickVocabularyCard(phrase: String) {
        val cleanPhrase = phrase.trim()
        if (cleanPhrase.isBlank()) {
            _uiState.value = _uiState.value.copy(message = "No speech recognized")
            return
        }
        val state = _uiState.value
        val sourceLanguage = state.quickVocabularySourceLanguage.trim().ifBlank { "Polish" }
        val targetLanguage = state.quickVocabularyTargetLanguage.trim().ifBlank { "Russian" }
        val visibleLessonsForQuickVocabulary = repository.loadLessons(includeHidden = false)
        val existingLesson = visibleLessonsForQuickVocabulary.firstOrNull { it.id.startsWith(QUICK_VOCABULARY_LESSON_ID) }
        val lessonId = existingLesson?.id ?: "${QUICK_VOCABULARY_LESSON_ID}_${UUID.randomUUID()}"
        val now = timestamp()
        val nextCardId = (existingLesson?.cards?.maxOfOrNull { it.id } ?: 0) + 1
        val newCard = Flashcard(
            id = nextCardId,
            nativeValue = cleanPhrase,
            correctValue = "Translation pending",
            hint = "Captured by voice. Add or generate the translation later.",
            madeAt = now,
            where = "Quick vocabulary microphone",
            log = listOf("$now - captured by quick vocabulary microphone"),
            type = "card",
            cardKind = "LN",
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage
        )
        val updatedLesson = if (existingLesson == null) {
            Lesson(
                id = lessonId,
                title = QUICK_VOCABULARY_LESSON_TITLE,
                lessonInfo = quickVocabularyLessonInfo(sourceLanguage, targetLanguage),
                cards = listOf(newCard),
                editable = true
            )
        } else {
            existingLesson.copy(
                title = existingLesson.title.ifBlank { QUICK_VOCABULARY_LESSON_TITLE },
                lessonInfo = quickVocabularyLessonInfo(sourceLanguage, targetLanguage),
                cards = existingLesson.cards + newCard,
                editable = true,
                hidden = false
            )
        }
        repository.saveLesson(updatedLesson)
        val visibleLessons = repository.loadLessons(state.showHiddenLessons)
        val hasServerTranslation = state.translationApiUrl.isNotBlank() && state.translationApiToken.isNotBlank()
        _uiState.value = state.copy(
            lessons = visibleLessons,
            selectedLessonIds = emptySet(),
            screen = AppScreen.CATALOG,
            message = when {
                hasServerTranslation -> "Added: $cleanPhrase. Translating..."
                state.useLocalTranslation -> "Added: $cleanPhrase. Local translation unavailable in this build"
                else -> "Added: $cleanPhrase"
            }
        )
        if (hasServerTranslation) {
            translateQuickVocabularyCard(
                lessonId = updatedLesson.id,
                cardId = newCard.id,
                phrase = cleanPhrase,
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                apiUrl = state.translationApiUrl,
                apiToken = state.translationApiToken
            )
        }
    }

    private fun translateQuickVocabularyCard(
        lessonId: String,
        cardId: Int,
        phrase: String,
        sourceLanguage: String,
        targetLanguage: String,
        apiUrl: String,
        apiToken: String
    ) {
        viewModelScope.launch {
            val translated = runCatching {
                if (apiUrl.isNotBlank() && apiToken.isNotBlank()) {
                    requestTranslation(apiUrl, apiToken, phrase, sourceLanguage, targetLanguage)
                } else {
                    ""
                }
            }.getOrNull().orEmpty().trim()
            if (translated.isBlank()) {
                _uiState.value = _uiState.value.copy(message = "Added: $phrase. Translation pending")
                return@launch
            }
            val lesson = repository.loadLessons(includeHidden = true).firstOrNull { it.id == lessonId }
                ?: return@launch
            val now = timestamp()
            val updatedLesson = lesson.copy(
                cards = lesson.cards.map { card ->
                    if (card.id == cardId) {
                        card.copy(
                            correctValue = translated,
                            hint = "Captured by voice and translated automatically.",
                            log = (card.log + "$now - translated automatically from $sourceLanguage to $targetLanguage").takeLast(100)
                        )
                    } else {
                        card
                    }
                },
                lessonInfo = quickVocabularyLessonInfo(sourceLanguage, targetLanguage),
                editable = true,
                hidden = false
            )
            repository.saveLesson(updatedLesson)
            val currentState = _uiState.value
            _uiState.value = currentState.copy(
                lessons = repository.loadLessons(currentState.showHiddenLessons),
                message = "Translation added: $phrase"
            )
        }
    }

    private suspend fun requestTranslation(
        apiUrl: String,
        apiToken: String,
        phrase: String,
        sourceLanguage: String,
        targetLanguage: String
    ): String = withContext(Dispatchers.IO) {
        val connection = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 30_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $apiToken")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        val payload = JSONObject()
            .put("text", phrase)
            .put("sourceLanguage", sourceLanguage)
            .put("targetLanguage", targetLanguage)
            .toString()
        connection.outputStream.use { stream ->
            stream.write(payload.toByteArray(Charsets.UTF_8))
        }
        val status = connection.responseCode
        val responseStream = if (status in 200..299) connection.inputStream else connection.errorStream
        val responseText = responseStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        connection.disconnect()
        if (status !in 200..299) return@withContext ""
        JSONObject(responseText).optString("translation", "").trim()
    }
    private fun List<Flashcard>.reindexCards(): List<Flashcard> {
        return mapIndexed { index, card -> card.copy(id = index + 1) }
    }



    private fun recordWrongAnswer(cardId: Int, answer: String) {
        val state = _uiState.value
        val lesson = state.selectedLesson ?: return
        val date = timestamp()
        val updatedCard = repository.appendWrongAnswer(lesson.id, cardId, answer.ifBlank { "(empty answer)" }, date) ?: return
        val lessons = repository.loadLessons(state.showHiddenLessons)
        val updatedLesson = lessons.firstOrNull { it.id == lesson.id } ?: lesson
        _uiState.value = state.copy(
            lessons = lessons,
            selectedLesson = updatedLesson,
            currentPortion = state.currentPortion.map { card -> if (card.id == cardId) updatedCard else card }
        )
    }
    private fun recordCardWork(cardId: Int, action: String) {
        val state = _uiState.value
        val lesson = state.selectedLesson ?: return
        val entry = "${timestamp()} - $action"
        val updatedCard = repository.appendCardLog(lesson.id, cardId, entry) ?: return
        val lessons = repository.loadLessons(state.showHiddenLessons)
        val updatedLesson = lessons.firstOrNull { it.id == lesson.id } ?: lesson
        _uiState.value = state.copy(
            lessons = lessons,
            selectedLesson = updatedLesson,
            currentPortion = state.currentPortion.map { card -> if (card.id == cardId) updatedCard else card }
        )
    }

    private fun Flashcard.editorFrontDraftText(): String {
        return if (kindCode() == "MK") {
            mistake.ifBlank { wrongAnswers.lastOrNull()?.answer.orEmpty() }
                .ifBlank { nativeValue.ifBlank { ru.ifBlank { value } } }
        } else {
            nativeValue.ifBlank { ru.ifBlank { value } }
        }
    }

    private fun Flashcard.editorCorrectDraftText(): String {
        return correctValue.ifBlank { pl.ifBlank { value } }
    }

    private fun timestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
    }
    private fun filterStudyCards(
        cards: List<Flashcard>,
        excludeMastered: Boolean,
        hideCompleted: Boolean,
        completedIds: Set<Int>
    ): List<Flashcard> {
        return cards.filter { card ->
            (!excludeMastered || card.starCount() < 3) && (!hideCompleted || card.id !in completedIds)
        }
    }

    private fun wrongLetterRatio(answer: String, expected: String): Double {
        val typed = normalize(answer)
        val target = normalize(expected)
        val maxLength = maxOf(typed.length, target.length)
        if (maxLength == 0) return 0.0
        val wrongCount = (0 until maxLength).count { index -> typed.getOrNull(index) != target.getOrNull(index) }
        return wrongCount.toDouble() / maxLength.toDouble()
    }

    private fun normalize(value: String): String {
        return value
            .replace('\u00A0', ' ')
            .replace('\u2007', ' ')
            .replace('\u202F', ' ')
            .replace(Regex("[\\p{P}\\s]+"), "")
            .lowercase(Locale.getDefault())
    }

    private fun quickVocabularyLessonInfo(sourceLanguage: String, targetLanguage: String): String {
        return "Quick voice captures. Source language: $sourceLanguage. Target language: $targetLanguage. Translation is generated by the configured server API when available; otherwise it is saved as pending. Local translation is disabled in this build because on-device models were not reliable enough."
    }

    private fun newLessonId(): String = "lesson_${UUID.randomUUID()}"

    companion object {
        private const val PORTION_SIZE = 20
        private const val QUICK_VOCABULARY_LESSON_ID = "quick_vocabulary"
        private const val QUICK_VOCABULARY_LESSON_TITLE = "New vocabulary"
    }
}
