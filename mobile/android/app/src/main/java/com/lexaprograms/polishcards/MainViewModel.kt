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
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class AppScreen {
    CATALOG,
    STUDY,
    TRANSLATE,
    EDITOR,
    SETTINGS
}

enum class WorkMode {
    CARDS,
    TESTS,
    TRANSLATE,
    SPLIT
}

enum class CardStartSide {
    POLISH,
    TRANSLATION
}

enum class DisplayTextSize {
    VERY_SMALL,
    SMALL,
    MEDIUM,
    LARGE
}

enum class ControlSize {
    VERY_SMALL,
    SMALL,
    MEDIUM
}

data class OfflineSpeechLanguage(
    val name: String,
    val tag: String
)

val OfflineSpeechLanguages = listOf(
    OfflineSpeechLanguage("English", "en-US"),
    OfflineSpeechLanguage("Polish", "pl-PL"),
    OfflineSpeechLanguage("Russian", "ru-RU"),
    OfflineSpeechLanguage("Belarusian", "be-BY"),
    OfflineSpeechLanguage("German", "de-DE"),
    OfflineSpeechLanguage("Spanish", "es-ES"),
    OfflineSpeechLanguage("Latvian", "lv-LV"),
    OfflineSpeechLanguage("Lithuanian", "lt-LT"),
    OfflineSpeechLanguage("Portuguese", "pt-PT")
)

val OpenAiSpeechModels = listOf(
    "gpt-4o-mini-transcribe",
    "gpt-4o-transcribe",
    "whisper-1",
    "gpt-4o-transcribe-diarize"
)

val OpenAiTextModels = listOf(
    "gpt-5.4-nano",
    "gpt-5.4-mini",
    "gpt-5.4",
    "gpt-5.5"
)

val OpenAiTtsModels = listOf(
    "gpt-4o-mini-tts",
    "tts-1",
    "tts-1-hd"
)

val OpenAiTtsVoices = listOf(
    "alloy",
    "ash",
    "ballad",
    "coral",
    "echo",
    "fable",
    "nova",
    "onyx",
    "sage",
    "shimmer",
    "verse",
    "marin",
    "cedar"
)

val BelarusianTtsProviders = listOf(
    "ElevenLabs",
    "OpenAI",
    "Device"
)

val ElevenLabsModels = listOf(
    "eleven_v3"
)

val ElevenLabsSpecialTtsLanguages = listOf(
    "en" to "EN - English",
    "es" to "ES - Spanish",
    "pl" to "PL - Polish",
    "ru" to "RU - Russian",
    "be" to "BY - Belarusian",
    "uk" to "UA - Ukrainian",
    "de" to "DE - German",
    "lv" to "LV - Latvian",
    "lt" to "LT - Lithuanian",
    "pt" to "PT - Portuguese"
)

val OpenAiCacheDurationOptions = listOf(
    "5 minutes" to CardRepository.DEFAULT_OPENAI_CACHE_DURATION_MINUTES,
    "1 day" to 1_440L,
    "7 days" to 10_080L,
    "30 days" to CardRepository.MAX_OPENAI_CACHE_DURATION_MINUTES,
    "Forever" to -1L
)

val OpenAiVoiceSilenceTimeoutOptions = listOf(
    "1 second" to CardRepository.MIN_OPENAI_VOICE_SILENCE_TIMEOUT_MS,
    "2 seconds" to 2_000L,
    "3 seconds" to 3_000L,
    "4 seconds" to 4_000L,
    "5 seconds" to CardRepository.DEFAULT_OPENAI_VOICE_SILENCE_TIMEOUT_MS,
    "6 seconds" to 6_000L,
    "7 seconds" to 7_000L,
    "8 seconds" to 8_000L,
    "9 seconds" to 9_000L,
    "10 seconds" to 10_000L,
    "15 seconds" to 15_000L
)

val CardStatusBlinkIntervalOptions = listOf(
    "Off" to CardRepository.CARD_STATUS_BLINK_OFF_MS,
    "0.5 seconds" to 500L,
    "1 second" to 1_000L,
    "2 seconds" to CardRepository.DEFAULT_CARD_STATUS_BLINK_INTERVAL_MS,
    "3 seconds" to 3_000L,
    "4 seconds" to 4_000L,
    "5 seconds" to 5_000L
)

private data class OpenAiTextResult(
    val text: String,
    val source: String
)

private data class CardTranslationInsight(
    val translation: String,
    val explanation: String,
    val rule: String,
    val examples: List<String>,
    val source: String
)

private data class CardTranslationInsightResult(
    val text: String,
    val source: String
)

private data class MistakeCorrectionInsight(
    val corrected: String,
    val explanation: String,
    val rules: List<String>,
    val examples: List<String>
)

private data class TrainCardDraft(
    val front: String,
    val back: String,
    val hint: String,
    val option: String
)

private data class SharedPostCardDraft(
    val front: String,
    val back: String,
    val hint: String,
    val mode: String
)

