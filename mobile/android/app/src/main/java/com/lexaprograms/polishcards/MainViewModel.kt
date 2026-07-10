package com.lexaprograms.polishcards

import android.app.Application
import android.net.Uri
import android.util.Base64
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
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class AppScreen {
    CATALOG,
    DIALOGS,
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

val OpenAiImageTextModels = listOf(
    "gpt-4o-mini",
    "gpt-4o",
    "gpt-5.4-mini",
    "gpt-5.4"
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

val CatReplySpeechRateOptions = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.25f, 2.5f)

val CatDialogRetentionDayOptions = listOf(1, 7, 14, 30, 60, 90, 180, 365)

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
    val mode: String,
    val original: String = "",
    val originalLanguage: String = ""
)

data class CatChatTurnResult(
    val reply: String,
    val analysis: String
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
    val original: String = "",
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
    val catDialogs: List<CatDialog> = emptyList(),
    val selectedCatDialog: CatDialog? = null,
    val catDialogRetentionDays: Int = CardRepository.DEFAULT_CAT_DIALOG_RETENTION_DAYS,
    val catReplySpeechRate: Float = CardRepository.DEFAULT_CAT_REPLY_SPEECH_RATE,
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
    val studySortDescending: Boolean = false,
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
    val serverUsername: String = "",
    val serverEmail: String = "",
    val serverSessionExpiresAtMillis: Long = 0L,
    val openAiSpeechModel: String = CardRepository.DEFAULT_OPENAI_SPEECH_MODEL,
    val openAiTextModel: String = CardRepository.DEFAULT_OPENAI_TEXT_MODEL,
    val openAiImageTextModel: String = CardRepository.DEFAULT_OPENAI_IMAGE_TEXT_MODEL,
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
    val translatedOriginalCardIds: Set<Int> = emptySet(),
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
        val serverSession = repository.loadMurrLexServerSession()
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
            openAiBaseUrl = serverSession.serverUrl,
            openAiApiKey = serverSession.accessToken,
            serverUsername = serverSession.username,
            serverEmail = serverSession.email,
            serverSessionExpiresAtMillis = serverSession.accessExpiresAtMillis,
            openAiSpeechModel = repository.loadOpenAiSpeechModel().takeIf { it in OpenAiSpeechModels }
                ?: CardRepository.DEFAULT_OPENAI_SPEECH_MODEL,
            openAiTextModel = repository.loadOpenAiTextModel().takeIf { it in OpenAiTextModels }
                ?: CardRepository.DEFAULT_OPENAI_TEXT_MODEL,
            openAiImageTextModel = repository.loadOpenAiImageTextModel().takeIf { it in OpenAiImageTextModels }
                ?: CardRepository.DEFAULT_OPENAI_IMAGE_TEXT_MODEL,
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
            catDialogRetentionDays = repository.loadCatDialogRetentionDays(),
            catReplySpeechRate = repository.loadCatReplySpeechRate(),
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
        if (serverSession.isAuthenticated) {
            refreshMurrLexServerSession(silent = true)
        }
        refreshLessons()
        refreshCatDialogs()
    }

    fun refreshLessons() {
        val lessons = repository.loadLessons(_uiState.value.showHiddenLessons)
        _uiState.value = _uiState.value.copy(
            lessons = lessons,
            selectedLessonIds = _uiState.value.selectedLessonIds.filter { id -> lessons.any { it.id == id } }.toSet()
        )
    }

    fun refreshCatDialogs() {
        val dialogs = repository.loadCatDialogs(_uiState.value.showHiddenLessons)
        _uiState.value = _uiState.value.copy(
            catDialogs = dialogs,
            selectedCatDialog = _uiState.value.selectedCatDialog?.let { selected ->
                dialogs.firstOrNull { it.id == selected.id }
            }
        )
    }

    fun toggleShowHiddenLessons() {
        val next = !_uiState.value.showHiddenLessons
        val lessons = repository.loadLessons(next)
        val dialogs = repository.loadCatDialogs(next)
        _uiState.value = _uiState.value.copy(
            showHiddenLessons = next,
            lessons = lessons,
            catDialogs = dialogs,
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
            val returnScreen = state.screen.takeUnless { it == AppScreen.SETTINGS } ?: AppScreen.CATALOG
            _uiState.value = state.copy(
                screen = AppScreen.SETTINGS,
                settingsReturnScreen = returnScreen
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
        val returnScreen = state.settingsReturnScreen.takeUnless { it == AppScreen.SETTINGS } ?: AppScreen.CATALOG
        _uiState.value = state.copy(
            screen = returnScreen,
            settingsReturnScreen = AppScreen.CATALOG,
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
        val cleaned = url.trim().trimEnd('/')
        repository.saveOpenAiBaseUrl(cleaned)
        _uiState.value = _uiState.value.copy(openAiBaseUrl = cleaned, translationApiUrl = cleaned)
    }

    fun setOpenAiApiKey(apiKey: String) {
        // Kept for binary compatibility with the older Settings UI. Provider keys never persist locally.
        _uiState.value = _uiState.value.copy(openAiApiKey = "", translationApiToken = "")
    }

    fun loginToMurrLexServer(login: String, password: String, register: Boolean = false, email: String = "") {
        val serverUrl = _uiState.value.openAiBaseUrl.trim()
        viewModelScope.launch {
            val result = if (register) {
                MurrLexServerClient.register(serverUrl, login.trim(), email.trim(), password)
            } else {
                MurrLexServerClient.login(serverUrl, login.trim(), password)
            }
            val session = result.session
            if (session == null) {
                _uiState.value = _uiState.value.copy(message = result.error.ifBlank { "MurrLex server login failed" })
            } else {
                applyMurrLexServerSession(session, "MurrLex server session active")
            }
        }
    }

    fun logoutFromMurrLexServer() {
        val session = repository.loadMurrLexServerSession()
        viewModelScope.launch {
            MurrLexServerClient.logout(session)
            repository.clearMurrLexServerSession()
            _uiState.value = _uiState.value.copy(
                openAiApiKey = "",
                translationApiToken = "",
                serverUsername = "",
                serverEmail = "",
                serverSessionExpiresAtMillis = 0L,
                message = "MurrLex server session ended"
            )
        }
    }

    fun refreshMurrLexServerSession(silent: Boolean = false) {
        val session = repository.loadMurrLexServerSession()
        if (!session.isAuthenticated) return
        viewModelScope.launch {
            val result = MurrLexServerClient.refresh(session)
            val refreshed = result.session
            if (refreshed == null) {
                repository.clearMurrLexServerSession()
                if (!silent) _uiState.value = _uiState.value.copy(message = result.error.ifBlank { "MurrLex server session expired" })
            } else {
                applyMurrLexServerSession(refreshed, if (silent) null else "MurrLex server session refreshed")
            }
        }
    }

    private fun applyMurrLexServerSession(session: MurrLexServerSession, message: String?) {
        repository.saveMurrLexServerSession(session)
        _uiState.value = _uiState.value.copy(
            openAiBaseUrl = session.serverUrl,
            openAiApiKey = session.accessToken,
            translationApiUrl = session.serverUrl,
            translationApiToken = session.accessToken,
            serverUsername = session.username,
            serverEmail = session.email,
            serverSessionExpiresAtMillis = session.accessExpiresAtMillis,
            message = message
        )
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

    fun setOpenAiImageTextModel(model: String) {
        val cleaned = model.takeIf { it in OpenAiImageTextModels } ?: CardRepository.DEFAULT_OPENAI_IMAGE_TEXT_MODEL
        repository.saveOpenAiImageTextModel(cleaned)
        _uiState.value = _uiState.value.copy(openAiImageTextModel = cleaned)
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

    fun setCatReplySpeechRate(rate: Float) {
        repository.saveCatReplySpeechRate(rate)
        _uiState.value = _uiState.value.copy(catReplySpeechRate = repository.loadCatReplySpeechRate())
    }

    fun setCatDialogRetentionDays(days: Int) {
        repository.saveCatDialogRetentionDays(days)
        _uiState.value = _uiState.value.copy(
            catDialogRetentionDays = repository.loadCatDialogRetentionDays(),
            catDialogs = repository.loadCatDialogs(_uiState.value.showHiddenLessons)
        )
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

    fun toggleCurrentCardFeatured() {
        val state = _uiState.value
        val selected = state.selectedLesson ?: return
        val current = state.currentCard ?: return
        val lesson = repository.loadLessons(includeHidden = true).firstOrNull { it.id == selected.id } ?: selected
        val updatedCards = lesson.cards.map { card ->
            if (card.id == current.id) {
                card.copy(featured = !card.featured)
            } else {
                card
            }
        }
        val updatedLesson = lesson.copy(cards = updatedCards, editable = true)
        repository.saveLesson(updatedLesson)
        val visibleLessons = repository.loadLessons(state.showHiddenLessons)
        _uiState.value = state.copy(
            lessons = visibleLessons,
            selectedLesson = visibleLessons.firstOrNull { it.id == updatedLesson.id } ?: updatedLesson,
            currentPortion = state.currentPortion.map { card -> updatedCards.firstOrNull { it.id == card.id } ?: card },
            message = if (current.featured) "Featured removed" else "Featured"
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
        shuffleRandom: Boolean,
        descending: Boolean = false
    ): List<Flashcard> {
        val ordered = when (mode) {
            StudyMode.ORIGINAL -> cards
            StudyMode.ALPHABETICAL -> cards.sortedBy { it.nativeText().lowercase(Locale.getDefault()) }
            StudyMode.RANDOM -> if (shuffleRandom) cards.shuffled() else cards
        }
        return if (descending && mode != StudyMode.RANDOM) ordered.asReversed() else ordered
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
            orderStudyCards(studyCards, mode, shuffleRandom = false, descending = session.sortDescending)
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
            settingsReturnScreen = AppScreen.CATALOG,
            mode = mode,
            studySortDescending = session.sortDescending,
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
        val cards = orderStudyCards(
            studyCards,
            baseState.mode,
            shuffleRandom = true,
            descending = baseState.studySortDescending
        )

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
                sortDescending = state.studySortDescending,
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
            settingsReturnScreen = AppScreen.CATALOG,
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

    fun openCatDialogs() {
        saveCurrentStudySession()
        val dialogs = repository.loadCatDialogs(_uiState.value.showHiddenLessons)
        _uiState.value = _uiState.value.copy(
            screen = AppScreen.DIALOGS,
            settingsReturnScreen = AppScreen.CATALOG,
            catDialogs = dialogs,
            selectedCatDialog = null,
            selectedLesson = null,
            selectedLessonIds = emptySet(),
            answer = "",
            editorLesson = null,
            cardDraft = CardDraft()
        )
    }

    fun startCatDialog(basicLanguage: String, targetLanguage: String): CatDialog {
        val now = timestamp()
        val nowMs = System.currentTimeMillis()
        val cleanBasic = basicLanguage.trim().ifBlank { _uiState.value.activeVocabularySourceLanguage }
        val cleanTarget = targetLanguage.trim().ifBlank { _uiState.value.activeVocabularyTargetLanguage }
        val dialog = CatDialog(
            id = "cat_${UUID.randomUUID()}",
            title = "${cleanBasic.shortLanguageCode()} - ${cleanTarget.shortLanguageCode()} Cat chat $now",
            basicLanguage = cleanBasic,
            targetLanguage = cleanTarget,
            createdAt = now,
            updatedAt = now,
            createdAtMillis = nowMs,
            updatedAtMillis = nowMs
        )
        repository.saveCatDialog(dialog)
        val dialogs = repository.loadCatDialogs(_uiState.value.showHiddenLessons)
        val saved = dialogs.firstOrNull { it.id == dialog.id } ?: dialog
        _uiState.value = _uiState.value.copy(catDialogs = dialogs, selectedCatDialog = saved)
        return saved
    }

    fun openCatDialog(dialog: CatDialog) {
        val fresh = repository.loadCatDialogs(includeHidden = true).firstOrNull { it.id == dialog.id } ?: dialog
        _uiState.value = _uiState.value.copy(selectedCatDialog = fresh)
    }

    fun appendCatDialogTurn(dialogId: String?, basicLanguage: String, targetLanguage: String, userText: String, replyText: String, analysisText: String): CatDialog {
        val state = _uiState.value
        val baseDialog = dialogId?.let { id ->
            repository.loadCatDialogs(includeHidden = true).firstOrNull { it.id == id }
        } ?: startCatDialog(basicLanguage, targetLanguage)
        val now = timestamp()
        val nowMs = System.currentTimeMillis()
        val userMessage = CatDialogMessage(
            id = "user_${UUID.randomUUID()}",
            text = userText.trim(),
            fromCat = false,
            createdAt = now,
            createdAtMillis = nowMs
        )
        val catMessage = CatDialogMessage(
            id = "cat_${UUID.randomUUID()}",
            text = replyText.trim(),
            fromCat = true,
            createdAt = now,
            createdAtMillis = nowMs,
            analysis = analysisText.trim()
        )
        val titleSeed = userText.trim().take(42).ifBlank { "Cat chat" }
        val updated = baseDialog.copy(
            title = baseDialog.title.ifBlank { titleSeed },
            basicLanguage = basicLanguage.trim().ifBlank { baseDialog.basicLanguage },
            targetLanguage = targetLanguage.trim().ifBlank { baseDialog.targetLanguage },
            messages = baseDialog.messages + userMessage + catMessage,
            updatedAt = now,
            updatedAtMillis = nowMs
        )
        repository.saveCatDialog(updated)
        val dialogs = repository.loadCatDialogs(state.showHiddenLessons)
        val saved = dialogs.firstOrNull { it.id == updated.id } ?: updated
        _uiState.value = _uiState.value.copy(catDialogs = dialogs, selectedCatDialog = saved)
        return saved
    }

    fun toggleCatDialogFeatured(dialog: CatDialog) {
        repository.setCatDialogFeatured(dialog.id, !dialog.featured)
        refreshCatDialogs()
    }

    fun setCatDialogMessageFeaturedSelection(dialogId: String?, messageId: String, selection: String) {
        val cleanDialogId = dialogId?.trim().orEmpty()
        val cleanMessageId = messageId.trim()
        val cleanSelection = selection.trim()
        if (cleanDialogId.isBlank() || cleanMessageId.isBlank() || cleanSelection.isBlank()) return
        repository.setCatDialogMessageFeaturedSelection(cleanDialogId, cleanMessageId, cleanSelection)
        refreshCatDialogs()
    }

    fun setCatDialogHidden(dialog: CatDialog, hidden: Boolean) {
        repository.setCatDialogHidden(dialog.id, hidden)
        refreshCatDialogs()
    }

    fun deleteCatDialog(dialogId: String) {
        repository.deleteCatDialog(dialogId)
        refreshCatDialogs()
    }

    fun moveCatDialog(draggedDialogId: String, targetDialogId: String) {
        repository.moveCatDialog(draggedDialogId, targetDialogId)
        refreshCatDialogs()
    }

    fun openLesson(lesson: Lesson, preferredCardId: Int? = null) {
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
        val openedState = restoredStudySessionState(baseState, freshLesson)
            ?: newPortionState(baseState)
        val preferredIndex = preferredCardId
            ?.let { cardId -> openedState.currentPortion.indexOfFirst { it.id == cardId } }
            ?.takeIf { it >= 0 }
        _uiState.value = if (preferredIndex != null) {
            openedState.copy(
                currentIndex = preferredIndex,
                cardTransitionDirection = 1,
                isBackVisible = defaultBackVisible(openedState.currentPortion.getOrNull(preferredIndex)),
                answer = "",
                answerFeedbackVisible = false,
                message = null
            )
        } else {
            openedState
        }
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
        val cards = orderStudyCards(
            studyCards,
            mode,
            shuffleRandom = mode == StudyMode.RANDOM,
            descending = state.studySortDescending
        )
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

    fun setStudySortDescending(descending: Boolean) {
        val state = _uiState.value
        val lesson = state.selectedLesson
        if (lesson == null) {
            _uiState.value = state.copy(studySortDescending = descending)
            return
        }
        val currentCardId = state.currentCard?.id
        val studyCards = filterStudyCards(lesson.cards, state.excludeMasteredCards, state.hideCompletedCards, state.completedCardIds)
        val cards = orderStudyCards(studyCards, state.mode, shuffleRandom = false, descending = descending)
        val nextIndex = currentCardId
            ?.let { id -> cards.indexOfFirst { it.id == id } }
            ?.takeIf { it >= 0 }
            ?: 0
        _uiState.value = state.copy(
            studySortDescending = descending,
            currentPortion = cards,
            currentIndex = nextIndex.coerceAtMost((cards.size - 1).coerceAtLeast(0)),
            answer = "",
            answerFeedbackVisible = false,
            isBackVisible = defaultBackVisible(cards.getOrNull(nextIndex)),
            message = if (descending) "Descending" else "Ascending"
        )
        saveCurrentStudySession()
    }

    fun startNewPortion() {
        val lesson = _uiState.value.selectedLesson ?: return
        repository.clearStudySession(lesson.id)
        _uiState.value = newPortionState(_uiState.value)
    }

    fun openCardFromMap(cardId: Int) {
        val state = _uiState.value
        val targetIndex = state.currentPortion.indexOfFirst { it.id == cardId }
        if (targetIndex < 0) return
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

    fun toggleOriginalCardText(cardId: Int) {
        val state = _uiState.value
        val card = state.currentPortion.firstOrNull { it.id == cardId } ?: return
        if (card.originalText().isBlank()) return
        if (cardId in state.translatedOriginalCardIds) {
            _uiState.value = state.copy(translatedOriginalCardIds = state.translatedOriginalCardIds - cardId)
            return
        }
        val basicLanguage = state.selectedLesson?.sourceLanguage.lessonLanguageOrNull()
            ?: state.activeVocabularySourceLanguage.trim().ifBlank { state.quickVocabularySourceLanguage }
        val currentNative = card.nativeText().trim()
        if (currentNative.isNotBlank() && !currentNative.isEmptyPlaceholder() && !currentNative.contains("pending", ignoreCase = true)) {
            _uiState.value = state.copy(translatedOriginalCardIds = state.translatedOriginalCardIds + cardId)
            return
        }
        viewModelScope.launch {
            val requestState = _uiState.value
            val sourceLanguage = card.sourceLanguage.lessonLanguageOrNull()
                ?: detectTextLanguageName(card.originalText())
            val result = runCatching {
                requestConfiguredOnlineTranslation(card.originalText(), sourceLanguage, basicLanguage, requestState)
            }.getOrNull() ?: OpenAiTextResult("", "")
            val translated = result.text.trim()
            if (translated.isBlank()) {
                _uiState.value = _uiState.value.copy(message = "Original translation failed")
                return@launch
            }
            applyTranslationToCard(
                cardId = cardId,
                translated = translated,
                targetBackSide = false,
                attribution = result.googleAttribution(),
                providerLabel = result.providerLabel()
            )
            _uiState.value = _uiState.value.copy(translatedOriginalCardIds = _uiState.value.translatedOriginalCardIds + cardId)
        }
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
                original = card.original,
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
                            original = cardDraft.original.trim(),
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
                            original = cardDraft.original.trim(),
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
                original = cardDraft.original.trim(),
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
                original = card.original,
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
                        original = draft.original.trim(),
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
                        original = draft.original.trim(),
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

    fun copyCurrentStudyCardToLessonEnd() {
        val state = _uiState.value
        val lesson = state.selectedLesson ?: return
        val source = state.currentCard ?: return
        val now = timestamp()
        val newCard = source.copy(
            id = (lesson.cards.maxOfOrNull { it.id } ?: 0) + 1,
            log = (source.log + "$now - card copied to lesson end").takeLast(100)
        )
        val updatedLesson = lesson.copy(cards = lesson.cards + newCard, editable = true)
        repository.saveLesson(updatedLesson)
        repository.clearStudySession(updatedLesson.id)
        val lessons = repository.loadLessons(state.showHiddenLessons)
        val savedLesson = lessons.firstOrNull { it.id == updatedLesson.id } ?: updatedLesson
        val orderedCards = orderStudyCards(
            filterStudyCards(savedLesson.cards, state.excludeMasteredCards, false, emptySet()),
            StudyMode.ORIGINAL,
            shuffleRandom = false
        )
        val targetIndex = orderedCards.indexOfFirst { it.id == newCard.id }.takeIf { it >= 0 }
            ?: orderedCards.lastIndex.coerceAtLeast(0)
        _uiState.value = state.copy(
            lessons = lessons,
            selectedLesson = savedLesson,
            mode = StudyMode.ORIGINAL,
            hideCompletedCards = false,
            currentPortion = orderedCards,
            currentIndex = targetIndex,
            cardTransitionDirection = 1,
            completedCardIds = emptySet(),
            portionCompletionSaved = false,
            isBackVisible = defaultBackVisible(orderedCards.getOrNull(targetIndex)),
            answer = "",
            answerFeedbackVisible = false,
            message = "Card copied"
        )
        saveCurrentStudySession()
    }

    fun moveCurrentStudyCardToLesson(destinationLessonId: String?) {
        val state = _uiState.value
        val sourceLesson = state.selectedLesson ?: return
        val sourceCard = state.currentCard ?: return
        val now = timestamp()
        val allLessons = repository.loadLessons(includeHidden = true)
        val freshSource = allLessons.firstOrNull { it.id == sourceLesson.id } ?: sourceLesson
        val cardToMove = freshSource.cards.firstOrNull { it.id == sourceCard.id } ?: sourceCard
        val sourceWithoutCard = freshSource.copy(
            cards = freshSource.cards.filterNot { it.id == sourceCard.id }.reindexCards(),
            editable = true
        )
        val destination = destinationLessonId
            ?.let { id -> allLessons.firstOrNull { it.id == id && it.id != freshSource.id } }
        val movedLesson = if (destination == null) {
            Lesson(
                id = newLessonId(),
                title = "${cardToMove.sourceLanguage.shortLanguageCode()} - ${cardToMove.targetLanguage.shortLanguageCode()} Moved ${displayTimestamp()}",
                lessonInfo = "Created by moving a card from ${freshSource.title}.",
                sourceLanguage = cardToMove.sourceLanguage.lessonLanguageOrNull() ?: freshSource.sourceLanguage,
                targetLanguage = cardToMove.targetLanguage.lessonLanguageOrNull() ?: freshSource.targetLanguage,
                cards = listOf(cardToMove.copy(id = 1, log = (cardToMove.log + "$now - moved from ${freshSource.title}").takeLast(100))),
                editable = true
            )
        } else {
            val nextId = (destination.cards.maxOfOrNull { it.id } ?: 0) + 1
            destination.copy(
                cards = destination.cards + cardToMove.copy(
                    id = nextId,
                    log = (cardToMove.log + "$now - moved from ${freshSource.title}").takeLast(100)
                ),
                editable = true,
                hidden = false
            )
        }
        repository.saveLesson(sourceWithoutCard)
        repository.saveLesson(movedLesson)
        repository.clearStudySession(sourceWithoutCard.id)
        repository.clearStudySession(movedLesson.id)
        val lessons = repository.loadLessons(state.showHiddenLessons)
        val savedDestination = lessons.firstOrNull { it.id == movedLesson.id } ?: movedLesson
        val movedCardId = savedDestination.cards.maxOfOrNull { it.id } ?: 1
        val orderedCards = orderStudyCards(savedDestination.cards, StudyMode.ORIGINAL, shuffleRandom = false)
        val targetIndex = orderedCards.indexOfFirst { it.id == movedCardId }.takeIf { it >= 0 }
            ?: orderedCards.lastIndex.coerceAtLeast(0)
        _uiState.value = state.copy(
            lessons = lessons,
            selectedLesson = savedDestination,
            selectedLessonIds = emptySet(),
            screen = AppScreen.STUDY,
            mode = StudyMode.ORIGINAL,
            hideCompletedCards = false,
            currentPortion = orderedCards,
            currentIndex = targetIndex,
            cardTransitionDirection = 1,
            completedCardIds = emptySet(),
            portionCompletionSaved = false,
            isBackVisible = defaultBackVisible(orderedCards.getOrNull(targetIndex)),
            answer = "",
            answerFeedbackVisible = false,
            message = "Card moved"
        )
        saveCurrentStudySession()
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

    fun addQuickVocabularyCard(
        phrase: String,
        recognitionLog: String = "",
        originalAudioPath: String = ""
    ) {
        val cleanPhrase = phrase.trim()
        if (cleanPhrase.isBlank()) {
            _uiState.value = _uiState.value.copy(message = "No speech recognized")
            return
        }
        val state = _uiState.value
        val sourceLanguage = state.activeVocabularySourceLanguage.trim().ifBlank { state.quickVocabularySourceLanguage }
        val targetLanguage = state.activeVocabularyTargetLanguage.trim().ifBlank { state.quickVocabularyTargetLanguage }
        val detectedLanguage = detectTextLanguageName(cleanPhrase)
        val markOriginal = shouldMarkOriginalLanguage(detectedLanguage, sourceLanguage)
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
            nativeValue = if (isMistakeCard) "" else if (markOriginal) "Translation pending" else cleanPhrase,
            correctValue = "Empty",
            hint = if (isMistakeCard) {
                "Captured as a same-language mistake. Correction pending."
            } else if (markOriginal) {
                "Captured original text in $detectedLanguage. Basic translation is pending."
            } else {
                "Captured by voice in the Basic language. Fill the Target language side later."
            },
            original = if (markOriginal) cleanPhrase else "",
            originalAudioPath = originalAudioPath,
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
            sourceLanguage = if (markOriginal) detectedLanguage else sourceLanguage,
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
                sourceLanguage = if (markOriginal) detectedLanguage else sourceLanguage,
                targetLanguage = targetLanguage
            )
        }
    }

    suspend fun requestCatChatReply(
        message: String,
        history: List<String>,
        sourceLanguage: String,
        targetLanguage: String
    ): String {
        val cleanMessage = message.trim()
        if (cleanMessage.isBlank()) return localCatChatReply(targetLanguage)
        val state = _uiState.value
        if (!state.shouldUseOpenAiOnline()) {
            return localCatChatReply(targetLanguage)
        }
        val reply = runCatching {
            requestOpenAiCatChatReply(
                message = cleanMessage,
                history = history.takeLast(8),
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                baseUrl = state.openAiBaseUrl,
                apiKey = state.openAiApiKey,
                model = state.openAiTextModel
            )
        }.getOrDefault("").trim()
        if (reply.isBlank()) return localCatChatReply(targetLanguage)
        logOpenAiActivity(
            action = "cat chat API",
            details = "${state.openAiTextModel}: $sourceLanguage -> $targetLanguage; ${cleanMessage.take(160)}"
        )
        return reply
    }

    suspend fun requestCatChatTurn(
        message: String,
        history: List<String>,
        sourceLanguage: String,
        targetLanguage: String
    ): CatChatTurnResult {
        val reply = requestCatChatReply(message, history, sourceLanguage, targetLanguage)
        val analysis = requestCatChatAnalysis(message, sourceLanguage, targetLanguage)
        return CatChatTurnResult(reply = reply, analysis = analysis)
    }

    suspend fun requestCatChatAnalysis(
        message: String,
        sourceLanguage: String,
        targetLanguage: String
    ): String {
        val cleanMessage = message.trim()
        if (cleanMessage.isBlank()) return ""
        val state = _uiState.value
        if (!state.shouldUseOpenAiOnline()) {
            return localCatChatAnalysis(sourceLanguage)
        }
        return runCatching {
            requestOpenAiCatChatAnalysis(
                message = cleanMessage,
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                baseUrl = state.openAiBaseUrl,
                apiKey = state.openAiApiKey,
                model = state.openAiTextModel
            )
        }.getOrDefault("").ifBlank {
            localCatChatAnalysis(sourceLanguage)
        }
    }

    fun addCatChatCard(
        phrase: String,
        basicLanguage: String,
        answerLanguage: String,
        phraseIsAnswerLanguage: Boolean,
        featured: Boolean = false,
        mistakeAnalysis: String = ""
    ) {
        val cleanPhrase = phrase.trim()
        if (cleanPhrase.isBlank()) {
            _uiState.value = _uiState.value.copy(message = "No phrase selected")
            return
        }
        val state = _uiState.value
        val cleanBasicLanguage = basicLanguage.trim().ifBlank { state.activeVocabularySourceLanguage }
        val learningLanguage = answerLanguage.trim().ifBlank { state.activeVocabularyTargetLanguage }
        val detectedLanguage = detectTextLanguageName(cleanPhrase)
        val markOriginal = !phraseIsAnswerLanguage && shouldMarkOriginalLanguage(detectedLanguage, cleanBasicLanguage)
        val isMistake = mistakeAnalysis.isNotBlank()
        val selectedHint = cleanPhrase.selectionHint()
        val lessonTitle = quickVocabularyLessonTitle(cleanBasicLanguage, learningLanguage, createdAt = displayTimestamp())
        val existingLesson = repository.loadLessons(includeHidden = false)
            .filter { lesson -> lesson.matchesQuickVocabularyPair(cleanBasicLanguage, learningLanguage) }
            .maxByOrNull { lesson -> lesson.cards.lastOrNull()?.madeAt ?: "" }
        val lessonId = existingLesson?.id
            ?: "${QUICK_VOCABULARY_LESSON_ID}_${cleanBasicLanguage.safeIdPart()}_${learningLanguage.safeIdPart()}_cat_${UUID.randomUUID()}"
        val now = timestamp()
        val newCard = Flashcard(
            id = (existingLesson?.cards?.size ?: 0) + 1,
            nativeValue = when {
                isMistake -> "Hint: correct ${cleanPhrase.selectionHint()}"
                else -> "Hint: $selectedHint"
            },
            correctValue = when {
                isMistake -> mistakeAnalysis.trim().ifBlank { "Correction pending" }
                else -> cleanPhrase
            },
            hint = if (isMistake) {
                "Original: $cleanPhrase\n\nCreated from Cat chat language analysis.\n\n$mistakeAnalysis"
            } else if (phraseIsAnswerLanguage) {
                "Created from a selected Cat answer. Front side gives a hint; Target side gives the selected answer."
            } else if (markOriginal) {
                "Created from selected Cat chat text. Original language: $detectedLanguage. Front side gives a hint; answer side gives the selected text."
            } else if (featured) {
                "Featured phrase from Cat chat. Front side gives a hint; answer side gives the selected text. Keep highlighted until manually reviewed."
            } else {
                "Created from selected Cat chat text. Front side gives a hint; answer side gives the selected text."
            },
            original = if (markOriginal || isMistake) cleanPhrase else "",
            madeAt = now,
            where = "Cat chat",
            log = buildList {
                add("$now - created from Cat chat")
                add("$now - chat pair $cleanBasicLanguage -> $learningLanguage")
                if (featured) add("$now - marked featured from chat selection")
                if (isMistake) add("$now - created as mistake from cat analysis")
                if (phraseIsAnswerLanguage) add("$now - created from cat answer in $learningLanguage")
                if (markOriginal) add("$now - original text detected as $detectedLanguage")
            },
            type = "card",
            cardKind = if (isMistake) "MK" else "LN",
            sourceLanguage = cleanBasicLanguage,
            targetLanguage = learningLanguage,
            featured = featured
        )
        val updatedLesson = if (existingLesson == null) {
            Lesson(
                id = lessonId,
                title = lessonTitle,
                lessonInfo = quickVocabularyLessonInfo(cleanBasicLanguage, learningLanguage),
                sourceLanguage = cleanBasicLanguage,
                targetLanguage = learningLanguage,
                cards = listOf(newCard).reindexCards(),
                editable = true
            )
        } else {
            existingLesson.copy(
                title = existingLesson.title.ifBlank { lessonTitle },
                lessonInfo = quickVocabularyLessonInfo(cleanBasicLanguage, learningLanguage),
                sourceLanguage = existingLesson.sourceLanguage.lessonLanguageOrNull() ?: cleanBasicLanguage,
                targetLanguage = existingLesson.targetLanguage.lessonLanguageOrNull() ?: learningLanguage,
                cards = (existingLesson.cards + newCard).reindexCards(),
                editable = true,
                hidden = false
            )
        }
        repository.saveLesson(updatedLesson)
        repository.clearStudySession(updatedLesson.id)
        val newCardId = updatedLesson.cards.lastOrNull()?.id ?: 1
        val visibleLessons = repository.loadLessons(state.showHiddenLessons)
        _uiState.value = state.copy(
            lessons = visibleLessons,
            selectedLesson = state.selectedLesson?.let { selected ->
                visibleLessons.firstOrNull { it.id == selected.id }
            } ?: state.selectedLesson,
            selectedLessonIds = emptySet(),
            message = when {
                isMistake -> "Mistake card created"
                featured -> "Featured card created"
                else -> "Card created"
            }
        )
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
        val detectedLanguage = detectTextLanguageName(originalText)
        val markOriginal = shouldMarkOriginalLanguage(detectedLanguage, sourceLanguage)
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
            nativeValue = if (markOriginal) "Translation pending" else originalText,
            correctValue = translatedText,
            hint = buildTranslatorHint("Created automatically from Translate mode.", state.translationAttribution),
            original = if (markOriginal) originalText else "",
            madeAt = now,
            where = "Translate mode",
            log = listOf("$now - created automatically from Translate mode with $providerLabel"),
            type = "card",
            cardKind = "LN",
            sourceLanguage = if (markOriginal) detectedLanguage else sourceLanguage,
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
        val detectedLanguage = detectTextLanguageName(originalText)
        val markOriginal = shouldMarkOriginalLanguage(detectedLanguage, sourceLanguage)
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
            nativeValue = if (markOriginal) "Translation pending" else originalText,
            correctValue = translatedText,
            hint = if (translatedText == "Empty") {
                "Created from Translate mode. Translation is pending."
            } else {
                buildTranslatorHint(
                    "Created from Translate mode. Basic: $sourceLanguage. Target: $targetLanguage.",
                    state.translationAttribution
                )
            },
            original = if (markOriginal) originalText else "",
            madeAt = now,
            where = "Translate mode",
            log = listOf("$now - created from Translate mode as $sourceLanguage to $targetLanguage"),
            type = "card",
            cardKind = "LN",
            sourceLanguage = if (markOriginal) detectedLanguage else sourceLanguage,
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
        val cleanText = sharedText.trim().limitWords(SHARED_POST_WORD_LIMIT)
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
                lessonInfo = "Imported from a shared post. Basic/Native language: $sourceLanguage. Target/Learning language: $targetLanguage. Source text was limited to $SHARED_POST_WORD_LIMIT words and summarized into retelling cards.",
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                cards = drafts.mapIndexed { index, draft ->
                    Flashcard(
                        id = index + 1,
                        nativeValue = draft.front,
                        correctValue = draft.back,
                        hint = draft.hint.ifBlank {
                            if (draft.mode.equals("retelling_sentence", ignoreCase = true)) {
                                "Shared post sentence card for retelling."
                            } else {
                                "Shared post card generated for retelling."
                            }
                        },
                        original = draft.original,
                        madeAt = now,
                        where = "Shared post import",
                        log = listOf("$now - imported from shared post as ${draft.mode.ifBlank { "sentence_translation" }}"),
                        type = "card",
                        cardKind = "LN",
                        sourceLanguage = draft.originalLanguage.lessonLanguageOrNull() ?: sourceLanguage,
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

    fun importSharedPostUrl(rawUrl: String) {
        val normalizedUrl = normalizeSharedPostUrl(rawUrl)
        if (normalizedUrl.isBlank()) {
            _uiState.value = _uiState.value.copy(message = "Enter URL")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(message = "Loading URL...")
            val pageText = runCatching {
                fetchSharedPostUrlText(normalizedUrl, URL_ARTICLE_WORD_LIMIT).ifBlank {
                    telegramPublicPostUrl(normalizedUrl)?.let { fetchSharedPostUrlText(it, URL_ARTICLE_WORD_LIMIT) }.orEmpty()
                }
            }.getOrDefault("").limitWords(URL_ARTICLE_WORD_LIMIT)
            if (pageText.isBlank()) {
                _uiState.value = _uiState.value.copy(message = "URL text not found")
                return@launch
            }
            importSharedArticleCards(pageText)
        }
    }

    fun importImageTextCards(imageUri: Uri, appendToCurrentLesson: Boolean = false) {
        importImageTextCards(listOf(imageUri), appendToCurrentLesson)
    }

    fun importImageTextCards(imageUris: List<Uri>, appendToCurrentLesson: Boolean = false) {
        val selectedUris = imageUris.take(IMAGE_TEXT_MAX_IMAGE_COUNT)
        val state = _uiState.value
        if (selectedUris.isEmpty()) {
            _uiState.value = state.copy(message = "No image selected")
            return
        }
        if (!state.shouldUseOpenAiOnline()) {
            _uiState.value = state.copy(message = "OpenAI image text needs online mode and API key")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(message = "Reading image text...")
            val requestState = _uiState.value
            val sourceLesson = if (appendToCurrentLesson) requestState.selectedLesson else null
            val sourceLanguage = sourceLesson?.sourceLanguage.lessonLanguageOrNull()
                ?: requestState.activeVocabularySourceLanguage.trim().ifBlank { requestState.quickVocabularySourceLanguage }
            val targetLanguage = sourceLesson?.targetLanguage.lessonLanguageOrNull()
                ?: requestState.activeVocabularyTargetLanguage.trim().ifBlank { requestState.quickVocabularyTargetLanguage }
            val recognizedTexts = selectedUris.mapIndexedNotNull { index, uri ->
                _uiState.value = _uiState.value.copy(message = "Reading image ${index + 1}/${selectedUris.size}...")
                val text = runCatching {
                    requestOpenAiImageText(
                        imageUri = uri,
                        interfaceLanguage = requestState.interfaceLanguage,
                        baseUrl = requestState.openAiBaseUrl,
                        apiKey = requestState.openAiApiKey,
                        model = requestState.openAiImageTextModel
                    )
                }.getOrElse { error ->
                    _uiState.value = _uiState.value.copy(message = "Image text failed: ${error.message.orEmpty().take(80)}")
                    ""
                }.trim()
                text.takeIf { it.isNotBlank() }
            }
            if (recognizedTexts.isEmpty()) {
                _uiState.value = _uiState.value.copy(message = "No readable text found in image")
                return@launch
            }
            val drafts = recognizedTexts.mapIndexedNotNull { index, recognizedText ->
                _uiState.value = _uiState.value.copy(message = "Creating image card ${index + 1}/${recognizedTexts.size}...")
                val translated = runCatching {
                    requestConfiguredOnlineTranslation(
                        phrase = recognizedText,
                        sourceLanguage = sourceLanguage,
                        targetLanguage = targetLanguage,
                        state = requestState
                    ).text.trim()
                }.getOrDefault("")
                buildOriginalAwareDraft(
                    originalText = recognizedText,
                    sourceLanguage = sourceLanguage,
                    targetLanguage = targetLanguage,
                    state = requestState,
                    hint = "Image OCR card. One selected image or camera photo created this card.",
                    mode = "image_ocr"
                ) ?: SharedPostCardDraft(
                    front = recognizedText,
                    back = translated.ifBlank { "Empty" },
                    hint = "Image OCR card. One selected image or camera photo created this card.",
                    mode = "image_ocr"
                )
            }
            if (drafts.isEmpty()) {
                _uiState.value = _uiState.value.copy(message = "Image card creation failed")
                return@launch
            }
            logOpenAiActivity(
                action = "image text API/cache",
                details = "${requestState.openAiImageTextModel}; ${selectedUris.size} image(s); ${recognizedTexts.joinToString(" ").take(180)}"
            )
            saveImageOcrCards(
                drafts = drafts,
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                appendLesson = sourceLesson,
                requestState = requestState
            )
        }
    }

    private fun saveImageOcrCards(
        drafts: List<SharedPostCardDraft>,
        sourceLanguage: String,
        targetLanguage: String,
        appendLesson: Lesson?,
        requestState: StudyUiState
    ) {
        val now = timestamp()
        val displayNow = displayTimestamp()
        val baseCards = appendLesson?.cards.orEmpty()
        val firstNewId = (baseCards.maxOfOrNull { it.id } ?: 0) + 1
        val newCards = drafts.mapIndexed { index, draft ->
            Flashcard(
                id = firstNewId + index,
                nativeValue = draft.front,
                correctValue = draft.back,
                hint = draft.hint,
                original = draft.original,
                madeAt = now,
                where = if (appendLesson != null) "Image OCR in lesson" else "Image OCR import",
                log = listOf("$now - imported from image OCR as ${draft.mode}; OpenAI image model ${requestState.openAiImageTextModel}; text model ${requestState.openAiTextModel}"),
                type = "card",
                cardKind = "LN",
                sourceLanguage = draft.originalLanguage.lessonLanguageOrNull() ?: sourceLanguage,
                targetLanguage = targetLanguage
            )
        }
        val lesson = appendLesson?.copy(
            cards = baseCards + newCards,
            editable = true,
            hidden = false
        ) ?: Lesson(
            id = "${SHARED_POST_LESSON_ID}_${sourceLanguage.safeIdPart()}_${targetLanguage.safeIdPart()}_image_${UUID.randomUUID()}",
            title = "${sourceLanguage.shortLanguageCode()} - ${targetLanguage.shortLanguageCode()} Image $displayNow",
            lessonInfo = "Imported from image OCR. Basic/Native language: $sourceLanguage. Target/Learning language: $targetLanguage. One selected image or camera photo creates one card.",
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage,
            cards = newCards.mapIndexed { index, card -> card.copy(id = index + 1) },
            editable = true
        )
        repository.saveLesson(lesson)
        repository.clearStudySession(lesson.id)
        val lessons = repository.loadLessons(requestState.showHiddenLessons)
        val savedLesson = lessons.firstOrNull { it.id == lesson.id } ?: lesson
        val completedIds: Set<Int> = emptySet()
        val nextMode = if (appendLesson != null) StudyMode.ORIGINAL else requestState.mode
        val nextHideCompleted = if (appendLesson != null) false else requestState.hideCompletedCards
        val studyCards = filterStudyCards(
            savedLesson.cards,
            requestState.excludeMasteredCards,
            nextHideCompleted,
            completedIds
        )
        val orderedCards = orderStudyCards(studyCards, nextMode, shuffleRandom = false)
        val newCardIds = newCards.map { it.id }.toSet()
        val targetCardId = newCards.firstOrNull()?.id
        val targetIndex = targetCardId
            ?.let { id -> orderedCards.indexOfFirst { it.id == id } }
            ?.takeIf { it >= 0 }
            ?: 0
        val nextPortion = if (appendLesson != null) {
            orderedCards
        } else {
            orderedCards.take(PORTION_SIZE)
        }
        val nextIndex = if (appendLesson != null) {
            targetIndex.coerceAtMost((nextPortion.size - 1).coerceAtLeast(0))
        } else {
            0
        }
        _uiState.value = requestState.copy(
            lessons = lessons,
            selectedLesson = savedLesson,
            selectedLessonIds = emptySet(),
            screen = AppScreen.STUDY,
            workMode = WorkMode.CARDS,
            mode = nextMode,
            hideCompletedCards = nextHideCompleted,
            currentPortion = nextPortion,
            currentIndex = nextIndex,
            completedCardIds = completedIds.intersect(nextPortion.map { it.id }.toSet()),
            portionCompletionSaved = false,
            isBackVisible = defaultBackVisible(nextPortion.getOrNull(nextIndex)),
            answer = "",
            message = if (appendLesson != null) {
                "Added image cards: ${newCards.size}"
            } else {
                "Image cards: ${newCards.size}"
            }
        )
        saveCurrentStudySession()
    }

    private fun importSharedArticleCards(articleText: String) {
        val cleanText = articleText.trim().limitWords(URL_ARTICLE_WORD_LIMIT)
        if (cleanText.isBlank()) {
            _uiState.value = _uiState.value.copy(message = "Article text is empty")
            return
        }
        val state = _uiState.value
        val sourceLanguage = state.activeVocabularySourceLanguage.trim().ifBlank { state.quickVocabularySourceLanguage }
        val targetLanguage = state.activeVocabularyTargetLanguage.trim().ifBlank { state.quickVocabularyTargetLanguage }
        viewModelScope.launch {
            val requestState = _uiState.value
            val drafts = if (requestState.shouldUseOpenAiOnline()) {
                runCatching {
                    requestOpenAiSharedArticleCards(
                        articleText = cleanText,
                        sourceLanguage = sourceLanguage,
                        targetLanguage = targetLanguage,
                        interfaceLanguage = requestState.interfaceLanguage,
                        baseUrl = requestState.openAiBaseUrl,
                        apiKey = requestState.openAiApiKey,
                        model = requestState.openAiTextModel
                    )
                }.getOrNull().orEmpty()
            } else {
                buildSharedArticleCardsWithTranslator(cleanText, sourceLanguage, targetLanguage, requestState)
            }
            if (drafts.isEmpty()) {
                _uiState.value = _uiState.value.copy(message = "Article import failed")
                return@launch
            }
            val now = timestamp()
            val displayNow = displayTimestamp()
            val lesson = Lesson(
                id = "${SHARED_POST_LESSON_ID}_${sourceLanguage.safeIdPart()}_${targetLanguage.safeIdPart()}_${UUID.randomUUID()}",
                title = "${sourceLanguage.shortLanguageCode()} - ${targetLanguage.shortLanguageCode()} Article $displayNow",
                lessonInfo = "Imported from URL/article. Basic/Native language: $sourceLanguage. Target/Learning language: $targetLanguage. Source text was limited to $URL_ARTICLE_WORD_LIMIT words and summarized into thesis cards.",
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                cards = drafts.mapIndexed { index, draft ->
                    Flashcard(
                        id = index + 1,
                        nativeValue = draft.front,
                        correctValue = draft.back,
                        hint = draft.hint.ifBlank { "Article thesis card for retelling." },
                        original = draft.original,
                        madeAt = now,
                        where = "URL article import",
                        log = listOf("$now - imported from URL article as ${draft.mode.ifBlank { "article_thesis" }}"),
                        type = "card",
                        cardKind = "LN",
                        sourceLanguage = draft.originalLanguage.lessonLanguageOrNull() ?: sourceLanguage,
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
                message = "Article cards: ${savedLesson.cards.size}"
            )
            saveCurrentStudySession()
            if (requestState.shouldUseOpenAiOnline()) {
                logOpenAiActivity(
                    action = "url article cards API",
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

    private fun translateCatAnswerCardToBasic(
        lessonId: String,
        cardId: Int,
        phrase: String,
        answerLanguage: String,
        basicLanguage: String
    ) {
        viewModelScope.launch {
            val requestState = _uiState.value
            val result = runCatching {
                requestConfiguredOnlineTranslation(phrase, answerLanguage, basicLanguage, requestState)
            }.getOrNull() ?: OpenAiTextResult("", "")
            val translated = result.text.trim()
            if (translated.isBlank()) {
                _uiState.value = _uiState.value.copy(message = "Cat answer card created. Basic translation pending")
                return@launch
            }
            if (result.isOpenAiSource()) {
                logOpenAiActivity(
                    action = "cat answer card translation ${result.source}",
                    details = "${requestState.openAiTextModel}: $answerLanguage -> $basicLanguage; $phrase => $translated"
                )
            }
            val lesson = repository.loadLessons(includeHidden = true).firstOrNull { it.id == lessonId }
                ?: return@launch
            val now = timestamp()
            val updatedLesson = lesson.copy(
                cards = lesson.cards.map { card ->
                    if (card.id == cardId) {
                        card.copy(
                            nativeValue = translated,
                            hint = buildTranslatorHint(
                                "Created from a Cat chat answer. The cat answer stays on the Target side; this Basic side was translated automatically.",
                                result.googleAttribution()
                            ),
                            log = (card.log + "$now - cat answer translated from $answerLanguage to $basicLanguage with ${result.providerLabel()}").takeLast(100)
                        )
                    } else {
                        card
                    }
                },
                lessonInfo = quickVocabularyLessonInfo(basicLanguage, answerLanguage),
                editable = true,
                hidden = false
            )
            repository.saveLesson(updatedLesson)
            val currentState = _uiState.value
            val lessons = repository.loadLessons(currentState.showHiddenLessons)
            _uiState.value = currentState.copy(
                lessons = lessons,
                selectedLesson = currentState.selectedLesson?.let { selected ->
                    lessons.firstOrNull { it.id == selected.id }
                } ?: currentState.selectedLesson,
                message = "${result.providerLabel()}: Cat answer card"
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
        if (!state.shouldUseOpenAiOnline()) return OpenAiTextResult("", "MurrLex server session required")
        return requestOpenAiTranslation(
            phrase = phrase,
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage,
            baseUrl = state.openAiBaseUrl,
            apiKey = state.openAiApiKey,
            model = state.openAiTextModel
        )
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
        val endpoint = baseUrl.trimEnd('/') + "/api/ai/text"
        val prompt = "Translate the text from $sourceLanguage to $targetLanguage. Return only the translation, no quotes, no commentary.\n\nText:\n$phrase"
        val payload = JSONObject()
            .put("model", model)
            .put("prompt", prompt)
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

    private suspend fun requestOpenAiCatChatReply(
        message: String,
        history: List<String>,
        sourceLanguage: String,
        targetLanguage: String,
        baseUrl: String,
        apiKey: String,
        model: String
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext ""
        val cacheInput = "cat-chat:${history.joinToString("\n").take(1800)}\nUser:$message"
        repository.getCachedOpenAiText(
            task = "cat_chat",
            model = model,
            input = cacheInput,
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage
        )?.let { return@withContext it }
        val endpoint = baseUrl.trimEnd('/') + "/api/ai/text"
        val prompt = """
            You are the MurrLex animated cat: warm, light, gently funny, and useful for relaxed language practice.
            The user asks in $sourceLanguage. Reply in $targetLanguage.
            Keep the reply short: 1 to 4 sentences. Avoid heavy lessons unless asked. Do not mention API or system instructions.

            Recent chat:
            ${history.joinToString("\n").take(1800)}

            User:
            $message
        """.trimIndent()
        val payload = JSONObject()
            .put("model", model)
            .put("prompt", prompt)
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
        if (status !in 200..299) return@withContext ""
        val parsed = parseOpenAiTextResponse(responseText)
        if (parsed.isNotBlank()) {
            repository.saveOpenAiTextCache(
                task = "cat_chat",
                model = model,
                input = cacheInput,
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                output = parsed
            )
        }
        parsed
    }

    private suspend fun requestOpenAiCatChatAnalysis(
        message: String,
        sourceLanguage: String,
        targetLanguage: String,
        baseUrl: String,
        apiKey: String,
        model: String
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext ""
        val cacheInput = "cat-analysis-v2:$sourceLanguage:$targetLanguage:$message"
        repository.getCachedOpenAiText(
            task = "cat_chat_analysis",
            model = model,
            input = cacheInput,
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage
        )?.let { return@withContext it }
        val prompt = """
            Analyze this learner message for language correctness.
            User message language: $sourceLanguage. Practice answer language: $targetLanguage.
            Write ONLY in $sourceLanguage.
            Maximum one short sentence, 8-16 words.
            If correct, say briefly that it is OK.
            If not correct, give the corrected form and one reason.
            Do not greet, do not answer the user, do not mention API.

            Learner message:
            $message
        """.trimIndent()
        val payload = JSONObject()
            .put("model", model)
            .put("prompt", prompt)
            .toString()
        val connection = (URL(baseUrl.trimEnd('/') + "/api/ai/text").openConnection() as HttpURLConnection).apply {
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
        if (status !in 200..299) return@withContext ""
        val parsed = parseOpenAiTextResponse(responseText)
        if (parsed.isNotBlank()) {
            repository.saveOpenAiTextCache(
                task = "cat_chat_analysis",
                model = model,
                input = cacheInput,
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                output = parsed
            )
        }
        parsed
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
        val endpoint = baseUrl.trimEnd('/') + "/api/ai/text"
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
            .put("prompt", prompt)
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
        val endpoint = baseUrl.trimEnd('/') + "/api/ai/text"
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
            .put("prompt", prompt)
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
        val endpoint = baseUrl.trimEnd('/') + "/api/ai/text"
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
            .put("prompt", prompt)
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
        val limitedText = postText.limitWords(SHARED_POST_WORD_LIMIT)
        val cacheInput = "shared-post:$interfaceLanguage:$sourceLanguage:$targetLanguage:$limitedText"
        repository.getCachedOpenAiText(
            task = "shared_post_cards",
            model = model,
            input = cacheInput,
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage
        )?.let { cached -> return@withContext parseSharedPostCardDrafts(cached) }
        val endpoint = baseUrl.trimEnd('/') + "/api/ai/text"
        val prompt = """
            Convert this shared social post into MurrLex retelling flashcards.
            Use only the first $SHARED_POST_WORD_LIMIT words.
            Basic/Native language: $sourceLanguage
            Target/Learning language: $targetLanguage
            User interface language for hints: $interfaceLanguage

            Rules:
            1. Create one flashcard for each meaningful source sentence.
            2. Skip only empty, duplicated, service, navigation, or purely decorative text.
            3. Each card must be a Basic -> Target pair:
               front = the sentence in $sourceLanguage.
               back = natural translation of the same sentence in $targetLanguage.
            4. If the post is already in $targetLanguage, still make front in $sourceLanguage and back in $targetLanguage.
            5. If the source sentence is not in $sourceLanguage, set original to that exact source sentence and originalLanguage to its language name. Otherwise original must be empty.
            6. Hints must be in the user's interface language: $interfaceLanguage, and should briefly name the sentence's role in the retelling.
            7. Keep front/back concise enough for a flashcard.
            8. Return strict JSON only with key cards. cards is an array of objects with keys: front, back, hint, mode, original, originalLanguage.
               mode must be retelling_sentence.

            Post:
            $limitedText
        """.trimIndent()
        val payload = JSONObject()
            .put("model", model)
            .put("prompt", prompt)
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

    private suspend fun requestOpenAiImageText(
        imageUri: Uri,
        interfaceLanguage: String,
        baseUrl: String,
        apiKey: String,
        model: String
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext ""
        val resolver = getApplication<Application>().contentResolver
        val mimeType = resolver.getType(imageUri)?.takeIf { it.startsWith("image/") } ?: "image/jpeg"
        val bytes = resolver.openInputStream(imageUri)?.use { it.readBytes() } ?: return@withContext ""
        if (bytes.isEmpty() || bytes.size > IMAGE_TEXT_MAX_BYTES) return@withContext ""
        val imageHash = bytes.sha256Hex()
        val cacheInput = "image-text:$interfaceLanguage:$mimeType:$imageHash"
        repository.getCachedOpenAiText(
            task = "image_text_ocr",
            model = model,
            input = cacheInput,
            sourceLanguage = "",
            targetLanguage = ""
        )?.let { cached -> return@withContext cached }
        val endpoint = baseUrl.trimEnd('/') + "/api/ai/image-text"
        val prompt = """
            Extract readable text from this screenshot or photo for MurrLex.
            User interface language: $interfaceLanguage

            Rules:
            1. Return only the text that is visible in the image.
            2. Preserve the reading order, paragraphs, and punctuation when possible.
            3. Skip app chrome, decorative icons, buttons, and unrelated navigation if they are not part of the main text.
            4. Do not describe the image.
            5. If there is no readable text, return an empty string.
        """.trimIndent()
        val boundary = "MurrLexImage${System.currentTimeMillis()}"
        val lineBreak = "\r\n"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 60_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "MurrLex/${BuildConfig.VERSION_NAME}")
        }
        connection.outputStream.use { output ->
            fun field(name: String, value: String) {
                output.write("--$boundary$lineBreak".toByteArray(Charsets.UTF_8))
                output.write("Content-Disposition: form-data; name=\"$name\"$lineBreak$lineBreak".toByteArray(Charsets.UTF_8))
                output.write(value.toByteArray(Charsets.UTF_8))
                output.write(lineBreak.toByteArray(Charsets.UTF_8))
            }
            field("model", model)
            field("prompt", prompt)
            output.write("--$boundary$lineBreak".toByteArray(Charsets.UTF_8))
            output.write("Content-Disposition: form-data; name=\"image\"; filename=\"image.jpg\"$lineBreak".toByteArray(Charsets.UTF_8))
            output.write("Content-Type: $mimeType$lineBreak$lineBreak".toByteArray(Charsets.UTF_8))
            output.write(bytes)
            output.write(lineBreak.toByteArray(Charsets.UTF_8))
            output.write("--$boundary--$lineBreak".toByteArray(Charsets.UTF_8))
        }
        val status = connection.responseCode
        val responseText = (if (status in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { it.readText() }
            .orEmpty()
        connection.disconnect()
        if (status !in 200..299) return@withContext ""
        val parsedText = runCatching { JSONObject(responseText).optString("text") }.getOrDefault("").trim()
        if (parsedText.isNotBlank()) {
            repository.saveOpenAiTextCache(
                task = "image_text_ocr",
                model = model,
                input = cacheInput,
                sourceLanguage = "",
                targetLanguage = "",
                output = parsedText
            )
        }
        parsedText
    }

    private suspend fun requestOpenAiSharedArticleCards(
        articleText: String,
        sourceLanguage: String,
        targetLanguage: String,
        interfaceLanguage: String,
        baseUrl: String,
        apiKey: String,
        model: String
    ): List<SharedPostCardDraft> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext emptyList()
        val limitedText = articleText.limitWords(URL_ARTICLE_WORD_LIMIT)
        val cacheInput = "url-article:$interfaceLanguage:$sourceLanguage:$targetLanguage:$limitedText"
        repository.getCachedOpenAiText(
            task = "url_article_cards",
            model = model,
            input = cacheInput,
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage
        )?.let { cached -> return@withContext parseSharedPostCardDrafts(cached) }
        val endpoint = baseUrl.trimEnd('/') + "/api/ai/text"
        val prompt = """
            Convert this article or ordinary website text into MurrLex thesis flashcards.
            Use only the first $URL_ARTICLE_WORD_LIMIT words.
            Basic/Native language: $sourceLanguage
            Target/Learning language: $targetLanguage
            User interface language for hints: $interfaceLanguage

            Rules:
            1. Prepare concise theses of the article, roughly one thesis per 50 source words.
            2. Select the main arguments, facts, causes, consequences, and conclusions.
            3. Each card must be a Basic -> Target pair:
               front = one thesis in $sourceLanguage.
               back = natural translation of the same thesis in $targetLanguage.
            4. If the source text is already in $targetLanguage, still make front in $sourceLanguage and back in $targetLanguage.
            5. If the source thesis is not in $sourceLanguage, set original to the closest source-language thesis/sentence and originalLanguage to its language name. Otherwise original must be empty.
            6. Hints must be in the user's interface language: $interfaceLanguage, and should briefly name the thesis role in the article.
            7. Return strict JSON only with key cards. cards is an array of objects with keys: front, back, hint, mode, original, originalLanguage.
               mode must be article_thesis.

            Article:
            $limitedText
        """.trimIndent()
        val payload = JSONObject()
            .put("model", model)
            .put("prompt", prompt)
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
                task = "url_article_cards",
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
        val limitedText = postText.limitWords(SHARED_POST_WORD_LIMIT)
        return limitedText.splitSharedPostSentences()
            .mapNotNull { sentence ->
                val translated = runCatching {
                    requestConfiguredOnlineTranslation(sentence, sourceLanguage, targetLanguage, state).text.trim()
                }.getOrDefault("")
                if (translated.isBlank()) null else {
                    buildOriginalAwareDraft(
                        originalText = sentence,
                        sourceLanguage = sourceLanguage,
                        targetLanguage = targetLanguage,
                        state = state,
                        hint = "Shared post sentence card for retelling.",
                        mode = "retelling_sentence"
                    ) ?: SharedPostCardDraft(
                        front = sentence,
                        back = translated,
                        hint = "Shared post sentence card for retelling.",
                        mode = "retelling_sentence"
                    )
                }
            }
    }

    private suspend fun buildSharedArticleCardsWithTranslator(
        articleText: String,
        sourceLanguage: String,
        targetLanguage: String,
        state: StudyUiState
    ): List<SharedPostCardDraft> {
        val limitedText = articleText.limitWords(URL_ARTICLE_WORD_LIMIT)
        return limitedText.splitIntoApproximateTheses(50).mapNotNull { thesis ->
            val translated = runCatching {
                requestConfiguredOnlineTranslation(thesis, sourceLanguage, targetLanguage, state).text.trim()
            }.getOrDefault("")
            if (translated.isBlank()) null else {
                buildOriginalAwareDraft(
                    originalText = thesis,
                    sourceLanguage = sourceLanguage,
                    targetLanguage = targetLanguage,
                    state = state,
                    hint = "Article thesis card for retelling.",
                    mode = "article_thesis"
                ) ?: SharedPostCardDraft(
                    front = thesis,
                    back = translated,
                    hint = "Article thesis card for retelling.",
                    mode = "article_thesis"
                )
            }
        }
    }

    private suspend fun buildOriginalAwareDraft(
        originalText: String,
        sourceLanguage: String,
        targetLanguage: String,
        state: StudyUiState,
        hint: String,
        mode: String
    ): SharedPostCardDraft? {
        val cleanOriginal = originalText.trim()
        if (cleanOriginal.isBlank()) return null
        val detectedLanguage = detectTextLanguageName(cleanOriginal)
        val markOriginal = shouldMarkOriginalLanguage(detectedLanguage, sourceLanguage)
        if (!markOriginal) return null
        val front = runCatching {
            requestConfiguredOnlineTranslation(cleanOriginal, detectedLanguage, sourceLanguage, state).text.trim()
        }.getOrDefault("").ifBlank { "Translation pending" }
        val back = if (sameLanguageName(detectedLanguage, targetLanguage)) {
            cleanOriginal
        } else {
            runCatching {
                requestConfiguredOnlineTranslation(cleanOriginal, detectedLanguage, targetLanguage, state).text.trim()
            }.getOrDefault("").ifBlank { "Empty" }
        }
        return SharedPostCardDraft(
            front = front,
            back = back,
            hint = "$hint\nOriginal language: $detectedLanguage.",
            mode = mode,
            original = cleanOriginal,
            originalLanguage = detectedLanguage
        )
    }

    private suspend fun fetchSharedPostUrlText(urlText: String, wordLimit: Int): String = withContext(Dispatchers.IO) {
        val connection = (URL(urlText).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 25_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "text/html,text/plain,application/xhtml+xml")
            setRequestProperty("User-Agent", "MurrLex/${BuildConfig.VERSION_NAME}")
        }
        val status = connection.responseCode
        val contentType = connection.contentType.orEmpty().lowercase(Locale.ROOT)
        val body = (if (status in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { it.readText() }
            .orEmpty()
        connection.disconnect()
        if (status !in 200..299) return@withContext ""
        val text = if (contentType.contains("html")) {
            body.extractReadableSharedPostText(urlText)
        } else {
            body
        }
        text.normalizeSharedText().limitWords(wordLimit)
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
                        mode = cardJson.optString("mode").trim().ifBlank { "sentence_translation" },
                        original = cardJson.optString("original").trim(),
                        originalLanguage = cardJson.optString("originalLanguage").trim()
                            .ifBlank {
                                cardJson.optString("original_language").trim()
                            }
                    )
                )
            }
        }
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
        root.optString("output").trim().takeIf { it.isNotBlank() }?.let { return it }
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
        return useOpenAiModels && openAiBaseUrl.isNotBlank() && openAiApiKey.isNotBlank()
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
            normalized == "mixed" -> "MX"
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

    private fun localCatChatReply(targetLanguage: String): String {
        return when (freeOnlineLanguageCode(targetLanguage)) {
            "ru" -> "Я тут, мурчу и поддерживаю. Спроси что-нибудь ещё, а я отвечу коротко и без занудства."
            "pl" -> "Jestem tutaj i lekko mrucze. Zapytaj jeszcze raz, a odpowiem krótko i bez ciężkiej miny."
            "be" -> "Я тут і мякка мурчу. Спытай яшчэ што-небудзь, адкажу коратка і без занудства."
            "es" -> "Estoy aquí, ronroneando suave. Pregunta algo más y respondo corto, sin drama."
            "de" -> "Ich bin da und schnurre leise. Frag ruhig weiter, ich antworte kurz und entspannt."
            "lv" -> "Es esmu te un klusi murrāju. Pajautā vēl, atbildēšu īsi un mierīgi."
            "lt" -> "Aš čia ir tyliai murkiu. Klausk dar, atsakysiu trumpai ir lengvai."
            "pt" -> "Estou aqui, ronronando de leve. Pergunta mais alguma coisa e respondo curto, sem drama."
            else -> "I am here, softly purring. Ask me one more thing and I will keep it light and useful."
        }
    }

    private fun localCatChatAnalysis(sourceLanguage: String): String {
        return when (freeOnlineLanguageCode(sourceLanguage)) {
            "ru" -> "Коротко: проверь форму слова и порядок слов."
            "pl" -> "Krotko: sprawdz forme slowa i szyk zdania."
            "be" -> "Коратка: правер форму слова і парадак слоў."
            "es" -> "Breve: revisa la forma de la palabra y el orden."
            "de" -> "Kurz: pruefe Wortform und Satzstellung."
            "lv" -> "Isi: parbaudi varda formu un vardu secibu."
            "lt" -> "Trumpai: patikrink zodzio forma ir zodziu tvarka."
            "pt" -> "Curto: confira a forma da palavra e a ordem."
            else -> "Quick check: review word form and order."
        }
    }

    private fun String.selectionHint(): String {
        val clean = trim()
        if (clean.isBlank()) return "..."
        val words = clean.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.size > 1) {
            return words.take(6).joinToString(" ") { word ->
                if (word.length <= 2) "${word.firstOrNull() ?: '?'}..."
                else "${word.first()}...${word.last()}"
            } + if (words.size > 6) " ..." else ""
        }
        return if (clean.length <= 2) {
            "${clean.first()}..."
        } else {
            "${clean.first()}...${clean.last()}"
        }
    }

    private fun String.limitWords(limit: Int): String {
        return trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .take(limit)
            .joinToString(" ")
    }

    private fun normalizeSharedPostUrl(rawUrl: String): String {
        val clean = rawUrl.trim()
        if (clean.isBlank()) return ""
        return when {
            clean.startsWith("http://", ignoreCase = true) ||
                clean.startsWith("https://", ignoreCase = true) -> clean
            clean.contains('.') -> "https://$clean"
            else -> ""
        }
    }

    private fun detectTextLanguageName(text: String): String {
        val lower = text.lowercase(Locale.ROOT)
        val languageHits = buildList {
            if (lower.any { it in "\u045e" } || Regex("\\b(\u0433\u044d\u0442\u0430|\u0456|\u044f\u043a\u0456|\u0448\u0442\u043e|\u0434\u0437\u0435)\\b").containsMatchIn(lower)) add("Belarusian")
            if (lower.any { it in "\u0457\u0454\u0491" }) add("Ukrainian")
            if (lower.any { it in "\u0105\u0107\u0119\u0142\u0144\u00f3\u015b\u017a\u017c" }) add("Polish")
            if (lower.any { it in "\u0101\u0113\u0123\u012b\u0137\u013c\u0146\u016b" }) add("Latvian")
            if (lower.any { it in "\u0117\u012f\u0173" }) add("Lithuanian")
            if (lower.any { it in "\u00e3\u00f5\u00e7" }) add("Portuguese")
            if (lower.any { it in "\u00e4\u00f6\u00fc\u00df" }) add("German")
            if (lower.any { it in "\u00f1\u00bf\u00a1" }) add("Spanish")
            if (lower.any { it in '\u0430'..'\u044f' || it == '\u0451' } &&
                none { it in listOf("Belarusian", "Ukrainian") }
            ) add("Russian")
            if (lower.any { it in 'a'..'z' } && isEmpty()) add("English")
        }.distinct()
        return when {
            languageHits.isEmpty() -> "Mixed"
            languageHits.size > 1 -> "Mixed"
            else -> languageHits.first()
        }
    }

    private fun sameLanguageName(left: String, right: String): Boolean {
        return freeOnlineLanguageCode(left).equals(freeOnlineLanguageCode(right), ignoreCase = true) ||
            left.shortLanguageCode().equals(right.shortLanguageCode(), ignoreCase = true)
    }

    private fun shouldMarkOriginalLanguage(detectedLanguage: String, basicLanguage: String): Boolean {
        if (detectedLanguage.isBlank() || detectedLanguage.equals("Mixed", ignoreCase = true)) return true
        return !sameLanguageName(detectedLanguage, basicLanguage)
    }

    private fun telegramPublicPostUrl(urlText: String): String? {
        val clean = urlText.trim()
        val match = Regex("""https?://t\.me/([^/?#]+)/(\d+)""", RegexOption.IGNORE_CASE).find(clean) ?: return null
        val channel = match.groupValues[1]
        val postId = match.groupValues[2]
        return "https://t.me/s/$channel/$postId"
    }

    private fun String.extractReadableSharedPostText(sourceUrl: String): String {
        val candidates = buildList {
            extractTelegramWidgetMessageText().takeIf { it.isMeaningfulSharedText() }?.let(::add)
            extractMetaDescriptionText().takeIf { it.isMeaningfulSharedText() }?.let(::add)
            extractArticleBodyText().takeIf { it.isMeaningfulSharedText() }?.let(::add)
            extractParagraphText().takeIf { it.isMeaningfulSharedText() }?.let(::add)
            htmlToPlainText().takeIf { it.isMeaningfulSharedText() }?.let(::add)
        }
        val best = candidates.maxByOrNull { it.sharedTextScore() }.orEmpty()
        return if (sourceUrl.contains("t.me/", ignoreCase = true) && !best.isMeaningfulSharedText()) {
            ""
        } else {
            best
        }
    }

    private fun String.extractArticleBodyText(): String {
        val cleaned = removeHtmlBoilerplate()
        val blocks = buildList {
            Regex("(?is)<article\\b[^>]*>(.*?)</article>").findAll(cleaned).forEach { match ->
                add(match.groupValues[1].htmlToPlainText())
            }
            Regex("(?is)<main\\b[^>]*>(.*?)</main>").findAll(cleaned).forEach { match ->
                add(match.groupValues[1].htmlToPlainText())
            }
            Regex("(?is)<div\\b[^>]*(?:class|id)\\s*=\\s*['\"][^'\"]*(?:article|content|post|news|text|story)[^'\"]*['\"][^>]*>(.*?)</div>").findAll(cleaned).forEach { match ->
                add(match.groupValues[1].htmlToPlainText())
            }
        }.filter { it.isMeaningfulSharedText() }
        return blocks.maxByOrNull { it.sharedTextScore() }.orEmpty()
    }

    private fun String.extractParagraphText(): String {
        val paragraphs = Regex("(?is)<p\\b[^>]*>(.*?)</p>").findAll(removeHtmlBoilerplate())
            .map { it.groupValues[1].htmlToPlainText() }
            .filter { paragraph ->
                paragraph.isMeaningfulSharedText() &&
                    !paragraph.contains("cookie", ignoreCase = true) &&
                    !paragraph.contains("advert", ignoreCase = true)
            }
            .toList()
        return paragraphs.joinToString(" ").normalizeSharedText()
    }

    private fun String.removeHtmlBoilerplate(): String {
        return replace(Regex("(?is)<script[^>]*>.*?</script>"), " ")
            .replace(Regex("(?is)<style[^>]*>.*?</style>"), " ")
            .replace(Regex("(?is)<noscript[^>]*>.*?</noscript>"), " ")
            .replace(Regex("(?is)<header[^>]*>.*?</header>"), " ")
            .replace(Regex("(?is)<footer[^>]*>.*?</footer>"), " ")
            .replace(Regex("(?is)<nav[^>]*>.*?</nav>"), " ")
            .replace(Regex("(?is)<aside[^>]*>.*?</aside>"), " ")
            .replace(Regex("(?is)<form[^>]*>.*?</form>"), " ")
    }

    private fun String.extractTelegramWidgetMessageText(): String {
        val messageBlocks = Regex(
            "(?is)<[^>]+class\\s*=\\s*['\"][^'\"]*tgme_widget_message_text[^'\"]*['\"][^>]*>(.*?)</[^>]+>"
        ).findAll(this).map { match ->
            match.groupValues[1].htmlToPlainText()
        }.filter { it.isMeaningfulSharedText() }.toList()
        return messageBlocks.maxByOrNull { it.sharedTextScore() }.orEmpty()
    }

    private fun String.extractMetaDescriptionText(): String {
        val descriptions = Regex("(?is)<meta\\s+([^>]+)>").findAll(this).mapNotNull { match ->
            val attrs = match.groupValues[1]
            val property = attrs.htmlAttr("property").lowercase(Locale.ROOT)
            val name = attrs.htmlAttr("name").lowercase(Locale.ROOT)
            val content = attrs.htmlAttr("content").decodeBasicHtmlEntities().normalizeSharedText()
            when {
                content.isBlank() -> null
                property in listOf("og:description", "twitter:description") -> content
                name in listOf("description", "twitter:description") -> content
                else -> null
            }
        }.filter { it.isMeaningfulSharedText() }.toList()
        return descriptions.maxByOrNull { it.sharedTextScore() }.orEmpty()
    }

    private fun String.htmlAttr(name: String): String {
        return Regex("(?is)\\b${Regex.escape(name)}\\s*=\\s*(['\"])(.*?)\\1")
            .find(this)
            ?.groupValues
            ?.getOrNull(2)
            .orEmpty()
    }

    private fun String.htmlToPlainText(): String {
        return removeHtmlBoilerplate()
            .replace(Regex("(?is)<br\\s*/?>|</p>|</div>|</li>|</h[1-6]>"), ". ")
            .replace(Regex("(?is)<[^>]+>"), " ")
            .decodeBasicHtmlEntities()
            .normalizeSharedText()
    }

    private fun String.decodeBasicHtmlEntities(): String {
        return replace(Regex("&#(\\d+);")) { match ->
                match.groupValues[1].toIntOrNull()?.toChar()?.toString().orEmpty()
            }
            .replace(Regex("&#x([0-9a-fA-F]+);")) { match ->
                match.groupValues[1].toIntOrNull(16)?.toChar()?.toString().orEmpty()
            }
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
    }

    private fun String.normalizeSharedText(): String {
        return replace(Regex("(?is)<script[^>]*>.*?</script>"), " ")
            .replace(Regex("https?://\\S+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun String.isMeaningfulSharedText(): Boolean {
        val clean = normalizeSharedText()
        if (clean.split(Regex("\\s+")).count { it.isNotBlank() } < 4) return false
        if (clean.contains("<script", ignoreCase = true)) return false
        val serviceNoise = listOf(
            "telegram: view",
            "download",
            "context",
            "embed",
            "view in telegram",
            "data-telegram-post"
        )
        val lower = clean.lowercase(Locale.ROOT)
        if (serviceNoise.count { lower.contains(it) } >= 2) return false
        return clean.any { it.isLetter() }
    }

    private fun String.sharedTextScore(): Int {
        val clean = normalizeSharedText()
        val cyrillic = clean.count { it in '\u0400'..'\u04FF' }
        val letters = clean.count { it.isLetter() }
        val punctuation = clean.count { it in ".!?;:,-" }
        val noisePenalty = listOf("telegram", "download", "embed", "script", "view").count {
            clean.contains(it, ignoreCase = true)
        } * 25
        return cyrillic * 3 + letters + punctuation * 2 - noisePenalty
    }

    private fun String.splitSharedPostSentences(): List<String> {
        return replace('\n', ' ')
            .split(Regex("(?<=[.!?。！？])\\s+|(?<=[.!?])"))
            .map { it.trim().trim('-', '–', '—') }
            .filter { it.length >= 2 }
            .ifEmpty { listOf(trim()) }
    }

    private fun String.splitIntoApproximateTheses(targetWords: Int): List<String> {
        val theses = mutableListOf<String>()
        val buffer = mutableListOf<String>()
        var wordCount = 0
        splitSharedPostSentences().forEach { sentence ->
            val sentenceWords = sentence.split(Regex("\\s+")).count { it.isNotBlank() }
            if (buffer.isNotEmpty() && wordCount + sentenceWords > targetWords) {
                theses += buffer.joinToString(" ")
                buffer.clear()
                wordCount = 0
            }
            buffer += sentence
            wordCount += sentenceWords
        }
        if (buffer.isNotEmpty()) theses += buffer.joinToString(" ")
        return theses
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
        private const val SHARED_POST_WORD_LIMIT = 500
        private const val URL_ARTICLE_WORD_LIMIT = 2_000
        private const val IMAGE_TEXT_MAX_BYTES = 12 * 1024 * 1024
        private const val IMAGE_TEXT_MAX_IMAGE_COUNT = 5
        private const val GOOGLE_TRANSLATE_ATTRIBUTION = "Powered by Google Translator"
            }
}

private fun ByteArray.sha256Hex(): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(this)
    return digest.joinToString("") { byte -> "%02x".format(byte) }
}

private fun WorkMode.displayLabel(): String {
    return when (this) {
        WorkMode.CARDS -> "Cards"
        WorkMode.TESTS -> "Tests"
        WorkMode.TRANSLATE -> "Translate"
        WorkMode.SPLIT -> "Split"
    }
}
