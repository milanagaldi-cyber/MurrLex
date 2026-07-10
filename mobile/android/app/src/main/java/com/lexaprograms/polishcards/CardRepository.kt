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
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.random.Random

class CardRepository(private val context: Context) {
    private val preferences = context.getSharedPreferences("polish_cards_store", Context.MODE_PRIVATE)
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    init {
        // Provider keys belong exclusively to the MurrLex server.
        preferences.edit().remove(KEY_ELEVENLABS_API_KEY).apply()
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

    fun loadDisplayTextSize(): DisplayTextSize {
        val raw = preferences.getString(KEY_DISPLAY_TEXT_SIZE, DisplayTextSize.MEDIUM.name)
        return runCatching { DisplayTextSize.valueOf(raw ?: DisplayTextSize.MEDIUM.name) }
            .getOrDefault(DisplayTextSize.MEDIUM)
    }

    fun saveDisplayTextSize(size: DisplayTextSize) {
        preferences.edit().putString(KEY_DISPLAY_TEXT_SIZE, size.name).apply()
    }

    fun loadControlSize(): ControlSize {
        val raw = preferences.getString(KEY_CONTROL_SIZE, ControlSize.MEDIUM.name)
        return runCatching { ControlSize.valueOf(raw ?: ControlSize.MEDIUM.name) }
            .getOrDefault(ControlSize.MEDIUM)
    }

    fun saveControlSize(size: ControlSize) {
        preferences.edit().putString(KEY_CONTROL_SIZE, size.name).apply()
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

    fun loadOnboardingCompleted(): Boolean {
        return preferences.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun saveOnboardingCompleted(completed: Boolean) {
        preferences.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    fun loadQuickVocabularySourceLanguage(): String {
        return preferences.getString(KEY_QUICK_VOCABULARY_SOURCE_LANGUAGE, "Russian") ?: "Russian"
    }

    fun saveQuickVocabularySourceLanguage(language: String) {
        preferences.edit().putString(KEY_QUICK_VOCABULARY_SOURCE_LANGUAGE, language.trim().ifBlank { "Russian" }).apply()
    }

    fun loadQuickVocabularyTargetLanguage(): String {
        return preferences.getString(KEY_QUICK_VOCABULARY_TARGET_LANGUAGE, "Polish") ?: "Polish"
    }

    fun saveQuickVocabularyTargetLanguage(language: String) {
        preferences.edit().putString(KEY_QUICK_VOCABULARY_TARGET_LANGUAGE, language.trim().ifBlank { "Polish" }).apply()
    }



    fun loadUseLocalTranslation(): Boolean {
        return preferences.getBoolean(KEY_USE_LOCAL_TRANSLATION, false)
    }

    fun saveUseLocalTranslation(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_USE_LOCAL_TRANSLATION, enabled).apply()
    }

    fun loadAutoSaveTranslatorCards(): Boolean {
        return preferences.getBoolean(KEY_AUTO_SAVE_TRANSLATOR_CARDS, false)
    }

    fun saveAutoSaveTranslatorCards(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_AUTO_SAVE_TRANSLATOR_CARDS, enabled).apply()
    }

    fun loadTranslationApiUrl(): String {
        return loadOpenAiBaseUrl()
    }

    fun saveTranslationApiUrl(url: String) {
        saveOpenAiBaseUrl(url)
    }

    fun loadTranslationApiToken(): String {
        return loadOpenAiApiKey()
    }

    fun saveTranslationApiToken(token: String) {
        saveOpenAiApiKey(token)
    }

    fun loadUseOpenAiModels(): Boolean {
        return preferences.getBoolean(KEY_USE_OPENAI_MODELS, true)
    }

    fun saveUseOpenAiModels(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_USE_OPENAI_MODELS, enabled).apply()
    }

    fun loadMurrLexServerSession(): MurrLexServerSession {
        return MurrLexServerSession(
            serverUrl = preferences.getString(KEY_MURRLEX_SERVER_URL, DEFAULT_MURRLEX_SERVER_URL)
                ?.trim()
                ?.trimEnd('/')
                .orEmpty(),
            accessToken = SecureSessionValue.decrypt(preferences.getString(KEY_MURRLEX_SERVER_ACCESS_TOKEN, "").orEmpty()),
            refreshToken = SecureSessionValue.decrypt(preferences.getString(KEY_MURRLEX_SERVER_REFRESH_TOKEN, "").orEmpty()),
            accessExpiresAtMillis = preferences.getLong(KEY_MURRLEX_SERVER_ACCESS_EXPIRES_AT, 0L),
            username = preferences.getString(KEY_MURRLEX_SERVER_USERNAME, "").orEmpty(),
            email = preferences.getString(KEY_MURRLEX_SERVER_EMAIL, "").orEmpty()
        )
    }

    fun saveMurrLexServerSession(session: MurrLexServerSession) {
        preferences.edit()
            .putString(KEY_MURRLEX_SERVER_URL, session.serverUrl.trim().trimEnd('/'))
            .putString(KEY_MURRLEX_SERVER_ACCESS_TOKEN, SecureSessionValue.encrypt(session.accessToken))
            .putString(KEY_MURRLEX_SERVER_REFRESH_TOKEN, SecureSessionValue.encrypt(session.refreshToken))
            .putLong(KEY_MURRLEX_SERVER_ACCESS_EXPIRES_AT, session.accessExpiresAtMillis)
            .putString(KEY_MURRLEX_SERVER_USERNAME, session.username)
            .putString(KEY_MURRLEX_SERVER_EMAIL, session.email)
            .remove(KEY_OPENAI_API_KEY)
            .remove(KEY_ELEVENLABS_API_KEY)
            .apply()
    }

    fun saveMurrLexServerUrl(serverUrl: String) {
        preferences.edit().putString(KEY_MURRLEX_SERVER_URL, serverUrl.trim().trimEnd('/')).apply()
    }

    fun clearMurrLexServerSession() {
        preferences.edit()
            .remove(KEY_MURRLEX_SERVER_ACCESS_TOKEN)
            .remove(KEY_MURRLEX_SERVER_REFRESH_TOKEN)
            .remove(KEY_MURRLEX_SERVER_ACCESS_EXPIRES_AT)
            .remove(KEY_MURRLEX_SERVER_USERNAME)
            .remove(KEY_MURRLEX_SERVER_EMAIL)
            .remove(KEY_OPENAI_API_KEY)
            .remove(KEY_ELEVENLABS_API_KEY)
            .apply()
    }

    fun loadOpenAiBaseUrl(): String {
        return loadMurrLexServerSession().serverUrl
    }

    fun saveOpenAiBaseUrl(url: String) {
        saveMurrLexServerUrl(url)
    }

    fun loadOpenAiApiKey(): String {
        return loadMurrLexServerSession().accessToken
    }

    fun saveOpenAiApiKey(apiKey: String) {
        // Provider API keys are intentionally not stored on the device anymore.
    }

    fun loadOpenAiSpeechModel(): String {
        return preferences.getString(KEY_OPENAI_SPEECH_MODEL, DEFAULT_OPENAI_SPEECH_MODEL)
            ?: DEFAULT_OPENAI_SPEECH_MODEL
    }

    fun saveOpenAiSpeechModel(model: String) {
        preferences.edit().putString(KEY_OPENAI_SPEECH_MODEL, model.trim().ifBlank { DEFAULT_OPENAI_SPEECH_MODEL }).apply()
    }

    fun loadOpenAiTextModel(): String {
        return preferences.getString(KEY_OPENAI_TEXT_MODEL, DEFAULT_OPENAI_TEXT_MODEL)
            ?: DEFAULT_OPENAI_TEXT_MODEL
    }

    fun saveOpenAiTextModel(model: String) {
        preferences.edit().putString(KEY_OPENAI_TEXT_MODEL, model.trim().ifBlank { DEFAULT_OPENAI_TEXT_MODEL }).apply()
    }

    fun loadOpenAiImageTextModel(): String {
        return preferences.getString(KEY_OPENAI_IMAGE_TEXT_MODEL, DEFAULT_OPENAI_IMAGE_TEXT_MODEL)
            ?: DEFAULT_OPENAI_IMAGE_TEXT_MODEL
    }

    fun saveOpenAiImageTextModel(model: String) {
        preferences.edit().putString(KEY_OPENAI_IMAGE_TEXT_MODEL, model.trim().ifBlank { DEFAULT_OPENAI_IMAGE_TEXT_MODEL }).apply()
    }

    fun loadOpenAiTtsModel(): String {
        return preferences.getString(KEY_OPENAI_TTS_MODEL, DEFAULT_OPENAI_TTS_MODEL)
            ?: DEFAULT_OPENAI_TTS_MODEL
    }

    fun saveOpenAiTtsModel(model: String) {
        preferences.edit().putString(KEY_OPENAI_TTS_MODEL, model.trim().ifBlank { DEFAULT_OPENAI_TTS_MODEL }).apply()
    }

    fun loadOpenAiTtsVoice(): String {
        return preferences.getString(KEY_OPENAI_TTS_VOICE, DEFAULT_OPENAI_TTS_VOICE)
            ?: DEFAULT_OPENAI_TTS_VOICE
    }

    fun saveOpenAiTtsVoice(voice: String) {
        preferences.edit().putString(KEY_OPENAI_TTS_VOICE, voice.trim().ifBlank { DEFAULT_OPENAI_TTS_VOICE }).apply()
    }

    fun loadBelarusianTtsProvider(): String {
        return preferences.getString(KEY_BELARUSIAN_TTS_PROVIDER, DEFAULT_BELARUSIAN_TTS_PROVIDER)
            ?: DEFAULT_BELARUSIAN_TTS_PROVIDER
    }

    fun saveBelarusianTtsProvider(provider: String) {
        preferences.edit()
            .putString(KEY_BELARUSIAN_TTS_PROVIDER, provider.trim().ifBlank { DEFAULT_BELARUSIAN_TTS_PROVIDER })
            .apply()
    }

    fun loadElevenLabsApiKey(): String {
        return ""
    }

    fun saveElevenLabsApiKey(apiKey: String) {
        preferences.edit().remove(KEY_ELEVENLABS_API_KEY).apply()
    }

    fun loadElevenLabsModel(): String {
        return preferences.getString(KEY_ELEVENLABS_MODEL, DEFAULT_ELEVENLABS_MODEL)
            ?: DEFAULT_ELEVENLABS_MODEL
    }

    fun saveElevenLabsModel(model: String) {
        preferences.edit().putString(KEY_ELEVENLABS_MODEL, model.trim().ifBlank { DEFAULT_ELEVENLABS_MODEL }).apply()
    }

    fun loadElevenLabsVoiceId(): String {
        return preferences.getString(KEY_ELEVENLABS_VOICE_ID, DEFAULT_ELEVENLABS_BELARUSIAN_VOICE_ID)
            ?: DEFAULT_ELEVENLABS_BELARUSIAN_VOICE_ID
    }

    fun saveElevenLabsVoiceId(voiceId: String) {
        preferences.edit()
            .putString(KEY_ELEVENLABS_VOICE_ID, voiceId.trim().ifBlank { DEFAULT_ELEVENLABS_BELARUSIAN_VOICE_ID })
            .apply()
    }

    fun loadElevenLabsTtsLanguageCodes(): Set<String> {
        val raw = preferences.getString(KEY_ELEVENLABS_TTS_LANGUAGE_CODES, null)
        if (raw.isNullOrBlank()) return DEFAULT_ELEVENLABS_TTS_LANGUAGE_CODES
        return runCatching {
            val array = JSONArray(raw)
            buildSet {
                for (index in 0 until array.length()) {
                    val code = array.optString(index).trim().lowercase(Locale.ROOT)
                    if (code.isNotBlank()) add(code)
                }
            }.ifEmpty { DEFAULT_ELEVENLABS_TTS_LANGUAGE_CODES }
        }.getOrDefault(DEFAULT_ELEVENLABS_TTS_LANGUAGE_CODES)
    }

    fun saveElevenLabsTtsLanguageCodes(languageCodes: Set<String>) {
        val array = JSONArray()
        languageCodes
            .map { it.trim().lowercase(Locale.ROOT) }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
            .forEach(array::put)
        preferences.edit()
            .putString(KEY_ELEVENLABS_TTS_LANGUAGE_CODES, array.toString())
            .apply()
    }

    fun loadOpenAiCacheDurationMinutes(): Long {
        return preferences.getLong(KEY_OPENAI_CACHE_DURATION_MINUTES, DEFAULT_OPENAI_CACHE_DURATION_MINUTES)
    }

    fun loadOpenAiVoiceSilenceTimeoutMs(): Long {
        return preferences.getLong(KEY_OPENAI_VOICE_SILENCE_TIMEOUT_MS, DEFAULT_OPENAI_VOICE_SILENCE_TIMEOUT_MS)
            .coerceIn(MIN_OPENAI_VOICE_SILENCE_TIMEOUT_MS, MAX_OPENAI_VOICE_SILENCE_TIMEOUT_MS)
    }

    fun loadCardStatusBlinkIntervalMs(): Long {
        val saved = preferences.getLong(KEY_CARD_STATUS_BLINK_INTERVAL_MS, DEFAULT_CARD_STATUS_BLINK_INTERVAL_MS)
        return if (saved in CARD_STATUS_BLINK_INTERVAL_OPTIONS_MS) saved else DEFAULT_CARD_STATUS_BLINK_INTERVAL_MS
    }

    fun loadCatReplySpeechRate(): Float {
        return preferences.getFloat(KEY_CAT_REPLY_SPEECH_RATE, DEFAULT_CAT_REPLY_SPEECH_RATE)
            .coerceIn(MIN_CAT_REPLY_SPEECH_RATE, MAX_CAT_REPLY_SPEECH_RATE)
    }

    fun saveCatReplySpeechRate(rate: Float) {
        preferences.edit()
            .putFloat(KEY_CAT_REPLY_SPEECH_RATE, rate.coerceIn(MIN_CAT_REPLY_SPEECH_RATE, MAX_CAT_REPLY_SPEECH_RATE))
            .apply()
    }

    fun loadCatDialogRetentionDays(): Int {
        return preferences.getInt(KEY_CAT_DIALOG_RETENTION_DAYS, DEFAULT_CAT_DIALOG_RETENTION_DAYS)
            .coerceIn(MIN_CAT_DIALOG_RETENTION_DAYS, MAX_CAT_DIALOG_RETENTION_DAYS)
    }

    fun saveCatDialogRetentionDays(days: Int) {
        preferences.edit()
            .putInt(KEY_CAT_DIALOG_RETENTION_DAYS, days.coerceIn(MIN_CAT_DIALOG_RETENTION_DAYS, MAX_CAT_DIALOG_RETENTION_DAYS))
            .apply()
        pruneCatDialogs()
    }

    fun saveCardStatusBlinkIntervalMs(intervalMs: Long) {
        val normalized = if (intervalMs in CARD_STATUS_BLINK_INTERVAL_OPTIONS_MS) {
            intervalMs
        } else {
            DEFAULT_CARD_STATUS_BLINK_INTERVAL_MS
        }
        preferences.edit().putLong(KEY_CARD_STATUS_BLINK_INTERVAL_MS, normalized).apply()
    }

    fun saveOpenAiVoiceSilenceTimeoutMs(timeoutMs: Long) {
        preferences.edit()
            .putLong(
                KEY_OPENAI_VOICE_SILENCE_TIMEOUT_MS,
                timeoutMs.coerceIn(MIN_OPENAI_VOICE_SILENCE_TIMEOUT_MS, MAX_OPENAI_VOICE_SILENCE_TIMEOUT_MS)
            )
            .apply()
    }

    fun saveOpenAiCacheDurationMinutes(minutes: Long) {
        val normalized = when {
            minutes < 0L -> -1L
            minutes <= 5L -> DEFAULT_OPENAI_CACHE_DURATION_MINUTES
            minutes > MAX_OPENAI_CACHE_DURATION_MINUTES -> MAX_OPENAI_CACHE_DURATION_MINUTES
            else -> minutes
        }
        preferences.edit().putLong(KEY_OPENAI_CACHE_DURATION_MINUTES, normalized).apply()
    }

    fun getCachedOpenAiText(
        task: String,
        model: String,
        input: String,
        sourceLanguage: String,
        targetLanguage: String
    ): String? {
        val key = openAiCacheKey(task, model, input, sourceLanguage, targetLanguage)
        val file = File(openAiCacheDir(), "$key.json")
        if (!file.exists()) return null
        val entry = runCatching { JSONObject(file.readText(Charsets.UTF_8)) }.getOrNull() ?: return null
        val createdAt = entry.optLong("createdAt", 0L)
        if (!isOpenAiCacheEntryFresh(createdAt)) {
            file.delete()
            return null
        }
        return entry.optString("output").trim().ifBlank { null }
    }

    fun saveOpenAiTextCache(
        task: String,
        model: String,
        input: String,
        sourceLanguage: String,
        targetLanguage: String,
        output: String
    ) {
        if (output.isBlank()) return
        val key = openAiCacheKey(task, model, input, sourceLanguage, targetLanguage)
        val file = File(openAiCacheDir(), "$key.json")
        openAiCacheDir().mkdirs()
        val entry = JSONObject()
            .put("type", "text")
            .put("task", task)
            .put("model", model)
            .put("sourceLanguage", sourceLanguage)
            .put("targetLanguage", targetLanguage)
            .put("createdAt", System.currentTimeMillis())
            .put("output", output)
        file.writeText(entry.toString(), Charsets.UTF_8)
    }

    fun clearOpenAiCache(): Int {
        return listOf(openAiCacheDir(), cardCacheDir()).sumOf { dir ->
            if (!dir.exists()) {
                0
            } else {
                dir.walkBottomUp()
                    .filter { it != dir }
                    .count { it.delete() }
            }
        }
    }

    fun addOpenAiActivityLog(action: String, details: String = "") {
        val cleanAction = action.trim()
        if (cleanAction.isBlank()) return
        val now = System.currentTimeMillis()
        val next = prunedOpenAiActivityLogArray(now)
        next.put(
            JSONObject()
                .put("createdAt", now)
                .put("action", cleanAction)
                .put("details", details.trim().take(1200))
        )
        preferences.edit().putString(KEY_OPENAI_ACTIVITY_LOG, next.toString()).apply()
    }

    fun loadOpenAiActivityLog(): List<String> {
        val now = System.currentTimeMillis()
        val pruned = prunedOpenAiActivityLogArray(now)
        preferences.edit().putString(KEY_OPENAI_ACTIVITY_LOG, pruned.toString()).apply()
        val formatter = SimpleDateFormat("MM-dd HH:mm", Locale.ROOT)
        return (0 until pruned.length()).mapNotNull { index ->
            val entry = pruned.optJSONObject(index) ?: return@mapNotNull null
            val createdAt = entry.optLong("createdAt", 0L)
            val action = entry.optString("action").trim()
            val details = entry.optString("details").trim()
            if (createdAt <= 0L || action.isBlank()) return@mapNotNull null
            buildString {
                append(formatter.format(Date(createdAt)))
                append(" - ")
                append(action)
                if (details.isNotBlank()) {
                    append(" - ")
                    append(details)
                }
            }
        }.asReversed()
    }

    fun loadOfflineSpeechLanguageTag(): String {
        return preferences.getString(KEY_OFFLINE_SPEECH_LANGUAGE, "en-US") ?: "en-US"
    }

    fun saveOfflineSpeechLanguageTag(languageTag: String) {
        preferences.edit().putString(KEY_OFFLINE_SPEECH_LANGUAGE, languageTag.trim().ifBlank { "en-US" }).apply()
    }

    fun loadOfflineSpeechStatus(languageTag: String): String {
        return preferences.getString(offlineSpeechStatusKey(languageTag), OFFLINE_SPEECH_STATUS_NOT_DOWNLOADED)
            ?: OFFLINE_SPEECH_STATUS_NOT_DOWNLOADED
    }

    fun saveOfflineSpeechStatus(languageTag: String, status: String) {
        preferences.edit().putString(offlineSpeechStatusKey(languageTag), status).apply()
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
            .mapNotNull { pair ->
                val weight = notificationWeight(pair.second)
                if (weight > 0) pair to weight else null
            }
        val totalWeight = weightedCards.sumOf { it.second }
        if (totalWeight <= 0) return null
        var pick = Random.nextInt(totalWeight)
        for ((pair, weight) in weightedCards) {
            if (pick < weight) return pair
            pick -= weight
        }
        return weightedCards.lastOrNull()?.first
    }

    private fun notificationWeight(card: Flashcard): Int {
        return when (card.starCount()) {
            1 -> 79
            2 -> 20
            3 -> 1
            else -> 0
        }
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

    fun loadCatDialogs(includeHidden: Boolean = false): List<CatDialog> {
        pruneCatDialogs()
        return loadStoredCatDialogs().filter { includeHidden || !it.hidden }
    }

    fun saveCatDialog(dialog: CatDialog) {
        val nowMs = System.currentTimeMillis()
        val now = storageTimestamp()
        val stored = loadStoredCatDialogs()
        val existing = stored.firstOrNull { it.id == dialog.id }
        val savedDialog = dialog.copy(
            createdAt = existing?.createdAt?.takeIf { it.isNotBlank() } ?: dialog.createdAt.ifBlank { now },
            updatedAt = now,
            createdAtMillis = existing?.createdAtMillis?.takeIf { it > 0L } ?: dialog.createdAtMillis.takeIf { it > 0L } ?: nowMs,
            updatedAtMillis = nowMs
        )
        val next = stored.filterNot { it.id == savedDialog.id } + savedDialog
        preferences.edit().putString(KEY_CAT_DIALOGS, json.encodeToString(next)).apply()
        pruneCatDialogs()
    }

    fun deleteCatDialog(dialogId: String) {
        val next = loadStoredCatDialogs().filterNot { it.id == dialogId }
        preferences.edit().putString(KEY_CAT_DIALOGS, json.encodeToString(next)).apply()
    }

    fun setCatDialogHidden(dialogId: String, hidden: Boolean) {
        val next = loadStoredCatDialogs().map { dialog ->
            if (dialog.id == dialogId) dialog.copy(hidden = hidden, updatedAt = storageTimestamp(), updatedAtMillis = System.currentTimeMillis()) else dialog
        }
        preferences.edit().putString(KEY_CAT_DIALOGS, json.encodeToString(next)).apply()
    }

    fun setCatDialogFeatured(dialogId: String, featured: Boolean) {
        val next = loadStoredCatDialogs().map { dialog ->
            if (dialog.id == dialogId) dialog.copy(featured = featured, updatedAt = storageTimestamp(), updatedAtMillis = System.currentTimeMillis()) else dialog
        }
        preferences.edit().putString(KEY_CAT_DIALOGS, json.encodeToString(next)).apply()
    }

    fun setCatDialogMessageFeaturedSelection(dialogId: String, messageId: String, selection: String) {
        val cleanSelection = selection.trim()
        if (dialogId.isBlank() || messageId.isBlank() || cleanSelection.isBlank()) return
        val now = storageTimestamp()
        val nowMs = System.currentTimeMillis()
        val next = loadStoredCatDialogs().map { dialog ->
            if (dialog.id == dialogId) {
                dialog.copy(
                    messages = dialog.messages.map { message ->
                        if (message.id == messageId) message.copy(featuredSelection = cleanSelection) else message
                    },
                    updatedAt = now,
                    updatedAtMillis = nowMs
                )
            } else {
                dialog
            }
        }
        preferences.edit().putString(KEY_CAT_DIALOGS, json.encodeToString(next)).apply()
    }

    fun moveCatDialog(draggedDialogId: String, targetDialogId: String) {
        if (draggedDialogId == targetDialogId) return
        val dialogs = loadStoredCatDialogs().toMutableList()
        val fromIndex = dialogs.indexOfFirst { it.id == draggedDialogId }
        val toIndex = dialogs.indexOfFirst { it.id == targetDialogId }
        if (fromIndex !in dialogs.indices || toIndex !in dialogs.indices) return
        val moved = dialogs.removeAt(fromIndex)
        dialogs.add(toIndex, moved)
        preferences.edit().putString(KEY_CAT_DIALOGS, json.encodeToString(dialogs)).apply()
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
        val storedLessons = loadStoredLessons()
        val existingLesson = storedLessons.firstOrNull { it.id == lesson.id }
        val savedAt = storageTimestamp()
        val createdAt = listOf(existingLesson?.createdAt, lesson.createdAt).firstOrNull { !it.isNullOrBlank() } ?: savedAt
        val lessonForStorage = lesson.withHeaderLanguages().copy(
            title = lesson.title.cleanKindSuffix(),
            createdAt = createdAt,
            updatedAt = savedAt,
            editable = true
        )
        val lessons = storedLessons.filterNot { it.id == lesson.id } + lessonForStorage
        val lessonOrder = (loadLessonOrder() + lesson.id).distinct()
        preferences.edit()
            .putString(KEY_LESSONS, json.encodeToString(lessons))
            .putString(KEY_HIDDEN_LESSONS, json.encodeToString(hiddenLessonIds))
            .putString(KEY_LESSON_ORDER, json.encodeToString(lessonOrder))
            .commit()
        syncLessonCardCache(lesson.id, lessonForStorage)
        pruneOriginalVoiceCache(lessons)
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
        lessonIds.forEach(::deleteLessonCardCache)
        pruneOriginalVoiceCache(lessons)
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
        return runCatching { json.decodeFromString<List<Lesson>>(raw) }
            .getOrDefault(emptyList())
            .map { it.withHeaderLanguages() }
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
                    sourceLanguage = cards.firstOrNull()?.sourceLanguage.lessonLanguageOrEmpty(),
                    targetLanguage = cards.firstOrNull()?.targetLanguage.lessonLanguageOrEmpty(),
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
                    sourceLanguage = lesson.sourceLanguage.lessonLanguageOrEmpty()
                        .ifBlank { lesson.cards.firstOrNull()?.sourceLanguage.lessonLanguageOrEmpty() },
                    targetLanguage = lesson.targetLanguage.lessonLanguageOrEmpty()
                        .ifBlank { lesson.cards.firstOrNull()?.targetLanguage.lessonLanguageOrEmpty() },
                    cards = lesson.cards.reindexCards()
                )
            }
            else -> throw SerializationException("Lesson JSON must be an object or an array of cards")
        }
    }

    private fun List<Flashcard>.reindexCards(): List<Flashcard> {
        return mapIndexed { index, card -> card.copy(id = index + 1) }
    }

    private fun Lesson.withHeaderLanguages(): Lesson {
        return copy(
            sourceLanguage = sourceLanguage.lessonLanguageOrEmpty()
                .ifBlank { cards.firstOrNull()?.sourceLanguage.lessonLanguageOrEmpty() },
            targetLanguage = targetLanguage.lessonLanguageOrEmpty()
                .ifBlank { cards.firstOrNull()?.targetLanguage.lessonLanguageOrEmpty() }
        )
    }

    private fun storageTimestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
    }

    private fun loadStoredCatDialogs(): List<CatDialog> {
        val raw = preferences.getString(KEY_CAT_DIALOGS, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<CatDialog>>(raw) }.getOrDefault(emptyList())
    }

    private fun pruneCatDialogs() {
        val retentionDays = loadCatDialogRetentionDays()
        val cutoff = System.currentTimeMillis() - retentionDays * 86_400_000L
        val stored = loadStoredCatDialogs()
        val next = stored.filter { dialog ->
            dialog.featured || (dialog.updatedAtMillis.takeIf { it > 0L } ?: dialog.createdAtMillis) >= cutoff
        }
        if (next.size != stored.size) {
            preferences.edit().putString(KEY_CAT_DIALOGS, json.encodeToString(next)).apply()
        }
    }

    private fun String?.lessonLanguageOrEmpty(): String {
        val cleaned = orEmpty().trim()
        return if (cleaned.equals("Mixed", ignoreCase = true)) "" else cleaned
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
    private fun offlineSpeechStatusKey(languageTag: String): String = "$KEY_OFFLINE_SPEECH_STATUS_PREFIX$languageTag"
    private fun openAiCacheDir(): File = File(context.cacheDir, "openai_cache")
    private fun cardCacheDir(): File = File(context.filesDir, "card_cache")
    private fun openAiCacheKey(vararg parts: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val raw = parts.joinToString(separator = "\u001F") { it.normalizedCacheLookupText() }
        return digest.digest(raw.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    private fun syncLessonCardCache(lessonId: String, lesson: Lesson) {
        if (lessonId.isBlank()) return
        val dir = cardCacheDir()
        dir.mkdirs()
        val lessonPrefix = "${lessonId.safeCacheFilePart()}_"
        val currentNames = lesson.cards.map { card -> cardCacheFileName(lessonId, card.id) }.toSet()
        dir.listFiles()
            ?.filter { file -> file.name.startsWith(lessonPrefix) && file.name !in currentNames }
            ?.forEach { it.delete() }
        lesson.cards.forEach { card ->
            val entry = JSONObject()
                .put("type", "card")
                .put("lessonId", lessonId)
                .put("cardId", card.id)
                .put("sourceLanguage", card.sourceLanguage.ifBlank { lesson.sourceLanguage })
                .put("targetLanguage", card.targetLanguage.ifBlank { lesson.targetLanguage })
                .put("nativeValue", card.nativeText())
                .put("correctValue", card.correctText())
                .put("hint", card.hintText())
                .put("original", card.originalText())
                .put("originalAudioPath", card.originalAudioPathText())
                .put("where", card.whereText())
                .put("madeAt", card.madeAtText())
                .put("updatedAt", System.currentTimeMillis())
            File(dir, cardCacheFileName(lessonId, card.id)).writeText(entry.toString(), Charsets.UTF_8)
        }
    }

    private fun deleteLessonCardCache(lessonId: String) {
        val lessonPrefix = "${lessonId.safeCacheFilePart()}_"
        cardCacheDir().listFiles()
            ?.filter { it.name.startsWith(lessonPrefix) }
            ?.forEach { it.delete() }
    }

    private fun originalVoiceCacheDir(): File = File(cardCacheDir(), "original_voice")

    private fun pruneOriginalVoiceCache(lessons: List<Lesson>) {
        val dir = originalVoiceCacheDir()
        val referenced = lessons
            .flatMap { lesson -> lesson.cards }
            .mapNotNull { card -> card.originalAudioPathText().takeIf { it.isNotBlank() } }
            .map { path -> runCatching { File(path).canonicalPath }.getOrDefault(path) }
            .toSet()
        dir.listFiles()
            ?.filter { file -> file.isFile && runCatching { file.canonicalPath }.getOrDefault(file.absolutePath) !in referenced }
            ?.forEach { it.delete() }
    }

    private fun cardCacheFileName(lessonId: String, cardId: Int): String {
        return "${lessonId.safeCacheFilePart()}_$cardId.json"
    }

    private fun String.safeCacheFilePart(): String {
        return replace(Regex("[^A-Za-z0-9_.-]+"), "_").ifBlank { "item" }
    }

    private fun String.normalizedCacheLookupText(): String {
        return lowercase(Locale.ROOT)
            .replace(Regex("\\p{Punct}+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun isOpenAiCacheEntryFresh(createdAt: Long): Boolean {
        if (createdAt <= 0L) return false
        val minutes = loadOpenAiCacheDurationMinutes()
        if (minutes < 0L) return true
        val maxAgeMs = minutes * 60_000L
        return System.currentTimeMillis() - createdAt <= maxAgeMs
    }

    private fun prunedOpenAiActivityLogArray(now: Long): JSONArray {
        val cutoff = now - OPENAI_ACTIVITY_LOG_RETENTION_MS
        val raw = preferences.getString(KEY_OPENAI_ACTIVITY_LOG, "[]") ?: "[]"
        val source = runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
        val next = JSONArray()
        for (index in 0 until source.length()) {
            val entry = source.optJSONObject(index) ?: continue
            if (entry.optLong("createdAt", 0L) >= cutoff) {
                next.put(entry)
            }
        }
        return next
    }

    companion object {
        const val OFFLINE_SPEECH_STATUS_NOT_DOWNLOADED = "Not downloaded"
        const val OFFLINE_SPEECH_STATUS_DOWNLOADING = "Downloading"
        const val OFFLINE_SPEECH_STATUS_READY = "Ready"
        const val OFFLINE_SPEECH_STATUS_ERROR = "Error"
        const val OFFLINE_SPEECH_STATUS_NOT_SUPPORTED = "Not supported"
        const val DEFAULT_MURRLEX_SERVER_URL = "https://ml-staging-api.lexaailabs.com"
        const val DEFAULT_OPENAI_BASE_URL = DEFAULT_MURRLEX_SERVER_URL
        const val DEFAULT_OPENAI_SPEECH_MODEL = "gpt-4o-mini-transcribe"
        const val DEFAULT_OPENAI_TEXT_MODEL = "gpt-5.4-mini"
        const val DEFAULT_OPENAI_IMAGE_TEXT_MODEL = "gpt-4o-mini"
        const val DEFAULT_OPENAI_TTS_MODEL = "gpt-4o-mini-tts"
        const val DEFAULT_OPENAI_TTS_VOICE = "coral"
        const val DEFAULT_BELARUSIAN_TTS_PROVIDER = "ElevenLabs"
        const val DEFAULT_ELEVENLABS_MODEL = "eleven_v3"
        const val DEFAULT_ELEVENLABS_BELARUSIAN_VOICE_ID = "q19tj6dG7gitafffmfLO"
        val DEFAULT_ELEVENLABS_TTS_LANGUAGE_CODES = setOf("be")
        const val DEFAULT_ELEVENLABS_OUTPUT_FORMAT = "mp3_44100_128"
        const val DEFAULT_OPENAI_CACHE_DURATION_MINUTES = 5L
        const val MAX_OPENAI_CACHE_DURATION_MINUTES = 43_200L
        const val DEFAULT_OPENAI_VOICE_SILENCE_TIMEOUT_MS = 5_000L
        const val MIN_OPENAI_VOICE_SILENCE_TIMEOUT_MS = 1_000L
        const val MAX_OPENAI_VOICE_SILENCE_TIMEOUT_MS = 30_000L
        const val CARD_STATUS_BLINK_OFF_MS = 0L
        const val DEFAULT_CARD_STATUS_BLINK_INTERVAL_MS = 2_000L
        val CARD_STATUS_BLINK_INTERVAL_OPTIONS_MS = setOf(0L, 500L, 1_000L, 2_000L, 3_000L, 4_000L, 5_000L)
        const val OPENAI_ACTIVITY_LOG_RETENTION_MS = 172_800_000L
        const val DEFAULT_CAT_DIALOG_RETENTION_DAYS = 30
        const val MIN_CAT_DIALOG_RETENTION_DAYS = 1
        const val MAX_CAT_DIALOG_RETENTION_DAYS = 365
        const val DEFAULT_CAT_REPLY_SPEECH_RATE = 1.0f
        const val MIN_CAT_REPLY_SPEECH_RATE = 0.5f
        const val MAX_CAT_REPLY_SPEECH_RATE = 2.5f

        private const val KEY_LESSONS = "lessons"
        private const val KEY_STATS = "stats"
        private const val KEY_HIDDEN_LESSONS = "hidden_lessons"
        private const val KEY_LESSON_ORDER = "lesson_order"
        private const val KEY_CARD_START_SIDE = "card_start_side"
        private const val KEY_NOTIFICATION_INTERVAL_MINUTES = "notification_interval_minutes"
        private const val KEY_MAX_ACTIVE_NOTIFICATIONS = "max_active_notifications"
        private const val KEY_EXCLUDE_MASTERED_CARDS = "exclude_mastered_cards"
        private const val KEY_SHOW_CARD_LOG = "show_card_log"
        private const val KEY_DISPLAY_TEXT_SIZE = "display_text_size"
        private const val KEY_CONTROL_SIZE = "control_size"
        private const val KEY_SOUND_EFFECTS_ENABLED = "sound_effects_enabled"
        private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        private const val KEY_INTERFACE_LANGUAGE = "interface_language"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_QUICK_VOCABULARY_SOURCE_LANGUAGE = "quick_vocabulary_source_language"
        private const val KEY_QUICK_VOCABULARY_TARGET_LANGUAGE = "quick_vocabulary_target_language"
        private const val KEY_USE_LOCAL_TRANSLATION = "use_local_translation"
        private const val KEY_AUTO_SAVE_TRANSLATOR_CARDS = "auto_save_translator_cards"
        private const val KEY_TRANSLATION_API_URL = "translation_api_url"
        private const val KEY_TRANSLATION_API_TOKEN = "translation_api_token"
        private const val KEY_USE_OPENAI_MODELS = "use_openai_models"
        private const val KEY_OPENAI_BASE_URL = "openai_base_url"
        private const val KEY_OPENAI_API_KEY = "openai_api_key"
        private const val KEY_MURRLEX_SERVER_URL = "murrlex_server_url"
        private const val KEY_MURRLEX_SERVER_ACCESS_TOKEN = "murrlex_server_access_token"
        private const val KEY_MURRLEX_SERVER_REFRESH_TOKEN = "murrlex_server_refresh_token"
        private const val KEY_MURRLEX_SERVER_ACCESS_EXPIRES_AT = "murrlex_server_access_expires_at"
        private const val KEY_MURRLEX_SERVER_USERNAME = "murrlex_server_username"
        private const val KEY_MURRLEX_SERVER_EMAIL = "murrlex_server_email"
        private const val KEY_OPENAI_SPEECH_MODEL = "openai_speech_model"
        private const val KEY_OPENAI_TEXT_MODEL = "openai_text_model"
        private const val KEY_OPENAI_IMAGE_TEXT_MODEL = "openai_image_text_model"
        private const val KEY_OPENAI_TTS_MODEL = "openai_tts_model"
        private const val KEY_OPENAI_TTS_VOICE = "openai_tts_voice"
        private const val KEY_BELARUSIAN_TTS_PROVIDER = "belarusian_tts_provider"
        private const val KEY_ELEVENLABS_API_KEY = "elevenlabs_api_key"
        private const val KEY_ELEVENLABS_MODEL = "elevenlabs_model"
        private const val KEY_ELEVENLABS_VOICE_ID = "elevenlabs_voice_id"
        private const val KEY_ELEVENLABS_TTS_LANGUAGE_CODES = "elevenlabs_tts_language_codes"
        private const val KEY_OPENAI_CACHE_DURATION_MINUTES = "openai_cache_duration_minutes"
        private const val KEY_OPENAI_VOICE_SILENCE_TIMEOUT_MS = "openai_voice_silence_timeout_ms"
        private const val KEY_CARD_STATUS_BLINK_INTERVAL_MS = "card_status_blink_interval_ms"
        private const val KEY_CAT_DIALOGS = "cat_dialogs"
        private const val KEY_CAT_DIALOG_RETENTION_DAYS = "cat_dialog_retention_days"
        private const val KEY_CAT_REPLY_SPEECH_RATE = "cat_reply_speech_rate"
        private const val KEY_OPENAI_ACTIVITY_LOG = "openai_activity_log"
        private const val KEY_OFFLINE_SPEECH_LANGUAGE = "offline_speech_language"
        private const val KEY_OFFLINE_SPEECH_STATUS_PREFIX = "offline_speech_status_"
        private const val KEY_STUDY_SESSION_PREFIX = "study_session_"
    }
}