data class LessonDraft(
    val id: String = "",
    val title: String = "",
    val lessonInfo: String = "",
    val sourceLanguage: String = "",
    val targetLanguage: String = "",
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
    val settingsReturnScreen: AppScreen = AppScreen.CATALOG,
    val lessons: List<Lesson> = emptyList(),
    val selectedLessonIds: Set<String> = emptySet(),
    val showHiddenLessons: Boolean = false,
    val selectedLesson: Lesson? = null,
    val workMode: WorkMode = WorkMode.CARDS,
    val translationInput: String = "",
    val translationOutput: String = "",
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
    val onboardingCompleted: Boolean = false,
    val quickVocabularySourceLanguage: String = "Russian",
    val quickVocabularyTargetLanguage: String = "Polish",
    val activeVocabularySourceLanguage: String = quickVocabularySourceLanguage,
    val activeVocabularyTargetLanguage: String = quickVocabularyTargetLanguage,
    val useLocalTranslation: Boolean = false,
    val autoSaveTranslatorCards: Boolean = false,
    val translationApiUrl: String = "",
    val translationApiToken: String = "",
    val useOpenAiModels: Boolean = false,
    val openAiBaseUrl: String = CardRepository.DEFAULT_OPENAI_BASE_URL,
    val openAiApiKey: String = "",
    val openAiSpeechModel: String = CardRepository.DEFAULT_OPENAI_SPEECH_MODEL,
    val openAiTextModel: String = CardRepository.DEFAULT_OPENAI_TEXT_MODEL,
    val openAiTtsModel: String = CardRepository.DEFAULT_OPENAI_TTS_MODEL,
    val openAiTtsVoice: String = CardRepository.DEFAULT_OPENAI_TTS_VOICE,
    val belarusianTtsProvider: String = CardRepository.DEFAULT_BELARUSIAN_TTS_PROVIDER,
    val elevenLabsApiKey: String = "",
    val elevenLabsModel: String = CardRepository.DEFAULT_ELEVENLABS_MODEL,
    val elevenLabsVoiceId: String = CardRepository.DEFAULT_ELEVENLABS_BELARUSIAN_VOICE_ID,
    val elevenLabsTtsLanguageCodes: Set<String> = CardRepository.DEFAULT_ELEVENLABS_TTS_LANGUAGE_CODES,
    val openAiCacheDurationMinutes: Long = CardRepository.DEFAULT_OPENAI_CACHE_DURATION_MINUTES,
    val openAiVoiceSilenceTimeoutMs: Long = CardRepository.DEFAULT_OPENAI_VOICE_SILENCE_TIMEOUT_MS,
    val cardStatusBlinkIntervalMs: Long = CardRepository.DEFAULT_CARD_STATUS_BLINK_INTERVAL_MS,
    val openAiActivityLog: List<String> = emptyList(),
    val translationAttribution: String = "",
    val offlineSpeechLanguageTag: String = "en-US",
    val offlineSpeechStatuses: Map<String, String> = OfflineSpeechLanguages.associate { language ->
        language.tag to CardRepository.OFFLINE_SPEECH_STATUS_NOT_DOWNLOADED
    },
    val offlineSpeechDownloadingTag: String? = null,
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
    val notificationAnswered: Boolean = false,
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
            cardStartSide = CardStartSide.POLISH,
            excludeMasteredCards = false,
            hideCompletedCards = false,
            showCardLog = repository.loadShowCardLog(),
            displayTextSize = repository.loadDisplayTextSize(),
            controlSize = repository.loadControlSize(),
            interfaceLanguage = repository.loadInterfaceLanguage(detectSystemInterfaceLanguage()),
            onboardingCompleted = repository.loadOnboardingCompleted(),
            quickVocabularySourceLanguage = repository.loadQuickVocabularySourceLanguage(),
            quickVocabularyTargetLanguage = repository.loadQuickVocabularyTargetLanguage(),
            useLocalTranslation = repository.loadUseLocalTranslation(),
            autoSaveTranslatorCards = repository.loadAutoSaveTranslatorCards(),
            translationApiUrl = repository.loadTranslationApiUrl(),
            translationApiToken = repository.loadTranslationApiToken(),
            useOpenAiModels = repository.loadUseOpenAiModels(),
            openAiBaseUrl = repository.loadOpenAiBaseUrl(),
            openAiApiKey = repository.loadOpenAiApiKey(),
            openAiSpeechModel = repository.loadOpenAiSpeechModel().takeIf { it in OpenAiSpeechModels }
                ?: CardRepository.DEFAULT_OPENAI_SPEECH_MODEL,
            openAiTextModel = repository.loadOpenAiTextModel().takeIf { it in OpenAiTextModels }
                ?: CardRepository.DEFAULT_OPENAI_TEXT_MODEL,
            openAiTtsModel = repository.loadOpenAiTtsModel().takeIf { it in OpenAiTtsModels }
                ?: CardRepository.DEFAULT_OPENAI_TTS_MODEL,
            openAiTtsVoice = repository.loadOpenAiTtsVoice().takeIf { it in OpenAiTtsVoices }
                ?: CardRepository.DEFAULT_OPENAI_TTS_VOICE,
            belarusianTtsProvider = repository.loadBelarusianTtsProvider().takeIf { it in BelarusianTtsProviders }
                ?: CardRepository.DEFAULT_BELARUSIAN_TTS_PROVIDER,
            elevenLabsApiKey = repository.loadElevenLabsApiKey(),
            elevenLabsModel = repository.loadElevenLabsModel().takeIf { it in ElevenLabsModels }
                ?: CardRepository.DEFAULT_ELEVENLABS_MODEL,
            elevenLabsVoiceId = repository.loadElevenLabsVoiceId().trim()
                .ifBlank { CardRepository.DEFAULT_ELEVENLABS_BELARUSIAN_VOICE_ID },
            elevenLabsTtsLanguageCodes = repository.loadElevenLabsTtsLanguageCodes(),
            openAiCacheDurationMinutes = repository.loadOpenAiCacheDurationMinutes(),
            openAiVoiceSilenceTimeoutMs = repository.loadOpenAiVoiceSilenceTimeoutMs(),
            cardStatusBlinkIntervalMs = repository.loadCardStatusBlinkIntervalMs(),
            openAiActivityLog = repository.loadOpenAiActivityLog(),
            offlineSpeechLanguageTag = repository.loadOfflineSpeechLanguageTag()
                .takeIf { tag -> OfflineSpeechLanguages.any { it.tag == tag } }
                ?: "en-US",
            offlineSpeechStatuses = OfflineSpeechLanguages.associate { language ->
                language.tag to repository.loadOfflineSpeechStatus(language.tag)
            },
            offlineSpeechDownloadingTag = OfflineSpeechLanguages
                .firstOrNull { language ->
                    repository.loadOfflineSpeechStatus(language.tag) == CardRepository.OFFLINE_SPEECH_STATUS_DOWNLOADING
                }
                ?.tag,
            soundEffectsEnabled = repository.loadSoundEffectsEnabled(),
            vibrationEnabled = repository.loadVibrationEnabled(),
            notificationIntervalMinutes = repository.loadNotificationIntervalMinutes(),
            notificationIntervalDraft = repository.loadNotificationIntervalMinutes().toString(),
            notificationMaxDraft = repository.loadMaxActiveNotifications().toString()
        )
        _uiState.value = _uiState.value.copy(
            activeVocabularySourceLanguage = _uiState.value.quickVocabularySourceLanguage,
            activeVocabularyTargetLanguage = _uiState.value.quickVocabularyTargetLanguage
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

    fun setWorkMode(mode: WorkMode) {
        val opensTranslator = mode == WorkMode.TRANSLATE || mode == WorkMode.SPLIT
        _uiState.value = _uiState.value.copy(
            workMode = mode,
            screen = if (opensTranslator) AppScreen.TRANSLATE else _uiState.value.screen,
            translationInput = if (opensTranslator) "" else _uiState.value.translationInput,
            translationOutput = if (opensTranslator) "" else _uiState.value.translationOutput,
            message = mode.displayLabel()
        )
    }

    fun openTranslationMode(mode: WorkMode = _uiState.value.workMode) {
        val nextMode = if (mode == WorkMode.SPLIT) WorkMode.SPLIT else WorkMode.TRANSLATE
        _uiState.value = _uiState.value.copy(
            workMode = nextMode,
            screen = AppScreen.TRANSLATE,
            translationInput = "",
            translationOutput = "",
            message = nextMode.displayLabel()
        )
    }

    fun updateTranslationInput(value: String) {
        _uiState.value = _uiState.value.copy(translationInput = value, message = null)
    }

    fun updateTranslationOutput(value: String, attribution: String = _uiState.value.translationAttribution) {
        val state = _uiState.value
        val cleanOutput = value.trim()
        val cleanInput = state.translationInput.trim()
        if (state.translationOutput == value && state.translationAttribution == attribution) return
        _uiState.value = state.copy(translationOutput = value, translationAttribution = attribution, message = null)
        if (state.autoSaveTranslatorCards && cleanInput.isNotBlank() && cleanOutput.isNotBlank()) {
            saveTranslatedCardFromTranslator(cleanInput, cleanOutput)
        }
    }

    fun translateOnlineText(
        text: String,
        sourceLanguage: String,
        targetLanguage: String
    ) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) {
            updateTranslationOutput("", "")
            return
        }
        viewModelScope.launch {
            val requestState = _uiState.value
            val result = runCatching {
                requestConfiguredOnlineTranslation(cleanText, sourceLanguage, targetLanguage, requestState)
            }.getOrNull() ?: OpenAiTextResult("", "")
            val translated = result.text.trim()
            val currentState = _uiState.value
            if (currentState.translationInput.trim() != cleanText) return@launch
            if (translated.isBlank()) {
                _uiState.value = currentState.copy(message = "Online translation failed")
            } else {
                updateTranslationOutput(
                    translated,
                    attribution = result.googleAttribution()
                )
                if (result.isOpenAiSource()) {
                    logOpenAiActivity(
                        action = "translation ${result.source}",
                        details = "${requestState.openAiTextModel}: $sourceLanguage -> $targetLanguage; $cleanText => $translated"
                    )
                    _uiState.value = _uiState.value.copy(message = "OpenAI translation: ${result.source}")
                }
            }
        }
    }

    fun clearTranslationInput() {
        _uiState.value = _uiState.value.copy(
            translationInput = "",
            translationOutput = "",
            translationAttribution = "",
            message = "Cleared"
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
            notificationAnswered = false,
            closeAfterNotificationAnswer = false
        )
    }
    fun openSettings() {
        val state = _uiState.value
        if (state.screen == AppScreen.SETTINGS) {
            closeSettings()
        } else {
            _uiState.value = state.copy(
                screen = AppScreen.SETTINGS,
                settingsReturnScreen = state.screen
            )
        }
    }

    fun navigateBack() {
        if (_uiState.value.screen == AppScreen.SETTINGS) {
            closeSettings()
        } else {
            openCatalog()
        }
    }

    private fun closeSettings() {
        val state = _uiState.value
        val hasUnsavedDrafts = state.notificationIntervalDraft != repository.loadNotificationIntervalMinutes().toString() ||
            state.notificationMaxDraft != repository.loadMaxActiveNotifications().toString()
        _uiState.value = state.copy(
            screen = state.settingsReturnScreen,
            message = if (hasUnsavedDrafts) "Some settings were not saved" else null
        )
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
        val cleaned = language.trim().ifBlank { "Russian" }
        repository.saveQuickVocabularySourceLanguage(cleaned)
        _uiState.value = _uiState.value.copy(
            quickVocabularySourceLanguage = cleaned,
            activeVocabularySourceLanguage = cleaned,
            message = "Basic language: $cleaned"
        )
    }

    fun swapQuickVocabularyLanguages() {
        val state = _uiState.value
        val source = state.quickVocabularySourceLanguage
        val target = state.quickVocabularyTargetLanguage
        repository.saveQuickVocabularySourceLanguage(target)
        repository.saveQuickVocabularyTargetLanguage(source)
        _uiState.value = state.copy(
            quickVocabularySourceLanguage = target,
            quickVocabularyTargetLanguage = source,
            activeVocabularySourceLanguage = target,
            activeVocabularyTargetLanguage = source,
            translationInput = state.translationOutput,
            translationOutput = state.translationInput,
            message = "Languages swapped"
        )
    }

    fun setActiveVocabularySourceLanguage(language: String) {
        val cleaned = language.trim().ifBlank { _uiState.value.quickVocabularySourceLanguage }
        _uiState.value = _uiState.value.copy(
            activeVocabularySourceLanguage = cleaned,
            message = "Current Basic language: $cleaned"
        )
    }

    fun setActiveVocabularyTargetLanguage(language: String) {
        val cleaned = language.trim().ifBlank { _uiState.value.quickVocabularyTargetLanguage }
        _uiState.value = _uiState.value.copy(
            activeVocabularyTargetLanguage = cleaned,
            message = "Current Target language: $cleaned"
        )
    }

    fun swapActiveVocabularyLanguages() {
        val state = _uiState.value
        _uiState.value = state.copy(
            activeVocabularySourceLanguage = state.activeVocabularyTargetLanguage,
            activeVocabularyTargetLanguage = state.activeVocabularySourceLanguage,
            translationInput = state.translationOutput,
            translationOutput = state.translationInput,
            message = "Current language pair swapped"
        )
    }

    fun setQuickVocabularyTargetLanguage(language: String) {
        val cleaned = language.trim().ifBlank { "Polish" }
        repository.saveQuickVocabularyTargetLanguage(cleaned)
        _uiState.value = _uiState.value.copy(
            quickVocabularyTargetLanguage = cleaned,
            activeVocabularyTargetLanguage = cleaned,
            message = "Target language: $cleaned"
        )
    }

    fun completeOnboarding(
        interfaceLanguage: String,
        knownLanguage: String,
        learningLanguage: String,
        explanationLanguage: String
    ) {
        val cleanKnown = knownLanguage.trim().ifBlank { "Russian" }
        val cleanLearning = learningLanguage.trim().ifBlank { "Polish" }
        val cleanExplanation = explanationLanguage.trim().ifBlank { cleanKnown }
        val normalizedInterface = normalizeInterfaceLanguage(interfaceLanguage)
            .takeIf { interfaceLanguage.isNotBlank() }
            ?: interfaceCodeForLanguageName(cleanKnown)
        repository.saveInterfaceLanguage(normalizedInterface)
        repository.saveQuickVocabularySourceLanguage(cleanKnown)
        repository.saveQuickVocabularyTargetLanguage(cleanLearning)
        repository.saveOnboardingCompleted(true)
        _uiState.value = _uiState.value.copy(
            interfaceLanguage = normalizedInterface,
            quickVocabularySourceLanguage = cleanKnown,
            quickVocabularyTargetLanguage = cleanLearning,
            activeVocabularySourceLanguage = cleanKnown,
            activeVocabularyTargetLanguage = cleanLearning,
            onboardingCompleted = true,
            message = if (cleanExplanation != cleanKnown) {
                "Basic language saved. AI explanation language will be used when the AI model is connected."
            } else {
                null
            }
        )
    }



    fun setUseLocalTranslation(enabled: Boolean) {
        val wasEnabled = _uiState.value.useLocalTranslation
        repository.saveUseLocalTranslation(enabled)
        _uiState.value = _uiState.value.copy(
            useLocalTranslation = enabled,
            message = when {
                enabled && !wasEnabled -> "Offline mode enabled. Using downloaded language models."
                !enabled && wasEnabled -> "Online translation mode enabled"
                else -> _uiState.value.message
            }
        )
    }

    fun setAutoSaveTranslatorCards(enabled: Boolean) {
        repository.saveAutoSaveTranslatorCards(enabled)
        _uiState.value = _uiState.value.copy(
            autoSaveTranslatorCards = enabled,
            message = if (enabled) "Translator auto-save enabled" else "Translator auto-save disabled"
        )
    }

    fun setTranslationApiUrl(url: String) {
        setOpenAiBaseUrl(url)
    }

    fun setTranslationApiToken(token: String) {
        setOpenAiApiKey(token)
    }

    fun setUseOpenAiModels(enabled: Boolean) {
        repository.saveUseOpenAiModels(enabled)
        _uiState.value = _uiState.value.copy(
            useOpenAiModels = enabled,
            message = if (enabled) "OpenAI models enabled" else "OpenAI models disabled"
        )
    }

    fun setOpenAiBaseUrl(url: String) {
        val cleaned = url.trim().ifBlank { CardRepository.DEFAULT_OPENAI_BASE_URL }
        repository.saveOpenAiBaseUrl(cleaned)
        _uiState.value = _uiState.value.copy(openAiBaseUrl = cleaned, translationApiUrl = cleaned)
    }

    fun setOpenAiApiKey(apiKey: String) {
        val cleaned = apiKey.trim()
        repository.saveOpenAiApiKey(cleaned)
        _uiState.value = _uiState.value.copy(openAiApiKey = cleaned, translationApiToken = cleaned)
    }

    fun setOpenAiSpeechModel(model: String) {
        val cleaned = model.takeIf { it in OpenAiSpeechModels } ?: CardRepository.DEFAULT_OPENAI_SPEECH_MODEL
        repository.saveOpenAiSpeechModel(cleaned)
        _uiState.value = _uiState.value.copy(openAiSpeechModel = cleaned)
    }

    fun setOpenAiTextModel(model: String) {
        val cleaned = model.takeIf { it in OpenAiTextModels } ?: CardRepository.DEFAULT_OPENAI_TEXT_MODEL
        repository.saveOpenAiTextModel(cleaned)
        _uiState.value = _uiState.value.copy(openAiTextModel = cleaned)
    }

    fun setOpenAiTtsModel(model: String) {
        val cleaned = model.takeIf { it in OpenAiTtsModels } ?: CardRepository.DEFAULT_OPENAI_TTS_MODEL
        repository.saveOpenAiTtsModel(cleaned)
        _uiState.value = _uiState.value.copy(openAiTtsModel = cleaned)
    }

    fun setOpenAiTtsVoice(voice: String) {
        val cleaned = voice.takeIf { it in OpenAiTtsVoices } ?: CardRepository.DEFAULT_OPENAI_TTS_VOICE
        repository.saveOpenAiTtsVoice(cleaned)
        _uiState.value = _uiState.value.copy(openAiTtsVoice = cleaned)
    }

    fun setBelarusianTtsProvider(provider: String) {
        val cleaned = provider.takeIf { it in BelarusianTtsProviders } ?: CardRepository.DEFAULT_BELARUSIAN_TTS_PROVIDER
        repository.saveBelarusianTtsProvider(cleaned)
        _uiState.value = _uiState.value.copy(belarusianTtsProvider = cleaned)
    }

    fun setElevenLabsApiKey(apiKey: String) {
        val cleaned = apiKey.trim()
        repository.saveElevenLabsApiKey(cleaned)
        _uiState.value = _uiState.value.copy(elevenLabsApiKey = cleaned)
    }

    fun setElevenLabsModel(model: String) {
        val cleaned = model.takeIf { it in ElevenLabsModels } ?: CardRepository.DEFAULT_ELEVENLABS_MODEL
        repository.saveElevenLabsModel(cleaned)
        _uiState.value = _uiState.value.copy(elevenLabsModel = cleaned)
    }

    fun setElevenLabsVoiceId(voiceId: String) {
        val cleaned = voiceId.trim().ifBlank { CardRepository.DEFAULT_ELEVENLABS_BELARUSIAN_VOICE_ID }
        repository.saveElevenLabsVoiceId(cleaned)
        _uiState.value = _uiState.value.copy(elevenLabsVoiceId = cleaned)
    }

    fun setElevenLabsTtsLanguageEnabled(languageCode: String, enabled: Boolean) {
        val cleanedCode = languageCode.trim().lowercase(Locale.ROOT)
        if (cleanedCode.isBlank()) return
        val nextCodes = _uiState.value.elevenLabsTtsLanguageCodes.toMutableSet().apply {
            if (enabled) add(cleanedCode) else remove(cleanedCode)
        }
        repository.saveElevenLabsTtsLanguageCodes(nextCodes)
        _uiState.value = _uiState.value.copy(elevenLabsTtsLanguageCodes = repository.loadElevenLabsTtsLanguageCodes())
    }

    fun setOpenAiCacheDurationMinutes(minutes: Long) {
        repository.saveOpenAiCacheDurationMinutes(minutes)
        _uiState.value = _uiState.value.copy(openAiCacheDurationMinutes = repository.loadOpenAiCacheDurationMinutes())
    }

    fun setOpenAiVoiceSilenceTimeoutMs(timeoutMs: Long) {
        repository.saveOpenAiVoiceSilenceTimeoutMs(timeoutMs)
        _uiState.value = _uiState.value.copy(openAiVoiceSilenceTimeoutMs = repository.loadOpenAiVoiceSilenceTimeoutMs())
    }

    fun setCardStatusBlinkIntervalMs(intervalMs: Long) {
        repository.saveCardStatusBlinkIntervalMs(intervalMs)
        _uiState.value = _uiState.value.copy(cardStatusBlinkIntervalMs = repository.loadCardStatusBlinkIntervalMs())
    }

    fun clearOpenAiCache() {
        val deleted = repository.clearOpenAiCache()
        logOpenAiActivity("cache cleared", "$deleted files")
        _uiState.value = _uiState.value.copy(message = "App cache cleared: $deleted files")
    }

    fun logOpenAiActivity(action: String, details: String = "") {
        repository.addOpenAiActivityLog(action, details)
        _uiState.value = _uiState.value.copy(openAiActivityLog = repository.loadOpenAiActivityLog())
    }

    fun appendCurrentCardLog(entry: String) {
        val cardId = _uiState.value.currentCard?.id ?: return
        appendCardLog(cardId, entry)
    }

    fun appendCardLog(cardId: Int, entry: String) {
        val cleanEntry = entry.trim()
        if (cleanEntry.isBlank()) return
        val state = _uiState.value
        val selected = state.selectedLesson ?: return
        val lesson = repository.loadLessons(includeHidden = true).firstOrNull { it.id == selected.id } ?: selected
        val logEntry = "${timestamp()} - $cleanEntry"
        var changed = false
        val updatedCards = lesson.cards.map { card ->
            if (card.id == cardId) {
                changed = true
                card.copy(log = (card.log + logEntry).takeLast(100))
            } else {
                card
            }
        }
        if (!changed) return
        val updatedLesson = lesson.copy(cards = updatedCards, editable = true)
        repository.saveLesson(updatedLesson)
        val visibleLessons = repository.loadLessons(state.showHiddenLessons)
        _uiState.value = state.copy(
            lessons = visibleLessons,
            selectedLesson = if (state.selectedLesson?.id == updatedLesson.id) updatedLesson else state.selectedLesson,
            currentPortion = state.currentPortion.map { card ->
                updatedCards.firstOrNull { it.id == card.id } ?: card
            }
        )
        saveCurrentStudySession()
    }

    fun setOfflineSpeechLanguage(languageTag: String) {
        val normalized = OfflineSpeechLanguages.firstOrNull { it.tag == languageTag }?.tag ?: "en-US"
        repository.saveOfflineSpeechLanguageTag(normalized)
        _uiState.value = _uiState.value.copy(
            offlineSpeechLanguageTag = normalized,
            message = "Speech recognition language: $normalized"
        )
    }

    fun setOfflineSpeechStatus(languageTag: String, status: String, message: String? = null) {
        if (OfflineSpeechLanguages.none { it.tag == languageTag }) return
        repository.saveOfflineSpeechStatus(languageTag, status)
        val nextStatuses = _uiState.value.offlineSpeechStatuses + (languageTag to status)
        _uiState.value = _uiState.value.copy(
            offlineSpeechStatuses = nextStatuses,
            offlineSpeechDownloadingTag = if (status == CardRepository.OFFLINE_SPEECH_STATUS_DOWNLOADING) {
                languageTag
            } else {
                _uiState.value.offlineSpeechDownloadingTag.takeUnless { it == languageTag }
            },
            message = message
        )
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
    private fun defaultBackVisible(card: Flashcard? = _uiState.value.currentCard): Boolean {
        val currentCard = card ?: return _uiState.value.cardStartSide == CardStartSide.TRANSLATION
        val frontMissing = currentCard.nativeText().isMissingCardSide()
        val backMissing = currentCard.correctText().isMissingCardSide()
        return when {
            frontMissing && !backMissing -> true
            backMissing && !frontMissing -> false
            else -> _uiState.value.cardStartSide == CardStartSide.TRANSLATION
        }
    }

    private fun String.isMissingCardSide(): Boolean {
        val clean = trim()
        return clean.isBlank() || clean.isEmptyPlaceholder() || clean.equals("Translation pending", ignoreCase = true)
    }

    private fun String.isEmptyPlaceholder(): Boolean = trim().equals("Empty", ignoreCase = true)

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

    private fun restoredStudySessionState(baseState: StudyUiState, lesson: Lesson): StudyUiState? {
        val session = repository.loadStudySession(lesson.id) ?: return null
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
        if (restoredPortion.isEmpty() && lesson.cards.isNotEmpty()) {
            repository.clearStudySession(lesson.id)
            return null
        }
        val validIds = restoredPortion.map { it.id }.toSet()
        val restoredIndex = session.currentCardId
            .takeIf { it > 0 }
            ?.let { cardId -> restoredPortion.indexOfFirst { it.id == cardId } }
            ?.takeIf { it >= 0 }
            ?: session.currentIndex
        val safeIndex = restoredIndex.coerceIn(0, (restoredPortion.size - 1).coerceAtLeast(0))

        return baseState.copy(
            selectedLesson = lesson,
            selectedLessonIds = emptySet(),
            screen = AppScreen.STUDY,
            mode = mode,
            excludeMasteredCards = session.excludeMasteredCards,
            hideCompletedCards = false,
            currentPortion = restoredPortion,
            currentIndex = safeIndex,
            completedCardIds = session.completedCardIds.filter { it in validIds }.toSet(),
            portionCompletionSaved = session.portionCompletionSaved,
            isBackVisible = defaultBackVisible(restoredPortion.getOrNull(safeIndex)),
            answer = session.answer,
            answerFeedbackVisible = session.answerFeedbackVisible,
            message = null
        )
    }

    private fun restoreStudySession(lesson: Lesson): Boolean {
        val restoredState = restoredStudySessionState(_uiState.value, lesson) ?: return false
        _uiState.value = restoredState
        return true
    }

    private fun newPortionState(baseState: StudyUiState, clearCompleted: Boolean = true): StudyUiState {
        val lesson = baseState.selectedLesson ?: return baseState
        val completedIds = if (clearCompleted) emptySet() else baseState.completedCardIds
        val studyCards = filterStudyCards(lesson.cards, baseState.excludeMasteredCards, baseState.hideCompletedCards, completedIds)
        val cards = orderStudyCards(studyCards, baseState.mode, shuffleRandom = true)

        return baseState.copy(
            currentPortion = cards,
            currentIndex = 0,
            completedCardIds = completedIds,
            portionCompletionSaved = false,
            isBackVisible = defaultBackVisible(cards.firstOrNull()),
            answer = "",
            answerFeedbackVisible = false,
            message = null
        )
    }

    private fun downgradeOpenedNotificationIfNeeded() {
        val state = _uiState.value
        if (!state.openedFromNotification || state.notificationAnswered) return
        val lesson = state.selectedLesson ?: return
        val card = state.currentCard ?: return
        if (card.starCount() != 2) {
            _uiState.value = state.copy(openedFromNotification = false)
            return
        }

        val logEntry = "${timestamp()} - notification opened without answer, stars changed to 1"
        val updatedCards = lesson.cards.map { lessonCard ->
            if (lessonCard.id == card.id) {
                lessonCard.copy(
                    stars = starsForCount(1),
                    log = (lessonCard.log + logEntry).takeLast(100)
                )
            } else {
                lessonCard
            }
        }
        val updatedLesson = lesson.copy(cards = updatedCards, editable = true)
        repository.saveLesson(updatedLesson)
        val lessons = repository.loadLessons(state.showHiddenLessons)
        val savedLesson = lessons.firstOrNull { it.id == lesson.id } ?: updatedLesson
        _uiState.value = state.copy(
            lessons = lessons,
            selectedLesson = savedLesson,
            currentPortion = state.currentPortion.map { portionCard ->
                savedLesson.cards.firstOrNull { it.id == portionCard.id } ?: portionCard
            },
            openedFromNotification = false,
            notificationAnswered = true
        )
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
            "lv" -> "lv"
            "lt" -> "lt"
            "pt" -> "pt"
            "ua", "uk" -> "uk"
            "ru" -> "ru"
            "pl" -> "pl"
            else -> "en"
        }
    }

    private fun interfaceCodeForLanguageName(language: String): String {
        return when (language.trim().lowercase(Locale.ROOT)) {
            "german", "deutsch", "de" -> "de"
            "belarusian", "belarus", "by", "be" -> "be"
            "spanish", "espanol", "es" -> "es"
            "latvian", "latviesu", "lv" -> "lv"
            "lithuanian", "lietuviu", "lt" -> "lt"
            "portuguese", "portugues", "pt" -> "pt"
            "ukrainian", "ua", "uk" -> "uk"
            "russian", "ru" -> "ru"
            "polish", "polski", "pl" -> "pl"
            else -> "en"
        }
    }
    fun openCatalog() {
        downgradeOpenedNotificationIfNeeded()
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
        val state = _uiState.value
        val lessons = repository.loadLessons(state.showHiddenLessons)
        val freshLesson = lessons.firstOrNull { it.id == lesson.id } ?: lesson
        val baseState = state.copy(
            lessons = lessons,
            selectedLesson = freshLesson,
            selectedLessonIds = emptySet(),
            screen = AppScreen.STUDY,
            workMode = if (state.workMode == WorkMode.TESTS) WorkMode.TESTS else WorkMode.CARDS,
            currentIndex = 0,
            answer = "",
            answerFeedbackVisible = false,
            message = null
        )
        _uiState.value = restoredStudySessionState(baseState, freshLesson)
            ?: newPortionState(baseState)
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
        val baseState = _uiState.value.copy(
            lessons = repository.loadLessons(_uiState.value.showHiddenLessons),
            selectedLesson = nextLesson,
            selectedLessonIds = emptySet(),
            screen = AppScreen.STUDY,
            completedCardIds = emptySet(),
            portionCompletionSaved = false,
            cardTransitionDirection = 1,
            answer = "",
            answerFeedbackVisible = false,
            message = null
        )
        _uiState.value = restoredStudySessionState(baseState, nextLesson)
            ?: newPortionState(baseState)
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
            isBackVisible = defaultBackVisible(cards.getOrNull(nextIndex)),
            message = mode.displayLabel()
        )
        saveCurrentStudySession()
    }

    fun startNewPortion() {
        val lesson = _uiState.value.selectedLesson ?: return
        repository.clearStudySession(lesson.id)
        _uiState.value = newPortionState(_uiState.value)
    }


    fun previousCard() {
        val state = _uiState.value
        if (state.currentPortion.isEmpty()) return
        val nextIndex = (state.currentIndex - 1).coerceAtLeast(0)
        _uiState.value = state.copy(
            currentIndex = nextIndex,
            cardTransitionDirection = -1,
            isBackVisible = defaultBackVisible(state.currentPortion.getOrNull(nextIndex)),
            answer = "",
            answerFeedbackVisible = false,
            message = null
        )
        saveCurrentStudySession()
    }

    fun nextCard() {
        val state = _uiState.value
        if (state.currentPortion.isEmpty()) return
        val nextIndex = (state.currentIndex + 1).coerceAtMost(state.currentPortion.lastIndex)
        _uiState.value = state.copy(
            currentIndex = nextIndex,
            cardTransitionDirection = 1,
            isBackVisible = defaultBackVisible(state.currentPortion.getOrNull(nextIndex)),
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
            isBackVisible = defaultBackVisible(state.currentPortion.firstOrNull()),
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
        if (targetIndex < 0) {
            goToLastCompletedCard()
            return
        }
        _uiState.value = state.copy(
            currentIndex = targetIndex,
            cardTransitionDirection = if (targetIndex >= state.currentIndex) 1 else -1,
            isBackVisible = defaultBackVisible(state.currentPortion.getOrNull(targetIndex)),
            answer = "",
            answerFeedbackVisible = false,
            message = null
        )
        saveCurrentStudySession()
    }

    fun goToLastCompletedCard() {
        val state = _uiState.value
        if (state.currentPortion.isEmpty()) return
        val targetIndex = state.currentPortion.lastIndex
        _uiState.value = state.copy(
            currentIndex = targetIndex,
            cardTransitionDirection = if (targetIndex >= state.currentIndex) 1 else -1,
            isBackVisible = defaultBackVisible(state.currentPortion.getOrNull(targetIndex)),
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
        val expected = card.expectedAnswerText(state.isBackVisible)
        if (state.answer.isBlank()) {
            _uiState.value = state.copy(answerFeedbackVisible = false, message = "Enter answer")
        } else if (normalize(state.answer) == normalize(expected)) {
            _uiState.value = state.copy(answerFeedbackVisible = false, message = "Correct")
        } else {
            _uiState.value = state.copy(
                answerFeedbackVisible = true,
                message = if (wrongLetterRatio(state.answer, expected) > 0.5) "Wrong" else null
            )
        }
        saveCurrentStudySession()
    }

    fun checkAnswer() {
        val state = _uiState.value
        val card = state.currentCard ?: return
        val expected = card.expectedAnswerText(state.isBackVisible)

        if (normalize(state.answer) == normalize(expected)) {
            recordCardWork(card.id, "correct")
            addStarForCorrectTypedAnswer(card.id)
            completeCurrentCard(message = "CorrectStar")
        } else {
            recordWrongAnswer(card.id, state.answer)
            _uiState.value = _uiState.value.copy(answerFeedbackVisible = true, message = "Try again")
            saveCurrentStudySession()
        }
    }

    fun submitTestAnswer(choice: String) {
        val state = _uiState.value
        val card = state.currentCard ?: return
        val expected = card.expectedAnswerText(state.isBackVisible)
        if (normalize(choice) == normalize(expected)) {
            recordCardWork(card.id, "test correct")
            addStarForCorrectTypedAnswer(card.id)
            val latest = _uiState.value
            val nextCompletedCardIds = latest.completedCardIds + card.id
            val selectedLesson = latest.selectedLesson
            val currentPortionIds = latest.currentPortion.map { it.id }.toSet()
            val portionComplete = latest.currentPortion.isNotEmpty() &&
                nextCompletedCardIds.intersect(currentPortionIds).size == latest.currentPortion.size
            val shouldSavePortionCompletion = portionComplete && !latest.portionCompletionSaved && selectedLesson != null
            if (shouldSavePortionCompletion) {
                repository.incrementCompletedCount(selectedLesson.id)
                val lessons = repository.loadLessons(latest.showHiddenLessons)
                _uiState.value = latest.copy(
                    lessons = lessons,
                    selectedLesson = lessons.firstOrNull { it.id == selectedLesson.id } ?: selectedLesson,
                    answer = choice,
                    answerFeedbackVisible = false,
                    completedCardIds = nextCompletedCardIds,
                    portionCompletionSaved = true,
                    isBackVisible = !state.isBackVisible,
                    message = "CorrectStar"
                )
            } else {
                _uiState.value = latest.copy(
                    answer = choice,
                    answerFeedbackVisible = false,
                    completedCardIds = nextCompletedCardIds,
                    portionCompletionSaved = latest.portionCompletionSaved || portionComplete,
                    isBackVisible = !state.isBackVisible,
                    message = "CorrectStar"
                )
            }
            saveCurrentStudySession()
        } else {
            recordWrongAnswer(card.id, choice)
            _uiState.value = _uiState.value.copy(
                answer = choice,
                isBackVisible = state.isBackVisible,
                answerFeedbackVisible = true,
                message = "Try again"
            )
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

    fun saveCorrectSideFromAnswer() {
        val state = _uiState.value
        val lesson = state.selectedLesson ?: return
        val card = state.currentCard ?: return
        val cleanAnswer = state.answer.trim()
        if (cleanAnswer.isBlank()) {
            _uiState.value = state.copy(message = "Enter answer", answerFeedbackVisible = false)
            return
        }
        val now = timestamp()
        val updatedCards = lesson.cards.map { lessonCard ->
            if (lessonCard.id == card.id) {
                lessonCard.copy(
                    correctValue = cleanAnswer,
                    log = lessonCard.log + "$now - updated correct side from test edit"
                )
            } else {
                lessonCard
            }
        }
        val updatedCard = updatedCards.firstOrNull { it.id == card.id } ?: card
        val updatedLesson = lesson.copy(cards = updatedCards, editable = true)
        repository.saveLesson(updatedLesson)
        val lessons = repository.loadLessons(state.showHiddenLessons)
        val savedLesson = lessons.firstOrNull { it.id == updatedLesson.id } ?: updatedLesson
        _uiState.value = state.copy(
            lessons = lessons,
            selectedLesson = savedLesson,
            currentPortion = state.currentPortion.map { portionCard ->
                if (portionCard.id == updatedCard.id) updatedCard else portionCard
            },
            isBackVisible = defaultBackVisible(updatedCard),
            answer = "",
            answerFeedbackVisible = false,
            message = "Saved"
        )
        saveCurrentStudySession()
    }
    fun saveVisibleSideFromAnswer(isBackVisible: Boolean) {
        val state = _uiState.value
        val lesson = state.selectedLesson ?: return
        val card = state.currentCard ?: return
        val cleanAnswer = state.answer.trim()
        if (cleanAnswer.isBlank()) {
            _uiState.value = state.copy(message = "Enter answer", answerFeedbackVisible = false)
            return
        }
        val now = timestamp()
        val updatedCards = lesson.cards.map { lessonCard ->
            if (lessonCard.id != card.id) {
                lessonCard
            } else if (isBackVisible) {
                lessonCard.copy(
                    correctValue = cleanAnswer,
                    log = lessonCard.log + "$now - updated visible correct side"
                )
            } else if (lessonCard.kindCode() == "MK") {
                lessonCard.copy(
                    mistake = cleanAnswer,
                    log = lessonCard.log + "$now - updated visible mistake side"
                )
            } else {
                lessonCard.copy(
                    nativeValue = cleanAnswer,
                    log = lessonCard.log + "$now - updated visible native side"
                )
            }
        }
        val updatedCard = updatedCards.firstOrNull { it.id == card.id } ?: card
        val updatedLesson = lesson.copy(cards = updatedCards, editable = true)
        repository.saveLesson(updatedLesson)
        val lessons = repository.loadLessons(state.showHiddenLessons)
        val savedLesson = lessons.firstOrNull { it.id == updatedLesson.id } ?: updatedLesson
        _uiState.value = state.copy(
            lessons = lessons,
            selectedLesson = savedLesson,
            currentPortion = state.currentPortion.map { portionCard ->
                if (portionCard.id == updatedCard.id) updatedCard else portionCard
            },
            isBackVisible = defaultBackVisible(updatedCard),
            answer = "",
            answerFeedbackVisible = false,
            message = "Saved"
        )
        saveCurrentStudySession()
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
            (message == null || message == "Correct" || message == "CorrectStar")

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
                notificationAnswered = state.openedFromNotification || state.notificationAnswered,
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
                notificationAnswered = state.openedFromNotification || state.notificationAnswered,
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
        val state = _uiState.value
        val sourceLanguage = state.activeVocabularySourceLanguage.trim().ifBlank { state.quickVocabularySourceLanguage }
        val targetLanguage = state.activeVocabularyTargetLanguage.trim().ifBlank { state.quickVocabularyTargetLanguage }
        val now = timestamp()
        val lesson = Lesson(
            id = newLessonId(),
            title = "$targetLanguage New lesson",
            lessonInfo = quickVocabularyLessonInfo(sourceLanguage, targetLanguage),
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage,
            cards = listOf(emptyLessonCard(1, sourceLanguage, targetLanguage, now, "Created with new lesson")),
            editable = true
        )
        repository.saveLesson(lesson)
        val lessons = repository.loadLessons(state.showHiddenLessons)
        val savedLesson = lessons.firstOrNull { it.id == lesson.id } ?: lesson
        _uiState.value = state.copy(
            lessons = lessons,
            selectedLesson = savedLesson,
            selectedLessonIds = emptySet(),
            screen = AppScreen.STUDY,
            mode = StudyMode.ORIGINAL,
            currentPortion = savedLesson.cards,
            currentIndex = 0,
            completedCardIds = emptySet(),
            portionCompletionSaved = false,
            isBackVisible = defaultBackVisible(savedLesson.cards.firstOrNull()),
            answer = "",
            answerFeedbackVisible = false,
            message = "New lesson created"
        )
        saveCurrentStudySession()
    }

    fun addEmptyCardToCurrentLesson() {
        val state = _uiState.value
        val lesson = state.selectedLesson ?: return
        val sampleCard = lesson.cards.firstOrNull()
        val sourceLanguage = lesson.sourceLanguage.lessonLanguageOrNull()
            ?: sampleCard?.sourceLanguage.lessonLanguageOrNull()
            ?: state.activeVocabularySourceLanguage.trim().ifBlank { state.quickVocabularySourceLanguage }
        val targetLanguage = lesson.targetLanguage.lessonLanguageOrNull()
            ?: sampleCard?.targetLanguage.lessonLanguageOrNull()
            ?: state.activeVocabularyTargetLanguage.trim().ifBlank { state.quickVocabularyTargetLanguage }
        val now = timestamp()
        val nextCardId = (lesson.cards.maxOfOrNull { it.id } ?: 0) + 1
        val newCard = emptyLessonCard(nextCardId, sourceLanguage, targetLanguage, now, "Created from study plus button")
        val updatedLesson = lesson.copy(cards = lesson.cards + newCard, editable = true)
        repository.saveLesson(updatedLesson)
        repository.clearStudySession(updatedLesson.id)
        val lessons = repository.loadLessons(state.showHiddenLessons)
        val savedLesson = lessons.firstOrNull { it.id == updatedLesson.id } ?: updatedLesson
        val updatedPortion = orderStudyCards(
            filterStudyCards(savedLesson.cards, state.excludeMasteredCards, false, emptySet()),
            StudyMode.ORIGINAL,
            shuffleRandom = false
        )
        val targetIndex = updatedPortion.indexOfFirst { it.id == newCard.id }.takeIf { it >= 0 } ?: 0
        _uiState.value = state.copy(
            lessons = lessons,
            selectedLesson = savedLesson,
            selectedLessonIds = emptySet(),
            mode = StudyMode.ORIGINAL,
            hideCompletedCards = false,
            currentPortion = updatedPortion,
            currentIndex = targetIndex,
            cardTransitionDirection = 1,
            completedCardIds = emptySet(),
            portionCompletionSaved = false,
            isBackVisible = defaultBackVisible(updatedPortion.getOrNull(targetIndex)),
            answer = "",
            answerFeedbackVisible = false,
            message = "Empty card added"
        )
        saveCurrentStudySession()
    }

    fun resetLessonProgress() {
        val state = _uiState.value
        val lesson = state.selectedLesson ?: return
        repository.clearStudySession(lesson.id)
        val now = timestamp()
        val resetCards = lesson.cards.map { card ->
            if (card.stars == 0) {
                card
            } else {
                card.copy(
                    stars = 0,
                    log = (card.log + "$now - stars reset with lesson progress").takeLast(100)
                )
            }
        }
        val updatedLesson = lesson.copy(cards = resetCards, editable = true)
        repository.saveLesson(updatedLesson)
        val cards = orderStudyCards(
            filterStudyCards(updatedLesson.cards, state.excludeMasteredCards, false, emptySet()),
            state.mode,
            shuffleRandom = false
        )
        _uiState.value = state.copy(
            lessons = repository.loadLessons(state.showHiddenLessons),
            selectedLesson = updatedLesson,
            currentPortion = cards,
            currentIndex = 0,
            completedCardIds = emptySet(),
            portionCompletionSaved = false,
            hideCompletedCards = false,
            isBackVisible = defaultBackVisible(cards.firstOrNull()),
            answer = "",
            answerFeedbackVisible = false,
            message = "Progress and stars reset"
        )
        saveCurrentStudySession()
    }

    fun editLesson(lesson: Lesson) {
        _uiState.value = _uiState.value.copy(
            screen = AppScreen.EDITOR,
            selectedLessonIds = emptySet(),
            editorLesson = LessonDraft(
                id = lesson.id,
                title = lesson.title,
                lessonInfo = lesson.lessonInfo,
                sourceLanguage = lesson.sourceLanguage.lessonLanguageOrNull()
                    ?: lesson.cards.firstOrNull()?.sourceLanguage.lessonLanguageOrNull().orEmpty(),
                targetLanguage = lesson.targetLanguage.lessonLanguageOrNull()
                    ?: lesson.cards.firstOrNull()?.targetLanguage.lessonLanguageOrNull().orEmpty(),
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

    fun updateLessonSourceLanguage(language: String) {
        val draft = _uiState.value.editorLesson ?: return
        _uiState.value = _uiState.value.copy(editorLesson = draft.copy(sourceLanguage = language.trim()))
    }

    fun updateLessonTargetLanguage(language: String) {
        val draft = _uiState.value.editorLesson ?: return
        _uiState.value = _uiState.value.copy(editorLesson = draft.copy(targetLanguage = language.trim()))
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
                            type = cardDraft.type.trim().ifBlank { "card" },
                            sourceLanguage = card.sourceLanguage.lessonLanguageOrNull() ?: lesson.sourceLanguage,
                            targetLanguage = card.targetLanguage.lessonLanguageOrNull() ?: lesson.targetLanguage
                        )
                    } else {
                        card.copy(
                            nativeValue = frontText,
                            correctValue = cardDraft.correctValue.trim(),
                            hint = cardDraft.hint.trim(),
                            madeAt = cardDraft.madeAt.trim(),
                            where = cardDraft.where.trim(),
                            type = cardDraft.type.trim().ifBlank { "card" },
                            sourceLanguage = card.sourceLanguage.lessonLanguageOrNull() ?: lesson.sourceLanguage,
                            targetLanguage = card.targetLanguage.lessonLanguageOrNull() ?: lesson.targetLanguage
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
                type = cardDraft.type.trim().ifBlank { "card" },
                sourceLanguage = lesson.sourceLanguage.lessonLanguageOrNull()
                    ?: lesson.cards.firstOrNull()?.sourceLanguage.lessonLanguageOrNull().orEmpty(),
                targetLanguage = lesson.targetLanguage.lessonLanguageOrNull()
                    ?: lesson.cards.firstOrNull()?.targetLanguage.lessonLanguageOrNull().orEmpty()
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
            sourceLanguage = updatedLesson.sourceLanguage,
            targetLanguage = updatedLesson.targetLanguage,
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

    fun deleteCardsFromLesson(cardIds: Set<Int>) {
        val state = _uiState.value
        val lesson = state.editorLesson ?: return
        if (cardIds.isEmpty()) return
        val cards = lesson.cards.filterNot { it.id in cardIds }.reindexCards()
        _uiState.value = state.copy(
            editorLesson = lesson.copy(cards = cards),
            cardDraft = if (state.cardDraft.editingCardId in cardIds) CardDraft() else state.cardDraft,
            message = "Deleted ${cardIds.size} cards"
        )
    }

    fun copyCardsFromEditorToLesson(cardIds: Set<Int>, destinationLessonId: String?) {
        val state = _uiState.value
        val sourceLesson = state.editorLesson ?: return
        val selectedCards = sourceLesson.cards.filter { it.id in cardIds }
        if (selectedCards.isEmpty()) return
        val now = timestamp()
        val destination = destinationLessonId
            ?.let { id -> repository.loadLessons(includeHidden = false).firstOrNull { it.id == id } }
        val updatedDestination = if (destination == null) {
            Lesson(
                id = newLessonId(),
                title = "${sourceLesson.title.ifBlank { "Cards" }} copy",
                lessonInfo = sourceLesson.lessonInfo,
                sourceLanguage = sourceLesson.sourceLanguage,
                targetLanguage = sourceLesson.targetLanguage,
                cards = selectedCards.mapIndexed { index, card ->
                    card.copy(id = index + 1, log = (card.log + "$now - copied from ${sourceLesson.title}").takeLast(100))
                },
                editable = true
            )
        } else {
            val startId = destination.cards.maxOfOrNull { it.id } ?: 0
            destination.copy(
                cards = destination.cards + selectedCards.mapIndexed { index, card ->
                    card.copy(id = startId + index + 1, log = (card.log + "$now - copied from ${sourceLesson.title}").takeLast(100))
                },
                editable = true,
                hidden = false
            )
        }
        repository.saveLesson(updatedDestination)
        _uiState.value = state.copy(
            lessons = repository.loadLessons(state.showHiddenLessons),
            message = "Copied ${selectedCards.size} cards"
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
            sourceLanguage = draft.sourceLanguage.lessonLanguageOrNull()
                ?: draft.cards.firstOrNull()?.sourceLanguage.lessonLanguageOrNull().orEmpty(),
            targetLanguage = draft.targetLanguage.lessonLanguageOrNull()
                ?: draft.cards.firstOrNull()?.targetLanguage.lessonLanguageOrNull().orEmpty(),
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


    private fun emptyLessonCard(id: Int, sourceLanguage: String, targetLanguage: String, now: String, logText: String): Flashcard {
        return Flashcard(
            id = id,
            nativeValue = "Empty",
            correctValue = "Empty",
            hint = "Fill this empty card with voice input or typed text.",
            madeAt = now,
            where = "MurrLex",
            log = listOf("$now - $logText"),
            type = "card",
            cardKind = "LN",
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage
        )
    }

    fun addQuickVocabularyCard(phrase: String, recognitionLog: String = "") {
        val cleanPhrase = phrase.trim()
        if (cleanPhrase.isBlank()) {
            _uiState.value = _uiState.value.copy(message = "No speech recognized")
            return
        }
        val state = _uiState.value
        val sourceLanguage = state.activeVocabularySourceLanguage.trim().ifBlank { state.quickVocabularySourceLanguage }
        val targetLanguage = state.activeVocabularyTargetLanguage.trim().ifBlank { state.quickVocabularyTargetLanguage }
        val isMistakeCard = freeOnlineLanguageCode(sourceLanguage).equals(freeOnlineLanguageCode(targetLanguage), ignoreCase = true)
        val lessonTitle = quickVocabularyLessonTitle(sourceLanguage, targetLanguage, createdAt = displayTimestamp())
        val visibleLessonsForQuickVocabulary = repository.loadLessons(includeHidden = false)
        val existingLesson = visibleLessonsForQuickVocabulary
            .filter { lesson ->
                lesson.matchesQuickVocabularyPair(sourceLanguage, targetLanguage)
            }
            .maxByOrNull { lesson -> lesson.cards.firstOrNull()?.madeAt ?: "" }
        val lessonId = existingLesson?.id
            ?: "${QUICK_VOCABULARY_LESSON_ID}_${sourceLanguage.safeIdPart()}_${targetLanguage.safeIdPart()}_${UUID.randomUUID()}"
        val now = timestamp()
        val newCard = Flashcard(
            id = 1,
            nativeValue = if (isMistakeCard) "" else cleanPhrase,
            correctValue = "Empty",
            hint = if (isMistakeCard) {
                "Captured as a same-language mistake. Correction pending."
            } else {
                "Captured by voice in the Basic language. Fill the Target language side later."
            },
            madeAt = now,
            where = "Quick vocabulary microphone",
            log = buildList {
                add("$now - captured by quick vocabulary microphone as $sourceLanguage")
                if (isMistakeCard) add("$now - Basic and Target languages matched; created as Mistake card")
                recognitionLog.trim().takeIf { it.isNotBlank() }?.let { add("$now - $it") }
            },
            mistake = if (isMistakeCard) cleanPhrase else "",
            wrongAnswers = if (isMistakeCard) listOf(MistakeRecord(cleanPhrase, now)) else emptyList(),
            type = "card",
            cardKind = if (isMistakeCard) "MK" else "LN",
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage
        )
        val updatedLesson = if (existingLesson == null) {
            Lesson(
                id = lessonId,
                title = lessonTitle,
                lessonInfo = quickVocabularyLessonInfo(sourceLanguage, targetLanguage),
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                cards = listOf(newCard).reindexCards(),
                editable = true
            )
        } else {
            existingLesson.copy(
                title = existingLesson.title.ifBlank { lessonTitle },
                lessonInfo = quickVocabularyLessonInfo(sourceLanguage, targetLanguage),
                sourceLanguage = existingLesson.sourceLanguage.lessonLanguageOrNull() ?: sourceLanguage,
                targetLanguage = existingLesson.targetLanguage.lessonLanguageOrNull() ?: targetLanguage,
                cards = (listOf(newCard) + existingLesson.cards).reindexCards(),
                editable = true,
                hidden = false
            )
        }
        repository.saveLesson(updatedLesson)
        repository.clearStudySession(updatedLesson.id)
        val visibleLessons = repository.loadLessons(state.showHiddenLessons)
        val savedSelectedLesson = state.selectedLesson?.let { selected ->
            visibleLessons.firstOrNull { it.id == selected.id }
        } ?: state.selectedLesson
        _uiState.value = state.copy(
            lessons = visibleLessons,
            selectedLesson = savedSelectedLesson,
            selectedLessonIds = emptySet(),
            message = "Added: $cleanPhrase"
        )
        if (isMistakeCard && state.shouldUseOpenAiOnline()) {
            correctMistakeCardWithOpenAi(
                lessonId = updatedLesson.id,
                cardId = 1,
                phrase = cleanPhrase,
                language = sourceLanguage,
                requestState = state
            )
            return
        }
        if (!state.useLocalTranslation || state.shouldUseOpenAiOnline()) {
            translateQuickVocabularyCard(
                lessonId = updatedLesson.id,
                cardId = 1,
                phrase = cleanPhrase,
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage
            )
        }
    }

    fun generateTrainCardsFromMistake(cardId: Int, options: List<String>) {
        val selectedOptions = options.map { it.trim() }.filter { it.isNotBlank() }.distinct().take(10)
        if (selectedOptions.isEmpty()) {
            _uiState.value = _uiState.value.copy(message = "Select at least one option")
            return
        }
        val state = _uiState.value
        val selectedLesson = state.selectedLesson ?: return
        val sourceCard = selectedLesson.cards.firstOrNull { it.id == cardId } ?: return
        if (sourceCard.kindCode() != "MK") {
            _uiState.value = state.copy(message = "Train cards can be generated only from Mistake cards")
            return
        }
        if (!state.shouldUseOpenAiOnline()) {
            _uiState.value = state.copy(message = "OpenAI online model is required for Train cards")
            return
        }
        viewModelScope.launch {
            val requestState = _uiState.value
            val drafts = runCatching {
                requestOpenAiTrainCards(
                    sourceCard = sourceCard,
                    options = selectedOptions,
                    interfaceLanguage = requestState.interfaceLanguage,
                    baseUrl = requestState.openAiBaseUrl,
                    apiKey = requestState.openAiApiKey,
                    model = requestState.openAiTextModel
                )
            }.getOrNull().orEmpty()
            if (drafts.isEmpty()) {
                _uiState.value = _uiState.value.copy(message = "Train card generation failed")
                return@launch
            }
            val lesson = repository.loadLessons(includeHidden = true).firstOrNull { it.id == selectedLesson.id } ?: selectedLesson
            val now = timestamp()
            val sameLanguage = sourceCard.sourceLanguage.ifBlank { sourceCard.targetLanguage }
            val newCards = drafts.take(10).mapIndexed { index, draft ->
                Flashcard(
                    id = lesson.cards.size + index + 1,
                    nativeValue = draft.front,
                    correctValue = draft.back,
                    hint = draft.hint.take(1_200),
                    madeAt = now,
                    where = "Generated from Mistake card ${sourceCard.id}",
                    log = listOf("$now - Train card generated from Mistake card ${sourceCard.id} with OpenAI ${requestState.openAiTextModel}; option: ${draft.option}"),
                    type = "card",
                    cardKind = "TR",
                    sourceLanguage = sameLanguage,
                    targetLanguage = sameLanguage
                )
            }
            val updatedCards = lesson.cards.map { card ->
                if (card.id == sourceCard.id) {
                    card.copy(
                        log = (card.log + "$now - generated ${newCards.size} Train cards with OpenAI ${requestState.openAiTextModel}").takeLast(100)
                    )
                } else {
                    card
                }
            } + newCards
            val updatedLesson = lesson.copy(cards = updatedCards, editable = true, hidden = false)
            repository.saveLesson(updatedLesson)
            val lessons = repository.loadLessons(requestState.showHiddenLessons)
            val savedLesson = lessons.firstOrNull { it.id == updatedLesson.id } ?: updatedLesson
            _uiState.value = requestState.copy(
                lessons = lessons,
                selectedLesson = savedLesson,
                currentPortion = if (requestState.selectedLesson?.id == updatedLesson.id &&
                    requestState.currentPortion.any { it.id == sourceCard.id }
                ) {
                    requestState.currentPortion.map { portionCard ->
                        updatedCards.firstOrNull { it.id == portionCard.id } ?: portionCard
                    } + newCards
                } else {
                    requestState.currentPortion
                },
                message = "Train cards added: ${newCards.size}"
            )
            saveCurrentStudySession()
            logOpenAiActivity(
                action = "train cards API",
                details = "${requestState.openAiTextModel}; from Mistake card ${sourceCard.id}; ${newCards.size} cards"
            )
        }
    }

    fun applyQuickVocabularyTranslation(
        phrase: String,
        translated: String,
        sourceLanguage: String,
        targetLanguage: String,
        attribution: String = GOOGLE_TRANSLATE_ATTRIBUTION,
        providerLabel: String = "Google Translator"
    ) {
        val cleanPhrase = phrase.trim()
        val cleanTranslation = translated.trim()
        if (cleanPhrase.isBlank() || cleanTranslation.isBlank()) return
        val state = _uiState.value
        val lessonsAll = repository.loadLessons(includeHidden = true)
        val lesson = lessonsAll
            .filter { lesson ->
                lesson.matchesQuickVocabularyPair(sourceLanguage, targetLanguage) &&
                    lesson.cards.any { card ->
                        card.nativeText().trim().equals(cleanPhrase, ignoreCase = true) &&
                            card.sourceLanguage.equals(sourceLanguage, ignoreCase = true) &&
                            card.targetLanguage.equals(targetLanguage, ignoreCase = true)
                    }
            }
            .maxByOrNull { lesson -> lesson.cards.firstOrNull()?.madeAt ?: "" }
            ?: return
        var updated = false
        val now = timestamp()
        val updatedCards = lesson.cards.map { card ->
            if (!updated &&
                card.nativeText().trim().equals(cleanPhrase, ignoreCase = true) &&
                card.sourceLanguage.equals(sourceLanguage, ignoreCase = true) &&
                card.targetLanguage.equals(targetLanguage, ignoreCase = true) &&
                card.correctText().isMissingCardSide()
            ) {
                updated = true
                card.copy(
                    correctValue = cleanTranslation,
                    hint = card.hint.withTranslationAttribution(attribution),
                    log = (card.log + "$now - translated automatically with $providerLabel").takeLast(100)
                )
            } else {
                card
            }
        }
        if (!updated) return
        val updatedLesson = lesson.copy(cards = updatedCards, editable = true, hidden = false)
        repository.saveLesson(updatedLesson)
        val visibleLessons = repository.loadLessons(state.showHiddenLessons)
        val selectedLesson = state.selectedLesson?.let { selected ->
            if (selected.id == updatedLesson.id) updatedLesson else visibleLessons.firstOrNull { it.id == selected.id } ?: selected
        }
        _uiState.value = state.copy(
            lessons = visibleLessons,
            selectedLesson = selectedLesson,
            currentPortion = if (state.selectedLesson?.id == updatedLesson.id) {
                state.currentPortion.map { portionCard -> updatedCards.firstOrNull { it.id == portionCard.id } ?: portionCard }
            } else {
                state.currentPortion
            },
            message = "${providerLabel}: $cleanPhrase"
        )
        saveCurrentStudySession()
    }

    fun applyTranslationToCard(
        cardId: Int,
        translated: String,
        targetBackSide: Boolean,
        attribution: String = GOOGLE_TRANSLATE_ATTRIBUTION,
        providerLabel: String = "Google Translator",
        insightText: String = ""
    ) {
        val cleanTranslation = translated.trim()
        if (cleanTranslation.isBlank()) return
        val state = _uiState.value
        val selected = state.selectedLesson ?: return
        val lesson = repository.loadLessons(includeHidden = true).firstOrNull { it.id == selected.id } ?: selected
        val now = timestamp()
        var updatedCard: Flashcard? = null
        val updatedCards = lesson.cards.map { card ->
            if (card.id == cardId) {
                val updated = if (targetBackSide) {
                    card.copy(
                        correctValue = cleanTranslation,
                        hint = card.hint.withTranslationInsight(insightText, attribution),
                    log = (card.log + "$now - correct side translated with $providerLabel").takeLast(100)
                    )
                } else {
                    card.copy(
                        nativeValue = cleanTranslation,
                        hint = card.hint.withTranslationInsight(insightText, attribution),
                    log = (card.log + "$now - native side translated with $providerLabel").takeLast(100)
                    )
                }
                updatedCard = updated
                updated
            } else {
                card
            }
        }
        val updatedLesson = lesson.copy(cards = updatedCards, editable = true)
        repository.saveLesson(updatedLesson)
        val lessons = repository.loadLessons(state.showHiddenLessons)
        val savedLesson = lessons.firstOrNull { it.id == updatedLesson.id } ?: updatedLesson
        _uiState.value = state.copy(
            lessons = lessons,
            selectedLesson = savedLesson,
            currentPortion = state.currentPortion.map { portionCard ->
                if (portionCard.id == cardId) updatedCard ?: portionCard else portionCard
            },
            answer = "",
            answerFeedbackVisible = false,
            message = providerLabel
        )
        saveCurrentStudySession()
    }

    fun translateCardTextIntoAnswer(text: String, sourceLanguage: String, targetLanguage: String) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) {
            _uiState.value = _uiState.value.copy(message = "Nothing to translate")
            return
        }
        viewModelScope.launch {
            val requestState = _uiState.value
            val result = runCatching {
                requestConfiguredOnlineTranslation(cleanText, sourceLanguage, targetLanguage, requestState)
            }.getOrNull() ?: OpenAiTextResult("", "")
            val translated = result.text.trim()
            if (translated.isBlank()) {
                _uiState.value = _uiState.value.copy(message = "Translation failed")
                return@launch
            }
            if (result.isOpenAiSource()) {
                logOpenAiActivity(
                    action = "card translation ${result.source}",
                    details = "${requestState.openAiTextModel}: $sourceLanguage -> $targetLanguage; $cleanText => $translated"
                )
            }
            _uiState.value = _uiState.value.copy(
                answer = translated,
                message = result.providerLabel()
            )
        }
    }

    fun translateAndApplyCardText(
        cardId: Int,
        text: String,
        sourceLanguage: String,
        targetLanguage: String,
        targetBackSide: Boolean
    ) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) return
        viewModelScope.launch {
            val requestState = _uiState.value
            val openAiPreferred = requestState.shouldUseOpenAiOnline()
            val insight = if (openAiPreferred) {
                runCatching {
                    requestOpenAiCardTranslationInsight(
                        phrase = cleanText,
                        sourceLanguage = sourceLanguage,
                        targetLanguage = targetLanguage,
                        interfaceLanguage = requestState.interfaceLanguage,
                        baseUrl = requestState.openAiBaseUrl,
                        apiKey = requestState.openAiApiKey,
                        model = requestState.openAiTextModel
                    )
                }.getOrNull()
            } else {
                null
            }
            val result = insight?.let { CardTranslationInsightResult(it.translation, it.source) }
                ?: runCatching {
                    val plain = requestConfiguredOnlineTranslation(cleanText, sourceLanguage, targetLanguage, requestState)
                    CardTranslationInsightResult(plain.text, plain.source)
                }.getOrNull()
                ?: CardTranslationInsightResult("", "")
            val translated = result.text.trim()
            if (translated.isBlank()) {
                _uiState.value = _uiState.value.copy(message = "Translation pending")
                return@launch
            }
            if (result.isOpenAiSource()) {
                logOpenAiActivity(
                    action = "card auto-translation ${result.source}",
                    details = "${requestState.openAiTextModel}: $sourceLanguage -> $targetLanguage; $cleanText => $translated"
                )
            }
            applyTranslationToCard(
                cardId = cardId,
                translated = translated,
                targetBackSide = targetBackSide,
                attribution = result.googleAttribution(),
                providerLabel = result.providerLabel(requestState.openAiTextModel),
                insightText = insight?.toCardHint().orEmpty()
            )
        }
    }


    private fun saveTranslatedCardFromTranslator(originalText: String, translatedText: String) {
        val state = _uiState.value
        val sourceLanguage = state.activeVocabularySourceLanguage.trim().ifBlank { state.quickVocabularySourceLanguage }
        val targetLanguage = state.activeVocabularyTargetLanguage.trim().ifBlank { state.quickVocabularyTargetLanguage }
        val lessonTitle = quickVocabularyLessonTitle(sourceLanguage, targetLanguage, createdAt = displayTimestamp())
        val visibleLessonsForQuickVocabulary = repository.loadLessons(includeHidden = false)
        val existingLesson = visibleLessonsForQuickVocabulary
            .filter { lesson ->
                lesson.matchesQuickVocabularyPair(sourceLanguage, targetLanguage)
            }
            .maxByOrNull { lesson -> lesson.cards.firstOrNull()?.madeAt ?: "" }
        val lessonId = existingLesson?.id
            ?: "${QUICK_VOCABULARY_LESSON_ID}_${sourceLanguage.safeIdPart()}_${targetLanguage.safeIdPart()}_${UUID.randomUUID()}"
        val now = timestamp()
        val providerLabel = if (state.translationAttribution.contains("Google", ignoreCase = true)) {
            "Google Translator"
        } else {
            "Translator"
        }
        var cardCreated = false
        val newCard = Flashcard(
            id = 1,
            nativeValue = originalText,
            correctValue = translatedText,
            hint = buildTranslatorHint("Created automatically from Translate mode.", state.translationAttribution),
            madeAt = now,
            where = "Translate mode",
            log = listOf("$now - created automatically from Translate mode with $providerLabel"),
            type = "card",
            cardKind = "LN",
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage
        )
        val updatedLesson = if (existingLesson == null) {
            cardCreated = true
            Lesson(
                id = lessonId,
                title = lessonTitle,
                lessonInfo = quickVocabularyLessonInfo(sourceLanguage, targetLanguage),
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                cards = listOf(newCard).reindexCards(),
                editable = true
            )
        } else {
            var updatedExisting = false
            val updatedCards = existingLesson.cards.map { card ->
                if (!updatedExisting && card.nativeText().trim().equals(originalText, ignoreCase = true)) {
                    updatedExisting = true
                    if (card.correctText().trim() == translatedText) {
                        card
                    } else {
                        card.copy(
                            correctValue = translatedText,
                            hint = card.hint.withTranslationAttribution(state.translationAttribution),
                            log = (card.log + "$now - translation updated automatically with $providerLabel").takeLast(100)
                        )
                    }
                } else {
                    card
                }
            }
            val nextCards = if (updatedExisting) {
                updatedCards
            } else {
                cardCreated = true
                (listOf(newCard) + existingLesson.cards).reindexCards()
            }
            existingLesson.copy(
                title = existingLesson.title.ifBlank { lessonTitle },
                lessonInfo = quickVocabularyLessonInfo(sourceLanguage, targetLanguage),
                sourceLanguage = existingLesson.sourceLanguage.lessonLanguageOrNull() ?: sourceLanguage,
                targetLanguage = existingLesson.targetLanguage.lessonLanguageOrNull() ?: targetLanguage,
                cards = nextCards,
                editable = true,
                hidden = false
            )
        }
        repository.saveLesson(updatedLesson)
        if (cardCreated) repository.clearStudySession(updatedLesson.id)
        val currentState = _uiState.value
        val visibleLessons = repository.loadLessons(currentState.showHiddenLessons)
        val selectedLesson = currentState.selectedLesson?.let { selected ->
            visibleLessons.firstOrNull { it.id == selected.id }
        } ?: currentState.selectedLesson
        _uiState.value = currentState.copy(
            lessons = visibleLessons,
            selectedLesson = selectedLesson,
            selectedLessonIds = emptySet(),
            screen = AppScreen.TRANSLATE,
            message = if (cardCreated) "Card created" else "Card updated"
        )
    }
    fun addTranslationCard() {
        val state = _uiState.value
        val originalText = state.translationInput.trim()
        if (originalText.isBlank()) {
            _uiState.value = state.copy(message = "Enter text")
            return
        }
        val translatedText = state.translationOutput.trim().ifBlank { "Empty" }
        val sourceLanguage = state.activeVocabularySourceLanguage.trim().ifBlank { state.quickVocabularySourceLanguage }
        val targetLanguage = state.activeVocabularyTargetLanguage.trim().ifBlank { state.quickVocabularyTargetLanguage }
        val lessonTitle = quickVocabularyLessonTitle(sourceLanguage, targetLanguage, createdAt = displayTimestamp())
        val visibleLessonsForQuickVocabulary = repository.loadLessons(includeHidden = false)
        val existingLesson = visibleLessonsForQuickVocabulary
            .filter { lesson ->
                lesson.matchesQuickVocabularyPair(sourceLanguage, targetLanguage)
            }
            .maxByOrNull { lesson -> lesson.cards.firstOrNull()?.madeAt ?: "" }
        val lessonId = existingLesson?.id
            ?: "${QUICK_VOCABULARY_LESSON_ID}_${sourceLanguage.safeIdPart()}_${targetLanguage.safeIdPart()}_${UUID.randomUUID()}"
        val now = timestamp()
        val newCard = Flashcard(
            id = 1,
            nativeValue = originalText,
            correctValue = translatedText,
            hint = if (translatedText == "Empty") {
                "Created from Translate mode. Translation is pending."
            } else {
                buildTranslatorHint(
                    "Created from Translate mode. Basic: $sourceLanguage. Target: $targetLanguage.",
                    state.translationAttribution
                )
            },
            madeAt = now,
            where = "Translate mode",
            log = listOf("$now - created from Translate mode as $sourceLanguage to $targetLanguage"),
            type = "card",
            cardKind = "LN",
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage
        )
        val updatedLesson = if (existingLesson == null) {
            Lesson(
                id = lessonId,
                title = lessonTitle,
                lessonInfo = quickVocabularyLessonInfo(sourceLanguage, targetLanguage),
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                cards = listOf(newCard).reindexCards(),
                editable = true
            )
        } else {
            existingLesson.copy(
                title = existingLesson.title.ifBlank { lessonTitle },
                lessonInfo = quickVocabularyLessonInfo(sourceLanguage, targetLanguage),
                sourceLanguage = existingLesson.sourceLanguage.lessonLanguageOrNull() ?: sourceLanguage,
                targetLanguage = existingLesson.targetLanguage.lessonLanguageOrNull() ?: targetLanguage,
                cards = (listOf(newCard) + existingLesson.cards).reindexCards(),
                editable = true,
                hidden = false
            )
        }
        repository.saveLesson(updatedLesson)
        repository.clearStudySession(updatedLesson.id)
        val visibleLessons = repository.loadLessons(state.showHiddenLessons)
        val savedSelectedLesson = state.selectedLesson?.let { selected ->
            visibleLessons.firstOrNull { it.id == selected.id }
        } ?: state.selectedLesson
        _uiState.value = state.copy(
            lessons = visibleLessons,
            selectedLesson = savedSelectedLesson,
            selectedLessonIds = emptySet(),
            message = if (translatedText == "Empty") {
                "Added to ${quickVocabularyLessonShortTitle(sourceLanguage, targetLanguage)}: $originalText. Translation pending"
            } else {
                "Added to ${quickVocabularyLessonShortTitle(sourceLanguage, targetLanguage)}: $originalText"
            }
        )
    }

    fun importSharedPostCards(sharedText: String) {
        val cleanText = sharedText.trim().limitWords(100)
        if (cleanText.isBlank()) {
            _uiState.value = _uiState.value.copy(message = "Shared post is empty")
            return
        }
        val state = _uiState.value
        val sourceLanguage = state.activeVocabularySourceLanguage.trim().ifBlank { state.quickVocabularySourceLanguage }
        val targetLanguage = state.activeVocabularyTargetLanguage.trim().ifBlank { state.quickVocabularyTargetLanguage }
        viewModelScope.launch {
            val requestState = _uiState.value
            val drafts = if (requestState.shouldUseOpenAiOnline()) {
                runCatching {
                    requestOpenAiSharedPostCards(
                        postText = cleanText,
                        sourceLanguage = sourceLanguage,
                        targetLanguage = targetLanguage,
                        interfaceLanguage = requestState.interfaceLanguage,
                        baseUrl = requestState.openAiBaseUrl,
                        apiKey = requestState.openAiApiKey,
                        model = requestState.openAiTextModel
                    )
                }.getOrNull().orEmpty()
            } else {
                buildSharedPostCardsWithTranslator(cleanText, sourceLanguage, targetLanguage, requestState)
            }
            if (drafts.isEmpty()) {
                _uiState.value = _uiState.value.copy(message = "Shared post import failed")
                return@launch
            }
            val now = timestamp()
            val displayNow = displayTimestamp()
            val lesson = Lesson(
                id = "${SHARED_POST_LESSON_ID}_${sourceLanguage.safeIdPart()}_${targetLanguage.safeIdPart()}_${UUID.randomUUID()}",
                title = "${sourceLanguage.shortLanguageCode()} - ${targetLanguage.shortLanguageCode()} Shared post $displayNow",
                lessonInfo = "Imported from a shared post. Basic/Native language: $sourceLanguage. Target/Learning language: $targetLanguage. Source text was limited to 100 words.",
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                cards = drafts.mapIndexed { index, draft ->
                    Flashcard(
                        id = index + 1,
                        nativeValue = draft.front,
                        correctValue = draft.back,
                        hint = draft.hint.ifBlank {
                            if (draft.mode.equals("target_vocabulary", ignoreCase = true)) {
                                "Shared post was treated as Target-language vocabulary."
                            } else {
                                "Shared post sentence translated into the Target language."
                            }
                        },
                        madeAt = now,
                        where = "Shared post import",
                        log = listOf("$now - imported from shared post as ${draft.mode.ifBlank { "sentence_translation" }}"),
                        type = "card",
                        cardKind = "LN",
                        sourceLanguage = sourceLanguage,
                        targetLanguage = targetLanguage
                    )
                },
                editable = true
            )
            repository.saveLesson(lesson)
            repository.clearStudySession(lesson.id)
            val lessons = repository.loadLessons(requestState.showHiddenLessons)
            val savedLesson = lessons.firstOrNull { it.id == lesson.id } ?: lesson
            _uiState.value = requestState.copy(
                lessons = lessons,
                selectedLesson = savedLesson,
                selectedLessonIds = emptySet(),
                screen = AppScreen.STUDY,
                workMode = WorkMode.CARDS,
                currentPortion = savedLesson.cards.take(PORTION_SIZE),
                currentIndex = 0,
                completedCardIds = emptySet(),
                portionCompletionSaved = false,
                isBackVisible = false,
                answer = "",
                message = "Shared post cards: ${savedLesson.cards.size}"
            )
            saveCurrentStudySession()
            if (requestState.shouldUseOpenAiOnline()) {
                logOpenAiActivity(
                    action = "shared post cards API",
                    details = "${requestState.openAiTextModel}; $sourceLanguage -> $targetLanguage; ${savedLesson.cards.size} cards"
                )
            }
        }
    }

    private fun translateQuickVocabularyCard(
        lessonId: String,
        cardId: Int,
        phrase: String,
        sourceLanguage: String,
        targetLanguage: String
    ) {
        viewModelScope.launch {
            val requestState = _uiState.value
            val result = runCatching {
                requestConfiguredOnlineTranslation(phrase, sourceLanguage, targetLanguage, requestState)
            }.getOrNull() ?: OpenAiTextResult("", "")
            val translated = result.text.trim()
            if (translated.isBlank()) {
                _uiState.value = _uiState.value.copy(message = "Added: $phrase. Target translation pending")
                return@launch
            }
            if (result.isOpenAiSource()) {
                logOpenAiActivity(
                    action = "quick vocabulary translation ${result.source}",
                    details = "${requestState.openAiTextModel}: $sourceLanguage -> $targetLanguage; $phrase => $translated"
                )
            }
            val lesson = repository.loadLessons(includeHidden = true).firstOrNull { it.id == lessonId }
                ?: return@launch
            val now = timestamp()
            val updatedLesson = lesson.copy(
                cards = lesson.cards.map { card ->
                    if (card.id == cardId) {
                        card.copy(
                            correctValue = translated,
                            hint = buildTranslatorHint(
                                "Captured by voice in the Basic language and translated automatically into the Target side.",
                                result.googleAttribution()
                            ),
                            log = (card.log + "$now - translated automatically from $sourceLanguage to $targetLanguage with ${result.providerLabel()}").takeLast(100)
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
                message = "${result.providerLabel()}: $phrase"
            )
        }
    }

    private fun correctMistakeCardWithOpenAi(
        lessonId: String,
        cardId: Int,
        phrase: String,
        language: String,
        requestState: StudyUiState
    ) {
        viewModelScope.launch {
            val insight = runCatching {
                requestOpenAiMistakeCorrection(
                    phrase = phrase,
                    language = language,
                    interfaceLanguage = requestState.interfaceLanguage,
                    baseUrl = requestState.openAiBaseUrl,
                    apiKey = requestState.openAiApiKey,
                    model = requestState.openAiTextModel
                )
            }.getOrNull()
            val corrected = insight?.corrected.orEmpty().trim()
            if (corrected.isBlank()) {
                _uiState.value = _uiState.value.copy(message = "Mistake card added. Correction pending")
                return@launch
            }
            val correction = insight ?: return@launch
            val lesson = repository.loadLessons(includeHidden = true).firstOrNull { it.id == lessonId } ?: return@launch
            val now = timestamp()
            val updatedCards = lesson.cards.map { card ->
                if (card.id == cardId) {
                    card.copy(
                        correctValue = corrected,
                        hint = correction.toMistakeHint(requestState.openAiTextModel),
                        log = (card.log + "$now - same-language mistake corrected with OpenAI ${requestState.openAiTextModel}").takeLast(100)
                    )
                } else {
                    card
                }
            }
            val updatedLesson = lesson.copy(cards = updatedCards, editable = true, hidden = false)
            repository.saveLesson(updatedLesson)
            val currentState = _uiState.value
            val lessons = repository.loadLessons(currentState.showHiddenLessons)
            val savedLesson = lessons.firstOrNull { it.id == updatedLesson.id } ?: updatedLesson
            _uiState.value = currentState.copy(
                lessons = lessons,
                selectedLesson = if (currentState.selectedLesson?.id == updatedLesson.id) savedLesson else currentState.selectedLesson,
                currentPortion = currentState.currentPortion.map { portionCard ->
                    updatedCards.firstOrNull { it.id == portionCard.id } ?: portionCard
                },
                message = "Mistake corrected with OpenAI"
            )
            saveCurrentStudySession()
            logOpenAiActivity(
                action = "mistake correction API",
                details = "${requestState.openAiTextModel}: $language; ${phrase.take(180)} => ${corrected.take(180)}"
            )
        }
    }

    private suspend fun requestConfiguredOnlineTranslation(
        phrase: String,
        sourceLanguage: String,
        targetLanguage: String,
        state: StudyUiState
    ): OpenAiTextResult {
        return if (state.shouldUseOpenAiOnline()) {
            requestOpenAiTranslation(
                phrase = phrase,
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                baseUrl = state.openAiBaseUrl,
                apiKey = state.openAiApiKey,
                model = state.openAiTextModel
            )
        } else {
            OpenAiTextResult(
                text = requestFreeOnlineTranslation(phrase, sourceLanguage, targetLanguage),
                source = "Google API"
            )
        }
    }

    private suspend fun requestFreeOnlineTranslation(
        phrase: String,
        sourceLanguage: String,
        targetLanguage: String
    ): String = withContext(Dispatchers.IO) {
        val sourceCode = freeOnlineLanguageCode(sourceLanguage)
        val targetCode = freeOnlineLanguageCode(targetLanguage)
        val encodedText = URLEncoder.encode(phrase, Charsets.UTF_8.name())
        val url = URL(
            "https://translate.googleapis.com/translate_a/single" +
                "?client=gtx&sl=$sourceCode&tl=$targetCode&dt=t&q=$encodedText"
        )
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12_000
            readTimeout = 20_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "MurrLex/${BuildConfig.VERSION_NAME}")
        }
        val status = connection.responseCode
        val responseText = (if (status in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { it.readText() }
            .orEmpty()
        connection.disconnect()
        if (status !in 200..299) return@withContext ""
        val chunks = JSONArray(responseText).optJSONArray(0) ?: return@withContext ""
        buildString {
            for (index in 0 until chunks.length()) {
                append(chunks.optJSONArray(index)?.optString(0).orEmpty())
            }
        }.trim()
    }

    private suspend fun requestOpenAiTranslation(
        phrase: String,
        sourceLanguage: String,
        targetLanguage: String,
        baseUrl: String,
        apiKey: String,
        model: String
    ): OpenAiTextResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext OpenAiTextResult("", "")
        repository.getCachedOpenAiText(
            task = "translation",
            model = model,
            input = phrase,
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage
        )?.let { return@withContext OpenAiTextResult(it, "cache") }
        val endpoint = baseUrl.trimEnd('/') + "/responses"
        val prompt = "Translate the text from $sourceLanguage to $targetLanguage. Return only the translation, no quotes, no commentary.\n\nText:\n$phrase"
        val payload = JSONObject()
            .put("model", model)
            .put("input", prompt)
            .toString()
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 45_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "MurrLex/${BuildConfig.VERSION_NAME}")
        }
        connection.outputStream.use { stream ->
            stream.write(payload.toByteArray(Charsets.UTF_8))
        }
        val status = connection.responseCode
        val responseText = (if (status in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { it.readText() }
            .orEmpty()
        connection.disconnect()
        if (status !in 200..299) return@withContext OpenAiTextResult("", "")
        val parsed = parseOpenAiTextResponse(responseText)
        if (parsed.isNotBlank()) {
            repository.saveOpenAiTextCache(
                task = "translation",
                model = model,
                input = phrase,
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                output = parsed
            )
        }
        OpenAiTextResult(parsed, "API")
    }

    private suspend fun requestOpenAiCardTranslationInsight(
        phrase: String,
        sourceLanguage: String,
        targetLanguage: String,
        interfaceLanguage: String,
        baseUrl: String,
        apiKey: String,
        model: String
    ): CardTranslationInsight = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext CardTranslationInsight("", "", "", emptyList(), "")
        val cacheInput = "card-insight:$interfaceLanguage:$phrase"
        repository.getCachedOpenAiText(
            task = "card_translation_insight",
            model = model,
            input = cacheInput,
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage
        )?.let { cached ->
            return@withContext parseCardTranslationInsight(cached).copy(source = "cache")
        }
        val endpoint = baseUrl.trimEnd('/') + "/responses"
        val prompt = """
            Translate the text from $sourceLanguage to $targetLanguage for a language-learning flashcard.
            Return strict JSON only with keys: translation, explanation, rule, examples.
            explanation, rule, and examples must be in the user's interface language: $interfaceLanguage.
            Keep all explanation/rule/examples together short enough to read in under one minute.
            examples must be an array of 2 short examples.

            Text:
            $phrase
        """.trimIndent()
        val payload = JSONObject()
            .put("model", model)
            .put("input", prompt)
            .toString()
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 45_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "MurrLex/${BuildConfig.VERSION_NAME}")
        }
        connection.outputStream.use { stream -> stream.write(payload.toByteArray(Charsets.UTF_8)) }
        val status = connection.responseCode
        val responseText = (if (status in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { it.readText() }
            .orEmpty()
        connection.disconnect()
        if (status !in 200..299) return@withContext CardTranslationInsight("", "", "", emptyList(), "")
        val parsedText = parseOpenAiTextResponse(responseText)
        if (parsedText.isNotBlank()) {
            repository.saveOpenAiTextCache(
                task = "card_translation_insight",
                model = model,
                input = cacheInput,
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                output = parsedText
            )
        }
        parseCardTranslationInsight(parsedText).copy(source = "API")
    }

    private suspend fun requestOpenAiMistakeCorrection(
        phrase: String,
        language: String,
        interfaceLanguage: String,
        baseUrl: String,
        apiKey: String,
        model: String
    ): MistakeCorrectionInsight = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext MistakeCorrectionInsight("", "", emptyList(), emptyList())
        val cacheInput = "mistake-correction:$interfaceLanguage:$phrase"
        repository.getCachedOpenAiText(
            task = "mistake_correction",
            model = model,
            input = cacheInput,
            sourceLanguage = language,
            targetLanguage = language
        )?.let { cached -> return@withContext parseMistakeCorrectionInsight(cached) }
        val endpoint = baseUrl.trimEnd('/') + "/responses"
        val prompt = """
            The learner said or wrote this in $language:
            $phrase

            Correct it in the same language. If it is already correct, keep the corrected value identical and explain why.
            Return strict JSON only with keys: corrected, explanation, rules, examples.
            explanation, rules, and examples must be in the user's interface language: $interfaceLanguage.
            rules must be an array containing all applicable grammar, spelling, pronunciation, or usage rules.
            examples must be an array of 2-4 short examples.
        """.trimIndent()
        val payload = JSONObject()
            .put("model", model)
            .put("input", prompt)
            .toString()
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 60_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "MurrLex/${BuildConfig.VERSION_NAME}")
        }
        connection.outputStream.use { stream -> stream.write(payload.toByteArray(Charsets.UTF_8)) }
        val status = connection.responseCode
        val responseText = (if (status in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { it.readText() }
            .orEmpty()
        connection.disconnect()
        if (status !in 200..299) return@withContext MistakeCorrectionInsight("", "", emptyList(), emptyList())
        val parsedText = parseOpenAiTextResponse(responseText)
        if (parsedText.isNotBlank()) {
            repository.saveOpenAiTextCache(
                task = "mistake_correction",
                model = model,
                input = cacheInput,
                sourceLanguage = language,
                targetLanguage = language,
                output = parsedText
            )
        }
        parseMistakeCorrectionInsight(parsedText)
    }

    private suspend fun requestOpenAiTrainCards(
        sourceCard: Flashcard,
        options: List<String>,
        interfaceLanguage: String,
        baseUrl: String,
        apiKey: String,
        model: String
    ): List<TrainCardDraft> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext emptyList()
        val mistake = sourceCard.mistakeText().ifBlank { sourceCard.nativeText() }
        val corrected = sourceCard.correctText()
        val language = sourceCard.sourceLanguage.ifBlank { sourceCard.targetLanguage }
        val cacheInput = "train-cards:$interfaceLanguage:${options.joinToString("|")}:$mistake:$corrected:${sourceCard.hintText()}"
        repository.getCachedOpenAiText(
            task = "train_cards",
            model = model,
            input = cacheInput,
            sourceLanguage = language,
            targetLanguage = language
        )?.let { cached -> return@withContext parseTrainCardDrafts(cached) }
        val endpoint = baseUrl.trimEnd('/') + "/responses"
        val prompt = """
            Create language-learning Train flashcards from this Mistake card.
            Language: $language
            Learner mistake: $mistake
            Correct version: $corrected
            Existing explanation:
            ${sourceCard.hintText()}

            Create one card for each selected option:
            ${options.joinToString("\n") { "- $it" }}

            Return strict JSON only with key cards. cards must be an array of objects with keys: front, back, hint, option.
            front and back are the two card sides in $language.
            hint must explain the rule or drill goal in the user's interface language: $interfaceLanguage.
            These generated cards must reinforce the material and must not ask for more generated cards.
        """.trimIndent()
        val payload = JSONObject()
            .put("model", model)
            .put("input", prompt)
            .toString()
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 60_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "MurrLex/${BuildConfig.VERSION_NAME}")
        }
        connection.outputStream.use { stream -> stream.write(payload.toByteArray(Charsets.UTF_8)) }
        val status = connection.responseCode
        val responseText = (if (status in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { it.readText() }
            .orEmpty()
        connection.disconnect()
        if (status !in 200..299) return@withContext emptyList()
        val parsedText = parseOpenAiTextResponse(responseText)
        if (parsedText.isNotBlank()) {
            repository.saveOpenAiTextCache(
                task = "train_cards",
                model = model,
                input = cacheInput,
                sourceLanguage = language,
                targetLanguage = language,
                output = parsedText
            )
        }
        parseTrainCardDrafts(parsedText)
    }

    private suspend fun requestOpenAiSharedPostCards(
        postText: String,
        sourceLanguage: String,
        targetLanguage: String,
        interfaceLanguage: String,
        baseUrl: String,
        apiKey: String,
        model: String
    ): List<SharedPostCardDraft> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext emptyList()
        val limitedText = postText.limitWords(100)
        val cacheInput = "shared-post:$interfaceLanguage:$sourceLanguage:$targetLanguage:$limitedText"
        repository.getCachedOpenAiText(
            task = "shared_post_cards",
            model = model,
            input = cacheInput,
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage
        )?.let { cached -> return@withContext parseSharedPostCardDrafts(cached) }
        val endpoint = baseUrl.trimEnd('/') + "/responses"
        val prompt = """
            Convert this shared social post into MurrLex language-learning flashcards.
            Use only the first 100 words.
            Basic/Native language: $sourceLanguage
            Target/Learning language: $targetLanguage
            User interface language for hints: $interfaceLanguage

            Rules:
            1. If the post is not already in the Target/Learning language, split it into sentence cards.
               front = original sentence, back = translation into $targetLanguage.
            2. If the post is already in the Target/Learning language, create vocabulary cards for useful words or short phrases.
               front = translation/explanation in $sourceLanguage, back = the original $targetLanguage word or phrase.
            3. Return no more than 20 cards.
            4. Keep front/back concise.
            5. Return strict JSON only with key cards. cards is an array of objects with keys: front, back, hint, mode.
               mode must be sentence_translation or target_vocabulary.

            Post:
            $limitedText
        """.trimIndent()
        val payload = JSONObject()
            .put("model", model)
            .put("input", prompt)
            .toString()
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 60_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "MurrLex/${BuildConfig.VERSION_NAME}")
        }
        connection.outputStream.use { stream -> stream.write(payload.toByteArray(Charsets.UTF_8)) }
        val status = connection.responseCode
        val responseText = (if (status in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { it.readText() }
            .orEmpty()
        connection.disconnect()
        if (status !in 200..299) return@withContext emptyList()
        val parsedText = parseOpenAiTextResponse(responseText)
        if (parsedText.isNotBlank()) {
            repository.saveOpenAiTextCache(
                task = "shared_post_cards",
                model = model,
                input = cacheInput,
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                output = parsedText
            )
        }
        parseSharedPostCardDrafts(parsedText)
    }

    private suspend fun buildSharedPostCardsWithTranslator(
        postText: String,
        sourceLanguage: String,
        targetLanguage: String,
        state: StudyUiState
    ): List<SharedPostCardDraft> {
        val limitedText = postText.limitWords(100)
        val looksTarget = guessedLanguageCode(limitedText)
            ?.equals(freeOnlineLanguageCode(targetLanguage), ignoreCase = true) == true
        return if (looksTarget) {
            limitedText.contentWords()
                .distinctBy { it.lowercase(Locale.ROOT) }
                .take(20)
                .mapNotNull { word ->
                    val translated = runCatching {
                        requestConfiguredOnlineTranslation(word, targetLanguage, sourceLanguage, state).text.trim()
                    }.getOrDefault("")
                    if (translated.isBlank()) null else {
                        SharedPostCardDraft(
                            front = translated,
                            back = word,
                            hint = "Shared post was already in $targetLanguage; this card memorizes a useful Target-language word.",
                            mode = "target_vocabulary"
                        )
                    }
                }
        } else {
            limitedText.splitSharedPostSentences()
                .take(20)
                .mapNotNull { sentence ->
                    val translated = runCatching {
                        requestConfiguredOnlineTranslation(sentence, sourceLanguage, targetLanguage, state).text.trim()
                    }.getOrDefault("")
                    if (translated.isBlank()) null else {
                        SharedPostCardDraft(
                            front = sentence,
                            back = translated,
                            hint = "Shared post sentence translated into $targetLanguage.",
                            mode = "sentence_translation"
                        )
                    }
                }
        }
    }

    private fun parseCardTranslationInsight(text: String): CardTranslationInsight {
        val clean = text.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val json = runCatching { JSONObject(clean) }.getOrNull()
        if (json == null) return CardTranslationInsight(clean, "", "", emptyList(), "")
        val examplesJson = json.optJSONArray("examples")
        val examples = buildList {
            if (examplesJson != null) {
                for (index in 0 until examplesJson.length()) {
                    examplesJson.optString(index).trim().takeIf { it.isNotBlank() }?.let(::add)
                }
            }
        }.take(2)
        return CardTranslationInsight(
            translation = json.optString("translation").trim(),
            explanation = json.optString("explanation").trim(),
            rule = json.optString("rule").trim(),
            examples = examples,
            source = ""
        )
    }

    private fun parseMistakeCorrectionInsight(text: String): MistakeCorrectionInsight {
        val clean = cleanJsonText(text)
        val json = runCatching { JSONObject(clean) }.getOrNull()
            ?: return MistakeCorrectionInsight(clean, "", emptyList(), emptyList())
        return MistakeCorrectionInsight(
            corrected = json.optString("corrected").trim(),
            explanation = json.optString("explanation").trim(),
            rules = json.optJSONArray("rules").toStringList().ifEmpty {
                json.optString("rules").split('\n').map { it.trim().trim('-', ' ') }.filter { it.isNotBlank() }
            },
            examples = json.optJSONArray("examples").toStringList().take(4)
        )
    }

    private fun parseTrainCardDrafts(text: String): List<TrainCardDraft> {
        val clean = cleanJsonText(text)
        val rootObject = runCatching { JSONObject(clean) }.getOrNull()
        val cardsJson = rootObject?.optJSONArray("cards") ?: runCatching { JSONArray(clean) }.getOrNull() ?: return emptyList()
        return buildList {
            for (index in 0 until cardsJson.length()) {
                val cardJson = cardsJson.optJSONObject(index) ?: continue
                val front = cardJson.optString("front").trim()
                val back = cardJson.optString("back").trim()
                if (front.isBlank() || back.isBlank()) continue
                add(
                    TrainCardDraft(
                        front = front,
                        back = back,
                        hint = cardJson.optString("hint").trim(),
                        option = cardJson.optString("option").trim().ifBlank { "Train" }
                    )
                )
            }
        }
    }

    private fun parseSharedPostCardDrafts(text: String): List<SharedPostCardDraft> {
        val clean = cleanJsonText(text)
        val rootObject = runCatching { JSONObject(clean) }.getOrNull()
        val cardsJson = rootObject?.optJSONArray("cards") ?: runCatching { JSONArray(clean) }.getOrNull() ?: return emptyList()
        return buildList {
            for (index in 0 until cardsJson.length()) {
                val cardJson = cardsJson.optJSONObject(index) ?: continue
                val front = cardJson.optString("front").trim()
                val back = cardJson.optString("back").trim()
                if (front.isBlank() || back.isBlank()) continue
                add(
                    SharedPostCardDraft(
                        front = front,
                        back = back,
                        hint = cardJson.optString("hint").trim(),
                        mode = cardJson.optString("mode").trim().ifBlank { "sentence_translation" }
                    )
                )
            }
        }.take(20)
    }

    private fun cleanJsonText(text: String): String {
        return text.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                optString(index).trim().takeIf { it.isNotBlank() }?.let(::add)
            }
        }
    }

    private fun parseOpenAiTextResponse(responseText: String): String {
        val root = runCatching { JSONObject(responseText) }.getOrNull() ?: return ""
        root.optString("output_text").trim().takeIf { it.isNotBlank() }?.let { return it }
        val output = root.optJSONArray("output") ?: return ""
        val builder = StringBuilder()
        for (outputIndex in 0 until output.length()) {
            val item = output.optJSONObject(outputIndex) ?: continue
            val content = item.optJSONArray("content") ?: continue
            for (contentIndex in 0 until content.length()) {
                val contentItem = content.optJSONObject(contentIndex) ?: continue
                val text = contentItem.optString("text")
                    .ifBlank { contentItem.optString("output_text") }
                if (text.isNotBlank()) builder.append(text)
            }
        }
        return builder.toString().trim()
    }

    private fun freeOnlineLanguageCode(language: String): String {
        return when (language.trim().lowercase(Locale.ROOT)) {
            "english", "en" -> "en"
            "spanish", "es", "espanol" -> "es"
            "polish", "pl", "polski" -> "pl"
            "russian", "ru" -> "ru"
            "belarusian", "belarus", "by", "be" -> "be"
            "ukrainian", "uk", "ua" -> "uk"
            "german", "de", "deutsch" -> "de"
            "latvian", "lv" -> "lv"
            "lithuanian", "lt" -> "lt"
            "portuguese", "pt" -> "pt"
            else -> language.trim().takeIf { it.length in 2..3 }?.lowercase(Locale.ROOT) ?: "auto"
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

    private fun Flashcard.expectedAnswerText(isBackVisible: Boolean): String {
        return if (isBackVisible) nativeText() else correctText()
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


    private fun String.withTranslationAttribution(attribution: String): String {
        if (attribution.isBlank()) return trim()
        val clean = trim()
        return when {
            clean.isBlank() -> attribution
            clean.contains(attribution, ignoreCase = true) -> clean
            else -> "$clean\n$attribution"
        }
    }

    private fun String.withTranslationInsight(insight: String, attribution: String): String {
        val clean = trim()
        val insightClean = insight.trim()
        val withInsight = when {
            insightClean.isBlank() -> clean
            clean.isBlank() -> insightClean
            clean.contains(insightClean, ignoreCase = true) -> clean
            else -> "$clean\n\n$insightClean"
        }
        return withInsight.withTranslationAttribution(attribution)
    }

    private fun CardTranslationInsight.toCardHint(): String {
        val parts = buildList {
            explanation.takeIf { it.isNotBlank() }?.let { add("Explanation: $it") }
            rule.takeIf { it.isNotBlank() }?.let { add("Rule: $it") }
            if (examples.isNotEmpty()) add("Examples:\n${examples.joinToString("\n") { "- $it" }}")
        }
        return parts.joinToString("\n\n").take(1_200)
    }

    private fun MistakeCorrectionInsight.toMistakeHint(model: String): String {
        val parts = buildList {
            corrected.takeIf { it.isNotBlank() }?.let { add("Correction: $it") }
            explanation.takeIf { it.isNotBlank() }?.let { add("Explanation: $it") }
            if (rules.isNotEmpty()) add("Rules:\n${rules.joinToString("\n") { "- $it" }}")
            if (examples.isNotEmpty()) add("Examples:\n${examples.joinToString("\n") { "- $it" }}")
            add("Models: correction OpenAI $model")
        }
        return parts.joinToString("\n\n").take(1_500)
    }

    private fun OpenAiTextResult.isOpenAiSource(): Boolean {
        return source.equals("API", ignoreCase = true) || source.equals("cache", ignoreCase = true)
    }

    private fun OpenAiTextResult.googleAttribution(): String {
        return if (source == "Google API") GOOGLE_TRANSLATE_ATTRIBUTION else ""
    }

    private fun OpenAiTextResult.providerLabel(): String {
        return if (source == "Google API") {
            GOOGLE_TRANSLATE_ATTRIBUTION
        } else {
            "OpenAI translation ${_uiState.value.openAiTextModel}: $source"
        }
    }

    private fun CardTranslationInsightResult.isOpenAiSource(): Boolean {
        return source.equals("API", ignoreCase = true) || source.equals("cache", ignoreCase = true)
    }

    private fun CardTranslationInsightResult.googleAttribution(): String {
        return if (source == "Google API") GOOGLE_TRANSLATE_ATTRIBUTION else ""
    }

    private fun CardTranslationInsightResult.providerLabel(model: String): String {
        return if (source == "Google API") GOOGLE_TRANSLATE_ATTRIBUTION else "OpenAI translation $model: $source"
    }

    private fun buildTranslatorHint(base: String, attribution: String): String {
        return attribution.trim().takeIf { it.isNotBlank() }?.let { "$base\n$it" } ?: base
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

    private fun String?.lessonLanguageOrNull(): String? {
        val cleaned = orEmpty().trim()
        return cleaned.takeIf { it.isNotBlank() && !it.equals("Mixed", ignoreCase = true) }
    }

    private fun quickVocabularyLessonShortTitle(sourceLanguage: String, targetLanguage: String): String {
        return "${sourceLanguage.shortLanguageCode()} - ${targetLanguage.shortLanguageCode()} Vocabulary"
    }

    private fun StudyUiState.shouldUseOpenAiOnline(): Boolean {
        return useOpenAiModels && openAiApiKey.isNotBlank()
    }
    private fun quickVocabularyLessonTitle(sourceLanguage: String, targetLanguage: String, createdAt: String): String {
        return "${sourceLanguage.shortLanguageCode()} - ${targetLanguage.shortLanguageCode()} Vocabulary $createdAt"
    }

    private fun displayTimestamp(): String {
        return SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault()).format(Date())
    }

    private fun String.shortLanguageCode(): String {
        val normalized = trim().lowercase(Locale.ROOT)
        return when {
            normalized in listOf("pl", "pol", "polish", "polski") -> "PL"
            normalized in listOf("ru", "rus", "russian") -> "RU"
            normalized in listOf("en", "eng", "english") -> "EN"
            normalized in listOf("de", "deu", "ger", "german", "deutsch") -> "DE"
            normalized in listOf("es", "spa", "spanish", "espanol") -> "ES"
            normalized in listOf("be", "bel", "by", "belarusian") -> "BY"
            normalized in listOf("uk", "ua", "ukr", "ukrainian") -> "UA"
            normalized in listOf("lv", "lav", "latvian") -> "LV"
            normalized in listOf("lt", "lit", "lithuanian") -> "LT"
            normalized in listOf("pt", "por", "portuguese") -> "PT"
            normalized.length >= 2 -> normalized.take(2).uppercase(Locale.ROOT)
            else -> "XX"
        }
    }

    private fun quickVocabularyLessonInfo(sourceLanguage: String, targetLanguage: String): String {
        return "Quick voice captures. Basic/Native language: $sourceLanguage. Target/Learning language: $targetLanguage. The captured phrase is saved on the Basic side; translation is added to the Target side."
    }

    private fun String.limitWords(limit: Int): String {
        return trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .take(limit)
            .joinToString(" ")
    }

    private fun String.splitSharedPostSentences(): List<String> {
        return replace('\n', ' ')
            .split(Regex("(?<=[.!?。！？])\\s+|(?<=[.!?])"))
            .map { it.trim().trim('-', '–', '—') }
            .filter { it.length >= 2 }
            .ifEmpty { listOf(trim()) }
    }

    private fun String.contentWords(): List<String> {
        return split(Regex("[^\\p{L}\\p{M}'’-]+"))
            .map { it.trim('\'', '’', '-') }
            .filter { it.length >= 2 }
    }

    private fun guessedLanguageCode(text: String): String? {
        val lower = text.lowercase(Locale.ROOT)
        val cyrillicCount = lower.count { it in '\u0400'..'\u04FF' }
        val latinCount = lower.count { it in 'a'..'z' || it in '\u00C0'..'\u024F' }
        if (cyrillicCount > latinCount) {
            return when {
                Regex("[іїєґ]").containsMatchIn(lower) -> "uk"
                Regex("[ўі]").containsMatchIn(lower) -> "be"
                else -> "ru"
            }
        }
        return when {
            Regex("[ąćęłńóśźż]").containsMatchIn(lower) -> "pl"
            Regex("[āčēģīķļņšūž]").containsMatchIn(lower) -> "lv"
            Regex("[ąčęėįšųūž]").containsMatchIn(lower) -> "lt"
            Regex("[ãõç]").containsMatchIn(lower) -> "pt"
            Regex("[áéíñóúü]").containsMatchIn(lower) -> "es"
            Regex("[äöüß]").containsMatchIn(lower) -> "de"
            lower.isNotBlank() -> "en"
            else -> null
        }
    }

    private fun Lesson.matchesQuickVocabularyPair(sourceLanguage: String, targetLanguage: String): Boolean {
        if (!id.startsWith(QUICK_VOCABULARY_LESSON_ID)) return false
        val lessonSource = this.sourceLanguage.lessonLanguageOrNull()
        val lessonTarget = this.targetLanguage.lessonLanguageOrNull()
        return lessonSource?.equals(sourceLanguage, ignoreCase = true) == true &&
            lessonTarget?.equals(targetLanguage, ignoreCase = true) == true
    }

    private fun String.safeIdPart(): String {
        return trim().lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "target" }
    }

    private fun newLessonId(): String = "lesson_${UUID.randomUUID()}"

    companion object {
        private const val PORTION_SIZE = 20
        private const val QUICK_VOCABULARY_LESSON_ID = "quick_vocabulary"
        private const val SHARED_POST_LESSON_ID = "shared_post"
        private const val GOOGLE_TRANSLATE_ATTRIBUTION = "Powered by Google Translator"
            }
}

private fun WorkMode.displayLabel(): String {
    return when (this) {
        WorkMode.CARDS -> "Cards"
        WorkMode.TESTS -> "Tests"
        WorkMode.TRANSLATE -> "Translate"
        WorkMode.SPLIT -> "Split"
    }
}
