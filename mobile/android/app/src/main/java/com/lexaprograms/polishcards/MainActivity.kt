package com.lexaprograms.polishcards

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.speech.ModelDownloadListener
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.BackgroundColorSpan
import android.text.style.ClickableSpan
import android.text.style.StyleSpan
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.core.Alignment as RiveAlignment
import app.rive.runtime.kotlin.core.Fit
import app.rive.runtime.kotlin.core.Loop
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.io.File
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.security.MessageDigest
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt
import kotlin.random.Random
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.FileProvider

private val BrandSaladColor = Color(0xFF8FDCC4)
private val BrandRedColor = Color(0xFFC45F59)
private val OnlineStatusColor = Color(0xFF72D66A)
private val CompletedFrameColor = Color(0xFFE8F5E9)

private enum class CardAudioStatus {
    ONLINE,
    OFFLINE
}

private enum class VoiceInputTarget {
    ANSWER,
    QUICK_VOCABULARY,
    TRANSLATE_INPUT,
    TRANSLATE_OUTPUT,
    CARD_NATIVE,
    CARD_CORRECT,
    CARD_HINT,
    CARD_MADE_AT,
    CARD_WHERE,
    CAT_CHAT
}

private data class CatChatMessage(
    val id: String,
    val text: String,
    val fromCat: Boolean,
    val analysis: String = "",
    val analysisSourceText: String = "",
    val featuredSelection: String = ""
)

private data class CatChatWordSelection(
    val start: Int,
    val end: Int
)

private data class CatChatFeaturedPopup(
    val selection: String,
    val fromCat: Boolean
)

private fun List<CatDialogMessage>.toCatChatMessages(): List<CatChatMessage> {
    var lastUserText = ""
    return map { message ->
        val sourceText = if (message.fromCat) lastUserText else ""
        if (!message.fromCat) lastUserText = message.text
        CatChatMessage(
            id = message.id,
            text = message.text,
            fromCat = message.fromCat,
            analysis = message.analysis,
            analysisSourceText = sourceText,
            featuredSelection = message.featuredSelection
        )
    }
}

private enum class LanguageGameMode {
    QUIZ,
    MATCH,
    SPELL,
    FEED,
    GINGER_COMPOSE,
    GINGER_CANVAS,
    GINGER_HTML,
    PSEUDO_3D_WALK,
    EIGHT_BIT_PLATFORMER,
    SIXTEEN_BIT_SPIN_RUNNER,
    LIBGDX_SPIN_RUNNER,
    GODOT_RUNNER_LAB,
    LOTTIE_RUNNER_LAB,
    FLUTTER_RUNNER_LAB,
    RIVE_RUNNER_LAB,
    RIVE_LETTER_BLOCKS
}

private enum class RiveCatAction {
    RUN,
    SPIN,
    DANCE,
    CRAWL,
    MEOW,
    SCRATCH,
    TALK,
    STAND
}

private enum class LessonSortMode(val label: String) {
    CREATED("Created"),
    UPDATED("Modified"),
    TITLE("Title"),
    CARD_COUNT("Cards"),
    LANGUAGE_PAIR("Pair")
}

private data class LanguageGameEntry(
    val id: Int,
    val front: String,
    val back: String
)

private data class AnimationCatalogEntry(
    val number: String,
    val title: String,
    val lane: String,
    val category: String,
    val component: String,
    val status: String
)

private val MakeMistakeAnimationCatalog = listOf(
    AnimationCatalogEntry("ANIM-001", "Bouncy success", "reward-progress", "reward/success", "BouncySuccessCheckmark", "implemented"),
    AnimationCatalogEntry("ANIM-002", "Wrong answer shake", "mistake-feedback", "mistake/error", "WrongAnswerCardShake", "implemented"),
    AnimationCatalogEntry("ANIM-003", "Lesson card flip reveal", "lesson-ui", "card transitions", "LessonCardFlipReveal", "implemented"),
    AnimationCatalogEntry("ANIM-004", "Streak flame pulse", "reward-progress", "streaks and badges", "StreakFlamePulse", "product widget pro scene"),
    AnimationCatalogEntry("ANIM-005", "Microphone listening waveform", "voice-feedback", "voice/microphone feedback", "MicrophoneListeningWaveform", "product widget pro scene"),
    AnimationCatalogEntry("ANIM-006", "Character idle breathing blink", "character-reactions", "character/idle", "CharacterIdleBreathingBlink", "polished"),
    AnimationCatalogEntry("ANIM-007", "Character talking mouth flaps", "character-reactions", "character/reaction", "CharacterTalkingMouthFlaps", "polished"),
    AnimationCatalogEntry("ANIM-008", "Character surprised reaction", "character-reactions", "character/reaction", "CharacterSurprisedReaction", "polished"),
    AnimationCatalogEntry("ANIM-009", "Character encouraging nod", "character-reactions", "character/reaction", "CharacterEncouragingNod", "polished"),
    AnimationCatalogEntry("ANIM-010", "Character listening pose", "character-reactions", "character/voice", "CharacterListeningPose", "polished")
)

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

private fun Lesson.languagePairLabel(): String {
    val pair = listOf(sourceLanguage.shortLanguageCode(), targetLanguage.shortLanguageCode())
        .filter { it != "XX" }
        .joinToString(" -> ")
    return pair.ifBlank { "XX" }
}

private fun Lesson.createdDisplayText(): String {
    return createdAt.ifBlank { cards.firstOrNull()?.madeAt.orEmpty() }
}

private fun Lesson.updatedDisplayText(): String {
    return updatedAt.ifBlank {
        cards.mapNotNull { card -> card.madeAt.takeIf { it.isNotBlank() } }.maxOrNull().orEmpty()
    }
}

private fun Lesson.matchesCatalogQuery(query: String): Boolean {
    val normalized = query.trim()
    if (normalized.isBlank()) return true
    val haystack = listOf(
        title,
        lessonInfo,
        sourceLanguage,
        targetLanguage,
        sourceLanguage.shortLanguageCode(),
        targetLanguage.shortLanguageCode(),
        languagePairLabel(),
        cards.size.toString(),
        cards.joinToString(" ") { card ->
            listOf(
                card.nativeValue,
                card.correctValue,
                card.hint,
                card.original,
                card.mistake,
                card.value,
                card.pl,
                card.ru,
                card.where,
                card.log.joinToString(" "),
                card.wrongAnswers.joinToString(" ") { answer -> "${answer.answer} ${answer.date}" }
            ).joinToString(" ")
        }
    ).joinToString(" ")
    return haystack.contains(normalized, ignoreCase = true)
}

private fun Flashcard.searchableCardText(): String {
    return listOf(
        nativeValue,
        correctValue,
        hint,
        original,
        mistake,
        value,
        pl,
        ru,
        where,
        log.joinToString(" "),
        wrongAnswers.joinToString(" ") { answer -> "${answer.answer} ${answer.date}" }
    ).joinToString(" ")
}

private fun Flashcard.matchesCardSearch(query: String): Boolean {
    val normalized = query.trim()
    if (normalized.isBlank()) return false
    return searchableCardText().contains(normalized, ignoreCase = true)
}

private fun Lesson.cardContentMatchCount(query: String): Int {
    val normalized = query.trim()
    if (normalized.isBlank()) return 0
    return cards.count { it.matchesCardSearch(normalized) }
}

private fun Lesson.firstCardContentMatchId(query: String): Int? {
    val normalized = query.trim()
    if (normalized.isBlank()) return null
    return cards.firstOrNull { it.matchesCardSearch(normalized) }?.id
}

private fun List<Lesson>.catalogFilteredAndSorted(query: String, sortMode: LessonSortMode, descending: Boolean): List<Lesson> {
    val filtered = filter { it.matchesCatalogQuery(query) }
    val sorted = when (sortMode) {
        LessonSortMode.CREATED -> filtered.sortedWith(
            compareBy<Lesson> { it.createdDisplayText() }.thenBy { it.title.lowercase(Locale.ROOT) }
        )
        LessonSortMode.UPDATED -> filtered.sortedWith(
            compareBy<Lesson> { it.updatedDisplayText() }.thenBy { it.title.lowercase(Locale.ROOT) }
        )
        LessonSortMode.TITLE -> filtered.sortedBy { it.title.lowercase(Locale.ROOT) }
        LessonSortMode.CARD_COUNT -> filtered.sortedWith(
            compareBy<Lesson> { it.cards.size }.thenBy { it.title.lowercase(Locale.ROOT) }
        )
        LessonSortMode.LANGUAGE_PAIR -> filtered.sortedWith(
            compareBy<Lesson> { it.languagePairLabel() }.thenBy { it.title.lowercase(Locale.ROOT) }
        )
    }
    return if (descending) sorted.asReversed() else sorted
}

private data class SharedPostSignal(
    val id: Long,
    val text: String
)

private val TrainCardGenerationOptions = listOf(
    "Correct form recall",
    "Find the mistake",
    "Fill the blank",
    "Choose the rule",
    "Word order drill",
    "Pronunciation contrast",
    "Minimal pair",
    "Rewrite correctly",
    "Usage example",
    "One-minute rule check"
)

private data class OpenAiAudioResult(
    val file: File,
    val source: String
)

private data class ElevenLabsAudioResult(
    val file: File? = null,
    val source: String = "",
    val error: String = ""
)

private object AppAudioPlayer {
    private var player: MediaPlayer? = null

    fun hasActivePlayback(): Boolean = player != null

    fun stop(): Boolean {
        val hadPlayer = player != null
        player?.let { current ->
            runCatching { current.stop() }
            current.release()
        }
        player = null
        return hadPlayer
    }

    fun playFile(file: File, deleteOnFailure: Boolean = false): Boolean {
        stop()
        val nextPlayer = runCatching {
            MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    if (player === it) player = null
                    it.release()
                }
                setOnErrorListener { mediaPlayer, _, _ ->
                    if (player === mediaPlayer) player = null
                    mediaPlayer.release()
                    if (deleteOnFailure) file.delete()
                    true
                }
                prepare()
            }
        }.getOrElse {
            if (deleteOnFailure) file.delete()
            return false
        }
        player = nextPlayer
        return runCatching {
            nextPlayer.start()
        }.onFailure {
            if (player === nextPlayer) player = null
            nextPlayer.release()
            if (deleteOnFailure) file.delete()
        }.isSuccess
    }

    fun playResource(context: Context, resId: Int): Boolean {
        stop()
        val nextPlayer = runCatching { MediaPlayer.create(context, resId) }.getOrNull() ?: return false
        player = nextPlayer
        nextPlayer.setVolume(1f, 1f)
        nextPlayer.setOnCompletionListener {
            if (player === it) player = null
            it.release()
        }
        nextPlayer.setOnErrorListener { mediaPlayer, _, _ ->
            if (player === mediaPlayer) player = null
            mediaPlayer.release()
            true
        }
        return runCatching {
            nextPlayer.start()
        }.onFailure {
            if (player === nextPlayer) player = null
            nextPlayer.release()
        }.isSuccess
    }
}

private suspend fun requestOpenAiTranscriptionFile(
    audioFile: File,
    baseUrl: String,
    apiKey: String,
    model: String,
    languageTag: String
): String? = withContext(Dispatchers.IO) {
    if (apiKey.isBlank() || !audioFile.exists()) return@withContext null
    val boundary = "MurrLexBoundary${System.currentTimeMillis()}"
    val lineBreak = "\r\n"
    val connection = (URL(baseUrl.trimEnd('/') + "/api/ai/transcribe").openConnection() as HttpURLConnection).apply {
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
        languageTag.substringBefore('-').takeIf { it.length == 2 }?.let { field("language", it) }
        output.write("--$boundary$lineBreak".toByteArray(Charsets.UTF_8))
        output.write("Content-Disposition: form-data; name=\"file\"; filename=\"speech.m4a\"$lineBreak".toByteArray(Charsets.UTF_8))
        output.write("Content-Type: audio/mp4$lineBreak$lineBreak".toByteArray(Charsets.UTF_8))
        audioFile.inputStream().use { input -> input.copyTo(output) }
        output.write(lineBreak.toByteArray(Charsets.UTF_8))
        output.write("--$boundary--$lineBreak".toByteArray(Charsets.UTF_8))
    }
    val status = connection.responseCode
    val responseText = (if (status in 200..299) connection.inputStream else connection.errorStream)
        ?.bufferedReader(Charsets.UTF_8)
        ?.use { it.readText() }
        .orEmpty()
    connection.disconnect()
    if (status !in 200..299) return@withContext null
    runCatching { JSONObject(responseText).optString("text").trim() }.getOrNull()
}

private suspend fun requestOpenAiSpeechAudioFile(
    context: Context,
    text: String,
    baseUrl: String,
    apiKey: String,
    model: String,
    voice: String,
    cacheDurationMinutes: Long,
    speechRate: Float = 1f
): OpenAiAudioResult? = withContext(Dispatchers.IO) {
    if (apiKey.isBlank()) return@withContext null
    val normalizedSpeechRate = speechRate.coerceIn(0.5f, 2.5f)
    val cacheKey = openAiCacheKey("tts", model, voice, normalizedSpeechRate.toString(), text)
    val cacheDir = openAiCacheDir(context)
    val audioCacheFile = File(cacheDir, "$cacheKey.mp3")
    val metaCacheFile = File(cacheDir, "$cacheKey.json")
    if (audioCacheFile.exists() && metaCacheFile.exists()) {
        val createdAt = runCatching { JSONObject(metaCacheFile.readText(Charsets.UTF_8)).optLong("createdAt", 0L) }
            .getOrDefault(0L)
        if (isOpenAiCacheFresh(createdAt, cacheDurationMinutes)) {
            return@withContext OpenAiAudioResult(audioCacheFile, "cache")
        }
        audioCacheFile.delete()
        metaCacheFile.delete()
    }
    val payload = JSONObject()
        .put("model", model)
        .put("voice", voice)
        .put("text", text)
        .put("speed", normalizedSpeechRate.toDouble())
        .put("format", "mp3")
        .toString()
    val connection = (URL(baseUrl.trimEnd('/') + "/api/ai/speech").openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 20_000
        readTimeout = 45_000
        doOutput = true
        setRequestProperty("Authorization", "Bearer $apiKey")
        setRequestProperty("Content-Type", "application/json; charset=utf-8")
        setRequestProperty("Accept", "audio/mpeg")
        setRequestProperty("User-Agent", "MurrLex/${BuildConfig.VERSION_NAME}")
    }
    connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
    val status = connection.responseCode
    if (status !in 200..299) {
        connection.errorStream?.close()
        connection.disconnect()
        return@withContext null
    }
    cacheDir.mkdirs()
    connection.inputStream.use { input ->
        audioCacheFile.outputStream().use { outputStream -> input.copyTo(outputStream) }
    }
    connection.disconnect()
    val meta = JSONObject()
        .put("type", "audio")
        .put("task", "tts")
        .put("model", model)
        .put("voice", voice)
        .put("speed", normalizedSpeechRate.toDouble())
        .put("createdAt", System.currentTimeMillis())
        .put("file", audioCacheFile.name)
    metaCacheFile.writeText(meta.toString(), Charsets.UTF_8)
    OpenAiAudioResult(audioCacheFile, "API")
}

private suspend fun requestElevenLabsSpeechAudioFile(
    context: Context,
    text: String,
    languageCode: String,
    baseUrl: String,
    apiKey: String,
    model: String,
    voiceId: String,
    cacheDurationMinutes: Long
): ElevenLabsAudioResult = withContext(Dispatchers.IO) {
    if (apiKey.isBlank() || baseUrl.isBlank()) return@withContext ElevenLabsAudioResult(error = "MurrLex server session is missing")
    val cleanLanguageCode = languageCode.trim().lowercase(Locale.ROOT).ifBlank { "be" }
    val outputFormat = CardRepository.DEFAULT_ELEVENLABS_OUTPUT_FORMAT
    val cacheKey = openAiCacheKey("elevenlabs-tts", model, voiceId, outputFormat, cleanLanguageCode, text)
    val cacheDir = openAiCacheDir(context)
    val audioCacheFile = File(cacheDir, "$cacheKey.mp3")
    val metaCacheFile = File(cacheDir, "$cacheKey.json")
    if (audioCacheFile.exists() && metaCacheFile.exists()) {
        val createdAt = runCatching { JSONObject(metaCacheFile.readText(Charsets.UTF_8)).optLong("createdAt", 0L) }
            .getOrDefault(0L)
        if (isOpenAiCacheFresh(createdAt, cacheDurationMinutes)) {
            return@withContext ElevenLabsAudioResult(audioCacheFile, "cache")
        }
        audioCacheFile.delete()
        metaCacheFile.delete()
    }
    val endpoint = baseUrl.trimEnd('/') + "/api/ai/speech"
    val payload = JSONObject()
        .put("provider", "elevenlabs")
        .put("text", text)
        .put("model", model)
        .put("voiceId", voiceId)
        .toString()
    val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 20_000
        readTimeout = 45_000
        doOutput = true
        setRequestProperty("Authorization", "Bearer $apiKey")
        setRequestProperty("Content-Type", "application/json; charset=utf-8")
        setRequestProperty("Accept", "audio/mpeg")
        setRequestProperty("User-Agent", "MurrLex/${BuildConfig.VERSION_NAME}")
    }
    connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
    val status = connection.responseCode
    if (status !in 200..299) {
        val errorText = connection.errorStream
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { it.readText() }
            .orEmpty()
            .trim()
            .take(220)
        connection.disconnect()
        val details = if (errorText.isBlank()) "" else ": $errorText"
        return@withContext ElevenLabsAudioResult(error = "ElevenLabs HTTP $status$details")
    }
    cacheDir.mkdirs()
    connection.inputStream.use { input ->
        audioCacheFile.outputStream().use { outputStream -> input.copyTo(outputStream) }
    }
    connection.disconnect()
    val meta = JSONObject()
        .put("type", "audio")
        .put("task", "$cleanLanguageCode-tts")
        .put("provider", "ElevenLabs")
        .put("model", model)
        .put("voice", voiceId)
        .put("language_code", cleanLanguageCode)
        .put("output_format", outputFormat)
        .put("createdAt", System.currentTimeMillis())
        .put("file", audioCacheFile.name)
    metaCacheFile.writeText(meta.toString(), Charsets.UTF_8)
    ElevenLabsAudioResult(audioCacheFile, "API")
}

private fun playOpenAiAudioFile(file: File): Boolean {
    return AppAudioPlayer.playFile(file, deleteOnFailure = file.parentFile?.name != "openai_cache")
}

private fun cachedAudioMetadataLabel(audioFile: File): String {
    val metaFile = File(audioFile.parentFile, audioFile.nameWithoutExtension + ".json")
    if (!metaFile.exists()) return "unknown cached model"
    return runCatching {
        val meta = JSONObject(metaFile.readText(Charsets.UTF_8))
        val provider = meta.optString("provider").ifBlank { "cached TTS" }
        val model = meta.optString("model").ifBlank { "unknown model" }
        val voice = meta.optString("voice").ifBlank { "unknown voice" }
        val language = meta.optString("language_code").ifBlank { meta.optString("targetLanguage") }
        buildString {
            append(provider)
            append(" model ")
            append(model)
            append(", voice ")
            append(voice)
            if (language.isNotBlank()) append(", language ").append(language)
        }
    }.getOrDefault("unknown cached model")
}

private fun openAiCacheDir(context: Context): File = File(context.cacheDir, "openai_cache")

private fun openAiCacheKey(vararg parts: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    return digest.digest(parts.joinToString("\u001F") { it.normalizedCacheLookupText() }.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}

private fun String.normalizedCacheLookupText(): String {
    return lowercase(Locale.ROOT)
        .replace(Regex("\\p{Punct}+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun isOpenAiCacheFresh(createdAt: Long, cacheDurationMinutes: Long): Boolean {
    if (createdAt <= 0L) return false
    if (cacheDurationMinutes < 0L) return true
    return System.currentTimeMillis() - createdAt <= cacheDurationMinutes * 60_000L
}

private fun findCachedCardSideAudioFile(
    context: Context,
    text: String,
    languageTag: String,
    state: StudyUiState
): File? {
    val cleanText = text.trim()
    if (cleanText.isBlank()) return null
    val cacheDir = openAiCacheDir(context)
    val candidates = buildList {
        val elevenLabsLanguageCode = elevenLabsSpecialLanguageCode(languageTag, cleanText, state)
        if (elevenLabsLanguageCode != null) {
            add(
                openAiCacheKey(
                    "elevenlabs-tts",
                    state.elevenLabsModel,
                    state.elevenLabsVoiceId,
                    CardRepository.DEFAULT_ELEVENLABS_OUTPUT_FORMAT,
                    elevenLabsLanguageCode,
                    cleanText
                )
            )
        }
        add(openAiCacheKey("tts", state.openAiTtsModel, state.openAiTtsVoice, cleanText))
    }
    return candidates.asSequence()
        .distinct()
        .map { key -> File(cacheDir, "$key.mp3") to File(cacheDir, "$key.json") }
        .firstOrNull { (audioFile, metaFile) -> audioFile.exists() && metaFile.exists() }
        ?.first
}

private fun clearCachedCardSideAudioFiles(
    context: Context,
    text: String,
    languageTag: String,
    state: StudyUiState
): Int {
    val cleanText = text.trim()
    if (cleanText.isBlank()) return 0
    val cacheDir = openAiCacheDir(context)
    val candidates = buildList {
        val elevenLabsLanguageCode = elevenLabsSpecialLanguageCode(languageTag, cleanText, state)
        if (elevenLabsLanguageCode != null) {
            add(
                openAiCacheKey(
                    "elevenlabs-tts",
                    state.elevenLabsModel,
                    state.elevenLabsVoiceId,
                    CardRepository.DEFAULT_ELEVENLABS_OUTPUT_FORMAT,
                    elevenLabsLanguageCode,
                    cleanText
                )
            )
        }
        add(openAiCacheKey("tts", state.openAiTtsModel, state.openAiTtsVoice, cleanText))
    }
    return candidates
        .distinct()
        .sumOf { key ->
            listOf(File(cacheDir, "$key.mp3"), File(cacheDir, "$key.json"))
                .count { file -> file.exists() && file.delete() }
        }
}

private fun elevenLabsSpecialLanguageCode(languageTag: String, text: String, state: StudyUiState): String? {
    val candidates = listOf(
        languageTag.elevenLabsLanguageCodeFromTagOrName(),
        text.speechLanguageTagFromText()?.elevenLabsLanguageCodeFromTagOrName()
    )
    return candidates.firstOrNull { code -> code != null && code in state.elevenLabsTtsLanguageCodes }
}

private fun String.elevenLabsLanguageCodeFromTagOrName(): String? {
    val normalized = trim().lowercase(Locale.ROOT)
    return when {
        normalized == "en" || normalized.startsWith("en-") ||
            normalized in listOf("english", "angielski") -> "en"
        normalized == "es" || normalized.startsWith("es-") ||
            normalized in listOf("spanish", "espanol", "espa\u00f1ol") -> "es"
        normalized == "pl" || normalized.startsWith("pl-") ||
            normalized in listOf("polish", "polski") -> "pl"
        normalized == "ru" || normalized.startsWith("ru-") ||
            normalized in listOf("russian", "rosyjski") -> "ru"
        normalized == "by" || normalized == "be" || normalized.startsWith("be-") ||
            normalized in listOf("belarusian", "belarus") -> "be"
        normalized == "uk" || normalized == "ua" || normalized.startsWith("uk-") ||
            normalized in listOf("ukrainian") -> "uk"
        normalized == "de" || normalized.startsWith("de-") ||
            normalized in listOf("german", "deutsch") -> "de"
        normalized == "lv" || normalized.startsWith("lv-") ||
            normalized in listOf("latvian", "latviesu") -> "lv"
        normalized == "lt" || normalized.startsWith("lt-") ||
            normalized in listOf("lithuanian", "lietuviu") -> "lt"
        normalized == "pt" || normalized.startsWith("pt-") ||
            normalized in listOf("portuguese", "portugues") -> "pt"
        else -> null
    }
}

private data class UiText(
    val back: String,
    val hideHiddenLessons: String,
    val showHiddenLessons: String,
    val settings: String,
    val copiedToInput: String,
    val correct: String,
    val makeItRight: String,
    val alphabetical: String,
    val random: String,
    val showAll: String,
    val inPortion: String,
    val left: String,
    val done: String,
    val next: String,
    val repeat: String,
    val nextLesson: String,
    val lessonCompleteTitle: String,
    val lessonCompleteBody: String,
    val allDone: String,
    val nothingToShow: String,
    val emptyHint: String,
    val copy: String,
    val ok: String,
    val editCard: String,
    val editCardSubtitle: String,
    val mistakeSource: String,
    val hintOrRule: String,
    val madeAt: String,
    val where: String,
    val save: String,
    val delete: String,
    val cancel: String,
    val deleteCardTitle: String,
    val deleteCardText: String,
    val interfaceLanguage: String
)

private val LocalUiText = staticCompositionLocalOf { uiTextFor("en") }

@Composable
private fun rememberUiText(): UiText = LocalUiText.current

private fun uiTextFor(languageCode: String): UiText {
    return UiText(
        back = "Back",
        hideHiddenLessons = "Hide hidden lessons",
        showHiddenLessons = "Show hidden lessons",
        settings = "Settings",
        copiedToInput = "Copied to input",
        correct = "Correct",
        makeItRight = "Make it right",
        alphabetical = "Alphabetical",
        random = "Random",
        showAll = "Show all",
        inPortion = "In portion",
        left = "Left",
        done = "Done",
        next = "Next",
        repeat = "Repeat",
        nextLesson = "Next lesson",
        lessonCompleteTitle = "Excellent work. This lesson is complete!",
        lessonCompleteBody = "You answered every visible card.",
        allDone = "All done. Every visible card already has three stars.",
        nothingToShow = "Nothing to show yet.",
        emptyHint = "Turn on Show all or add cards to keep practicing.",
        copy = "Copy",
        ok = "OK",
        editCard = "Edit card",
        editCardSubtitle = "Update this card without leaving practice.",
        mistakeSource = "Mistake made / source value",
        hintOrRule = "Hint or rule",
        madeAt = "Made at",
        where = "Where",
        save = "Save",
        delete = "Delete",
        cancel = "Cancel",
        deleteCardTitle = "Delete card?",
        deleteCardText = "This card will be removed from the lesson.",
        interfaceLanguage = "Interface language"
    )
}

class MainActivity : ComponentActivity() {
    private var quickVoiceLaunchSignal by mutableStateOf(0)
    private var sharedPostSignal by mutableStateOf<SharedPostSignal?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIncomingIntent(intent)
        enableEdgeToEdge()
        setContent {
            MakeMistakeTheme {
                MakeMistakeApp(
                    quickVoiceLaunchSignal = quickVoiceLaunchSignal,
                    sharedPostSignal = sharedPostSignal,
                    onSharedPostConsumed = { sharedPostSignal = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_START_QUICK_VOICE -> {
                quickVoiceLaunchSignal++
            }
            Intent.ACTION_SEND -> {
                if (intent.type?.startsWith("text/") == true) {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                        ?: intent.getStringExtra(Intent.EXTRA_SUBJECT)
                        ?: return
                    sharedText.trim().takeIf { it.isNotBlank() }?.let { text ->
                        sharedPostSignal = SharedPostSignal(System.currentTimeMillis(), text)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MakeMistakeApp(
    viewModel: MainViewModel = viewModel(),
    quickVoiceLaunchSignal: Int = 0,
    sharedPostSignal: SharedPostSignal? = null,
    onSharedPostConsumed: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val ui = remember(state.interfaceLanguage) { uiTextFor(state.interfaceLanguage) }
    var showSplash by remember { mutableStateOf(quickVoiceLaunchSignal == 0) }
    var splashReady by remember { mutableStateOf(false) }
    var titleActivated by remember { mutableStateOf(false) }
    var messageBubbleVisible by remember { mutableStateOf(false) }
    var messageBubbleText by remember { mutableStateOf("") }
    var messageBubblePositive by remember { mutableStateOf(false) }
    var successStarVisible by remember { mutableStateOf(false) }
    val appTitleColor by animateColorAsState(
        targetValue = if (titleActivated) BrandSaladColor else BrandRedColor,
        animationSpec = tween(durationMillis = 2000),
        label = "appTitleColor"
    )
    val exportJson = remember { Json { prettyPrint = true; encodeDefaults = true } }
    var jsonPendingExport by remember { mutableStateOf<Pair<String, String>?>(null) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        viewModel.importLessons(uris)
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val pending = jsonPendingExport
        if (uri != null && pending != null) {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                writer.write(pending.second)
            }
        }
        jsonPendingExport = null
    }
    var imageImportIntoCurrentLesson by remember { mutableStateOf(false) }
    var pendingCameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraIntoCurrentLesson by remember { mutableStateOf(false) }
    val imageTextLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        val selectedUris = uris.take(5)
        if (selectedUris.isNotEmpty()) {
            viewModel.importImageTextCards(
                imageUris = selectedUris,
                appendToCurrentLesson = imageImportIntoCurrentLesson
            )
        }
        imageImportIntoCurrentLesson = false
    }
    val cameraImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val imageUri = pendingCameraImageUri
        val appendToCurrentLesson = pendingCameraIntoCurrentLesson
        pendingCameraImageUri = null
        pendingCameraIntoCurrentLesson = false
        if (saved && imageUri != null) {
            viewModel.importImageTextCards(
                imageUri = imageUri,
                appendToCurrentLesson = appendToCurrentLesson
            )
        } else {
            viewModel.showMessage("Photo was not saved")
        }
    }
    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    var voiceDialogVisible by remember { mutableStateOf(false) }
    var voiceRecording by remember { mutableStateOf(false) }
    var voiceStatus by remember { mutableStateOf("Hold to speak") }
    var voiceElapsedMs by remember { mutableStateOf(0L) }
    var pendingVoiceStart by remember { mutableStateOf(false) }
    var voiceLanguageTag by remember { mutableStateOf(Locale.getDefault().toLanguageTag()) }
    var voiceExpectedLanguage by remember { mutableStateOf("System language") }
    var voiceTarget by remember { mutableStateOf(VoiceInputTarget.ANSWER) }
    var voiceUseOfflineRecognition by remember { mutableStateOf(false) }
    var voiceUseOpenAiRecognition by remember { mutableStateOf(false) }
    var voiceNoMatchRetried by remember { mutableStateOf(false) }
    var voiceHoldActive by remember { mutableStateOf(false) }
    var voiceHoldReleasedAt by remember { mutableStateOf(0L) }
    var lastVoiceActivityAt by remember { mutableStateOf(0L) }
    var voiceSignalLevel by remember { mutableStateOf(0f) }
    var voiceLastSignalAt by remember { mutableStateOf(0L) }
    var openAiRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var openAiRecordingFile by remember { mutableStateOf<File?>(null) }
    var quickEditCardId by remember { mutableStateOf<Int?>(null) }
    val quickEditActive = quickEditCardId != null && state.currentCard?.id == quickEditCardId
    var showHeaderLessonInfo by remember { mutableStateOf(false) }
    var showResetProgressConfirm by remember { mutableStateOf(false) }
    var showTranslationDownloadDialog by remember { mutableStateOf(false) }
    var showLocalLanguageDialog by remember { mutableStateOf(false) }
    var showOnboardingLocalLanguageOffer by remember { mutableStateOf(false) }
    var downloadedTranslationLanguages by remember { mutableStateOf(setOf<String>()) }
var translationDownloadLanguage by remember { mutableStateOf(state.activeVocabularyTargetLanguage) }
var translationDownloadingLabel by remember { mutableStateOf<String?>(null) }
var translateAutoSpeakEnabled by remember { mutableStateOf(true) }
var suppressTranslateAutoSpeak by remember { mutableStateOf(false) }
var isDeviceOnline by remember { mutableStateOf(context.isNetworkAvailable()) }
val openAiTranslationPreferred = state.useOpenAiModels && isDeviceOnline && state.openAiApiKey.isNotBlank()
var suppressAudioStartUntilMs by remember { mutableStateOf(0L) }
var cardStatusBlinkOn by remember { mutableStateOf(false) }
var trainSourceCard by remember { mutableStateOf<Flashcard?>(null) }
var moveStudyCardDialogVisible by remember { mutableStateOf(false) }
var showStudyMap by remember { mutableStateOf(false) }
var showCatalogMap by remember { mutableStateOf(false) }
var restoreCatalogMapOnReturn by remember { mutableStateOf(false) }
var lessonSearchQuery by remember { mutableStateOf("") }
var lessonSortMode by remember { mutableStateOf(LessonSortMode.UPDATED) }
var lessonSortDescending by remember { mutableStateOf(true) }
val catalogListState = rememberLazyListState()
val catalogMapScrollState = rememberScrollState()
var returnToStudyMapOnBack by remember { mutableStateOf(false) }
var pendingSharedPostText by remember { mutableStateOf<String?>(null) }
var showUrlImportDialog by remember { mutableStateOf(false) }
var showStudyDisplayMenu by remember { mutableStateOf(false) }
var showCatChat by remember { mutableStateOf(false) }
var showCatMenu by remember { mutableStateOf(false) }
var showAnimationCatalog by remember { mutableStateOf(false) }
var showOriginalAnimation by remember { mutableStateOf(false) }
val animationScreenOpen = showAnimationCatalog || showOriginalAnimation
var activeLanguageGame by remember { mutableStateOf<LanguageGameMode?>(null) }
val fullScreenGameOpen = activeLanguageGame in setOf(
    LanguageGameMode.PSEUDO_3D_WALK,
    LanguageGameMode.EIGHT_BIT_PLATFORMER,
    LanguageGameMode.SIXTEEN_BIT_SPIN_RUNNER,
    LanguageGameMode.GODOT_RUNNER_LAB,
    LanguageGameMode.LOTTIE_RUNNER_LAB,
    LanguageGameMode.FLUTTER_RUNNER_LAB,
    LanguageGameMode.RIVE_RUNNER_LAB,
    LanguageGameMode.RIVE_LETTER_BLOCKS
)
var catChatInput by remember { mutableStateOf("") }
var catChatBasicLanguage by remember { mutableStateOf(state.activeVocabularySourceLanguage) }
var catChatTargetLanguage by remember { mutableStateOf(state.activeVocabularyTargetLanguage) }
var catChatBusy by remember { mutableStateOf(false) }
var catChatAutoSpeak by remember { mutableStateOf(true) }
var pendingCatReplyToSpeak by remember { mutableStateOf<String?>(null) }
var catChatMessages by remember { mutableStateOf<List<CatChatMessage>>(emptyList()) }
var catDialogId by remember { mutableStateOf<String?>(null) }
    fun forceRefreshOnlineState(showMessage: Boolean = true) {
        val currentOnline = context.isNetworkAvailable()
        isDeviceOnline = currentOnline
        if (showMessage) {
            viewModel.showMessage(if (currentOnline) "Online" else "Offline")
        }
    }
    var textToSpeechReady by remember { mutableStateOf(false) }
    val textToSpeech = remember {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            textToSpeechReady = status == TextToSpeech.SUCCESS
        }
        engine
    }
    var activeSpeechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    val voicePermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            when {
                granted -> pendingVoiceStart = true
                !granted -> viewModel.showMessage("Microphone permission denied")
            }
        }
    fun mlKitLanguageForEarlyRequest(language: String): String? {
        val tag = when (language.trim().lowercase(Locale.ROOT)) {
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
            else -> language.trim().takeIf { it.length in 2..3 }?.lowercase(Locale.ROOT).orEmpty()
        }
        return TranslateLanguage.fromLanguageTag(tag)
    }

    val requestGoogleOfflineTranslation: (
        String,
        String,
        String,
        (String) -> Unit,
        () -> Unit
    ) -> Unit = { text, sourceLanguageName, targetLanguageName, onSuccess, onFailure ->
        val cleanText = text.trim()
        if (cleanText.isNotBlank()) {
            val source = mlKitLanguageForEarlyRequest(sourceLanguageName)
            val target = mlKitLanguageForEarlyRequest(targetLanguageName)
            if (source == null || target == null) {
                onFailure()
            } else {
                val translator = Translation.getClient(
                    TranslatorOptions.Builder()
                        .setSourceLanguage(source)
                        .setTargetLanguage(target)
                        .build()
                )
                translator.downloadModelIfNeeded(DownloadConditions.Builder().build())
                    .addOnSuccessListener {
                        translator.translate(cleanText)
                            .addOnSuccessListener(onSuccess)
                            .addOnFailureListener { onFailure() }
                            .addOnCompleteListener { translator.close() }
                    }
                    .addOnFailureListener {
                        onFailure()
                        translator.close()
                    }
            }
        }
    }
    fun releaseSpeechRecognizer() {
        activeSpeechRecognizer?.destroy()
        activeSpeechRecognizer = null
    }

    fun offlineSpeechIntent(languageTag: String): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 60000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 60000L)
        }
    }

    fun onlineSpeechIntent(languageTag: String): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 60000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 60000L)
        }
    }

    fun cacheOriginalVoiceAudio(sourceFile: File?): String {
        if (sourceFile == null || !sourceFile.exists()) return ""
        val dir = File(context.filesDir, "card_cache/original_voice").apply { mkdirs() }
        val target = File(dir, "voice_${System.currentTimeMillis()}.m4a")
        return runCatching {
            sourceFile.copyTo(target, overwrite = true)
            target.absolutePath
        }.getOrDefault("")
    }

    fun sendCatChatMessage(rawInput: String = catChatInput) {
        val cleanInput = rawInput.trim()
        if (cleanInput.isBlank() || catChatBusy) return
        titleActivated = true
        showCatChat = true
        catChatInput = ""
        val history = catChatMessages
        val userMessageId = "user_${UUID.randomUUID()}"
        val catMessageId = "cat_${UUID.randomUUID()}"
        val sourceLanguage = catChatBasicLanguage
        val answerLanguage = catChatTargetLanguage
        val pendingUserMessage = CatChatMessage(
            id = userMessageId,
            text = cleanInput,
            fromCat = false
        )
        catChatMessages = history + pendingUserMessage
        catChatBusy = true
        coroutineScope.launch {
            val reply = viewModel.requestCatChatReply(
                message = cleanInput,
                history = history.map { if (it.fromCat) "Cat: ${it.text}" else "User: ${it.text}" },
                sourceLanguage = sourceLanguage,
                targetLanguage = answerLanguage
            )
            val pendingCatMessage = CatChatMessage(
                id = catMessageId,
                text = reply,
                fromCat = true,
                analysisSourceText = cleanInput
            )
            catChatMessages = history + pendingUserMessage + pendingCatMessage
            if (catChatAutoSpeak) {
                pendingCatReplyToSpeak = reply
            }
            val analysis = viewModel.requestCatChatAnalysis(
                message = cleanInput,
                sourceLanguage = sourceLanguage,
                targetLanguage = answerLanguage
            )
            catChatMessages = catChatMessages.map { message ->
                if (message.id == catMessageId) message.copy(analysis = analysis) else message
            }
            val savedDialog = viewModel.appendCatDialogTurn(
                dialogId = catDialogId,
                basicLanguage = sourceLanguage,
                targetLanguage = answerLanguage,
                userText = cleanInput,
                replyText = reply,
                analysisText = analysis
            )
            catDialogId = savedDialog.id
            catChatMessages = savedDialog.messages.toCatChatMessages()
            catChatBusy = false
        }
    }

    fun handleRecognizedVoiceText(spokenText: String, originalAudioPath: String = "") {
        val cleanText = spokenText.trim()
        if (cleanText.isBlank()) {
            viewModel.showMessage("No speech recognized")
            return
        }
        val recognitionLog = when {
            voiceUseOpenAiRecognition -> "speech recognized with OpenAI STT model ${state.openAiSpeechModel}, language $voiceLanguageTag"
            voiceUseOfflineRecognition -> "speech recognized with Android offline SpeechRecognizer, language $voiceLanguageTag"
            else -> "speech recognized with Android online SpeechRecognizer, language $voiceLanguageTag"
        }
        when (voiceTarget) {
            VoiceInputTarget.ANSWER -> {
                viewModel.updateAnswer(cleanText)
                viewModel.appendCurrentCardLog(recognitionLog)
                viewModel.showMessage("Voice input added")
            }
            VoiceInputTarget.QUICK_VOCABULARY -> {
                viewModel.addQuickVocabularyCard(cleanText, recognitionLog, originalAudioPath)
                val sameLanguageQuickVocabulary = mlKitLanguageTagForName(state.activeVocabularySourceLanguage)
                    .equals(mlKitLanguageTagForName(state.activeVocabularyTargetLanguage), ignoreCase = true)
                if (!sameLanguageQuickVocabulary && (state.useLocalTranslation || !isDeviceOnline) && !openAiTranslationPreferred) {
                    requestGoogleOfflineTranslation(
                        cleanText,
                        state.activeVocabularySourceLanguage,
                        state.activeVocabularyTargetLanguage,
                        { translated: String ->
                            viewModel.applyQuickVocabularyTranslation(
                                phrase = cleanText,
                                translated = translated,
                                sourceLanguage = state.activeVocabularySourceLanguage,
                                targetLanguage = state.activeVocabularyTargetLanguage
                            )
                        },
                        {}
                    )
                }
            }
            VoiceInputTarget.TRANSLATE_INPUT -> {
                viewModel.updateTranslationInput(cleanText)
                if ((state.useLocalTranslation || !isDeviceOnline) && !openAiTranslationPreferred) {
                    requestGoogleOfflineTranslation(
                        cleanText,
                        state.activeVocabularySourceLanguage,
                        state.activeVocabularyTargetLanguage,
                        { translated: String -> viewModel.updateTranslationOutput(translated, "Powered by Google Translator") },
                        { viewModel.showMessage("Offline translation models are not ready for this pair") }
                    )
                }
            }
            VoiceInputTarget.TRANSLATE_OUTPUT -> viewModel.updateTranslationOutput(cleanText, "")
            VoiceInputTarget.CARD_NATIVE -> viewModel.updateCardDraft(state.cardDraft.copy(nativeValue = cleanText))
            VoiceInputTarget.CARD_CORRECT -> viewModel.updateCardDraft(state.cardDraft.copy(correctValue = cleanText))
            VoiceInputTarget.CARD_HINT -> viewModel.updateCardDraft(state.cardDraft.copy(hint = cleanText))
            VoiceInputTarget.CARD_MADE_AT -> viewModel.updateCardDraft(state.cardDraft.copy(madeAt = cleanText))
            VoiceInputTarget.CARD_WHERE -> viewModel.updateCardDraft(state.cardDraft.copy(where = cleanText))
            VoiceInputTarget.CAT_CHAT -> {
                sendCatChatMessage(cleanText)
            }
        }
    }

    fun buildRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                voiceStatus = "Listening..."
            }

            override fun onBeginningOfSpeech() {
                voiceStatus = "Recording..."
                lastVoiceActivityAt = System.currentTimeMillis()
                voiceLastSignalAt = lastVoiceActivityAt
            }

            override fun onRmsChanged(rmsdB: Float) {
                if (rmsdB > 1.5f) {
                    lastVoiceActivityAt = System.currentTimeMillis()
                    voiceLastSignalAt = lastVoiceActivityAt
                    voiceSignalLevel = (rmsdB / 10f).coerceIn(0.1f, 1f)
                } else {
                    voiceSignalLevel = (voiceSignalLevel * 0.72f).takeIf { it >= 0.03f } ?: 0f
                }
            }
            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() {
                voiceRecording = false
                voiceSignalLevel = 0f
                voiceHoldActive = false
                voiceHoldReleasedAt = 0L
                voiceStatus = "Recognizing..."
            }

            override fun onError(error: Int) {
                voiceRecording = false
                voiceSignalLevel = 0f
                voiceHoldActive = false
                voiceHoldReleasedAt = 0L
                voiceDialogVisible = false
                releaseSpeechRecognizer()
                val message = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech service network error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission denied"
                    else -> "Voice input failed"
                }
                viewModel.showMessage(message + if (!context.isNetworkAvailable()) " You are offline." else "")
            }
            override fun onResults(results: Bundle?) {
                voiceRecording = false
                voiceSignalLevel = 0f
                voiceHoldActive = false
                voiceHoldReleasedAt = 0L
                voiceDialogVisible = false
                releaseSpeechRecognizer()
                val spokenText = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                    .trim()
                handleRecognizedVoiceText(spokenText)
            }
            override fun onPartialResults(partialResults: Bundle?) {
                partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.takeIf { it.isNotBlank() }
                    ?.let { voiceStatus = it }
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            releaseSpeechRecognizer()
        }
    }

    DisposableEffect(textToSpeech) {
        onDispose {
            AppAudioPlayer.stop()
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    DisposableEffect(context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val mainHandler = Handler(Looper.getMainLooper())
        fun refreshOnlineState() {
            mainHandler.post { isDeviceOnline = context.isNetworkAvailable() }
        }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                refreshOnlineState()
            }

            override fun onLost(network: Network) {
                refreshOnlineState()
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                refreshOnlineState()
            }
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                refreshOnlineState()
            }
        }
        connectivityManager.registerDefaultNetworkCallback(callback)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                receiver,
                IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        }
        refreshOnlineState()
        onDispose {
            runCatching { connectivityManager.unregisterNetworkCallback(callback) }
            runCatching { context.unregisterReceiver(receiver) }
        }
    }


    LaunchedEffect("cardStatusBlink", state.cardStatusBlinkIntervalMs) {
        cardStatusBlinkOn = false
        if (state.cardStatusBlinkIntervalMs > 0L) {
            while (true) {
                delay(state.cardStatusBlinkIntervalMs)
                cardStatusBlinkOn = true
                delay(280L)
                cardStatusBlinkOn = false
            }
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        CardNotificationWorker.schedule(context, state.notificationIntervalMinutes)
        val lessonId = (context as? MainActivity)?.intent?.getStringExtra(CardNotificationWorker.EXTRA_LESSON_ID)
        val cardId = (context as? MainActivity)?.intent?.getIntExtra(CardNotificationWorker.EXTRA_CARD_ID, -1) ?: -1
        viewModel.openCardFromNotification(lessonId, cardId)
    }
    LaunchedEffect(Unit) {
        if (state.soundEffectsEnabled) {
            launch(Dispatchers.Default) { prewarmFeedbackSound(context) }
        }
        splashReady = true
        delay(1300)
        showSplash = false
    }

    fun startVoiceInput(target: VoiceInputTarget = VoiceInputTarget.ANSWER) {
        if (voiceRecording) {
            voiceRecording = false
            voiceSignalLevel = 0f
            voiceStatus = "Recognizing..."
            if (voiceUseOpenAiRecognition) {
                val recorder = openAiRecorder
                openAiRecorder = null
                runCatching {
                    recorder?.stop()
                    recorder?.release()
                }.onFailure {
                    recorder?.release()
                }
            } else {
                activeSpeechRecognizer?.stopListening()
            }
            return
        }
        val useOfflineRecognition = state.useLocalTranslation || !isDeviceOnline
        val useOpenAiRecognition = state.useOpenAiModels &&
            isDeviceOnline &&
            state.openAiApiKey.isNotBlank() &&
            !useOfflineRecognition
        if (!useOpenAiRecognition && useOfflineRecognition && !SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            viewModel.showMessage("Offline speech recognition is not available on this phone.")
            return
        }
        if (!useOpenAiRecognition && !useOfflineRecognition && !SpeechRecognizer.isRecognitionAvailable(context)) {
            viewModel.showMessage("Speech recognition is not available on this phone.")
            return
        }
        val draftCard = state.cardDraft.editingCardId?.let { id ->
            state.selectedLesson?.cards?.firstOrNull { it.id == id }
                ?: state.editorLesson?.cards?.firstOrNull { it.id == id }
        }
        val lessonSampleCard = state.editorLesson?.cards?.firstOrNull() ?: state.selectedLesson?.cards?.firstOrNull()
        val language = when (target) {
            VoiceInputTarget.ANSWER -> if (quickEditActive) {
                state.currentCard.voiceLanguageForDisplayedSide(state.isBackVisible, state.interfaceLanguage, state.selectedLesson)
            } else {
                state.currentCard.voiceLanguageForCorrectSide(state.interfaceLanguage, state.selectedLesson)
            }
            VoiceInputTarget.QUICK_VOCABULARY -> languageForVoice(state.activeVocabularySourceLanguage, "", state.interfaceLanguage)
            VoiceInputTarget.TRANSLATE_INPUT -> languageForVoice(state.activeVocabularySourceLanguage, state.translationInput, state.interfaceLanguage)
            VoiceInputTarget.TRANSLATE_OUTPUT -> languageForVoice(state.activeVocabularyTargetLanguage, state.translationOutput, state.interfaceLanguage)
            VoiceInputTarget.CARD_NATIVE -> languageForVoice(
                draftCard?.sourceLanguage.asLessonLanguage()
                    ?: state.editorLesson?.sourceLanguage.asLessonLanguage()
                    ?: state.selectedLesson?.sourceLanguage.asLessonLanguage()
                    ?: lessonSampleCard?.sourceLanguage.asLessonLanguage()
                    ?: draftCard?.frontLabel()
                    ?: lessonSampleCard?.frontLabel()
                    ?: state.activeVocabularySourceLanguage,
                state.cardDraft.nativeValue,
                state.interfaceLanguage
            )
            VoiceInputTarget.CARD_CORRECT -> languageForVoice(
                draftCard?.targetLanguage.asLessonLanguage()
                    ?: state.editorLesson?.targetLanguage.asLessonLanguage()
                    ?: state.selectedLesson?.targetLanguage.asLessonLanguage()
                    ?: lessonSampleCard?.targetLanguage.asLessonLanguage()
                    ?: draftCard?.backLabel()
                    ?: lessonSampleCard?.backLabel()
                    ?: state.activeVocabularyTargetLanguage,
                state.cardDraft.correctValue,
                state.interfaceLanguage
            )
            VoiceInputTarget.CARD_HINT,
            VoiceInputTarget.CARD_MADE_AT,
            VoiceInputTarget.CARD_WHERE -> languageForVoice(state.interfaceLanguage, "", state.interfaceLanguage)
            VoiceInputTarget.CAT_CHAT -> languageForVoice(catChatBasicLanguage, "", state.interfaceLanguage)
        }
        val requestedSpeechTag = speechTagForDictionaryLanguage(language.second)
            ?: language.first.takeIf { it.isNotBlank() }
            ?: state.offlineSpeechLanguageTag
        val selectedSpeechLanguage = OfflineSpeechLanguages.firstOrNull { it.tag == requestedSpeechTag }
        val effectiveSpeechTag = selectedSpeechLanguage?.tag ?: requestedSpeechTag
        val effectiveSpeechName = selectedSpeechLanguage?.name ?: effectiveSpeechTag.speechLanguageDisplayName()
        val selectedSpeechStatus = state.offlineSpeechStatuses[effectiveSpeechTag]
            ?: CardRepository.OFFLINE_SPEECH_STATUS_NOT_DOWNLOADED
        if (!useOpenAiRecognition && useOfflineRecognition && selectedSpeechStatus != CardRepository.OFFLINE_SPEECH_STATUS_READY) {
            viewModel.showMessage("Download this language first for offline recognition.")
            return
        }
        voiceTarget = target
        voiceUseOfflineRecognition = useOfflineRecognition
        voiceUseOpenAiRecognition = useOpenAiRecognition
        voiceLanguageTag = effectiveSpeechTag
        voiceExpectedLanguage = if (useOpenAiRecognition) "OpenAI - $effectiveSpeechName" else effectiveSpeechName
        voiceSignalLevel = 0f
        voiceLastSignalAt = System.currentTimeMillis()
        viewModel.showMessage(
            "Speak $effectiveSpeechName${when {
                useOpenAiRecognition -> " with OpenAI"
                useOfflineRecognition -> " offline"
                else -> " online"
            }}"
        )
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            voicePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        pendingVoiceStart = true
    }

    fun stopVoiceInput() {
        if (!voiceRecording) return
        voiceRecording = false
        voiceSignalLevel = 0f
        voiceHoldActive = false
        voiceHoldReleasedAt = 0L
        voiceStatus = "Recognizing..."
        if (voiceUseOpenAiRecognition) {
            val recorder = openAiRecorder
            val audioFile = openAiRecordingFile
            openAiRecorder = null
            openAiRecordingFile = null
            voiceStatus = "Waiting for OpenAI..."
            runCatching {
                recorder?.stop()
                recorder?.release()
            }.onFailure {
                recorder?.release()
                viewModel.showMessage("Audio recording failed")
                return
            }
            voiceDialogVisible = false
            if (audioFile == null || !audioFile.exists()) {
                viewModel.showMessage("Audio recording failed")
                voiceDialogVisible = false
                return
            }
            coroutineScope.launch {
                val text = requestOpenAiTranscriptionFile(
                    audioFile = audioFile,
                    baseUrl = state.openAiBaseUrl,
                    apiKey = state.openAiApiKey,
                    model = state.openAiSpeechModel,
                    languageTag = voiceLanguageTag
                )
                val originalAudioPath = if (!text.isNullOrBlank()) cacheOriginalVoiceAudio(audioFile) else ""
                audioFile.delete()
                voiceDialogVisible = false
                viewModel.logOpenAiActivity(
                    action = "speech-to-text API",
                    details = "${state.openAiSpeechModel}; ${voiceLanguageTag}; ${text.orEmpty().take(180)}"
                )
                handleRecognizedVoiceText(text.orEmpty(), originalAudioPath)
            }
        } else {
            activeSpeechRecognizer?.stopListening()
        }
    }

    LaunchedEffect(sharedPostSignal?.id) {
        sharedPostSignal?.let { signal ->
            pendingSharedPostText = signal.text
            onSharedPostConsumed()
            showSplash = false
        }
    }

    LaunchedEffect(quickVoiceLaunchSignal, showSplash) {
        if (quickVoiceLaunchSignal > 0 && !showSplash) {
            titleActivated = true
            startVoiceInput(VoiceInputTarget.QUICK_VOCABULARY)
        }
    }

    suspend fun requestOpenAiTranscription(
        audioFile: File,
        baseUrl: String,
        apiKey: String,
        model: String,
        languageTag: String
    ): String? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || !audioFile.exists()) return@withContext null
        val boundary = "MurrLexBoundary${System.currentTimeMillis()}"
        val lineBreak = "\r\n"
        val connection = (URL(baseUrl.trimEnd('/') + "/api/ai/transcribe").openConnection() as HttpURLConnection).apply {
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
            languageTag.substringBefore('-').takeIf { it.length == 2 }?.let { field("language", it) }
            output.write("--$boundary$lineBreak".toByteArray(Charsets.UTF_8))
            output.write("Content-Disposition: form-data; name=\"file\"; filename=\"speech.m4a\"$lineBreak".toByteArray(Charsets.UTF_8))
            output.write("Content-Type: audio/mp4$lineBreak$lineBreak".toByteArray(Charsets.UTF_8))
            audioFile.inputStream().use { input -> input.copyTo(output) }
            output.write(lineBreak.toByteArray(Charsets.UTF_8))
            output.write("--$boundary--$lineBreak".toByteArray(Charsets.UTF_8))
        }
        val status = connection.responseCode
        val responseText = (if (status in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { it.readText() }
            .orEmpty()
        connection.disconnect()
        if (status !in 200..299) return@withContext null
        runCatching { JSONObject(responseText).optString("text").trim() }.getOrNull()
    }

    fun appendVisibleCardTtsLog(text: String, entry: String) {
        val currentCardText = state.currentCard?.displayedCardText(state.isBackVisible)?.trim().orEmpty()
        if (state.screen == AppScreen.STUDY && currentCardText.equals(text.trim(), ignoreCase = true)) {
            viewModel.appendCurrentCardLog(entry)
        }
    }

    fun speakText(text: String, languageTag: String) {
        if (System.currentTimeMillis() < suppressAudioStartUntilMs) {
            return
        }
        val cleanText = text.trim()
        if (cleanText.isBlank()) {
            viewModel.showMessage("Nothing to read")
            return
        }
        val currentOnline = context.isNetworkAvailable()
        if (isDeviceOnline != currentOnline) isDeviceOnline = currentOnline
        fun offlineSuffix(): String = if (!currentOnline) " You are offline." else ""
        textToSpeech.stop()
        val elevenLabsLanguageCode = elevenLabsSpecialLanguageCode(languageTag, cleanText, state)
        findCachedCardSideAudioFile(context, cleanText, languageTag, state)?.let { cachedAudio ->
            viewModel.logOpenAiActivity(
                action = "text-to-speech cache",
                details = "cached audio; ${cleanText.take(180)}"
            )
            viewModel.showMessage(if (currentOnline) "Cached TTS" else "Cached TTS. You are offline.")
            if (!playOpenAiAudioFile(cachedAudio)) {
                viewModel.showMessage("Speech playback failed${offlineSuffix()}")
            } else {
                appendVisibleCardTtsLog(
                    cleanText,
                    "speech played from cached TTS audio created by ${cachedAudioMetadataLabel(cachedAudio)}"
                )
            }
            return
        }
        if (elevenLabsLanguageCode != null &&
            currentOnline &&
            state.openAiApiKey.isNotBlank()
        ) {
            coroutineScope.launch {
                val audio = requestElevenLabsSpeechAudioFile(
                    context = context,
                    text = cleanText,
                    languageCode = elevenLabsLanguageCode,
                    baseUrl = state.openAiBaseUrl,
                    apiKey = state.openAiApiKey,
                    model = state.elevenLabsModel,
                    voiceId = state.elevenLabsVoiceId,
                    cacheDurationMinutes = state.openAiCacheDurationMinutes
                )
                val audioFile = audio.file
                if (audioFile == null) {
                    val error = audio.error.ifBlank { "ElevenLabs speech failed" }
                    viewModel.logOpenAiActivity(
                        action = "${elevenLabsLanguageCode.uppercase(Locale.ROOT)} TTS ElevenLabs error",
                        details = error.take(300)
                    )
                    viewModel.showMessage(error.take(180))
                } else {
                    viewModel.logOpenAiActivity(
                        action = "${elevenLabsLanguageCode.uppercase(Locale.ROOT)} TTS ElevenLabs ${audio.source}",
                        details = "${state.elevenLabsModel}/${state.elevenLabsVoiceId}; ${cleanText.take(180)}"
                    )
                    viewModel.showMessage("ElevenLabs ${elevenLabsLanguageCode.uppercase(Locale.ROOT)} TTS: ${audio.source}")
                    if (!playOpenAiAudioFile(audioFile)) {
                        viewModel.showMessage("Speech playback failed")
                    } else {
                        appendVisibleCardTtsLog(
                            cleanText,
                            "speech generated with ElevenLabs model ${state.elevenLabsModel}, voice ${state.elevenLabsVoiceId}, language $elevenLabsLanguageCode, source ${audio.source}"
                        )
                    }
                }
            }
            return
        }
        if (state.useOpenAiModels && currentOnline && state.openAiApiKey.isNotBlank()) {
            coroutineScope.launch {
                val audio = requestOpenAiSpeechAudioFile(
                    context = context,
                    text = cleanText,
                    baseUrl = state.openAiBaseUrl,
                    apiKey = state.openAiApiKey,
                    model = state.openAiTtsModel,
                    voice = state.openAiTtsVoice,
                    cacheDurationMinutes = state.openAiCacheDurationMinutes,
                    speechRate = state.catReplySpeechRate
                )
                if (audio == null) {
                    viewModel.showMessage("OpenAI speech failed${offlineSuffix()}")
                } else {
                    viewModel.logOpenAiActivity(
                        action = "text-to-speech ${audio.source}",
                        details = "${state.openAiTtsModel}/${state.openAiTtsVoice}; ${cleanText.take(180)}"
                    )
                    viewModel.showMessage("OpenAI TTS: ${audio.source}")
                    if (!playOpenAiAudioFile(audio.file)) {
                        viewModel.showMessage("Speech playback failed")
                    } else {
                        appendVisibleCardTtsLog(
                            cleanText,
                            "speech generated with OpenAI TTS model ${state.openAiTtsModel}, voice ${state.openAiTtsVoice}, source ${audio.source}"
                        )
                    }
                }
            }
            return
        }
        if (!textToSpeechReady) {
            viewModel.showMessage("Speech engine is not ready yet${offlineSuffix()}")
            return
        }
        AppAudioPlayer.stop()
        val languageResult = textToSpeech.setLanguage(Locale.forLanguageTag(languageTag))
        if (languageResult == TextToSpeech.LANG_MISSING_DATA || languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            viewModel.showMessage("Speech language is not supported on this device${offlineSuffix()}")
            return
        }
        textToSpeech.setSpeechRate(state.catReplySpeechRate.coerceIn(0.5f, 2.5f))
        textToSpeech.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "speech-${System.currentTimeMillis()}")
        appendVisibleCardTtsLog(cleanText, "speech played with device TextToSpeech, language $languageTag")
    }

    LaunchedEffect(pendingCatReplyToSpeak) {
        val reply = pendingCatReplyToSpeak ?: return@LaunchedEffect
        pendingCatReplyToSpeak = null
        speakText(
            reply,
            languageForVoice(catChatTargetLanguage, reply, state.interfaceLanguage).first
        )
    }

    fun shareCachedCardSideAudio(card: Flashcard, sideIsBack: Boolean) {
        val text = card.displayedCardText(sideIsBack).trim()
        if (text.isBlank()) {
            viewModel.showMessage("Nothing to share for this card side")
            return
        }
        val languageTag = card.speechLanguageTagForSide(sideIsBack, state.interfaceLanguage, state.selectedLesson)
        val cachedAudio = findCachedCardSideAudioFile(
            context = context,
            text = text,
            languageTag = languageTag,
            state = state
        )
        if (cachedAudio != null) {
            shareAudioFile(
                context = context,
                sourceFile = cachedAudio,
                fileName = "${card.audioFileBaseName(sideIsBack)}.mp3"
            )
            viewModel.appendCardLog(
                card.id,
                "shared cached TTS audio created by ${cachedAudioMetadataLabel(cachedAudio)} for ${card.sideLanguageCode(sideIsBack, state.selectedLesson)} side"
            )
            viewModel.showMessage("Cached audio ready to share")
            return
        }
        val elevenLabsLanguageCode = elevenLabsSpecialLanguageCode(languageTag, text, state)
        when {
            elevenLabsLanguageCode != null &&
                isDeviceOnline &&
                state.openAiApiKey.isNotBlank() -> coroutineScope.launch {
                    val audio = requestElevenLabsSpeechAudioFile(
                        context = context,
                        text = text,
                        languageCode = elevenLabsLanguageCode,
                        baseUrl = state.openAiBaseUrl,
                        apiKey = state.openAiApiKey,
                        model = state.elevenLabsModel,
                        voiceId = state.elevenLabsVoiceId,
                        cacheDurationMinutes = state.openAiCacheDurationMinutes
                    )
                    val audioFile = audio.file
                    if (audioFile == null) {
                        val error = audio.error.ifBlank { "ElevenLabs audio download failed" }
                        viewModel.logOpenAiActivity(
                            action = "${elevenLabsLanguageCode.uppercase(Locale.ROOT)} audio file ElevenLabs error",
                            details = error.take(300)
                        )
                        viewModel.showMessage(error.take(180))
                    } else {
                        viewModel.logOpenAiActivity(
                            action = "${elevenLabsLanguageCode.uppercase(Locale.ROOT)} audio file ${audio.source}",
                            details = "${state.elevenLabsModel}/${state.elevenLabsVoiceId}; ${text.take(180)}"
                        )
                        viewModel.appendCardLog(
                            card.id,
                            "audio file generated with ElevenLabs model ${state.elevenLabsModel}, voice ${state.elevenLabsVoiceId}, language $elevenLabsLanguageCode, source ${audio.source}"
                        )
                        shareAudioFile(context, audioFile, "${card.audioFileBaseName(sideIsBack)}.mp3")
                    }
                }
            state.useOpenAiModels && isDeviceOnline && state.openAiApiKey.isNotBlank() -> coroutineScope.launch {
                val audio = requestOpenAiSpeechAudioFile(
                    context = context,
                    text = text,
                    baseUrl = state.openAiBaseUrl,
                    apiKey = state.openAiApiKey,
                    model = state.openAiTtsModel,
                    voice = state.openAiTtsVoice,
                    cacheDurationMinutes = state.openAiCacheDurationMinutes,
                    speechRate = state.catReplySpeechRate
                )
                if (audio == null) {
                    viewModel.showMessage("OpenAI audio download failed")
                } else {
                    viewModel.logOpenAiActivity(
                        action = "card audio file ${audio.source}",
                        details = "${state.openAiTtsModel}/${state.openAiTtsVoice}; ${text.take(180)}"
                    )
                    viewModel.appendCardLog(
                        card.id,
                        "audio file generated with OpenAI TTS model ${state.openAiTtsModel}, voice ${state.openAiTtsVoice}, source ${audio.source}"
                    )
                    shareAudioFile(context, audio.file, "${card.audioFileBaseName(sideIsBack)}.mp3")
                }
            }
            else -> viewModel.showMessage("No cached audio yet. Enable online TTS with a key first.")
        }
    }

    fun playOriginalVoiceAudio(card: Flashcard) {
        val path = card.originalAudioPathText().trim()
        if (path.isBlank()) {
            viewModel.showMessage("Original voice is not cached for this card")
            return
        }
        val file = File(path)
        if (!file.exists()) {
            viewModel.showMessage("Original voice file is missing")
            return
        }
        if (AppAudioPlayer.playFile(file, deleteOnFailure = false)) {
            viewModel.appendCardLog(card.id, "played original voice recording")
            viewModel.showMessage("Original voice")
        } else {
            viewModel.showMessage("Original voice playback failed")
        }
    }

    suspend fun requestOpenAiSpeechFile(
        context: Context,
        text: String,
        baseUrl: String,
        apiKey: String,
        model: String,
        voice: String
    ): File? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext null
        val payload = JSONObject()
            .put("model", model)
            .put("voice", voice)
            .put("text", text)
            .put("format", "mp3")
            .toString()
        val connection = (URL(baseUrl.trimEnd('/') + "/api/ai/speech").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 45_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "audio/mpeg")
            setRequestProperty("User-Agent", "MurrLex/${BuildConfig.VERSION_NAME}")
        }
        connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
        val status = connection.responseCode
        if (status !in 200..299) {
            connection.errorStream?.close()
            connection.disconnect()
            return@withContext null
        }
        val output = File(context.cacheDir, "openai_tts_${System.currentTimeMillis()}.mp3")
        connection.inputStream.use { input ->
            output.outputStream().use { outputStream -> input.copyTo(outputStream) }
        }
        connection.disconnect()
        output
    }

    fun playAudioFile(file: File) {
        if (!AppAudioPlayer.playFile(file, deleteOnFailure = true)) {
            viewModel.showMessage("Speech playback failed")
        }
    }

    fun mlKitLanguage(language: String): String? {
        val tag = when (language.trim().lowercase(Locale.ROOT)) {
            "english", "en" -> "en"
            "spanish", "es", "espanol" -> "es"
            "polish", "pl", "polski" -> "pl"
            "russian", "ru" -> "ru"
            "belarusian", "belarus", "by", "be" -> "be"
            "ukrainian", "uk", "ua" -> "uk"
            "german", "de", "deutsch" -> "de"
            else -> language.trim().takeIf { it.length in 2..3 }?.lowercase(Locale.ROOT).orEmpty()
        }
        return TranslateLanguage.fromLanguageTag(tag)
    }

    fun refreshDownloadedTranslationLanguages() {
        RemoteModelManager.getInstance()
            .getDownloadedModels(TranslateRemoteModel::class.java)
            .addOnSuccessListener { models ->
                downloadedTranslationLanguages = models
                    .mapNotNull { model -> dictionaryLanguageCodeForMlKitTag(model.language) }
                    .toSet()
            }
    }

    fun downloadGoogleLanguageModel(languageName: String, onComplete: (Boolean) -> Unit = {}) {
        val languageCode = mlKitLanguage(languageName)
        val displayCode = dictionaryLanguageCode(languageName)
        if (languageCode == null) {
            viewModel.showMessage("Translation language is not supported")
            onComplete(false)
            return
        }
        translationDownloadingLabel = "Downloading $displayCode"
        viewModel.showMessage("Downloading $displayCode")
        val model = TranslateRemoteModel.Builder(languageCode).build()
        RemoteModelManager.getInstance()
            .download(model, DownloadConditions.Builder().build())
            .addOnSuccessListener {
                translationDownloadingLabel = null
                refreshDownloadedTranslationLanguages()
                viewModel.showMessage("$displayCode ready")
                onComplete(true)
            }
            .addOnFailureListener {
                translationDownloadingLabel = null
                viewModel.showMessage("$displayCode download failed")
                onComplete(false)
            }
    }

    fun deleteGoogleLanguageModel(languageName: String) {
        val languageCode = mlKitLanguage(languageName)
        val displayCode = dictionaryLanguageCode(languageName)
        if (languageCode == null) {
            viewModel.showMessage("Translation language is not supported")
            return
        }
        translationDownloadingLabel = "Deleting $displayCode"
        val model = TranslateRemoteModel.Builder(languageCode).build()
        RemoteModelManager.getInstance()
            .deleteDownloadedModel(model)
            .addOnSuccessListener {
                translationDownloadingLabel = null
                refreshDownloadedTranslationLanguages()
                viewModel.showMessage("$displayCode translation model removed")
            }
            .addOnFailureListener {
                translationDownloadingLabel = null
                viewModel.showMessage("$displayCode delete failed")
            }
    }

    fun translateGoogleOfflineText(
        text: String,
        sourceLanguageName: String,
        targetLanguageName: String,
        onSuccess: (String) -> Unit,
        onFailure: () -> Unit = { viewModel.showMessage("Translation not available") }
    ) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) return
        val source = mlKitLanguage(sourceLanguageName)
        val target = mlKitLanguage(targetLanguageName)
        if (source == null || target == null) {
            viewModel.showMessage("Translation language is not supported")
            onFailure()
            return
        }
        val translator = Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(source)
                .setTargetLanguage(target)
                .build()
        )
        translator.downloadModelIfNeeded(DownloadConditions.Builder().build())
            .addOnSuccessListener {
                translator.translate(cleanText)
                    .addOnSuccessListener(onSuccess)
                    .addOnFailureListener {
                        onFailure()
                    }
                    .addOnCompleteListener { translator.close() }
            }
            .addOnFailureListener {
                onFailure()
                translator.close()
            }
    }
    fun translateWithGoogleOffline(
        text: String = state.translationInput,
        notifyOnFailure: Boolean = true
    ) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) {
            viewModel.updateTranslationOutput("", "")
            return
        }
        translateGoogleOfflineText(
            text = cleanText,
            sourceLanguageName = state.activeVocabularySourceLanguage,
            targetLanguageName = state.activeVocabularyTargetLanguage,
            onSuccess = { translated -> viewModel.updateTranslationOutput(translated, "Powered by Google Translator") },
            onFailure = {
                if (notifyOnFailure) viewModel.showMessage("Offline translation models are not ready for this pair")
            }
        )
    }

    fun downloadGoogleTranslationModels() {
        val sourceName = state.activeVocabularySourceLanguage
        val targetName = state.activeVocabularyTargetLanguage
        val sourceCode = dictionaryLanguageCode(sourceName)
        val targetCode = dictionaryLanguageCode(targetName)
        translationDownloadingLabel = "Downloading $sourceCode and $targetCode"
        downloadGoogleLanguageModel(sourceName) {
            downloadGoogleLanguageModel(targetName) {
                translationDownloadingLabel = null
                refreshDownloadedTranslationLanguages()
            }
        }
    }

    fun downloadOfflineSpeechModel(languageTag: String) {
        val language = OfflineSpeechLanguages.firstOrNull { it.tag == languageTag } ?: return
        if (state.offlineSpeechDownloadingTag != null) {
            viewModel.showMessage("Another offline speech model is downloading.")
            return
        }
        if (!SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            viewModel.setOfflineSpeechStatus(
                languageTag,
                CardRepository.OFFLINE_SPEECH_STATUS_NOT_SUPPORTED,
                "On-device speech recognition is not available on this phone."
            )
            return
        }
        viewModel.setOfflineSpeechLanguage(languageTag)
        viewModel.setOfflineSpeechStatus(
            languageTag,
            CardRepository.OFFLINE_SPEECH_STATUS_DOWNLOADING,
            "Downloading ${language.name} offline speech model"
        )
        val downloadRecognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        try {
            downloadRecognizer.triggerModelDownload(
                offlineSpeechIntent(languageTag),
                ContextCompat.getMainExecutor(context),
                object : ModelDownloadListener {
                    override fun onProgress(completedPercent: Int) {
                        viewModel.setOfflineSpeechStatus(
                            languageTag,
                            CardRepository.OFFLINE_SPEECH_STATUS_DOWNLOADING,
                            "Downloading ${language.name}: $completedPercent%"
                        )
                    }

                    override fun onSuccess() {
                        downloadRecognizer.destroy()
                        viewModel.setOfflineSpeechStatus(
                            languageTag,
                            CardRepository.OFFLINE_SPEECH_STATUS_READY,
                            "${language.name} offline speech is ready"
                        )
                    }

                    override fun onScheduled() {
                        downloadRecognizer.destroy()
                        viewModel.setOfflineSpeechStatus(
                            languageTag,
                            CardRepository.OFFLINE_SPEECH_STATUS_DOWNLOADING,
                            "Download scheduled by system"
                        )
                    }

                    override fun onError(error: Int) {
                        downloadRecognizer.destroy()
                        viewModel.setOfflineSpeechStatus(
                            languageTag,
                            CardRepository.OFFLINE_SPEECH_STATUS_ERROR,
                            "Offline speech download failed: error $error"
                        )
                    }
                }
            )
        } catch (_: Exception) {
            downloadRecognizer.destroy()
            viewModel.setOfflineSpeechStatus(
                languageTag,
                CardRepository.OFFLINE_SPEECH_STATUS_ERROR,
                "Offline speech download failed"
            )
        }
    }

    fun downloadLocalLanguage(languageName: String) {
        downloadGoogleLanguageModel(languageName)
        speechTagForDictionaryLanguage(languageName)?.let { tag ->
            if (state.offlineSpeechStatuses[tag] != CardRepository.OFFLINE_SPEECH_STATUS_READY) {
                downloadOfflineSpeechModel(tag)
            }
        }
    }

    fun downloadDefaultLocalLanguages() {
        listOf(state.quickVocabularySourceLanguage, state.quickVocabularyTargetLanguage)
            .distinctBy { dictionaryLanguageCode(it) }
            .forEach(::downloadLocalLanguage)
    }

    LaunchedEffect("downloadedTranslationLanguages") {
        refreshDownloadedTranslationLanguages()
    }

    LaunchedEffect(
        state.screen,
        state.translationInput,
        state.activeVocabularySourceLanguage,
        state.activeVocabularyTargetLanguage,
        state.useLocalTranslation,
        openAiTranslationPreferred,
        isDeviceOnline
    ) {
        if (state.screen != AppScreen.TRANSLATE) return@LaunchedEffect
        if (state.translationInput.isBlank()) {
            viewModel.updateTranslationOutput("", "")
            return@LaunchedEffect
        }
        delay(650)
        val useOfflineTranslation = (state.useLocalTranslation || !isDeviceOnline) && !openAiTranslationPreferred
        if (useOfflineTranslation) {
            translateWithGoogleOffline(state.translationInput, notifyOnFailure = false)
        } else {
            viewModel.translateOnlineText(
                text = state.translationInput,
                sourceLanguage = state.activeVocabularySourceLanguage,
                targetLanguage = state.activeVocabularyTargetLanguage
            )
        }
    }

    LaunchedEffect("translateAutoSpeak", state.screen, state.translationOutput, translateAutoSpeakEnabled) {
        if (state.screen == AppScreen.TRANSLATE && translateAutoSpeakEnabled && state.translationOutput.isNotBlank()) {
            if (suppressTranslateAutoSpeak) {
                suppressTranslateAutoSpeak = false
                return@LaunchedEffect
            }
            speakText(
                state.translationOutput,
                languageForVoice(state.activeVocabularyTargetLanguage, state.translationOutput, state.interfaceLanguage).first
            )
        }
    }
    LaunchedEffect(pendingVoiceStart) {
        if (!pendingVoiceStart) return@LaunchedEffect
        pendingVoiceStart = false
        if (!voiceUseOpenAiRecognition && voiceUseOfflineRecognition && !SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            viewModel.showMessage("Offline speech recognition is not available on this phone.")
            return@LaunchedEffect
        }
        if (!voiceUseOpenAiRecognition && !voiceUseOfflineRecognition && !SpeechRecognizer.isRecognitionAvailable(context)) {
            viewModel.showMessage("Speech recognition is not available on this phone.")
            return@LaunchedEffect
        }
        val selectedSpeechStatus = state.offlineSpeechStatuses[voiceLanguageTag]
            ?: CardRepository.OFFLINE_SPEECH_STATUS_NOT_DOWNLOADED
        if (!voiceUseOpenAiRecognition && voiceUseOfflineRecognition && selectedSpeechStatus != CardRepository.OFFLINE_SPEECH_STATUS_READY) {
            viewModel.showMessage("Download this language first for offline recognition.")
            return@LaunchedEffect
        }
        voiceElapsedMs = 0L
        voiceStatus = "Listening..."
        voiceDialogVisible = true
        voiceRecording = true
        voiceHoldActive = false
        voiceHoldReleasedAt = 0L
        lastVoiceActivityAt = System.currentTimeMillis()
        voiceSignalLevel = 0f
        voiceLastSignalAt = lastVoiceActivityAt
        try {
            if (voiceUseOpenAiRecognition) {
                releaseSpeechRecognizer()
                val audioFile = File(context.cacheDir, "openai_stt_${System.currentTimeMillis()}.m4a")
                openAiRecordingFile = audioFile
                openAiRecorder = MediaRecorder(context).apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioSamplingRate(44_100)
                    setAudioEncodingBitRate(128_000)
                    setOutputFile(audioFile.absolutePath)
                    prepare()
                    start()
                }
                voiceStatus = "Recording for OpenAI..."
                voiceLastSignalAt = System.currentTimeMillis()
                return@LaunchedEffect
            }
            releaseSpeechRecognizer()
            activeSpeechRecognizer = (
                if (voiceUseOfflineRecognition) {
                    SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                } else {
                    SpeechRecognizer.createSpeechRecognizer(context)
                }
            ).also { recognizer ->
                recognizer.setRecognitionListener(buildRecognitionListener())
                recognizer.startListening(
                    if (voiceUseOfflineRecognition) {
                        offlineSpeechIntent(voiceLanguageTag)
                    } else {
                        onlineSpeechIntent(voiceLanguageTag)
                    }
                )
            }
        } catch (_: Exception) {
            voiceRecording = false
            voiceSignalLevel = 0f
            voiceDialogVisible = false
            releaseSpeechRecognizer()
            viewModel.showMessage("Voice input failed")
        }
    }

    LaunchedEffect(voiceRecording) {
        while (voiceRecording) {
            delay(100)
            voiceElapsedMs += 100
            val now = System.currentTimeMillis()
            if (voiceUseOpenAiRecognition) {
                val amplitude = runCatching { openAiRecorder?.maxAmplitude ?: 0 }.getOrDefault(0)
                val normalizedSignal = (amplitude / 32767f).coerceIn(0f, 1f)
                if (normalizedSignal > 0.035f) {
                    val boosted = (normalizedSignal * 2.4f).coerceIn(0.12f, 1f)
                    voiceSignalLevel = boosted
                    voiceLastSignalAt = now
                    lastVoiceActivityAt = now
                } else {
                    voiceSignalLevel = (voiceSignalLevel * 0.72f).takeIf { it >= 0.03f } ?: 0f
                }
                when {
                    voiceHoldActive -> Unit
                    voiceHoldReleasedAt > 0L && now - voiceHoldReleasedAt >= 2000L -> stopVoiceInput()
                    now - voiceLastSignalAt >= state.openAiVoiceSilenceTimeoutMs -> stopVoiceInput()
                }
            } else {
                when {
                    voiceHoldActive -> Unit
                    voiceHoldReleasedAt > 0L && now - voiceHoldReleasedAt >= 2000L -> stopVoiceInput()
                    voiceHoldReleasedAt == 0L && now - lastVoiceActivityAt >= 10000L -> stopVoiceInput()
                }
            }
        }
    }

    LaunchedEffect(voiceDialogVisible, voiceRecording, voiceStatus) {
        while (voiceDialogVisible && !voiceRecording &&
            (voiceStatus.contains("Recognizing", ignoreCase = true) || voiceStatus.contains("Waiting", ignoreCase = true))
        ) {
            delay(140)
            voiceElapsedMs += 140
        }
    }

    if (showSplash) {
        if (splashReady) {
            SplashScreen(soundEffectsEnabled = state.soundEffectsEnabled, vibrationEnabled = state.vibrationEnabled)
        } else {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer))
        }
        return
    }

    LaunchedEffect(showSplash) {
        if (!showSplash) {
            titleActivated = true
        }
    }

    LaunchedEffect(state.closeAfterNotificationAnswer) {
        if (state.closeAfterNotificationAnswer) {
            (context as? MainActivity)?.finish()
        }
    }

    LaunchedEffect(state.screen) {
        if (state.screen != AppScreen.STUDY) {
            showStudyMap = false
            returnToStudyMapOnBack = false
            AppAudioPlayer.stop()
            textToSpeech.stop()
        }
        if (state.screen == AppScreen.CATALOG) {
            showCatalogMap = restoreCatalogMapOnReturn
        } else {
            titleActivated = true
        }
    }

    LaunchedEffect(state.message) {
        val message = state.message
        if (message != null) {
            var completed = false
            try {
                if (message == "CorrectStar") {
                    successStarVisible = true
                    delay(900)
                    completed = true
                    viewModel.consumeMessage()
                } else {
                    messageBubbleText = message
                    messageBubblePositive = false
                    messageBubbleVisible = true
                    delay(if (message.startsWith("Added:")) 2600 else 1000)
                    completed = true
                    viewModel.consumeMessage()
                }
            } finally {
                successStarVisible = false
                messageBubbleVisible = false
                if (!completed && state.message == message) {
                    viewModel.consumeMessage()
                }
            }
        } else {
            messageBubbleVisible = false
            successStarVisible = false
        }
    }

    CompositionLocalProvider(LocalUiText provides ui) {
    if (!state.onboardingCompleted) {
        OnboardingDialog(
            interfaceLanguage = state.interfaceLanguage,
            defaultKnownLanguage = state.quickVocabularySourceLanguage,
            defaultLearningLanguage = state.quickVocabularyTargetLanguage,
            onComplete = { interfaceLanguage, knownLanguage, learningLanguage, explanationLanguage ->
                viewModel.completeOnboarding(interfaceLanguage, knownLanguage, learningLanguage, explanationLanguage)
                showOnboardingLocalLanguageOffer = true
            }
        )
    }
    if (showResetProgressConfirm) {
        AlertDialog(
            onDismissRequest = { showResetProgressConfirm = false },
            title = { Text("Reset progress?") },
            text = { Text("Are you sure you want to reset progress for this lesson?") },
            confirmButton = {
                TextButton(onClick = {
                    showResetProgressConfirm = false
                    viewModel.resetLessonProgress()
                }) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showResetProgressConfirm = false }) { Text("Cancel") }
            }
        )
    }
    if (voiceDialogVisible) {
        VoiceInputDialog(
            status = voiceStatus,
            languageLabel = "${if (voiceUseOfflineRecognition) "Offline" else "Online"} - $voiceExpectedLanguage",
            elapsedMs = voiceElapsedMs,
            isRecording = voiceRecording,
            signalLevel = voiceSignalLevel,
            onHoldStart = {
                voiceHoldActive = true
                voiceHoldReleasedAt = 0L
            },
            onHoldEnd = {
                voiceHoldActive = false
                voiceHoldReleasedAt = System.currentTimeMillis()
            },
            onTapStop = { stopVoiceInput() }
        )
    }
    pendingSharedPostText?.let { sharedText ->
        SharedPostImportDialog(
            sharedText = sharedText,
            sourceLanguage = state.activeVocabularySourceLanguage,
            targetLanguage = state.activeVocabularyTargetLanguage,
            onDismiss = { pendingSharedPostText = null },
            onCreate = {
                pendingSharedPostText = null
                viewModel.importSharedPostCards(sharedText)
            }
        )
    }
    if (showUrlImportDialog) {
        UrlImportDialog(
            sourceLanguage = state.activeVocabularySourceLanguage,
            targetLanguage = state.activeVocabularyTargetLanguage,
            onDismiss = { showUrlImportDialog = false },
            onCreate = { url ->
                showUrlImportDialog = false
                viewModel.importSharedPostUrl(url)
            }
        )
    }
    trainSourceCard?.let { sourceCard ->
        TrainCardOptionsDialog(
            sourceCard = sourceCard,
            onDismiss = { trainSourceCard = null },
            onGenerate = { options ->
                trainSourceCard = null
                viewModel.generateTrainCardsFromMistake(sourceCard.id, options)
            }
        )
    }
    if (moveStudyCardDialogVisible) {
        MoveStudyCardDialog(
            currentLesson = state.selectedLesson,
            currentCard = state.currentCard,
            lessons = state.lessons,
            onDismiss = { moveStudyCardDialogVisible = false },
            onMove = { destinationLessonId ->
                moveStudyCardDialogVisible = false
                viewModel.moveCurrentStudyCardToLesson(destinationLessonId)
            }
        )
    }
    val headerLessonInfo = remember(
        state.selectedLesson?.id,
        state.selectedLesson?.title,
        state.selectedLesson?.lessonInfo
    ) {
        state.selectedLesson?.let { lesson ->
        buildString {
            append("Lesson title:\n")
            append(lesson.title.ifBlank { "Untitled lesson" })
            if (lesson.lessonInfo.isNotBlank()) {
                append("\n\nInfo:\n")
                append(lesson.lessonInfo)
            }
        }
        }.orEmpty()
    }
    val downloadedTranslationLabel = remember(downloadedTranslationLanguages) {
        downloadedTranslationLanguages.sorted().joinToString().ifBlank { "none" }
    }
    if (showHeaderLessonInfo && headerLessonInfo.isNotBlank()) {
        CardTextDialog(
            title = "Lesson info",
            text = headerLessonInfo,
            onDismiss = { showHeaderLessonInfo = false },
            onResetProgress = {
                showHeaderLessonInfo = false
                showResetProgressConfirm = true
            }
        )
    }
    if (showTranslationDownloadDialog) {
        AlertDialog(
            onDismissRequest = { showTranslationDownloadDialog = false },
            title = { Text("Download Google Translate languages") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Download offline translation models for ${nativeLanguageLabel(state.activeVocabularySourceLanguage)} -> ${nativeLanguageLabel(state.activeVocabularyTargetLanguage)}.")
                    Text("Downloaded: $downloadedTranslationLabel")
                    translationDownloadingLabel?.let { Text(it, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) }
                    Text(
                        text = "Translations are powered by Google Translate. Google disclaims warranties related to translation accuracy and reliability.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showTranslationDownloadDialog = false
                    downloadGoogleTranslationModels()
                }) { Text("Download") }
            },
            dismissButton = {
                TextButton(onClick = { showTranslationDownloadDialog = false }) { Text("Cancel") }
            }
        )
    }
    if (showOnboardingLocalLanguageOffer) {
        AlertDialog(
            onDismissRequest = { showOnboardingLocalLanguageOffer = false },
            title = { Text("Download local languages?") },
            text = {
                Text(
                    "Download local translation and speech recognition libraries for ${nativeLanguageLabel(state.quickVocabularySourceLanguage)} and ${nativeLanguageLabel(state.quickVocabularyTargetLanguage)} now, or do it later and keep working online."
                )
            },
            confirmButton = {
                Button(onClick = {
                    showOnboardingLocalLanguageOffer = false
                    downloadDefaultLocalLanguages()
                }) { Text("Download locally") }
            },
            dismissButton = {
                TextButton(onClick = { showOnboardingLocalLanguageOffer = false }) { Text("Later") }
            }
        )
    }
    if (showLocalLanguageDialog) {
        LocalLanguagesDialog(
            downloadedTranslationLanguages = downloadedTranslationLanguages,
            offlineSpeechStatuses = state.offlineSpeechStatuses,
            offlineSpeechDownloadingTag = state.offlineSpeechDownloadingTag,
            translationDownloadingLabel = translationDownloadingLabel,
            onDownloadLanguage = ::downloadLocalLanguage,
            onDeleteTranslationLanguage = ::deleteGoogleLanguageModel,
            onDismiss = { showLocalLanguageDialog = false }
        )
    }
    if (showCatChat) {
        CatChatDialog(
            messages = catChatMessages,
            input = catChatInput,
            basicLanguage = catChatBasicLanguage,
            targetLanguage = catChatTargetLanguage,
            busy = catChatBusy,
            autoSpeak = catChatAutoSpeak,
            replySpeed = state.catReplySpeechRate,
            onInputChange = { catChatInput = it },
            onBasicLanguageChange = { language ->
                catChatBasicLanguage = language
                speechTagForDictionaryLanguage(language)?.let(viewModel::setOfflineSpeechLanguage)
            },
            onTargetLanguageChange = { language -> catChatTargetLanguage = language },
            onAutoSpeakChange = { catChatAutoSpeak = it },
            onReplySpeedChange = viewModel::setCatReplySpeechRate,
            onVoiceInput = { startVoiceInput(VoiceInputTarget.CAT_CHAT) },
            onSpeakMessage = { text ->
                speakText(
                    text,
                    languageForVoice(catChatTargetLanguage, text, state.interfaceLanguage).first
                )
            },
            onDismiss = { showCatChat = false },
            onCreateCard = { phrase, fromCat ->
                titleActivated = true
                viewModel.addCatChatCard(
                    phrase = phrase,
                    basicLanguage = catChatBasicLanguage,
                    answerLanguage = catChatTargetLanguage,
                    phraseIsAnswerLanguage = fromCat
                )
            },
            onCreateMistakeCard = { phrase, analysis ->
                titleActivated = true
                viewModel.addCatChatCard(
                    phrase = phrase,
                    basicLanguage = catChatBasicLanguage,
                    answerLanguage = catChatTargetLanguage,
                    phraseIsAnswerLanguage = false,
                    mistakeAnalysis = analysis
                )
            },
            onCreateFeaturedCard = { phrase, fromCat ->
                titleActivated = true
                viewModel.addCatChatCard(
                    phrase = phrase,
                    basicLanguage = catChatBasicLanguage,
                    answerLanguage = catChatTargetLanguage,
                    phraseIsAnswerLanguage = fromCat,
                    featured = true
                )
            },
            onFeatureSelection = { messageId, selection ->
                catChatMessages = catChatMessages.map { message ->
                    if (message.id == messageId) message.copy(featuredSelection = selection) else message
                }
                viewModel.setCatDialogMessageFeaturedSelection(catDialogId, messageId, selection)
            },
            onSend = { sendCatChatMessage() }
        )
    }
    activeLanguageGame?.takeUnless {
        it in setOf(
            LanguageGameMode.PSEUDO_3D_WALK,
            LanguageGameMode.EIGHT_BIT_PLATFORMER,
            LanguageGameMode.SIXTEEN_BIT_SPIN_RUNNER,
            LanguageGameMode.GODOT_RUNNER_LAB,
            LanguageGameMode.LOTTIE_RUNNER_LAB,
            LanguageGameMode.FLUTTER_RUNNER_LAB,
            LanguageGameMode.RIVE_RUNNER_LAB,
            LanguageGameMode.RIVE_LETTER_BLOCKS
        )
    }?.let { gameMode ->
        LanguageGameDialog(
            mode = gameMode,
            cards = languageGameCards(state),
            onDismiss = { activeLanguageGame = null }
        )
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    if (animationScreenOpen || fullScreenGameOpen || state.screen != AppScreen.CATALOG) {
                        IconButton(onClick = {
                            titleActivated = true
                            if (fullScreenGameOpen) {
                                activeLanguageGame = null
                            } else if (showOriginalAnimation) {
                                showOriginalAnimation = false
                            } else if (showAnimationCatalog) {
                                showAnimationCatalog = false
                            } else if (state.screen == AppScreen.STUDY && returnToStudyMapOnBack && !showStudyMap) {
                                showStudyMap = true
                            } else {
                                returnToStudyMapOnBack = false
                                showStudyMap = false
                                viewModel.navigateBack()
                            }
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = ui.back)
                        }
                    }
                },
                title = {
                    if (showOriginalAnimation) {
                        Text("Анимация (оригинал)", fontWeight = FontWeight.SemiBold, color = appTitleColor)
                    } else if (showAnimationCatalog) {
                        Text("Animations", fontWeight = FontWeight.SemiBold, color = appTitleColor)
                    } else if (fullScreenGameOpen) {
                        Text(
                            when (activeLanguageGame) {
                                LanguageGameMode.EIGHT_BIT_PLATFORMER -> "8-bit Ginger Platformer"
                                LanguageGameMode.SIXTEEN_BIT_SPIN_RUNNER -> "16-bit Ginger Spin Runner"
                                LanguageGameMode.GODOT_RUNNER_LAB -> "Godot Runner Lab"
                                LanguageGameMode.LOTTIE_RUNNER_LAB -> "Lottie Runner Lab"
                                LanguageGameMode.FLUTTER_RUNNER_LAB -> "Flutter Runner Lab"
                                LanguageGameMode.RIVE_RUNNER_LAB -> "Rive Runner Lab"
                                LanguageGameMode.RIVE_LETTER_BLOCKS -> "Rive Letter Blocks"
                                else -> "Pseudo 3D Cat Walk"
                            },
                            fontWeight = FontWeight.SemiBold,
                            color = appTitleColor
                        )
                    } else {
                        MurrLexTitleRow(
                        isStudy = state.screen == AppScreen.STUDY,
                        isCatalog = state.screen == AppScreen.CATALOG,
                        isDeviceOnline = isDeviceOnline,
                        titleColor = appTitleColor,
                        catMenuExpanded = showCatMenu,
                        onCatChat = {
                            titleActivated = true
                            showCatMenu = false
                            catChatBasicLanguage = state.activeVocabularySourceLanguage
                            catChatTargetLanguage = state.activeVocabularyTargetLanguage
                            catDialogId = null
                            catChatMessages = emptyList()
                            catChatInput = ""
                            showCatChat = true
                        },
                        onCatMenuToggle = {
                            titleActivated = true
                            showCatMenu = !showCatMenu
                        },
                        onCatMenuDismiss = { showCatMenu = false },
                        onOpenAnimations = {
                            titleActivated = true
                            showCatMenu = false
                            showAnimationCatalog = true
                        },
                        onOpenOriginalAnimation = {
                            titleActivated = true
                            showCatMenu = false
                            showOriginalAnimation = true
                        },
                        onStartGame = { mode ->
                            titleActivated = true
                            showCatMenu = false
                            if (mode == LanguageGameMode.LIBGDX_SPIN_RUNNER) {
                                context.startActivity(
                                    LibGdxRunnerActivity.createIntent(
                                        context = context,
                                        cardsJson = languageGameCards(state).toLibGdxCardsJson()
                                    )
                                )
                            } else {
                                activeLanguageGame = mode
                            }
                        },
                        onImageImport = {
                            titleActivated = true
                            imageImportIntoCurrentLesson = false
                            imageTextLauncher.launch("image/*")
                        },
                        onTitleClick = {
                            titleActivated = true
                            when (state.screen) {
                                AppScreen.CATALOG -> {
                                    restoreCatalogMapOnReturn = true
                                    showCatalogMap = true
                                }
                                AppScreen.STUDY -> showStudyMap = true
                                else -> Unit
                            }
                        },
                        onStatusClick = {
                            titleActivated = true
                            forceRefreshOnlineState()
                            if (state.screen == AppScreen.TRANSLATE) {
                                showTranslationDownloadDialog = true
                            }
                        }
                    )
                    }
                },
                actions = {
                    if (!animationScreenOpen && !fullScreenGameOpen && state.screen == AppScreen.CATALOG) {
                        CompactTopIconButton(
                            icon = Icons.Default.Link,
                            contentDescription = "Import URL",
                            onClick = {
                            titleActivated = true
                            showUrlImportDialog = true
                        })
                        CompactTopIconButton(
                            icon = Icons.Default.Image,
                            contentDescription = "Import image text",
                            onClick = {
                            titleActivated = true
                            imageImportIntoCurrentLesson = false
                            imageTextLauncher.launch("image/*")
                        })
                        CompactTopIconButton(
                            icon = Icons.Default.PhotoCamera,
                            contentDescription = "Take photo for image text",
                            onClick = {
                            titleActivated = true
                            val imageUri = createCameraImageUri(context)
                            pendingCameraImageUri = imageUri
                            pendingCameraIntoCurrentLesson = false
                            cameraImageLauncher.launch(imageUri)
                        })
                        CompactTopIconButton(
                            icon = Icons.Default.Visibility,
                            contentDescription = if (state.showHiddenLessons) ui.hideHiddenLessons else ui.showHiddenLessons,
                            tint = if (state.showHiddenLessons) BrandSaladColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = {
                            titleActivated = true
                            viewModel.toggleShowHiddenLessons()
                        })
                    }
                    if (!animationScreenOpen && !fullScreenGameOpen && state.screen == AppScreen.STUDY) {
                        Box {
                            CompactTopIconButton(
                                icon = Icons.Default.Style,
                                contentDescription = "Display settings",
                                onClick = {
                                titleActivated = true
                                showStudyDisplayMenu = !showStudyDisplayMenu
                            })
                            DropdownMenu(
                                expanded = showStudyDisplayMenu,
                                onDismissRequest = { showStudyDisplayMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Sort: ${state.mode.displayLabel()}") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = state.mode.displayIcon(),
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        val nextMode = state.mode.nextMode()
                                        viewModel.setMode(nextMode)
                                        viewModel.showMessage(nextMode.displayLabel())
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sort direction: Ascending") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (!state.studySortDescending) Icons.Default.DoneAll else Icons.Default.KeyboardArrowUp,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        viewModel.setStudySortDescending(false)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sort direction: Descending") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (state.studySortDescending) Icons.Default.DoneAll else Icons.Default.KeyboardArrowDown,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        viewModel.setStudySortDescending(true)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (state.hideCompletedCards) "Show done" else "Hide done") },
                                    leadingIcon = { StudyHideDoneIcon(active = !state.hideCompletedCards) },
                                    onClick = {
                                        viewModel.setHideCompletedCards(!state.hideCompletedCards)
                                        viewModel.showMessage(if (!state.hideCompletedCards) "Hide done" else "Show done")
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (state.excludeMasteredCards) "Show three-star cards" else "Hide three-star cards") },
                                    leadingIcon = { StudyHideStarIcon(active = !state.excludeMasteredCards) },
                                    onClick = {
                                        viewModel.setShowAllCards(state.excludeMasteredCards)
                                    }
                                )
                            }
                        }
                        CompactTopIconButton(
                            icon = Icons.Default.Link,
                            contentDescription = "Import URL",
                            onClick = {
                            titleActivated = true
                            showUrlImportDialog = true
                        })
                        CompactTopIconButton(
                            icon = Icons.Default.Image,
                            contentDescription = "Choose image for current lesson",
                            onClick = {
                            titleActivated = true
                            imageImportIntoCurrentLesson = true
                            imageTextLauncher.launch("image/*")
                        })
                        CompactTopIconButton(
                            icon = Icons.Default.PhotoCamera,
                            contentDescription = "Take photo for current lesson",
                            onClick = {
                            titleActivated = true
                            val imageUri = createCameraImageUri(context)
                            pendingCameraImageUri = imageUri
                            pendingCameraIntoCurrentLesson = true
                            cameraImageLauncher.launch(imageUri)
                        })
                        CompactTopIconButton(
                            icon = Icons.Default.Add,
                            contentDescription = "Add empty card",
                            onClick = {
                            titleActivated = true
                            viewModel.addEmptyCardToCurrentLesson()
                        })
                    }
                    if (!animationScreenOpen && !fullScreenGameOpen && state.screen == AppScreen.STUDY && headerLessonInfo.isNotBlank()) {
                        CompactTopIconButton(
                            icon = Icons.Default.Info,
                            contentDescription = "Lesson info",
                            onClick = {
                            titleActivated = true
                            showHeaderLessonInfo = true
                        })
                    }
                    if (!animationScreenOpen && !fullScreenGameOpen) {
                        CompactTopIconButton(
                            icon = Icons.Default.Settings,
                            contentDescription = ui.settings,
                            onClick = {
                            titleActivated = true
                            viewModel.openSettings()
                        })
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (!animationScreenOpen && !showStudyMap && state.screen == AppScreen.STUDY && (state.workMode == WorkMode.CARDS || quickEditActive) && !state.isPortionFinished && state.currentCard != null) {
                AnswerBar(
                    answer = state.answer,
                    answerLabel = state.currentCard
                        ?.answerInputLabel(state.isBackVisible, state.selectedLesson)
                        .orEmpty()
                        .ifBlank { ui.makeItRight },
                    currentCard = state.currentCard,
                    isBackVisible = state.isBackVisible,
                    showOriginalText = state.currentCard.shouldShowOriginalText(
                        isBackVisible = state.isBackVisible,
                        translatedOriginalCardIds = state.translatedOriginalCardIds
                    ),
                    isCurrentCardDone = state.currentCard?.id in state.completedCardIds,
                    answerFeedbackVisible = state.answerFeedbackVisible,
                    isVoiceRecording = voiceRecording,
                    quickEditMode = quickEditActive,
                    translateTargetCode = state.currentCard
                        ?.answerTargetLanguage(state.isBackVisible, state.selectedLesson)
                        .toCardLanguageCode(),
                    onAnswerChange = viewModel::updateAnswer,
                    onCheck = viewModel::previewAnswer,
                    onOk = {
                        if (state.answer.isBlank()) {
                            viewModel.toggleCard()
                            return@AnswerBar
                        }
                        val typedCorrect = state.answer.isNotBlank() &&
                            state.currentCard?.let { card ->
                           normalizeAnswerText(state.answer) == normalizeAnswerText(card.expectedAnswerText(state.isBackVisible))
                            } == true
                        if (typedCorrect) {
                            performFeedback(context, state.soundEffectsEnabled, state.vibrationEnabled, FeedbackCue.SUCCESS)
                        }
                        viewModel.acceptCurrentCard()
                    },
                                        onSaveEmptySide = {
                        val cardBeforeSave = state.currentCard
                        val enteredText = state.answer.trim()
                        val editedSideShowsOriginal = cardBeforeSave.shouldShowOriginalText(
                            isBackVisible = state.isBackVisible,
                            translatedOriginalCardIds = state.translatedOriginalCardIds
                        )
                        val editedSideOldText = cardBeforeSave
                            ?.displayedCardText(state.isBackVisible, editedSideShowsOriginal)
                            .orEmpty()
                        val editedSideLanguageTag = cardBeforeSave
                            ?.speechLanguageTagForSide(
                                state.isBackVisible,
                                state.interfaceLanguage,
                                state.selectedLesson,
                                showOriginalText = editedSideShowsOriginal
                            )
                            .orEmpty()
                        if (cardBeforeSave != null && editedSideLanguageTag.isNotBlank()) {
                            val clearedFiles = clearCachedCardSideAudioFiles(context, editedSideOldText, editedSideLanguageTag, state) +
                                clearCachedCardSideAudioFiles(context, enteredText, editedSideLanguageTag, state)
                            if (clearedFiles > 0) {
                                viewModel.appendCurrentCardLog("manual edit cleared cached audio for visible side")
                            }
                        }
                        val fillBackSide = cardBeforeSave?.correctText()?.isEmptyPlaceholder() == true
                        viewModel.saveVisibleSideFromAnswer(state.isBackVisible)
                        quickEditCardId = null
                        if (!quickEditActive && (state.useLocalTranslation || !isDeviceOnline) && !openAiTranslationPreferred && cardBeforeSave != null && enteredText.isNotBlank()) {
                            val sourceLanguage = if (fillBackSide) cardBeforeSave.sourceLanguage.ifBlank { state.selectedLesson?.sourceLanguage.orEmpty() } else cardBeforeSave.targetLanguage.ifBlank { state.selectedLesson?.targetLanguage.orEmpty() }
                            val targetLanguage = if (fillBackSide) cardBeforeSave.targetLanguage.ifBlank { state.selectedLesson?.targetLanguage.orEmpty() } else cardBeforeSave.sourceLanguage.ifBlank { state.selectedLesson?.sourceLanguage.orEmpty() }
                            translateGoogleOfflineText(
                                text = enteredText,
                                sourceLanguageName = sourceLanguage,
                                targetLanguageName = targetLanguage,
                                onSuccess = { translated ->
                                    viewModel.applyTranslationToCard(
                                        cardId = cardBeforeSave.id,
                                        translated = translated,
                                        targetBackSide = fillBackSide
                                    )
                                },
                                onFailure = {}
                            )
                        } else if (!quickEditActive && cardBeforeSave != null && enteredText.isNotBlank()) {
                            val sourceLanguage = if (fillBackSide) cardBeforeSave.sourceLanguage.ifBlank { state.selectedLesson?.sourceLanguage.orEmpty() } else cardBeforeSave.targetLanguage.ifBlank { state.selectedLesson?.targetLanguage.orEmpty() }
                            val targetLanguage = if (fillBackSide) cardBeforeSave.targetLanguage.ifBlank { state.selectedLesson?.targetLanguage.orEmpty() } else cardBeforeSave.sourceLanguage.ifBlank { state.selectedLesson?.sourceLanguage.orEmpty() }
                            viewModel.translateAndApplyCardText(
                                cardId = cardBeforeSave.id,
                                text = enteredText,
                                sourceLanguage = sourceLanguage,
                                targetLanguage = targetLanguage,
                                targetBackSide = fillBackSide
                            )
                        }
                    },
                    onTranslateEmptySide = {
                        val card = state.currentCard ?: return@AnswerBar
                        val showOriginalText = card.shouldShowOriginalText(
                            isBackVisible = state.isBackVisible,
                            translatedOriginalCardIds = state.translatedOriginalCardIds
                        )
                        val sourceText = card.displayedCardText(state.isBackVisible, showOriginalText)
                        val sourceLanguage = card.visibleCardSideLanguage(
                            isBackVisible = state.isBackVisible,
                            lesson = state.selectedLesson,
                            showOriginalText = showOriginalText
                        )
                        val targetLanguage = card.answerTargetLanguage(
                            isBackVisible = state.isBackVisible,
                            lesson = state.selectedLesson
                        )
                        if ((state.useLocalTranslation || !isDeviceOnline) && !openAiTranslationPreferred) {
                            translateGoogleOfflineText(
                                text = sourceText,
                                sourceLanguageName = sourceLanguage,
                                targetLanguageName = targetLanguage,
                                onSuccess = { translated ->
                                    viewModel.updateAnswer(translated)
                                    viewModel.showMessage("Powered by Google Translator")
                                },
                                onFailure = { viewModel.showMessage("Offline translation models are not ready for this pair") }
                            )
                        } else {
                            viewModel.translateCardTextIntoAnswer(
                                text = sourceText,
                                sourceLanguage = sourceLanguage,
                                targetLanguage = targetLanguage
                            )
                        }
                    },
                    onClearAnswer = {
                        viewModel.updateAnswer("")
                        quickEditCardId = null
                    },
                    onCopy = { text ->
                        viewModel.updateAnswer(text)
                        copyToClipboard(context, text)
                        viewModel.showMessage(ui.copiedToInput)
                    },
                    onVoiceToggle = { startVoiceInput(VoiceInputTarget.ANSWER) },
                    onDismissAnswerFeedback = viewModel::dismissAnswerFeedback,
                    controlSize = state.controlSize,
                    onSpeak = {
                        val card = state.currentCard
                        if (card != null) {
                            val inputText = state.answer.trim()
                            val showOriginalText = card.shouldShowOriginalText(
                                isBackVisible = state.isBackVisible,
                                translatedOriginalCardIds = state.translatedOriginalCardIds
                            )
                            val textToRead = inputText.ifBlank { card.displayedCardText(state.isBackVisible, showOriginalText) }
                            val languageTag = if (inputText.isNotBlank()) {
                                inputText.speechLanguageTagFromText() ?: card.speechLanguageTag(state.interfaceLanguage, state.selectedLesson)
                            } else {
                                card.speechLanguageTagForSide(
                                    state.isBackVisible,
                                    state.interfaceLanguage,
                                    state.selectedLesson,
                                    showOriginalText = showOriginalText
                                )
                            }
                            performFeedback(context, state.soundEffectsEnabled, state.vibrationEnabled, FeedbackCue.TAP)
                            speakText(textToRead, languageTag)
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.changes.any { it.changedToDownIgnoreConsumed() }) {
                                val stoppedAppAudio = AppAudioPlayer.stop()
                                val stoppedTts = runCatching { textToSpeech.isSpeaking }.getOrDefault(false)
                                textToSpeech.stop()
                                if (stoppedAppAudio || stoppedTts) {
                                    suppressAudioStartUntilMs = System.currentTimeMillis() + 350L
                                }
                            }
                        }
                    }
                }
                .padding(padding)
        ) {
            val visibleCatalogLessons = state.lessons.catalogFilteredAndSorted(lessonSearchQuery, lessonSortMode, lessonSortDescending)
            if (activeLanguageGame == LanguageGameMode.SIXTEEN_BIT_SPIN_RUNNER) {
                SixteenBitGingerSpinRunnerScreen(
                    cards = languageGameCards(state),
                    onDismiss = { activeLanguageGame = null }
                )
            } else if (activeLanguageGame == LanguageGameMode.GODOT_RUNNER_LAB) {
                GodotRunnerLabScreen(
                    cards = languageGameCards(state),
                    onDismiss = { activeLanguageGame = null }
                )
            } else if (activeLanguageGame == LanguageGameMode.LOTTIE_RUNNER_LAB) {
                LottieRunnerLabScreen(
                    cards = languageGameCards(state),
                    onDismiss = { activeLanguageGame = null }
                )
            } else if (activeLanguageGame == LanguageGameMode.FLUTTER_RUNNER_LAB) {
                FlutterRunnerLabScreen(
                    cards = languageGameCards(state),
                    onDismiss = { activeLanguageGame = null }
                )
            } else if (activeLanguageGame == LanguageGameMode.RIVE_RUNNER_LAB) {
                val riveSpeechLanguage = state.selectedLesson?.targetLanguage.asLessonLanguage()
                    ?: state.activeVocabularyTargetLanguage
                RiveRunnerLabScreen(
                    cards = languageGameCards(state),
                    speechLanguageTag = languageForVoice(riveSpeechLanguage, "", state.interfaceLanguage).first,
                    onSpeak = { text, languageTag -> speakText(text, languageTag) },
                    onDismiss = { activeLanguageGame = null }
                )
            } else if (activeLanguageGame == LanguageGameMode.RIVE_LETTER_BLOCKS) {
                val riveSpeechLanguage = state.selectedLesson?.targetLanguage.asLessonLanguage()
                    ?: state.activeVocabularyTargetLanguage
                RiveLetterBlocksScreen(
                    cards = languageGameCards(state),
                    speechLanguageTag = languageForVoice(riveSpeechLanguage, "", state.interfaceLanguage).first,
                    onSpeak = { text, languageTag -> speakText(text, languageTag) },
                    onDismiss = { activeLanguageGame = null }
                )
            } else if (activeLanguageGame == LanguageGameMode.EIGHT_BIT_PLATFORMER) {
                EightBitGingerPlatformerScreen(
                    cards = languageGameCards(state),
                    onDismiss = { activeLanguageGame = null }
                )
            } else if (activeLanguageGame == LanguageGameMode.PSEUDO_3D_WALK) {
                Pseudo3DCatWalkScreen(
                    cards = languageGameCards(state),
                    onDismiss = { activeLanguageGame = null }
                )
            } else if (showOriginalAnimation) {
                OriginalAnimationScreen(onDismiss = { showOriginalAnimation = false })
            } else if (showAnimationCatalog) {
                AnimationCatalogScreen(
                    entries = MakeMistakeAnimationCatalog,
                    onDismiss = { showAnimationCatalog = false }
                )
            } else when (state.screen) {
                AppScreen.CATALOG -> {
                    if (showCatalogMap) {
                        CatalogIslandMapScreen(
                            lessons = visibleCatalogLessons,
                            searchQuery = lessonSearchQuery,
                            sortMode = lessonSortMode,
                            sortDescending = lessonSortDescending,
                            scrollState = catalogMapScrollState,
                            onSearchQueryChange = { lessonSearchQuery = it },
                            onSortModeChange = {
                                lessonSortMode = it
                                coroutineScope.launch { catalogMapScrollState.animateScrollTo(0) }
                            },
                            onSortDirectionChange = {
                                lessonSortDescending = it
                                coroutineScope.launch { catalogMapScrollState.animateScrollTo(0) }
                            },
                            onOpenLesson = { lesson ->
                                titleActivated = true
                                restoreCatalogMapOnReturn = true
                                viewModel.openLesson(lesson, lesson.firstCardContentMatchId(lessonSearchQuery))
                                showStudyMap = true
                            },
                            onBackToList = {
                                restoreCatalogMapOnReturn = false
                                showCatalogMap = false
                            }
                        )
                    } else {
                        LessonCatalogScreen(
                            lessons = visibleCatalogLessons,
                            selectedLessonIds = state.selectedLessonIds,
                            searchQuery = lessonSearchQuery,
                            sortMode = lessonSortMode,
                            sortDescending = lessonSortDescending,
                            listState = catalogListState,
                            onSearchQueryChange = { lessonSearchQuery = it },
                            onSortModeChange = {
                                lessonSortMode = it
                                coroutineScope.launch { catalogListState.animateScrollToItem(0) }
                            },
                            onSortDirectionChange = {
                                lessonSortDescending = it
                                coroutineScope.launch { catalogListState.animateScrollToItem(0) }
                            },
                            workMode = state.workMode,
                            onOpenLesson = { lesson ->
                                titleActivated = true
                                restoreCatalogMapOnReturn = false
                                viewModel.openLesson(lesson, lesson.firstCardContentMatchId(lessonSearchQuery))
                            },
                            onEditLesson = { titleActivated = true; viewModel.editLesson(it) },
                            onToggleLessonSelection = { titleActivated = true; viewModel.toggleLessonSelection(it) },
                            onDeleteSelectedLessons = { titleActivated = true; viewModel.deleteSelectedLessons() },
                            onDeleteLesson = { titleActivated = true; viewModel.deleteLesson(it) },
                            onClearSelection = { titleActivated = true; viewModel.clearLessonSelection() },
                            onMoveLesson = { from, to -> titleActivated = true; viewModel.moveLesson(from, to) },
                            onSaveLessonOrder = { titleActivated = true; viewModel.saveLessonOrder() },
                            onCreateLesson = { titleActivated = true; viewModel.createLesson() },
                            onCopyLesson = { titleActivated = true; viewModel.copyLesson(it) },
                            onDownloadLesson = { lesson ->
                                titleActivated = true
                                val jsonText = exportJson.encodeToString(lesson)
                                val fileName = "${lesson.title.exportFileName()}.json"
                                jsonPendingExport = fileName to jsonText
                                exportLauncher.launch(fileName)
                            },
                            onShareLesson = { lesson ->
                                titleActivated = true
                                shareJson(context, "${lesson.title.exportFileName()}.json", exportJson.encodeToString(lesson))
                            },
                            onSetLessonHidden = { lesson, hidden -> titleActivated = true; viewModel.setLessonHidden(lesson, hidden) },
                            quickVocabularySourceLanguage = state.activeVocabularySourceLanguage,
                            quickVocabularyTargetLanguage = state.activeVocabularyTargetLanguage,
                            onQuickVocabularySourceChange = { language ->
                                titleActivated = true
                                viewModel.setActiveVocabularySourceLanguage(language)
                                speechTagForDictionaryLanguage(language)?.let(viewModel::setOfflineSpeechLanguage)
                            },
                            onQuickVocabularyTargetChange = { language ->
                                titleActivated = true
                                viewModel.setActiveVocabularyTargetLanguage(language)
                            },
                            onSwapQuickVocabularyLanguages = {
                                titleActivated = true
                                val nextRecognitionLanguage = state.activeVocabularyTargetLanguage
                                viewModel.swapActiveVocabularyLanguages()
                                speechTagForDictionaryLanguage(nextRecognitionLanguage)?.let(viewModel::setOfflineSpeechLanguage)
                            },
                            onQuickVoiceInput = {
                                titleActivated = true
                                startVoiceInput(VoiceInputTarget.QUICK_VOCABULARY)
                            },
                            onWorkModeChange = { mode -> titleActivated = true; viewModel.setWorkMode(mode) },
                            onOpenCatDialogs = {
                                titleActivated = true
                                viewModel.openCatDialogs()
                            },
                            onImportLessons = {
                                titleActivated = true
                                importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                            }
                        )
                    }
                }
                AppScreen.DIALOGS -> CatDialogsScreen(
                    dialogs = state.catDialogs,
                    onOpenDialog = { dialog ->
                        titleActivated = true
                        viewModel.openCatDialog(dialog)
                        catDialogId = dialog.id
                        catChatBasicLanguage = dialog.basicLanguage
                        catChatTargetLanguage = dialog.targetLanguage
                        catChatMessages = dialog.messages.toCatChatMessages()
                        catChatInput = ""
                        showCatChat = true
                    },
                    onNewDialog = {
                        titleActivated = true
                        catDialogId = null
                        catChatBasicLanguage = state.activeVocabularySourceLanguage
                        catChatTargetLanguage = state.activeVocabularyTargetLanguage
                        catChatMessages = emptyList()
                        catChatInput = ""
                        showCatChat = true
                    },
                    onToggleFeatured = viewModel::toggleCatDialogFeatured,
                    onSetHidden = viewModel::setCatDialogHidden,
                    onDeleteDialog = viewModel::deleteCatDialog,
                    onMoveDialog = viewModel::moveCatDialog
                )
                AppScreen.TRANSLATE -> TranslateScreen(
                    state = state,
                    onSourceLanguageChange = { language ->
                        suppressTranslateAutoSpeak = true
                        viewModel.setActiveVocabularySourceLanguage(language)
                        speechTagForDictionaryLanguage(language)?.let(viewModel::setOfflineSpeechLanguage)
                    },
                    onTargetLanguageChange = { language ->
                        suppressTranslateAutoSpeak = true
                        viewModel.setActiveVocabularyTargetLanguage(language)
                    },
                    onInputChange = viewModel::updateTranslationInput,
                    onClear = viewModel::clearTranslationInput,
                    onAddCard = viewModel::addTranslationCard,
                    onSwapLanguages = {
                        suppressTranslateAutoSpeak = true
                        val nextRecognitionLanguage = state.activeVocabularyTargetLanguage
                        viewModel.swapActiveVocabularyLanguages()
                        speechTagForDictionaryLanguage(nextRecognitionLanguage)?.let(viewModel::setOfflineSpeechLanguage)
                    },
                    translateAutoSpeakEnabled = translateAutoSpeakEnabled,
                    onTranslateAutoSpeakChange = { translateAutoSpeakEnabled = it },
                    onSpeakTranslation = {
                        speakText(state.translationOutput, languageForVoice(state.activeVocabularyTargetLanguage, state.translationOutput, state.interfaceLanguage).first)
                    },
                    onSpeakInput = {
                        speakText(state.translationInput, languageForVoice(state.activeVocabularySourceLanguage, state.translationInput, state.interfaceLanguage).first)
                    },
                    onVoiceInput = { startVoiceInput(VoiceInputTarget.TRANSLATE_INPUT) },
                    onTargetVoiceInput = { startVoiceInput(VoiceInputTarget.TRANSLATE_OUTPUT) }
                )
                AppScreen.STUDY -> StudyScreen(
                    state = state,
                    isDeviceOnline = isDeviceOnline,
                    statusBlinkOn = cardStatusBlinkOn,
                    onModeChange = viewModel::setMode,
                    onShowAllCardsChange = viewModel::setShowAllCards,
                    onPreviousCard = viewModel::previousCard,
                    onNextCard = viewModel::nextCard,
                    onFirstCard = viewModel::goToFirstCard,
                    onFirstRemainingCard = viewModel::goToFirstRemainingCard,
                    onLastCompletedCard = viewModel::goToLastCompletedCard,
                    onNewPortion = viewModel::startNewPortion,
                    onNextLesson = viewModel::openNextVisibleLesson,
                    onOpenCatalog = viewModel::openCatalog,
                    showMap = showStudyMap,
                    onOpenCardFromMap = { cardId ->
                        viewModel.openCardFromMap(cardId)
                        showStudyMap = false
                        returnToStudyMapOnBack = true
                    },
                    onRefreshOnlineState = { forceRefreshOnlineState() },
                    onToggleCard = {
                        showStudyDisplayMenu = false
                        quickEditCardId = null
                        viewModel.toggleCard()
                    },
                    onToggleStar = { cardId, starIndex ->
                        performFeedback(context, state.soundEffectsEnabled, state.vibrationEnabled, FeedbackCue.TAP)
                        viewModel.toggleCardStar(cardId, starIndex)
                    },
                    onQuickEditCard = {
                        performFeedback(context, state.soundEffectsEnabled, state.vibrationEnabled, FeedbackCue.TAP)
                        if (quickEditActive) {
                            quickEditCardId = null
                            viewModel.editCurrentStudyCard()
                        } else {
                            val card = state.currentCard
                            val showOriginalText = card.shouldShowOriginalText(
                                isBackVisible = state.isBackVisible,
                                translatedOriginalCardIds = state.translatedOriginalCardIds
                            )
                            quickEditCardId = card?.id
                            viewModel.updateAnswer(card?.displayedCardText(state.isBackVisible, showOriginalText).orEmpty())
                        }
                    },
                    onEditCard = {
                        performFeedback(context, state.soundEffectsEnabled, state.vibrationEnabled, FeedbackCue.TAP)
                        viewModel.editCurrentStudyCard()
                    },
                    quickEditMode = quickEditActive,
                    showCardLog = state.showCardLog,
                    onTestAnswer = viewModel::submitTestAnswer,
                    onShareCard = { lesson, card ->
                        performFeedback(context, state.soundEffectsEnabled, state.vibrationEnabled, FeedbackCue.TAP)
                        shareSingleCard(context, exportJson, lesson, card)
                    },
                    onShareCardSideAudio = { card, sideIsBack ->
                        shareCachedCardSideAudio(card, sideIsBack)
                    },
                    onPlayOriginalVoice = { card -> playOriginalVoiceAudio(card) },
                    onDeleteCard = viewModel::deleteCurrentStudyCard,
                    onCopyCard = viewModel::copyCurrentStudyCardToLessonEnd,
                    onMoveCard = { moveStudyCardDialogVisible = true },
                    onToggleFeaturedCard = viewModel::toggleCurrentCardFeatured,
                    onToggleOriginalCardText = viewModel::toggleOriginalCardText,
                    onGenerateTrainCards = { card -> trainSourceCard = card }
                )
                AppScreen.SETTINGS -> SettingsScreen(
                    cardStartSide = state.cardStartSide,
                    excludeMasteredCards = state.excludeMasteredCards,
                    showCardLog = state.showCardLog,
                    interfaceLanguage = state.interfaceLanguage,
                    displayTextSize = state.displayTextSize,
                    controlSize = state.controlSize,
                    soundEffectsEnabled = state.soundEffectsEnabled,
                    vibrationEnabled = state.vibrationEnabled,
                    notificationIntervalDraft = state.notificationIntervalDraft,
                    notificationMaxDraft = state.notificationMaxDraft,
              quickVocabularySourceLanguage = state.quickVocabularySourceLanguage,
              quickVocabularyTargetLanguage = state.quickVocabularyTargetLanguage,
              useLocalTranslation = state.useLocalTranslation,
              autoSaveTranslatorCards = state.autoSaveTranslatorCards,
              translationApiUrl = state.translationApiUrl,
              translationApiToken = state.translationApiToken,
                    useOpenAiModels = state.useOpenAiModels,
                    openAiBaseUrl = state.openAiBaseUrl,
                    openAiApiKey = state.openAiApiKey,
                    serverUsername = state.serverUsername,
                    openAiSpeechModel = state.openAiSpeechModel,
                    openAiTextModel = state.openAiTextModel,
                    openAiImageTextModel = state.openAiImageTextModel,
                    openAiTtsModel = state.openAiTtsModel,
                    openAiTtsVoice = state.openAiTtsVoice,
                    belarusianTtsProvider = state.belarusianTtsProvider,
                    elevenLabsApiKey = state.elevenLabsApiKey,
                    elevenLabsModel = state.elevenLabsModel,
                    elevenLabsVoiceId = state.elevenLabsVoiceId,
                    elevenLabsTtsLanguageCodes = state.elevenLabsTtsLanguageCodes,
                    openAiCacheDurationMinutes = state.openAiCacheDurationMinutes,
                    openAiVoiceSilenceTimeoutMs = state.openAiVoiceSilenceTimeoutMs,
                    cardStatusBlinkIntervalMs = state.cardStatusBlinkIntervalMs,
                    catDialogRetentionDays = state.catDialogRetentionDays,
                    catReplySpeechRate = state.catReplySpeechRate,
                    openAiActivityLog = state.openAiActivityLog,
                    offlineSpeechLanguageTag = state.offlineSpeechLanguageTag,
                    offlineSpeechStatuses = state.offlineSpeechStatuses,
                    offlineSpeechDownloadingTag = state.offlineSpeechDownloadingTag,
                    downloadedTranslationLanguages = downloadedTranslationLanguages,
                    translationDownloadLanguage = translationDownloadLanguage,
                    translationDownloadingLabel = translationDownloadingLabel,
                    onTranslationDownloadLanguageChange = { translationDownloadLanguage = it },
                    onDownloadSelectedTranslationLanguage = { downloadGoogleLanguageModel(translationDownloadLanguage) },
                    onOfflineSpeechLanguageChange = viewModel::setOfflineSpeechLanguage,
                    onDownloadOfflineSpeechModel = ::downloadOfflineSpeechModel,
                    onOpenLocalLanguages = { showLocalLanguageDialog = true },
                    onCardStartSideChange = viewModel::setCardStartSide,
                    onExcludeMasteredCardsChange = viewModel::setExcludeMasteredCards,
                    onShowCardLogChange = viewModel::setShowCardLog,
                    onInterfaceLanguageChange = viewModel::setInterfaceLanguage,
                    onDisplayTextSizeChange = viewModel::setDisplayTextSize,
                    onControlSizeChange = viewModel::setControlSize,
                    onSoundEffectsEnabledChange = viewModel::setSoundEffectsEnabled,
                    onVibrationEnabledChange = viewModel::setVibrationEnabled,
                    onNotificationIntervalChange = viewModel::updateNotificationIntervalDraft,
                    onNotificationMaxChange = viewModel::updateNotificationMaxDraft,
              onQuickVocabularySourceChange = viewModel::setQuickVocabularySourceLanguage,
              onQuickVocabularyTargetChange = { language ->
                  viewModel.setQuickVocabularyTargetLanguage(language)
                  speechTagForDictionaryLanguage(language)?.let(viewModel::setOfflineSpeechLanguage)
              },
        onUseLocalTranslationChange = viewModel::setUseLocalTranslation,
        onAutoSaveTranslatorCardsChange = viewModel::setAutoSaveTranslatorCards,
        onTranslationApiUrlChange = viewModel::setTranslationApiUrl,
        onTranslationApiTokenChange = viewModel::setTranslationApiToken,
                    onUseOpenAiModelsChange = viewModel::setUseOpenAiModels,
                    onOpenAiBaseUrlChange = viewModel::setOpenAiBaseUrl,
                    onOpenAiApiKeyChange = viewModel::setOpenAiApiKey,
                    onMurrLexServerLogin = viewModel::loginToMurrLexServer,
                    onMurrLexServerLogout = viewModel::logoutFromMurrLexServer,
                    onOpenAiSpeechModelChange = viewModel::setOpenAiSpeechModel,
                    onOpenAiTextModelChange = viewModel::setOpenAiTextModel,
                    onOpenAiImageTextModelChange = viewModel::setOpenAiImageTextModel,
                    onOpenAiTtsModelChange = viewModel::setOpenAiTtsModel,
                    onOpenAiTtsVoiceChange = viewModel::setOpenAiTtsVoice,
                    onBelarusianTtsProviderChange = viewModel::setBelarusianTtsProvider,
                    onElevenLabsApiKeyChange = viewModel::setElevenLabsApiKey,
                    onElevenLabsModelChange = viewModel::setElevenLabsModel,
                    onElevenLabsVoiceIdChange = viewModel::setElevenLabsVoiceId,
                    onElevenLabsTtsLanguageEnabledChange = viewModel::setElevenLabsTtsLanguageEnabled,
                    onOpenAiCacheDurationChange = viewModel::setOpenAiCacheDurationMinutes,
                    onOpenAiVoiceSilenceTimeoutChange = viewModel::setOpenAiVoiceSilenceTimeoutMs,
                    onCardStatusBlinkIntervalChange = viewModel::setCardStatusBlinkIntervalMs,
                    onCatDialogRetentionDaysChange = viewModel::setCatDialogRetentionDays,
                    onCatReplySpeechRateChange = viewModel::setCatReplySpeechRate,
                    onClearOpenAiCache = viewModel::clearOpenAiCache,
                    onSaveNotificationInterval = { viewModel.saveNotificationInterval(context) },
                    onSaveNotificationMax = viewModel::saveNotificationMax,
                    onDownloadTranslationLanguages = { showTranslationDownloadDialog = true },
                    onDownloadSampleJson = {
                        val fileName = "make_mistake_json_template.json"
                        jsonPendingExport = fileName to sampleLessonJson()
                        exportLauncher.launch(fileName)
                    }
                )
                AppScreen.EDITOR -> LessonEditorScreen(
                    state = state,
                    onTitleChange = viewModel::updateLessonTitle,
                    onLessonInfoChange = viewModel::updateLessonInfo,
                    onLessonSourceLanguageChange = viewModel::updateLessonSourceLanguage,
                    onLessonTargetLanguageChange = viewModel::updateLessonTargetLanguage,
                    onCardDraftChange = viewModel::updateCardDraft,
                    onAddCard = viewModel::addCardToLesson,
                    onDeleteCard = viewModel::deleteCardFromLesson,
                    onCopyCard = viewModel::copyCardInLesson,
                    onMoveCard = viewModel::moveCard,
                    onEditCard = viewModel::editCardInLesson,
                    onCancelCardEditing = viewModel::cancelCardEditing,
                    onToggleCardStar = viewModel::toggleCardStar,
                    onSave = viewModel::saveEditedLesson,
                    onDeleteLesson = viewModel::deleteLesson,
                    onVoiceInputForCardField = { target -> startVoiceInput(target) },
                    onDeleteCards = viewModel::deleteCardsFromLesson,
                    onCopyCardsToLesson = viewModel::copyCardsFromEditorToLesson
                )
            }
            if (state.screen == AppScreen.STUDY && state.cardDraft.editingCardId != null) {
                StudyCardEditorDialog(
                    cardDraft = state.cardDraft,
                    onCardDraftChange = viewModel::updateCardDraft,
                    onSave = viewModel::saveCurrentStudyCard,
                    onDelete = viewModel::deleteCurrentStudyCard,
                    onDismiss = viewModel::cancelCardEditing,
                    onVoiceInput = { target -> startVoiceInput(target) }
                )
            }
            MessageBubble(
                text = messageBubbleText,
                visible = messageBubbleVisible,
                positive = messageBubblePositive,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 22.dp)
                    .zIndex(20f)
            )
            SuccessStar(
                visible = successStarVisible,
                modifier = Modifier
                    .align(Alignment.Center)
                    .zIndex(21f)
            )
        }
    }
}
}

@Composable
private fun SuccessStar(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(120)) + slideInVertically(
            animationSpec = tween(180),
            initialOffsetY = { it / 2 }
        ),
        exit = fadeOut(animationSpec = tween(420)) + slideOutVertically(
            animationSpec = tween(700),
            targetOffsetY = { -220 }
        ),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            tonalElevation = 8.dp,
            shadowElevation = 10.dp,
            color = Color(0xFFFFF6C7)
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = "Correct",
                modifier = Modifier.padding(16.dp).size(46.dp),
                tint = Color(0xFFFFC107)
            )
        }
    }
}

@Composable
private fun CompactTopIconButton(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(38.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(21.dp)
        )
    }
}

@Composable
private fun MurrLexTitleRow(
    isStudy: Boolean,
    isCatalog: Boolean,
    isDeviceOnline: Boolean,
    titleColor: Color,
    catMenuExpanded: Boolean,
    onCatChat: () -> Unit,
    onCatMenuToggle: () -> Unit,
    onCatMenuDismiss: () -> Unit,
    onOpenAnimations: () -> Unit,
    onOpenOriginalAnimation: () -> Unit,
    onStartGame: (LanguageGameMode) -> Unit,
    onImageImport: () -> Unit,
    onTitleClick: () -> Unit,
    onStatusClick: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            IconButton(
                onClick = onCatMenuToggle,
                modifier = Modifier.size(34.dp)
            ) {
                ProfessorCatMark(progress = 1f, modifier = Modifier.size(25.dp))
            }
            DropdownMenu(
                expanded = catMenuExpanded,
                onDismissRequest = onCatMenuDismiss
            ) {
                Column(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    DropdownMenuItem(
                        text = { Text("Cat chat") },
                        leadingIcon = {
                            ProfessorCatMark(progress = 1f, modifier = Modifier.size(24.dp))
                        },
                        onClick = onCatChat
                    )
                    DropdownMenuItem(
                        text = { Text("Animations") },
                        leadingIcon = { Icon(Icons.Default.Style, contentDescription = null) },
                        onClick = onOpenAnimations
                    )
                    DropdownMenuItem(
                        text = { Text("Анимация (оригинал)") },
                        leadingIcon = { Icon(Icons.Default.Style, contentDescription = null) },
                        onClick = onOpenOriginalAnimation
                    )
                    DropdownMenuItem(
                        text = { Text("Rive Runner Lab") },
                        leadingIcon = {
                            GingerFluffyCatMark(modifier = Modifier.size(24.dp))
                        },
                        onClick = { onStartGame(LanguageGameMode.RIVE_RUNNER_LAB) }
                    )
                    DropdownMenuItem(
                        text = { Text("Rive Letter Blocks") },
                        leadingIcon = {
                            GingerFluffyCatMark(modifier = Modifier.size(24.dp))
                        },
                        onClick = { onStartGame(LanguageGameMode.RIVE_LETTER_BLOCKS) }
                    )
                }
            }
        }
        if (!isCatalog && !isStudy) {
            IconButton(
                onClick = onImageImport,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    Icons.Default.Image,
                    contentDescription = "Import image text",
                    tint = titleColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Text(
            "MurrLex",
            fontWeight = FontWeight.SemiBold,
            color = titleColor,
            modifier = Modifier.clickable(onClick = onTitleClick)
        )
        Box(
            modifier = Modifier
                .padding(start = 7.dp, top = 2.dp)
                .size(9.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(if (isDeviceOnline) OnlineStatusColor else BrandRedColor)
                .clickable(onClick = onStatusClick)
        )
    }
}

@Composable
private fun CatChatDialog(
    messages: List<CatChatMessage>,
    input: String,
    basicLanguage: String,
    targetLanguage: String,
    busy: Boolean,
    autoSpeak: Boolean,
    replySpeed: Float,
    onInputChange: (String) -> Unit,
    onBasicLanguageChange: (String) -> Unit,
    onTargetLanguageChange: (String) -> Unit,
    onAutoSpeakChange: (Boolean) -> Unit,
    onReplySpeedChange: (Float) -> Unit,
    onVoiceInput: () -> Unit,
    onSpeakMessage: (String) -> Unit,
    onDismiss: () -> Unit,
    onCreateCard: (String, Boolean) -> Unit,
    onCreateMistakeCard: (String, String) -> Unit,
    onCreateFeaturedCard: (String, Boolean) -> Unit,
    onFeatureSelection: (String, String) -> Unit,
    onSend: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "catChat")
    val pulse by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "catChatPulse"
    )
    var speedMenuExpanded by remember { mutableStateOf(false) }
    var localNotice by remember { mutableStateOf("") }
    var featuredPopup by remember { mutableStateOf<CatChatFeaturedPopup?>(null) }
    val messageListState = rememberLazyListState()
    val tappedWordSelections = remember { mutableStateMapOf<String, Set<CatChatWordSelection>>() }
    val catReactions = listOf("😺", "😸", "😻", "😼", "🙀")
    LaunchedEffect(messages.map { "${it.id}:${it.text}:${it.analysis}" }.joinToString("|")) {
        val visibleIds = messages.map { it.id }.toSet()
        tappedWordSelections.keys.toList().forEach { messageId ->
            if (messageId !in visibleIds) tappedWordSelections.remove(messageId)
        }
        if (messages.isNotEmpty()) {
            delay(60)
            messageListState.animateScrollToItem(messages.lastIndex)
        }
    }
    LaunchedEffect(localNotice) {
        if (localNotice.isNotBlank()) {
            delay(1300)
            localNotice = ""
        }
    }
    featuredPopup?.let { popup ->
        AlertDialog(
            onDismissRequest = { featuredPopup = null },
            title = { Text("Featured") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(popup.selection, fontWeight = FontWeight.Bold)
                    Text(
                        "This featured fragment is linked inside the chat. Card hint: Hint: ${popup.selection.chatSelectionHint()}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onCreateCard(popup.selection, popup.fromCat)
                    localNotice = "Card created"
                    featuredPopup = null
                }) { Text("Create Card") }
            },
            dismissButton = {
                TextButton(onClick = { featuredPopup = null }) { Text("Close") }
            }
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProfessorCatMark(
                    progress = 1f,
                    modifier = Modifier
                        .size(46.dp)
                        .graphicsLayer {
                            scaleX = pulse
                            scaleY = pulse
                        }
                )
                Text("Cat chat", fontWeight = FontWeight.SemiBold)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 420.dp, max = 560.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CatChatLanguageButton(
                        label = "Basic",
                        language = basicLanguage,
                        onLanguageChange = onBasicLanguageChange,
                        modifier = Modifier.weight(1f)
                    )
                    CatChatLanguageButton(
                        label = "Target",
                        language = targetLanguage,
                        onLanguageChange = onTargetLanguageChange,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Auto voice",
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Switch(
                        checked = autoSpeak,
                        onCheckedChange = onAutoSpeakChange
                    )
                    Box {
                        OutlinedButton(
                            onClick = { speedMenuExpanded = true },
                            shape = RoundedCornerShape(18.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("${replySpeed}x")
                        }
                        DropdownMenu(
                            expanded = speedMenuExpanded,
                            onDismissRequest = { speedMenuExpanded = false }
                        ) {
                            CatReplySpeechRateOptions.forEach { rate ->
                                DropdownMenuItem(
                                    text = { Text("${rate}x") },
                                    onClick = {
                                        speedMenuExpanded = false
                                        onReplySpeedChange(rate)
                                    }
                                )
                            }
                        }
                    }
                }
                LazyColumn(
                    state = messageListState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (messages.isEmpty()) {
                        item {
                            Text(
                                "Ask something light. The cat answers in $targetLanguage.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }
                    items(messages, key = { it.id }) { message ->
                        CatChatBubble(
                            message = message,
                            selectedWordRanges = tappedWordSelections[message.id].orEmpty(),
                            onToggleWordSelection = { selection ->
                                val current = tappedWordSelections[message.id].orEmpty()
                                tappedWordSelections[message.id] = if (selection in current) {
                                    current - selection
                                } else {
                                    current + selection
                                }
                            },
                            onClearWordSelection = {
                                tappedWordSelections.remove(message.id)
                            },
                            onCreateCard = { phrase, fromCat ->
                                onCreateCard(phrase, fromCat)
                                localNotice = "Card created"
                            },
                            onCreateMistakeCard = { phrase, analysis ->
                                onCreateMistakeCard(phrase, analysis)
                                localNotice = "Mistake card created"
                            },
                            onCreateFeaturedCard = { phrase, fromCat ->
                                onCreateFeaturedCard(phrase, fromCat)
                                localNotice = "Featured card created"
                            },
                            onFeatureSelection = onFeatureSelection,
                            onFeaturedClick = { phrase, fromCat ->
                                featuredPopup = CatChatFeaturedPopup(phrase, fromCat)
                            },
                            onSpeakMessage = onSpeakMessage
                        )
                    }
                }
                if (busy) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                AnimatedVisibility(visible = localNotice.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = BrandSaladColor.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, BrandSaladColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            localNotice,
                            color = BrandSaladColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    catReactions.forEach { reaction ->
                        TextButton(
                            onClick = {
                                onInputChange(listOf(input.trim(), reaction).filter { it.isNotBlank() }.joinToString(" "))
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(reaction, fontSize = 18.sp)
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = onInputChange,
                        singleLine = false,
                        maxLines = 3,
                        placeholder = { Text("Message in $basicLanguage") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { onSend() }),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onVoiceInput,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "Voice input",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = onSend,
                        enabled = input.trim().isNotBlank() && !busy
                    ) {
                        Text("Send")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun CatChatLanguageButton(
    label: String,
    language: String,
    modifier: Modifier = Modifier,
    onLanguageChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("$label: ${language.shortLanguageCode()}", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            OfflineSpeechLanguages.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.name) },
                    onClick = {
                        expanded = false
                        onLanguageChange(option.name)
                    }
                )
            }
        }
    }
}

@Composable
private fun CatChatBubble(
    message: CatChatMessage,
    selectedWordRanges: Set<CatChatWordSelection>,
    onToggleWordSelection: (CatChatWordSelection) -> Unit,
    onClearWordSelection: () -> Unit,
    onCreateCard: (String, Boolean) -> Unit,
    onCreateMistakeCard: (String, String) -> Unit,
    onCreateFeaturedCard: (String, Boolean) -> Unit,
    onFeatureSelection: (String, String) -> Unit,
    onFeaturedClick: (String, Boolean) -> Unit,
    onSpeakMessage: (String) -> Unit
) {
    val bubbleColor = if (message.fromCat) Color(0xFFFFF2F6) else Color(0xFFEFF5F1)
    val alignment = if (message.fromCat) Alignment.Start else Alignment.End
    val selectedPhrase = remember(message.text, selectedWordRanges) {
        message.text.selectedChatPhrase(selectedWordRanges)
    }
    Column(
        horizontalAlignment = alignment,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = bubbleColor,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SelectableChatText(
                        text = message.text,
                        featuredSelection = message.featuredSelection,
                        selectedWordRanges = selectedWordRanges,
                        onFeaturedClick = { selected -> onFeaturedClick(selected, message.fromCat) },
                        onToggleWordSelection = onToggleWordSelection,
                        onFeature = { selected ->
                            onCreateFeaturedCard(selected, message.fromCat)
                            onFeatureSelection(message.id, selected)
                        },
                        onMakeCard = { selected -> onCreateCard(selected, message.fromCat) },
                        modifier = Modifier.weight(1f)
                    )
                    if (message.fromCat) {
                        IconButton(
                            onClick = { onSpeakMessage(message.text) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                                Icons.Default.VolumeUp,
                                contentDescription = "Speak cat answer",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    }
                }
                if (selectedPhrase.isNotBlank()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        Text(
                            selectedPhrase,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.width(120.dp)
                        )
                        OutlinedButton(
                            onClick = {
                                onCreateFeaturedCard(selectedPhrase, message.fromCat)
                                onFeatureSelection(message.id, selectedPhrase)
                                onClearWordSelection()
                            },
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Feature", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                onCreateCard(selectedPhrase, message.fromCat)
                                onClearWordSelection()
                            },
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Create Card", fontSize = 12.sp)
                        }
                    }
                }
                if (message.fromCat && message.analysis.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.74f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Cat check",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(message.analysis, fontSize = 13.sp)
                            OutlinedButton(
                                onClick = {
                                    onCreateMistakeCard(
                                        message.analysisSourceText.ifBlank { message.text },
                                        message.analysis
                                    )
                                },
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Create mistake card")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectableChatText(
    text: String,
    featuredSelection: String,
    selectedWordRanges: Set<CatChatWordSelection>,
    onFeaturedClick: (String) -> Unit,
    onToggleWordSelection: (CatChatWordSelection) -> Unit,
    onFeature: (String) -> Unit,
    onMakeCard: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                textSize = 15f
                setTextColor(android.graphics.Color.rgb(24, 24, 24))
                setTextIsSelectable(true)
                movementMethod = LinkMovementMethod.getInstance()
                highlightColor = android.graphics.Color.TRANSPARENT
            }
        },
        update = { view ->
            view.text = text.withChatSpans(featuredSelection, selectedWordRanges, onFeaturedClick)
            view.setOnTouchListener { touchedView, event ->
                if (event.action != MotionEvent.ACTION_UP) return@setOnTouchListener false
                val textView = touchedView as? TextView ?: return@setOnTouchListener false
                val layout = textView.layout ?: return@setOnTouchListener false
                val x = event.x.toInt() - textView.totalPaddingLeft + textView.scrollX
                val y = event.y.toInt() - textView.totalPaddingTop + textView.scrollY
                if (y < 0 || y > layout.height) return@setOnTouchListener false
                val line = layout.getLineForVertical(y)
                val offset = layout.getOffsetForHorizontal(line, x.toFloat()).coerceIn(0, textView.text.length)
                (textView.text as? Spannable)?.let { spannable ->
                    val clickableSpans = spannable.getSpans(offset, offset, ClickableSpan::class.java)
                    if (clickableSpans.isNotEmpty()) {
                        clickableSpans.first().onClick(textView)
                        return@setOnTouchListener true
                    }
                }
                textView.text.toString().chatWordSelectionAt(offset)?.let { selection ->
                    onToggleWordSelection(selection)
                    return@setOnTouchListener true
                }
                false
            }
            view.customSelectionActionModeCallback = object : ActionMode.Callback {
                override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                    menu.add(0, 1001, 0, "Feature")
                    menu.add(0, 1002, 1, "Create Cards")
                    return true
                }

                override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false

                override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                    val start = view.selectionStart.coerceAtMost(view.selectionEnd).coerceAtLeast(0)
                    val end = view.selectionStart.coerceAtLeast(view.selectionEnd).coerceAtMost(view.text.length)
                    val selected = view.text.substring(start, end).trim()
                    if (selected.isNotBlank()) {
                        when (item.itemId) {
                            1001 -> onFeature(selected)
                            1002 -> onMakeCard(selected)
                        }
                    }
                    mode.finish()
                    return true
                }

                override fun onDestroyActionMode(mode: ActionMode) = Unit
            }
        }
    )
}

private fun String.withChatSpans(
    featuredSelection: String,
    selectedWordRanges: Set<CatChatWordSelection>,
    onFeaturedClick: (String) -> Unit
): CharSequence {
    val spannable = SpannableString(this)
    selectedWordRanges.forEach { selection ->
        val start = selection.start.coerceIn(0, length)
        val end = selection.end.coerceIn(start, length)
        if (start < end) {
            spannable.setSpan(
                BackgroundColorSpan(android.graphics.Color.rgb(255, 238, 153)),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }
    val cleanSelection = featuredSelection.trim()
    if (isBlank() || cleanSelection.isBlank()) return spannable
    var start = indexOf(cleanSelection, ignoreCase = true)
    while (start >= 0) {
        val end = (start + cleanSelection.length).coerceAtMost(length)
        spannable.setSpan(
            StyleSpan(Typeface.BOLD),
            start,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) {
                    onFeaturedClick(cleanSelection)
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = true
                    ds.color = android.graphics.Color.rgb(47, 100, 86)
                    ds.typeface = Typeface.DEFAULT_BOLD
                }
            },
            start,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        start = indexOf(cleanSelection, startIndex = end, ignoreCase = true)
    }
    return spannable
}

private fun String.selectedChatPhrase(selectedWordRanges: Set<CatChatWordSelection>): String {
    if (isBlank() || selectedWordRanges.isEmpty()) return ""
    return selectedWordRanges
        .sortedWith(compareBy<CatChatWordSelection> { it.start }.thenBy { it.end })
        .mapNotNull { selection ->
            val start = selection.start.coerceIn(0, length)
            val end = selection.end.coerceIn(start, length)
            substring(start, end).trim().takeIf { it.isNotBlank() }
        }
        .joinToString(" ")
        .trim()
}

private fun String.chatWordSelectionAt(rawOffset: Int): CatChatWordSelection? {
    if (isBlank()) return null
    var offset = rawOffset.coerceIn(0, length - 1)
    if (!this[offset].isChatWordChar() && offset > 0 && this[offset - 1].isChatWordChar()) {
        offset -= 1
    }
    if (!this[offset].isChatWordChar()) return null
    var start = offset
    var end = offset + 1
    while (start > 0 && this[start - 1].isChatWordChar()) start -= 1
    while (end < length && this[end].isChatWordChar()) end += 1
    return CatChatWordSelection(start, end)
}

private fun Char.isChatWordChar(): Boolean {
    return isLetterOrDigit() || this == '\'' || this == '`' || this == '-' || this == '’'
}

private fun String.chatSelectionHint(): String {
    val clean = trim()
    if (clean.isBlank()) return "..."
    val words = clean.split(Regex("\\s+")).filter { it.isNotBlank() }
    if (words.size > 1) {
        return words.take(6).joinToString(" ") { word ->
            if (word.length <= 2) "${word.firstOrNull() ?: '?'}..."
            else "${word.first()}...${word.last()}"
        } + if (words.size > 6) " ..." else ""
    }
    return if (clean.length <= 2) "${clean.first()}..." else "${clean.first()}...${clean.last()}"
}

private fun languageGameCards(state: StudyUiState): List<LanguageGameEntry> {
    val selectedCards = state.selectedLesson?.cards.orEmpty()
    val sourceCards = if (selectedCards.isNotEmpty()) {
        selectedCards
    } else {
        state.lessons.flatMap { it.cards }
    }
    return sourceCards.mapIndexedNotNull { index, card ->
        val front = card.nativeText().trim()
        val back = card.correctText().trim()
        if (front.isBlank() || back.isBlank() ||
            front.equals("Empty", ignoreCase = true) ||
            back.equals("Empty", ignoreCase = true) ||
            front.equals("Translation pending", ignoreCase = true) ||
            back.equals("Translation pending", ignoreCase = true)
        ) {
            null
        } else {
            LanguageGameEntry(index, front, back)
        }
    }
}

private fun List<LanguageGameEntry>.toLibGdxCardsJson(): String {
    val array = JSONArray()
    take(40).forEach { card ->
        array.put(
            JSONObject()
                .put("front", card.front)
                .put("back", card.back)
        )
    }
    return array.toString()
}

@Composable
private fun OriginalAnimationScreen(
    onDismiss: () -> Unit
) {
    var selectedEntry by remember { mutableStateOf(MakeMistakeAnimationCatalog.firstOrNull()) }
    var playSignal by remember { mutableStateOf(0) }
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text("Original makemistake-animations", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Text("Анимация (оригинал)", fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 32.sp)
        Text(
            "All ANIM-001 through ANIM-010 demos are rendered from the original character and animation source.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
        Surface(
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            color = MaterialTheme.colorScheme.background
        ) {
            Text(
                selectedEntry?.lane.orEmpty().ifBlank { "animation/original" },
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                fontSize = 14.sp
            )
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            tonalElevation = 2.dp,
            shadowElevation = 3.dp,
            color = Color(0xFFF3FFFC),
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
        ) {
            selectedEntry?.let { entry ->
                OriginalAnimationLivePreview(entry = entry, playSignal = playSignal)
            }
        }
        selectedEntry?.let { entry ->
            Text(
                "${entry.number} ${entry.title}",
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "${entry.category} | ${entry.lane} | ${entry.component}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Text(
            "Use Replay to restart one-shot animations. Character loops keep moving continuously, matching the source app behavior.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.sp,
            lineHeight = 22.sp
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { playSignal++ },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Replay")
            }
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("Close")
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            MakeMistakeAnimationCatalog.forEach { entry ->
                OutlinedButton(
                    onClick = {
                        selectedEntry = entry
                        playSignal++
                    },
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(
                        1.dp,
                        if (selectedEntry?.number == entry.number) BrandSaladColor else MaterialTheme.colorScheme.outlineVariant
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(entry.number.removePrefix("ANIM-"), fontSize = 12.sp)
                }
            }
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.heightIn(max = 220.dp)
        ) {
            items(MakeMistakeAnimationCatalog) { entry ->
                AnimationCatalogRow(
                    entry = entry,
                    selected = selectedEntry?.number == entry.number,
                    onClick = {
                        selectedEntry = entry
                        playSignal++
                    }
                )
            }
        }
    }
}

@Composable
private fun OriginalAnimationLivePreview(entry: AnimationCatalogEntry, playSignal: Int) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        when (entry.number) {
            "ANIM-001" -> BouncySuccessCheckmark(playSignal)
            "ANIM-002" -> WrongAnswerCardShake(playSignal)
            "ANIM-003" -> LessonCardFlipReveal(playSignal)
            "ANIM-004" -> StreakFlamePulse(playSignal)
            "ANIM-005" -> MicrophoneListeningWaveform(playSignal)
            "ANIM-006" -> CharacterIdleBreathingBlink(playSignal)
            "ANIM-007" -> CharacterTalkingMouthFlaps(playSignal)
            "ANIM-008" -> CharacterSurprisedReaction(playSignal)
            "ANIM-009" -> CharacterEncouragingNod(playSignal)
            "ANIM-010" -> CharacterListeningPose(playSignal)
        }
    }
}

@Composable
private fun AnimationCatalogScreen(
    entries: List<AnimationCatalogEntry>,
    onDismiss: () -> Unit
) {
    var selectedEntry by remember { mutableStateOf(entries.firstOrNull()) }
    var playSignal by remember { mutableStateOf(0) }
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Text(
            "Live test screens from makemistake-animations. Pick an ANIM number and replay it full-size.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
        selectedEntry?.let { entry ->
            AnimationLivePreview(entry = entry, playSignal = playSignal, modifier = Modifier.weight(1f))
            Text(
                "${entry.number} ${entry.title}",
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "${entry.category} | ${entry.lane} | ${entry.component}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { playSignal++ },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Replay")
            }
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("Close")
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            entries.forEach { entry ->
                OutlinedButton(
                    onClick = {
                        selectedEntry = entry
                        playSignal++
                    },
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(
                        1.dp,
                        if (selectedEntry?.number == entry.number) BrandSaladColor else MaterialTheme.colorScheme.outlineVariant
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(entry.number.removePrefix("ANIM-"), fontSize = 12.sp)
                }
            }
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.heightIn(max = 190.dp)
        ) {
            items(entries) { entry ->
                AnimationCatalogRow(
                    entry = entry,
                    selected = selectedEntry?.number == entry.number,
                    onClick = {
                        selectedEntry = entry
                        playSignal++
                    }
                )
            }
        }
    }
}

@Composable
private fun AnimationCatalogRow(
    entry: AnimationCatalogEntry,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (selected) BrandSaladColor else MaterialTheme.colorScheme.outlineVariant),
        color = if (selected) BrandSaladColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(entry.number, fontWeight = FontWeight.Bold, color = BrandSaladColor)
                Text(entry.title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            }
            Text(
                "${entry.category} | ${entry.status}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun AnimationLivePreview(entry: AnimationCatalogEntry, playSignal: Int, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF8FAF7),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 280.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            when (entry.number) {
                "ANIM-001" -> PreviewBouncySuccess(playSignal)
                "ANIM-002" -> PreviewWrongAnswerShake(playSignal)
                "ANIM-003" -> PreviewCardFlip(playSignal)
                "ANIM-004" -> PreviewStreakFlame()
                "ANIM-005" -> PreviewMicrophoneWaveform()
                "ANIM-006" -> PreviewCharacterIdle()
                "ANIM-007" -> PreviewCharacterTalking()
                "ANIM-008" -> PreviewCharacterSurprised(playSignal)
                "ANIM-009" -> PreviewCharacterNod()
                "ANIM-010" -> PreviewCharacterListening()
            }
        }
    }
}

@Composable
private fun PreviewBouncySuccess(playSignal: Int) {
    var visible by remember(playSignal) { mutableStateOf(false) }
    LaunchedEffect(playSignal) {
        visible = false
        delay(50)
        visible = true
    }
    val scale by animateFloatAsState(if (visible) 1f else 0.55f, animationSpec = tween(420), label = "previewSuccessScale")
    Canvas(Modifier.size(150.dp).graphicsLayer { scaleX = scale; scaleY = scale }) {
        val c = Offset(size.width / 2f, size.height / 2f)
        drawCircle(Color(0xFFE0F7EE), radius = size.minDimension * 0.42f, center = c)
        drawCircle(BrandSaladColor, radius = size.minDimension * 0.36f, center = c, style = Stroke(width = 8f))
        drawLine(
            BrandSaladColor,
            Offset(size.width * 0.30f, size.height * 0.52f),
            Offset(size.width * 0.45f, size.height * 0.66f),
            strokeWidth = 10f
        )
        drawLine(
            BrandSaladColor,
            Offset(size.width * 0.45f, size.height * 0.66f),
            Offset(size.width * 0.72f, size.height * 0.36f),
            strokeWidth = 10f
        )
    }
}

@Composable
private fun PreviewWrongAnswerShake(playSignal: Int) {
    var phase by remember(playSignal) { mutableStateOf(0f) }
    LaunchedEffect(playSignal) {
        repeat(8) {
            phase = if (it % 2 == 0) 1f else -1f
            delay(55)
        }
        phase = 0f
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(2.dp, BrandRedColor.copy(alpha = 0.7f)),
        color = Color(0xFFFFF0F0),
        modifier = Modifier
            .width(190.dp)
            .height(112.dp)
            .graphicsLayer { translationX = phase * 14f }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(12.dp)
        ) {
            Text("Try again", fontWeight = FontWeight.Bold, color = BrandRedColor)
            Text("soft correction", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PreviewCardFlip(playSignal: Int) {
    var back by remember(playSignal) { mutableStateOf(false) }
    LaunchedEffect(playSignal) {
        back = false
        delay(180)
        back = true
    }
    val rotation by animateFloatAsState(if (back) 180f else 0f, animationSpec = tween(720), label = "previewFlip")
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (rotation < 90f) Color.White else Color(0xFFEAF7F2),
        border = BorderStroke(1.dp, BrandSaladColor.copy(alpha = 0.45f)),
        modifier = Modifier
            .width(190.dp)
            .height(120.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { if (rotation >= 90f) rotationY = 180f }
        ) {
            Text(if (rotation < 90f) "I learn daily" else "Uczę się codziennie", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun PreviewStreakFlame() {
    val transition = rememberInfiniteTransition(label = "previewFlame")
    val pulse by transition.animateFloat(0.85f, 1.12f, infiniteRepeatable(tween(780), RepeatMode.Reverse), label = "flamePulse")
    Canvas(Modifier.size(158.dp).graphicsLayer { scaleX = pulse; scaleY = pulse }) {
        val w = size.width
        val h = size.height
        val flame = Path().apply {
            moveTo(w * 0.50f, h * 0.14f)
            cubicTo(w * 0.82f, h * 0.38f, w * 0.72f, h * 0.78f, w * 0.50f, h * 0.90f)
            cubicTo(w * 0.25f, h * 0.76f, w * 0.18f, h * 0.44f, w * 0.50f, h * 0.14f)
        }
        drawPath(flame, Brush.verticalGradient(listOf(Color(0xFFFFD166), Color(0xFFFF6B4A))))
        drawCircle(Color.White.copy(alpha = 0.55f), radius = w * 0.12f, center = Offset(w * 0.50f, h * 0.62f))
    }
}

@Composable
private fun PreviewMicrophoneWaveform() {
    val transition = rememberInfiniteTransition(label = "previewWave")
    val phase by transition.animateFloat(0f, 6.28f, infiniteRepeatable(tween(950), RepeatMode.Restart), label = "wavePhase")
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Icons.Default.Mic, contentDescription = null, tint = BrandSaladColor, modifier = Modifier.size(44.dp))
        Canvas(Modifier.width(190.dp).height(72.dp)) {
            val barWidth = size.width / 16f
            repeat(12) { index ->
                val wave = abs(sin(phase + index * 0.7f))
                val barHeight = size.height * (0.18f + wave * 0.72f)
                val left = index * (barWidth * 1.32f)
                drawRoundRect(
                    color = BrandSaladColor.copy(alpha = 0.38f + wave * 0.48f),
                    topLeft = Offset(left, (size.height - barHeight) / 2f),
                    size = Size(barWidth, barHeight)
                )
            }
        }
    }
}

@Composable
private fun PreviewCharacterIdle() {
    val transition = rememberInfiniteTransition(label = "previewIdle")
    val bob by transition.animateFloat(-5f, 5f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "idleBob")
    ProfessorCatMark(progress = 1f, modifier = Modifier.size(118.dp).graphicsLayer { translationY = bob })
}

@Composable
private fun PreviewCharacterTalking() {
    val transition = rememberInfiniteTransition(label = "previewTalk")
    val scale by transition.animateFloat(0.95f, 1.08f, infiniteRepeatable(tween(320), RepeatMode.Reverse), label = "talkScale")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ProfessorCatMark(progress = 1f, modifier = Modifier.size(112.dp).graphicsLayer { scaleX = scale; scaleY = scale })
        Text("mrr... mrr...", color = BrandSaladColor, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PreviewCharacterSurprised(playSignal: Int) {
    var pop by remember(playSignal) { mutableStateOf(false) }
    LaunchedEffect(playSignal) {
        pop = false
        delay(60)
        pop = true
    }
    val scale by animateFloatAsState(if (pop) 1.18f else 0.82f, animationSpec = tween(300), label = "surprisePop")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ProfessorCatMark(progress = 1f, modifier = Modifier.size(112.dp).graphicsLayer { scaleX = scale; scaleY = scale })
        Text("!", color = BrandRedColor, fontSize = 30.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PreviewCharacterNod() {
    val transition = rememberInfiniteTransition(label = "previewNod")
    val tilt by transition.animateFloat(-7f, 7f, infiniteRepeatable(tween(680), RepeatMode.Reverse), label = "nodTilt")
    ProfessorCatMark(progress = 1f, modifier = Modifier.size(118.dp).graphicsLayer { rotationZ = tilt; translationY = abs(tilt) * 0.7f })
}

@Composable
private fun PreviewCharacterListening() {
    val transition = rememberInfiniteTransition(label = "previewListen")
    val phase by transition.animateFloat(0f, 6.28f, infiniteRepeatable(tween(1200), RepeatMode.Restart), label = "listenPhase")
    Box(contentAlignment = Alignment.Center) {
        ProfessorCatMark(progress = 1f, modifier = Modifier.size(112.dp))
        Canvas(Modifier.size(170.dp)) {
            repeat(3) { index ->
                val radius = size.minDimension * (0.34f + index * 0.10f + abs(sin(phase + index)) * 0.04f)
                drawCircle(
                    color = BrandSaladColor.copy(alpha = 0.16f - index * 0.035f),
                    radius = radius,
                    center = Offset(size.width / 2f, size.height / 2f),
                    style = Stroke(width = 4f)
                )
            }
        }
    }
}

@Composable
private fun LanguageGameDialog(
    mode: LanguageGameMode,
    cards: List<LanguageGameEntry>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (mode) {
                    LanguageGameMode.QUIZ -> "Quick quiz"
                    LanguageGameMode.MATCH -> "Match pairs"
                    LanguageGameMode.SPELL -> "Spell check"
                    LanguageGameMode.FEED -> "Feed the cat"
                    LanguageGameMode.GINGER_COMPOSE -> "Ginger Word Garden"
                    LanguageGameMode.GINGER_CANVAS -> "Ginger Canvas Pounce"
                    LanguageGameMode.GINGER_HTML -> "Ginger HTML5 Flash"
                    LanguageGameMode.PSEUDO_3D_WALK -> "Pseudo 3D Cat Walk"
                    LanguageGameMode.EIGHT_BIT_PLATFORMER -> "8-bit Ginger Platformer"
                    LanguageGameMode.SIXTEEN_BIT_SPIN_RUNNER -> "16-bit Ginger Spin Runner"
                    LanguageGameMode.LIBGDX_SPIN_RUNNER -> "LibGDX Ginger Runner"
                    LanguageGameMode.GODOT_RUNNER_LAB -> "Godot Runner Lab"
                    LanguageGameMode.LOTTIE_RUNNER_LAB -> "Lottie Runner Lab"
                    LanguageGameMode.FLUTTER_RUNNER_LAB -> "Flutter Runner Lab"
                    LanguageGameMode.RIVE_RUNNER_LAB -> "Rive Runner Lab"
                    LanguageGameMode.RIVE_LETTER_BLOCKS -> "Rive Letter Blocks"
                },
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            if (cards.isEmpty()) {
                Text("No ready cards for a game yet.")
            } else {
                when (mode) {
                    LanguageGameMode.QUIZ -> QuickQuizGame(cards)
                    LanguageGameMode.MATCH -> MatchPairsGame(cards)
                    LanguageGameMode.SPELL -> SpellCheckGame(cards)
                    LanguageGameMode.FEED -> FeedTheCatGame(cards)
                    LanguageGameMode.GINGER_COMPOSE -> GingerWordGardenGame(cards)
                    LanguageGameMode.GINGER_CANVAS -> GingerCanvasPounceGame(cards)
                    LanguageGameMode.GINGER_HTML -> GingerHtml5FlashGame(cards)
                    LanguageGameMode.PSEUDO_3D_WALK -> Pseudo3DCatWalkScreen(cards, onDismiss = {})
                    LanguageGameMode.EIGHT_BIT_PLATFORMER -> EightBitGingerPlatformerScreen(cards, onDismiss = {})
                    LanguageGameMode.SIXTEEN_BIT_SPIN_RUNNER -> SixteenBitGingerSpinRunnerScreen(cards, onDismiss = {})
                    LanguageGameMode.LIBGDX_SPIN_RUNNER -> Text("Opening LibGDX runner...")
                    LanguageGameMode.GODOT_RUNNER_LAB -> GodotRunnerLabScreen(cards, onDismiss = {})
                    LanguageGameMode.LOTTIE_RUNNER_LAB -> LottieRunnerLabScreen(cards, onDismiss = {})
                    LanguageGameMode.FLUTTER_RUNNER_LAB -> FlutterRunnerLabScreen(cards, onDismiss = {})
                    LanguageGameMode.RIVE_RUNNER_LAB -> RiveRunnerLabScreen(cards, speechLanguageTag = "en-US", onSpeak = { _, _ -> }, onDismiss = {})
                    LanguageGameMode.RIVE_LETTER_BLOCKS -> RiveLetterBlocksScreen(cards, speechLanguageTag = "en-US", onSpeak = { _, _ -> }, onDismiss = {})
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun QuickQuizGame(cards: List<LanguageGameEntry>) {
    val deck = remember(cards) { cards.shuffled(Random(cards.size * 17 + 5)).take(20) }
    var index by remember(deck) { mutableStateOf(0) }
    var score by remember(deck) { mutableStateOf(0) }
    var feedback by remember(deck) { mutableStateOf("") }
    val current = deck[index.coerceIn(0, deck.lastIndex)]
    val choices = remember(current, deck) {
        (listOf(current.back) + deck
            .filterNot { it.id == current.id }
            .map { it.back }
            .distinct()
            .shuffled(Random(current.id + deck.size))
            .take(3))
            .distinct()
            .shuffled(Random(current.back.hashCode()))
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("${index + 1} / ${deck.size}  Score: $score", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFFFE4EC),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                current.front,
                modifier = Modifier.padding(14.dp),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
        choices.forEach { choice ->
            OutlinedButton(
                onClick = {
                    val correct = normalizeAnswerText(choice) == normalizeAnswerText(current.back)
                    if (correct) score += 1
                    feedback = if (correct) "Correct" else "Answer: ${current.back}"
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(choice, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
        if (feedback.isNotBlank()) {
            Text(feedback, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(
                onClick = {
                    if (index < deck.lastIndex) {
                        index += 1
                        feedback = ""
                    } else {
                        index = 0
                        score = 0
                        feedback = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(if (index < deck.lastIndex) "Next" else "Restart")
            }
        }
    }
}

@Composable
private fun MatchPairsGame(cards: List<LanguageGameEntry>) {
    val round = remember(cards) { cards.shuffled(Random(cards.size * 23 + 7)).take(4) }
    val backItems = remember(round) { round.shuffled(Random(round.size * 31 + 3)) }
    var selectedFrontId by remember(round) { mutableStateOf<Int?>(null) }
    var selectedBackId by remember(round) { mutableStateOf<Int?>(null) }
    var matchedIds by remember(round) { mutableStateOf<Set<Int>>(emptySet()) }
    fun resolveSelection(frontId: Int?, backId: Int?) {
        if (frontId == null || backId == null) return
        if (frontId == backId) matchedIds = matchedIds + frontId
        selectedFrontId = null
        selectedBackId = null
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("${matchedIds.size} / ${round.size} matched", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                round.forEach { item ->
                    OutlinedButton(
                        onClick = {
                            selectedFrontId = item.id
                            resolveSelection(item.id, selectedBackId)
                        },
                        enabled = item.id !in matchedIds,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = if (selectedFrontId == item.id) {
                            ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFFFE4EC))
                        } else {
                            ButtonDefaults.outlinedButtonColors()
                        }
                    ) {
                        Text(item.front, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                backItems.forEach { item ->
                    OutlinedButton(
                        onClick = {
                            selectedBackId = item.id
                            resolveSelection(selectedFrontId, item.id)
                        },
                        enabled = item.id !in matchedIds,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = if (selectedBackId == item.id) {
                            ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFE4F6E8))
                        } else {
                            ButtonDefaults.outlinedButtonColors()
                        }
                    ) {
                        Text(item.back, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        if (matchedIds.size == round.size) {
            Text("Round complete", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SpellCheckGame(cards: List<LanguageGameEntry>) {
    val deck = remember(cards) { cards.shuffled(Random(cards.size * 29 + 11)).take(20) }
    var index by remember(deck) { mutableStateOf(0) }
    var input by remember(deck) { mutableStateOf("") }
    var feedback by remember(deck) { mutableStateOf("") }
    var score by remember(deck) { mutableStateOf(0) }
    val current = deck[index.coerceIn(0, deck.lastIndex)]
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("${index + 1} / ${deck.size}  Score: $score", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(current.front, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            value = input,
            onValueChange = {
                input = it
                feedback = ""
            },
            singleLine = false,
            maxLines = 3,
            placeholder = { Text("Type the target side") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                val correct = normalizeAnswerText(input) == normalizeAnswerText(current.back)
                if (correct) score += 1
                feedback = if (correct) "Correct" else "Answer: ${current.back}"
            },
            enabled = input.trim().isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Check")
        }
        if (feedback.isNotBlank()) {
            Text(feedback, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(
                onClick = {
                    if (index < deck.lastIndex) index += 1 else index = 0
                    input = ""
                    feedback = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (index < deck.lastIndex) "Next" else "Restart")
            }
        }
    }
}

@Composable
private fun FeedTheCatGame(cards: List<LanguageGameEntry>) {
    val deck = remember(cards) { cards.shuffled(Random(cards.size * 37 + 13)).take(12) }
    var index by remember(deck) { mutableStateOf(0) }
    var food by remember(deck) { mutableStateOf(0) }
    var feedback by remember(deck) { mutableStateOf("The cat is waiting for the right answer.") }
    val current = deck[index.coerceIn(0, deck.lastIndex)]
    val transition = rememberInfiniteTransition(label = "feedCat")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1350),
            repeatMode = RepeatMode.Restart
        ),
        label = "feedCatPulse"
    )
    val choices = remember(current, deck) {
        (listOf(current.back) + deck
            .filterNot { it.id == current.id }
            .map { it.back }
            .distinct()
            .shuffled(Random(current.id + 41))
            .take(3))
            .distinct()
            .shuffled(Random(current.front.hashCode()))
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(178.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val bowlY = size.height * 0.78f
                val bowlStart = size.width * 0.24f
                val bowlEnd = size.width * 0.76f
                drawLine(
                    color = BrandSaladColor.copy(alpha = 0.74f),
                    start = Offset(bowlStart, bowlY),
                    end = Offset(bowlEnd, bowlY),
                    strokeWidth = 22f
                )
                drawLine(
                    color = Color(0xFF25382F).copy(alpha = 0.26f),
                    start = Offset(bowlStart + 16f, bowlY + 18f),
                    end = Offset(bowlEnd - 16f, bowlY + 18f),
                    strokeWidth = 8f
                )
                val foodCount = (food + 1).coerceAtMost(12)
                repeat(foodCount) { item ->
                    val x = bowlStart + 24f + (item % 6) * ((bowlEnd - bowlStart - 48f) / 5f)
                    val y = bowlY - 16f - (item / 6) * 18f
                    drawCircle(
                        color = Color(0xFFFFC66D).copy(alpha = 0.9f),
                        radius = 8f + (item % 2) * 2f,
                        center = Offset(x, y)
                    )
                }
                repeat(7) { item ->
                    val x = size.width * (0.16f + item * 0.11f)
                    val y = ((pulse + item * 0.17f) % 1f) * size.height * 0.72f
                    drawCircle(
                        color = Color(0xFFFFC66D).copy(alpha = 0.22f + 0.38f * (1f - y / size.height)),
                        radius = 5f + (item % 3) * 2f,
                        center = Offset(x, y)
                    )
                }
            }
            ProfessorCatMark(
                progress = 1f,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(92.dp)
                    .graphicsLayer {
                        val scale = 1f + 0.06f * sin((pulse * Math.PI * 2).toFloat())
                        scaleX = scale
                        scaleY = scale
                        translationY = -4f * sin((pulse * Math.PI * 2).toFloat())
                    }
            )
            Text(
                text = if (food >= deck.size) "Full and happy" else "Feed me the right answer",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LinearProgressIndicator(
            progress = { (food.toFloat() / deck.size.toFloat()).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth()
        )
        Text("${index + 1} / ${deck.size}  Food: $food", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFFFE4EC),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                current.front,
                modifier = Modifier.padding(14.dp),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
        choices.forEach { choice ->
            OutlinedButton(
                onClick = {
                    val correct = normalizeAnswerText(choice) == normalizeAnswerText(current.back)
                    if (correct) {
                        food = (food + 1).coerceAtMost(deck.size)
                        feedback = "Good. The bowl is happier."
                    } else {
                        feedback = "Not food yet: ${current.back}"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(choice, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
        Text(feedback, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(
            onClick = {
                if (index < deck.lastIndex) {
                    index += 1
                } else {
                    index = 0
                    food = 0
                }
                feedback = "The cat is waiting for the right answer."
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(if (index < deck.lastIndex) "Next bite" else "Restart feast")
        }
    }
}

@Composable
private fun GingerWordGardenGame(cards: List<LanguageGameEntry>) {
    val deck = remember(cards) { cards.shuffled(Random(cards.size * 43 + 17)).take(16) }
    var index by remember(deck) { mutableStateOf(0) }
    var score by remember(deck) { mutableStateOf(0) }
    var feedback by remember(deck) { mutableStateOf("Pick the matching flower for the ginger cat.") }
    val current = deck[index.coerceIn(0, deck.lastIndex)]
    val choices = remember(current, deck) {
        (listOf(current.back) + deck
            .filterNot { it.id == current.id }
            .map { it.back }
            .distinct()
            .shuffled(Random(current.id + 53))
            .take(3))
            .distinct()
            .shuffled(Random(current.front.hashCode() + 19))
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFFFF7E8),
            border = BorderStroke(1.dp, Color(0xFFFFB45F).copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(12.dp)
            ) {
                GingerFluffyCatMark(modifier = Modifier.size(78.dp))
                Column(verticalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.weight(1f)) {
                    Text("Compose UI prototype", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(current.front, fontWeight = FontWeight.SemiBold)
                    Text("${index + 1} / ${deck.size}  Score: $score", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        choices.forEachIndexed { choiceIndex, choice ->
            OutlinedButton(
                onClick = {
                    val correct = normalizeAnswerText(choice) == normalizeAnswerText(current.back)
                    if (correct) {
                        score += 1
                        feedback = "The ginger cat saved this flower."
                    } else {
                        feedback = "Try again. Answer: ${current.back}"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = listOf(Color(0xFFFFF2D6), Color(0xFFE9F7EF), Color(0xFFFFEAF0), Color(0xFFEAF1FF))[choiceIndex % 4]
                )
            ) {
                Text(choice, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
        Text(feedback, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(
            onClick = {
                if (index < deck.lastIndex) index += 1 else index = 0
                feedback = "Pick the matching flower for the ginger cat."
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(if (index < deck.lastIndex) "Next flower" else "Restart garden")
        }
    }
}

@Composable
private fun GingerCanvasPounceGame(cards: List<LanguageGameEntry>) {
    val deck = remember(cards) { cards.shuffled(Random(cards.size * 47 + 21)).take(18) }
    var index by remember(deck) { mutableStateOf(0) }
    var score by remember(deck) { mutableStateOf(0) }
    var feedback by remember(deck) { mutableStateOf("Tap the correct yarn ball.") }
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    val current = deck[index.coerceIn(0, deck.lastIndex)]
    val transition = rememberInfiniteTransition(label = "gingerCanvas")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800),
            repeatMode = RepeatMode.Restart
        ),
        label = "gingerCanvasTime"
    )
    val choices = remember(current, deck) {
        (listOf(current.back) + deck
            .filterNot { it.id == current.id }
            .map { it.back }
            .distinct()
            .shuffled(Random(current.id + 67))
            .take(2))
            .distinct()
            .shuffled(Random(current.back.hashCode() + 31))
    }
    fun tokenCenter(indexInChoices: Int, size: Size): Offset {
        val lanes = listOf(0.24f, 0.5f, 0.76f)
        val x = size.width * lanes[indexInChoices.coerceIn(0, lanes.lastIndex)]
        val wave = sin(((t + indexInChoices * 0.19f) * Math.PI * 2).toFloat())
        val y = size.height * (0.34f + indexInChoices * 0.08f) + wave * size.height * 0.04f
        return Offset(x, y)
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Canvas prototype: ${index + 1} / ${deck.size}  Score: $score", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFFFF7E8),
            border = BorderStroke(1.dp, Color(0xFFFFB45F).copy(alpha = 0.42f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(current.front, modifier = Modifier.padding(12.dp), fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .onGloballyPositioned { canvasSize = Size(it.size.width.toFloat(), it.size.height.toFloat()) }
                .pointerInput(choices, current.id, canvasSize) {
                    detectTapGestures { tap ->
                        val radius = (canvasSize.width * 0.115f).coerceIn(40f, 68f)
                        choices.forEachIndexed { choiceIndex, choice ->
                            val center = tokenCenter(choiceIndex, canvasSize)
                            val dx = tap.x - center.x
                            val dy = tap.y - center.y
                            if (dx * dx + dy * dy <= radius * radius) {
                                val correct = normalizeAnswerText(choice) == normalizeAnswerText(current.back)
                                if (correct) {
                                    score += 1
                                    feedback = "Pounce! Correct."
                                    if (index < deck.lastIndex) index += 1 else index = 0
                                } else {
                                    feedback = "Soft miss. Answer: ${current.back}"
                                }
                            }
                        }
                    }
                }
        ) {
            drawRect(Color(0xFFEAF7F0))
            repeat(12) { blade ->
                val x = size.width * (blade + 1) / 13f
                drawLine(
                    color = BrandSaladColor.copy(alpha = 0.22f),
                    start = Offset(x, size.height),
                    end = Offset(x + sin((t + blade) * 2f) * 10f, size.height * 0.76f),
                    strokeWidth = 4f
                )
            }
            drawGingerFluffyCat(
                center = Offset(size.width * 0.5f, size.height * 0.76f),
                bodyRadius = size.minDimension * 0.18f,
                bob = sin((t * Math.PI * 2).toFloat()) * 6f
            )
            val radius = (size.width * 0.115f).coerceIn(40f, 68f)
            choices.forEachIndexed { choiceIndex, choice ->
                val center = tokenCenter(choiceIndex, size)
                drawLine(
                    color = Color(0xFFD78842).copy(alpha = 0.45f),
                    start = Offset(center.x, center.y - radius * 1.2f),
                    end = Offset(center.x, center.y - radius * 0.35f),
                    strokeWidth = 3f
                )
                drawCircle(Color(0xFFFFB45F), radius, center)
                drawCircle(Color(0xFF6A3B1D).copy(alpha = 0.28f), radius, center, style = Stroke(width = 4f))
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        color = android.graphics.Color.rgb(38, 44, 40)
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = radius * 0.26f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }
                    val label = choice.shortGameText()
                    val textY = center.y - (paint.descent() + paint.ascent()) / 2f
                    drawText(label, center.x, textY, paint)
                }
            }
        }
        Text(feedback, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Suppress("SetJavaScriptEnabled")
@Composable
private fun GingerHtml5FlashGame(cards: List<LanguageGameEntry>) {
    val gameCardsJson = remember(cards) {
        JSONArray().apply {
            cards.take(24).forEach { card ->
                put(JSONObject().apply {
                    put("front", card.front)
                    put("back", card.back)
                })
            }
        }.toString()
    }
    val html = remember(gameCardsJson) { gingerHtml5GameDocument(gameCardsJson) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("HTML5 Canvas prototype inside Android WebView", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Surface(
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFFFFB45F).copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth().height(420.dp)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        webViewClient = WebViewClient()
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = false
                        loadDataWithBaseURL("https://murrlex.local/", html, "text/html", "UTF-8", null)
                    }
                },
                update = { webView ->
                    val marker = html.hashCode()
                    if (webView.getTag() != marker) {
                        webView.setTag(marker)
                        webView.loadDataWithBaseURL("https://murrlex.local/", html, "text/html", "UTF-8", null)
                    }
                }
            )
        }
    }
}

@Composable
private fun Pseudo3DCatWalkScreen(
    cards: List<LanguageGameEntry>,
    onDismiss: () -> Unit
) {
    val fallbackDeck = remember {
        listOf(
            LanguageGameEntry(1, "cat", "kot"),
            LanguageGameEntry(2, "walk", "spacer"),
            LanguageGameEntry(3, "food", "jedzenie")
        )
    }
    val deck = remember(cards) {
        cards.ifEmpty { fallbackDeck }.shuffled(Random(cards.size * 59 + 29)).take(24)
    }
    var index by remember(deck) { mutableStateOf(0) }
    var lane by remember(deck) { mutableStateOf(1) }
    var depth by remember(deck) { mutableStateOf(0f) }
    var score by remember(deck) { mutableStateOf(0) }
    var lives by remember(deck) { mutableStateOf(3) }
    var message by remember(deck) { mutableStateOf("Нажми Задание, чтобы открыть единственную карточку.") }
    var activeTaskIndex by remember(deck) { mutableStateOf<Int?>(null) }
    var taskUsed by remember(deck) { mutableStateOf(false) }
    val current = activeTaskIndex?.let { deck[it % deck.size] }
    val choices = remember(current, deck) {
        val task = current ?: return@remember emptyList()
        (listOf(task.back) + deck
            .filterNot { it.id == task.id }
            .map { it.back }
            .distinct()
            .shuffled(Random(task.id + 83))
            .take(2))
            .distinct()
            .shuffled(Random(task.front.hashCode() + 43))
            .let { options ->
                if (options.size >= 3) options.take(3) else options + List(3 - options.size) { task.back }
            }
    }
    val transition = rememberInfiniteTransition(label = "pseudo3d")
    val idle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400),
            repeatMode = RepeatMode.Restart
        ),
        label = "pseudo3dIdle"
    )
    LaunchedEffect(depth) {
        if (depth > 0f) {
            while (depth > 0f) {
                delay(16L)
                depth = (depth - 0.035f).coerceAtLeast(0f)
            }
        }
    }
    fun startTask() {
        if (taskUsed || activeTaskIndex != null) {
            message = if (activeTaskIndex != null) "Задание уже открыто." else "Задание уже использовано."
            return
        }
        activeTaskIndex = index % deck.size
        taskUsed = true
        message = deck[index % deck.size].front
    }
    fun advance() {
        val task = current
        if (task == null) {
            depth = 1f
            message = "Задание появляется только по кнопке."
            return
        }
        val selected = choices.getOrNull(lane).orEmpty()
        val correct = normalizeAnswerText(selected) == normalizeAnswerText(task.back)
        depth = 1f
        if (correct) {
            score += 1
            lives += 1
            message = "Задание выполнено: +1 жизнь."
            index = if (index < deck.lastIndex) index + 1 else 0
        } else {
            message = "Ответ: ${task.back}"
        }
        activeTaskIndex = null
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            GingerFluffyCatMark(modifier = Modifier.size(58.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Pseudo 3D native Canvas experiment", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(current?.front ?: message, fontWeight = FontWeight.Bold, fontSize = 22.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("Score $score   Lives $lives   Task ${if (taskUsed) "0" else "1"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF11251F),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(choices, lane, current?.id) {
                        detectTapGestures { tap ->
                            val third = size.width / 3f
                            lane = when {
                                tap.x < third -> 0
                                tap.x > third * 2f -> 2
                                else -> 1
                            }
                            if (tap.y < size.height * 0.72f) advance()
                        }
                    }
            ) {
                val w = size.width
                val h = size.height
                val horizon = h * 0.28f
                val roadTop = w * 0.17f
                val roadBottom = w * 0.92f
                drawRect(Color(0xFF15382F))
                drawRect(Color(0xFFBFE7E1), topLeft = Offset(0f, 0f), size = Size(w, horizon))
                repeat(14) { star ->
                    val x = w * ((star * 37 % 100) / 100f)
                    val y = horizon * ((star * 19 % 80) / 100f)
                    drawCircle(Color.White.copy(alpha = 0.55f), 2.5f + (star % 3), Offset(x, y))
                }
                val road = Path().apply {
                    moveTo(w * 0.5f - roadTop, horizon)
                    lineTo(w * 0.5f + roadTop, horizon)
                    lineTo(w * 0.5f + roadBottom, h)
                    lineTo(w * 0.5f - roadBottom, h)
                    close()
                }
                drawPath(road, Color(0xFF2F423A))
                drawPath(road, Color(0xFFFFC66D).copy(alpha = 0.38f), style = Stroke(width = 5f))
                repeat(9) { row ->
                    val t = ((row / 8f) + depth * 0.18f) % 1f
                    val y = horizon + (h - horizon) * t
                    val spread = roadTop + (roadBottom - roadTop) * t
                    drawLine(
                        color = Color.White.copy(alpha = 0.12f + t * 0.25f),
                        start = Offset(w * 0.5f - spread, y),
                        end = Offset(w * 0.5f + spread, y),
                        strokeWidth = 1f + t * 5f
                    )
                }
                listOf(1f / 3f, 2f / 3f).forEach { split ->
                    val xTop = w * 0.5f + (split - 0.5f) * roadTop * 2f
                    val xBottom = w * 0.5f + (split - 0.5f) * roadBottom * 2f
                    drawLine(
                        color = Color.White.copy(alpha = 0.22f),
                        start = Offset(xTop, horizon),
                        end = Offset(xBottom, h),
                        strokeWidth = 3f
                    )
                }
                choices.forEachIndexed { choiceIndex, choice ->
                    val gateT = (0.34f + choiceIndex * 0.12f + depth * 0.42f).coerceIn(0f, 0.94f)
                    val y = horizon + (h - horizon) * gateT
                    val spread = roadTop + (roadBottom - roadTop) * gateT
                    val laneCenter = w * 0.5f + (choiceIndex - 1) * (spread * 0.53f)
                    val gateW = (w * (0.14f + gateT * 0.22f)).coerceAtMost(w * 0.34f)
                    val gateH = h * (0.09f + gateT * 0.09f)
                    val selected = choiceIndex == lane
                    val gateColor = if (selected) Color(0xFFFFB45F) else Color(0xFFEAF7F0)
                    drawRoundRect(
                        color = gateColor,
                        topLeft = Offset(laneCenter - gateW / 2f, y - gateH / 2f),
                        size = Size(gateW, gateH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f)
                    )
                    drawRoundRect(
                        color = Color(0xFF5C3218).copy(alpha = if (selected) 0.72f else 0.38f),
                        topLeft = Offset(laneCenter - gateW / 2f, y - gateH / 2f),
                        size = Size(gateW, gateH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f),
                        style = Stroke(width = if (selected) 6f else 3f)
                    )
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            color = android.graphics.Color.rgb(38, 44, 40)
                            textAlign = android.graphics.Paint.Align.CENTER
                            textSize = (gateH * 0.26f).coerceIn(18f, 32f)
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        }
                        drawText(choice.shortGameText(), laneCenter, y - (paint.descent() + paint.ascent()) / 2f, paint)
                    }
                }
                val catX = w * (0.27f + lane * 0.23f)
                drawGingerFluffyCat(
                    center = Offset(catX, h * 0.84f),
                    bodyRadius = (w * 0.095f).coerceIn(42f, 76f),
                    bob = sin((idle * Math.PI * 2).toFloat()) * 8f - depth * 18f
                )
            }
        }
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { lane = (lane - 1).coerceAtLeast(0) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Left") }
            Button(
                onClick = { advance() },
                modifier = Modifier.weight(1.25f),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Forward") }
            OutlinedButton(
                onClick = { lane = (lane + 1).coerceAtMost(2) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Right") }
        }
        Button(
            onClick = { startTask() },
            enabled = !taskUsed && activeTaskIndex == null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) { Text("Задание") }
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Close game")
        }
    }
}

private data class PixelPlatform(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

@Composable
private fun EightBitGingerPlatformerScreen(
    cards: List<LanguageGameEntry>,
    onDismiss: () -> Unit
) {
    val fallbackDeck = remember {
        listOf(
            LanguageGameEntry(1, "cat", "kot"),
            LanguageGameEntry(2, "jump", "skok"),
            LanguageGameEntry(3, "lesson", "lekcja")
        )
    }
    val deck = remember(cards) {
        cards.ifEmpty { fallbackDeck }.shuffled(Random(cards.size * 71 + 37)).take(18)
    }
    val platforms = remember {
        listOf(
            PixelPlatform(0f, 304f, 2200f, 28f),
            PixelPlatform(170f, 238f, 120f, 16f),
            PixelPlatform(370f, 204f, 130f, 16f),
            PixelPlatform(590f, 246f, 120f, 16f),
            PixelPlatform(820f, 188f, 150f, 16f),
            PixelPlatform(1110f, 230f, 130f, 16f),
            PixelPlatform(1420f, 202f, 160f, 16f)
        )
    }
    var playerX by remember(deck) { mutableStateOf(42f) }
    var playerY by remember(deck) { mutableStateOf(280f) }
    var velocityY by remember(deck) { mutableStateOf(0f) }
    var moveDir by remember(deck) { mutableStateOf(0) }
    var facingRight by remember(deck) { mutableStateOf(true) }
    var grounded by remember(deck) { mutableStateOf(false) }
    var score by remember(deck) { mutableStateOf(0) }
    var lives by remember(deck) { mutableStateOf(3) }
    var message by remember(deck) { mutableStateOf("Нажми Задание, чтобы открыть единственную карточку.") }
    var activeQuestionIndex by remember(deck) { mutableStateOf<Int?>(null) }
    var solvedBlocks by remember(deck) { mutableStateOf<Set<Int>>(emptySet()) }
    var taskUsed by remember(deck) { mutableStateOf(false) }
    val playerW = 18f
    val playerH = 25f
    val levelWidth = 1780f
    val questionBlocks = remember(deck) {
        deck.mapIndexed { index, card ->
            val x = 220f + index * 92f
            val y = when (index % 4) {
                0 -> 204f
                1 -> 170f
                2 -> 212f
                else -> 154f
            }
            index to Triple(x, y, card)
        }
    }
    val activeQuestion = activeQuestionIndex?.let { deck[it % deck.size] }
    val activeChoices = remember(activeQuestion, deck) {
        val current = activeQuestion ?: return@remember emptyList()
        (listOf(current.back) + deck
            .filterNot { it.id == current.id }
            .map { it.back }
            .distinct()
            .shuffled(Random(current.id + 97))
            .take(2))
            .distinct()
            .shuffled(Random(current.back.hashCode() + 61))
            .let { options -> if (options.size >= 3) options.take(3) else options + List(3 - options.size) { current.back } }
    }
    fun resetPlayer(hurt: Boolean = false) {
        playerX = 42f
        playerY = 280f
        velocityY = 0f
        grounded = false
        if (hurt) {
            lives = (lives - 1).coerceAtLeast(0)
            message = if (lives <= 1) "Tiny rewind. Try again." else "Ouch. Back to start."
        }
    }
    fun answerQuestion(choice: String) {
        val questionIndex = activeQuestionIndex ?: return
        val current = deck[questionIndex % deck.size]
        val correct = normalizeAnswerText(choice) == normalizeAnswerText(current.back)
        if (correct) {
            solvedBlocks = solvedBlocks + questionIndex
            score += 1
            lives += 1
            message = "Задание выполнено: +1 жизнь."
            activeQuestionIndex = null
        } else {
            message = "Ответ: ${current.back}"
            activeQuestionIndex = null
        }
    }
    fun startTask() {
        if (taskUsed || activeQuestionIndex != null) {
            message = if (activeQuestionIndex != null) "Задание уже открыто." else "Задание уже использовано."
            return
        }
        val nextIndex = (score + lives) % deck.size
        activeQuestionIndex = nextIndex
        taskUsed = true
        message = deck[nextIndex].front
    }
    LaunchedEffect(deck, moveDir, activeQuestionIndex) {
        while (true) {
            delay(16L)
            if (activeQuestionIndex != null) continue
            val prevY = playerY
            if (moveDir != 0) {
                facingRight = moveDir > 0
            }
            playerX = (playerX + moveDir * 2.55f).coerceIn(0f, levelWidth)
            velocityY = (velocityY + 0.42f).coerceAtMost(8f)
            playerY += velocityY
            grounded = false
            platforms.forEach { platform ->
                val horizontal = playerX + playerW > platform.x && playerX < platform.x + platform.width
                val wasAbove = prevY + playerH <= platform.y + 2f
                val nowOn = playerY + playerH >= platform.y && playerY + playerH <= platform.y + platform.height + 12f
                if (horizontal && wasAbove && nowOn && velocityY >= 0f) {
                    playerY = platform.y - playerH
                    velocityY = 0f
                    grounded = true
                }
            }
            if (playerY > 360f) {
                resetPlayer(hurt = true)
            }
        }
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GingerPixelCatMark(modifier = Modifier.size(44.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("8-bit Ginger Platformer", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("Score $score   Lives $lives", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF5BA7D1),
            border = BorderStroke(2.dp, Color(0xFF25382F)),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val scale = (size.height / 340f).coerceAtLeast(1f)
                val cameraX = (playerX - size.width / scale * 0.34f).coerceIn(0f, (levelWidth - size.width / scale).coerceAtLeast(0f))
                fun sx(worldX: Float) = (worldX - cameraX) * scale
                fun sy(worldY: Float) = worldY * scale
                drawRect(Color(0xFF75C7EF))
                repeat(5) { cloud ->
                    val x = sx(70f + cloud * 360f)
                    val y = sy(40f + (cloud % 2) * 24f)
                    drawRect(Color.White.copy(alpha = 0.82f), Offset(x, y), Size(34f * scale, 12f * scale))
                    drawRect(Color.White.copy(alpha = 0.82f), Offset(x + 12f * scale, y - 10f * scale), Size(34f * scale, 18f * scale))
                }
                platforms.forEach { platform ->
                    val topLeft = Offset(sx(platform.x), sy(platform.y))
                    val sizePx = Size(platform.width * scale, platform.height * scale)
                    drawRect(Color(0xFF7A4A28), topLeft, sizePx)
                    drawRect(Color(0xFFFFB45F), topLeft, Size(sizePx.width, 5f * scale))
                    val brickW = 18f * scale
                    var brickX = topLeft.x
                    while (brickX < topLeft.x + sizePx.width) {
                        drawRect(Color(0xFF5C3218).copy(alpha = 0.28f), Offset(brickX, topLeft.y), Size(1.5f * scale, sizePx.height))
                        brickX += brickW
                    }
                }
                questionBlocks.forEach { (blockIndex, blockData) ->
                    val blockX = blockData.first
                    val blockY = blockData.second
                    val solved = blockIndex in solvedBlocks
                    val x = sx(blockX)
                    val y = sy(blockY)
                    val blockSize = 28f * scale
                    drawRect(if (solved) Color(0xFF72B77D) else Color(0xFFFFC247), Offset(x, y), Size(blockSize, blockSize))
                    drawRect(Color(0xFF5C3218), Offset(x, y), Size(blockSize, 3f * scale))
                    drawRect(Color(0xFF5C3218), Offset(x, y), Size(3f * scale, blockSize))
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            isAntiAlias = false
                            color = android.graphics.Color.rgb(92, 50, 24)
                            textAlign = android.graphics.Paint.Align.CENTER
                            textSize = 19f * scale
                            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                        }
                        drawText(if (solved) "*" else "?", x + blockSize / 2f, y + blockSize * 0.72f, paint)
                    }
                }
                repeat(10) { hill ->
                    val x = sx(120f + hill * 210f)
                    val base = sy(304f)
                    drawRect(Color(0xFF4D9A68).copy(alpha = 0.55f), Offset(x, base - 32f * scale), Size(70f * scale, 32f * scale))
                    drawRect(Color(0xFF36784E).copy(alpha = 0.55f), Offset(x + 18f * scale, base - 52f * scale), Size(34f * scale, 52f * scale))
                }
                drawPixelGingerCat(
                    topLeft = Offset(sx(playerX), sy(playerY)),
                    pixel = (3.2f * scale).coerceIn(5f, 10f),
                    facingRight = facingRight
                )
            }
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFFFF7E8),
            border = BorderStroke(1.dp, Color(0xFFFFB45F).copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(activeQuestion?.front ?: message, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (activeQuestion != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        activeChoices.forEach { choice ->
                            OutlinedButton(
                                onClick = { answerQuestion(choice) },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(choice.shortGameText(), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { moveDir = -1 },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Left") }
            Button(
                onClick = {
                    if (grounded) {
                        velocityY = -8.2f
                        grounded = false
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Jump") }
            OutlinedButton(
                onClick = { moveDir = 1 },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Right") }
            TextButton(
                onClick = { moveDir = 0 },
                modifier = Modifier.weight(0.8f)
            ) { Text("Stop") }
        }
        Button(
            onClick = { startTask() },
            enabled = !taskUsed && activeQuestionIndex == null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) { Text("Задание") }
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Close game")
        }
    }
}

@Composable
private fun GodotRunnerLabScreen(
    cards: List<LanguageGameEntry>,
    onDismiss: () -> Unit
) {
    val fallbackDeck = remember {
        listOf(
            LanguageGameEntry(1, "move", "ruch"),
            LanguageGameEntry(2, "life", "zycie"),
            LanguageGameEntry(3, "task", "zadanie"),
            LanguageGameEntry(4, "path", "sciezka")
        )
    }
    val deck = remember(cards) {
        cards.ifEmpty { fallbackDeck }.shuffled(Random(cards.size * 89 + 53)).take(24)
    }
    var playerX by remember(deck) { mutableStateOf(0.42f) }
    var playerY by remember(deck) { mutableStateOf(0.68f) }
    var facingRight by remember(deck) { mutableStateOf(true) }
    var lives by remember(deck) { mutableStateOf(3) }
    var score by remember(deck) { mutableStateOf(0) }
    var taskUsed by remember(deck) { mutableStateOf(false) }
    var activeTaskIndex by remember(deck) { mutableStateOf<Int?>(null) }
    var message by remember(deck) { mutableStateOf("Godot-style scene loop. Нажми Задание для +1 жизни.") }
    val activeTask = activeTaskIndex?.let { deck[it % deck.size] }
    val choices = remember(activeTask, deck) {
        val task = activeTask ?: return@remember emptyList()
        (listOf(task.back) + deck
            .filterNot { it.id == task.id }
            .map { it.back }
            .distinct()
            .shuffled(Random(task.id + 151))
            .take(3))
            .distinct()
            .shuffled(Random(task.front.hashCode() + 127))
            .let { options -> if (options.size >= 4) options.take(4) else options + List(4 - options.size) { task.back } }
    }
    val transition = rememberInfiniteTransition(label = "godotRunnerLab")
    val clock by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1800), repeatMode = RepeatMode.Restart),
        label = "godotRunnerClock"
    )
    fun move(dx: Float, dy: Float) {
        playerX = (playerX + dx).coerceIn(0.10f, 0.90f)
        playerY = (playerY + dy).coerceIn(0.30f, 0.82f)
        if (dx != 0f) facingRight = dx > 0f
        message = "Move vector: ${"%.1f".format(dx)}, ${"%.1f".format(dy)}"
    }
    fun startTask() {
        if (taskUsed || activeTaskIndex != null) {
            message = if (activeTaskIndex != null) "Задание уже открыто." else "Задание уже использовано."
            return
        }
        val nextIndex = (score + lives) % deck.size
        activeTaskIndex = nextIndex
        taskUsed = true
        message = deck[nextIndex].front
    }
    fun answerTask(choice: String) {
        val task = activeTask ?: return
        val correct = normalizeAnswerText(choice) == normalizeAnswerText(task.back)
        if (correct) {
            lives += 1
            score += 1
            message = "Задание выполнено: +1 жизнь."
        } else {
            message = "Ответ: ${task.back}"
        }
        activeTaskIndex = null
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GingerPixelCatMark(modifier = Modifier.size(46.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Godot Runner Lab", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("Lives $lives   Score $score   Task ${if (taskUsed) "0" else "1"}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF17251E),
            border = BorderStroke(2.dp, Color(0xFF25382F)),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawRect(Color(0xFF223A37))
                repeat(7) { layer ->
                    val y = h * (0.24f + layer * 0.065f)
                    drawLine(
                        color = Color(0xFF8FDCC4).copy(alpha = 0.08f + layer * 0.025f),
                        start = Offset(0f, y),
                        end = Offset(w, y + sin(clock * 6.28f + layer) * 6f),
                        strokeWidth = 2f + layer
                    )
                }
                val path = Path().apply {
                    moveTo(w * 0.10f, h * 0.88f)
                    cubicTo(w * 0.25f, h * 0.72f, w * 0.25f, h * 0.52f, w * 0.46f, h * 0.50f)
                    cubicTo(w * 0.70f, h * 0.48f, w * 0.68f, h * 0.32f, w * 0.90f, h * 0.24f)
                }
                drawPath(path, Color(0xFFFFC66D).copy(alpha = 0.26f), style = Stroke(width = 36f))
                drawPath(path, Color(0xFF2F7A62), style = Stroke(width = 20f))
                repeat(14) { node ->
                    val x = w * (0.10f + (node % 7) * 0.13f)
                    val y = h * (0.82f - (node / 7) * 0.46f + sin(node + clock * 6.28f) * 0.012f)
                    drawCircle(Color(0xFF8FDCC4).copy(alpha = 0.55f), 7f, Offset(x, y))
                }
                val px = w * playerX
                val py = h * playerY + sin(clock * 6.28f) * 4f
                drawCircle(Color(0xFF263B35).copy(alpha = 0.28f), 34f, Offset(px + 3f, py + 31f))
                drawPixelGingerCat(
                    topLeft = Offset(px - 22f, py - 38f),
                    pixel = 7f,
                    facingRight = facingRight
                )
                activeTask?.let { task ->
                    drawRoundRect(
                        color = Color(0xFFFFF7E8),
                        topLeft = Offset(w * 0.12f, h * 0.06f),
                        size = Size(w * 0.76f, h * 0.18f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f)
                    )
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            color = android.graphics.Color.rgb(38, 56, 47)
                            textAlign = android.graphics.Paint.Align.CENTER
                            textSize = 28f
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        }
                        drawText(task.front.take(32), w * 0.50f, h * 0.16f, paint)
                    }
                }
            }
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFFFF7E8),
            border = BorderStroke(1.dp, Color(0xFFFFB45F).copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(activeTask?.front ?: message, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (activeTask != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        choices.forEach { choice ->
                            OutlinedButton(
                                onClick = { answerTask(choice) },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(choice.shortGameText(), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { move(-0.07f, 0f) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("Left") }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = { move(0f, -0.07f) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text("Up") }
                OutlinedButton(onClick = { move(0f, 0.07f) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text("Down") }
            }
            OutlinedButton(onClick = { move(0.07f, 0f) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("Right") }
        }
        Button(
            onClick = { startTask() },
            enabled = !taskUsed && activeTaskIndex == null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) { Text("Задание") }
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Close game")
        }
    }
}

@Composable
private fun LottieRunnerLabScreen(
    cards: List<LanguageGameEntry>,
    onDismiss: () -> Unit
) {
    val fallbackDeck = remember {
        listOf(
            LanguageGameEntry(1, "animation", "animacja"),
            LanguageGameEntry(2, "life", "zycie"),
            LanguageGameEntry(3, "move", "ruch"),
            LanguageGameEntry(4, "answer", "odpowiedz")
        )
    }
    val deck = remember(cards) {
        cards.ifEmpty { fallbackDeck }.shuffled(Random(cards.size * 97 + 67)).take(24)
    }
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.lottie_ginger_cat))
    val lottieProgress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )
    var playerX by remember(deck) { mutableStateOf(0.46f) }
    var playerY by remember(deck) { mutableStateOf(0.62f) }
    var facingRight by remember(deck) { mutableStateOf(true) }
    var lives by remember(deck) { mutableStateOf(3) }
    var score by remember(deck) { mutableStateOf(0) }
    var taskUsed by remember(deck) { mutableStateOf(false) }
    var activeTaskIndex by remember(deck) { mutableStateOf<Int?>(null) }
    var message by remember(deck) { mutableStateOf("Lottie-анимация + Compose game state. Нажми Задание.") }
    val activeTask = activeTaskIndex?.let { deck[it % deck.size] }
    val choices = remember(activeTask, deck) {
        val task = activeTask ?: return@remember emptyList()
        (listOf(task.back) + deck
            .filterNot { it.id == task.id }
            .map { it.back }
            .distinct()
            .shuffled(Random(task.id + 173))
            .take(3))
            .distinct()
            .shuffled(Random(task.front.hashCode() + 193))
            .let { options -> if (options.size >= 4) options.take(4) else options + List(4 - options.size) { task.back } }
    }
    val transition = rememberInfiniteTransition(label = "lottieRunnerLab")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1400), repeatMode = RepeatMode.Restart),
        label = "lottieRunnerPulse"
    )
    fun move(dx: Float, dy: Float) {
        playerX = (playerX + dx).coerceIn(0.12f, 0.88f)
        playerY = (playerY + dy).coerceIn(0.28f, 0.80f)
        if (dx != 0f) facingRight = dx > 0f
        message = "Compose moved the Lottie actor."
    }
    fun startTask() {
        if (taskUsed || activeTaskIndex != null) {
            message = if (activeTaskIndex != null) "Задание уже открыто." else "Задание уже использовано."
            return
        }
        val nextIndex = (score + lives + (playerX * 10).toInt()) % deck.size
        activeTaskIndex = nextIndex
        taskUsed = true
        message = deck[nextIndex].front
    }
    fun answerTask(choice: String) {
        val task = activeTask ?: return
        val correct = normalizeAnswerText(choice) == normalizeAnswerText(task.back)
        if (correct) {
            lives += 1
            score += 1
            message = "Задание выполнено: +1 жизнь."
        } else {
            message = "Ответ: ${task.back}"
        }
        activeTaskIndex = null
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(modifier = Modifier.size(46.dp)) {
                LottieAnimation(
                    composition = composition,
                    progress = { lottieProgress },
                    modifier = Modifier.fillMaxSize()
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Lottie Runner Lab", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("Lives $lives   Score $score   Task ${if (taskUsed) "0" else "1"}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF182B35),
            border = BorderStroke(2.dp, Color(0xFF25382F)),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    drawRect(Color(0xFF183540))
                    repeat(8) { lane ->
                        val y = h * (0.20f + lane * 0.085f)
                        drawLine(
                            color = Color(0xFF8FDCC4).copy(alpha = 0.10f + lane * 0.018f),
                            start = Offset(0f, y),
                            end = Offset(w, y + sin(pulse * 6.28f + lane) * 10f),
                            strokeWidth = 2f + lane
                        )
                    }
                    repeat(10) { step ->
                        val x = w * (0.10f + step * 0.088f)
                        val y = h * (0.78f - sin(step * 0.72f + pulse * 6.28f) * 0.05f)
                        drawCircle(Color(0xFFFFC66D).copy(alpha = 0.30f), 28f, Offset(x, y))
                        drawCircle(Color(0xFF2F7A62), 12f, Offset(x, y))
                    }
                    activeTask?.let {
                        drawRoundRect(
                            color = Color(0xFFFFF7E8),
                            topLeft = Offset(w * 0.10f, h * 0.06f),
                            size = Size(w * 0.80f, h * 0.17f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f)
                        )
                        drawContext.canvas.nativeCanvas.apply {
                            val paint = android.graphics.Paint().apply {
                                isAntiAlias = true
                                color = android.graphics.Color.rgb(38, 56, 47)
                                textAlign = android.graphics.Paint.Align.CENTER
                                textSize = 28f
                                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                            }
                            drawText(it.front.take(32), w * 0.50f, h * 0.16f, paint)
                        }
                    }
                }
                LottieAnimation(
                    composition = composition,
                    progress = { lottieProgress },
                    modifier = Modifier
                        .size(132.dp)
                        .align(Alignment.TopStart)
                        .offset(
                            x = maxWidth * playerX - 66.dp,
                            y = maxHeight * playerY - 66.dp
                        )
                        .graphicsLayer {
                            scaleX = if (facingRight) 1f else -1f
                        }
                )
            }
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFFFF7E8),
            border = BorderStroke(1.dp, Color(0xFFFFB45F).copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(activeTask?.front ?: message, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (activeTask != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        choices.forEach { choice ->
                            OutlinedButton(
                                onClick = { answerTask(choice) },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(choice.shortGameText(), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { move(-0.07f, 0f) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("Left") }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = { move(0f, -0.07f) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text("Up") }
                OutlinedButton(onClick = { move(0f, 0.07f) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text("Down") }
            }
            OutlinedButton(onClick = { move(0.07f, 0f) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("Right") }
        }
        Button(
            onClick = { startTask() },
            enabled = !taskUsed && activeTaskIndex == null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) { Text("Задание") }
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Close game")
        }
    }
}

@Composable
private fun RiveRunnerLabScreen(
    cards: List<LanguageGameEntry>,
    speechLanguageTag: String,
    onSpeak: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val fallbackDeck = remember {
        listOf(
            LanguageGameEntry(1, "spark", "iskra"),
            LanguageGameEntry(2, "bright", "jasny"),
            LanguageGameEntry(3, "motion", "ruch"),
            LanguageGameEntry(4, "life", "zycie")
        )
    }
    val deck = remember(cards) {
        cards.ifEmpty { fallbackDeck }.shuffled(Random(cards.size * 131 + 97)).take(24)
    }
    val questionBlocks = remember(deck) { List(12) { index -> 280f + index * 235f } }
    val opponents = remember(deck) { List(9) { index -> 470f + index * 305f } }
    var worldOffset by remember(deck) { mutableStateOf(0f) }
    var playerAltitude by remember(deck) { mutableStateOf(0f) }
    var velocityY by remember(deck) { mutableStateOf(0f) }
    var running by remember(deck) { mutableStateOf(true) }
    var facingRight by remember(deck) { mutableStateOf(true) }
    var lives by remember(deck) { mutableStateOf(3) }
    var score by remember(deck) { mutableStateOf(0) }
    var hitBlocks by remember(deck) { mutableStateOf<Set<Int>>(emptySet()) }
    var stompedOpponents by remember(deck) { mutableStateOf<Set<Int>>(emptySet()) }
    var activeWordIndex by remember(deck) { mutableStateOf<Int?>(null) }
    var invulnerableTicks by remember(deck) { mutableStateOf(0) }
    var catAction by remember(deck) { mutableStateOf(RiveCatAction.RUN) }
    var actionTicks by remember(deck) { mutableStateOf(0) }
    var boost by remember(deck) { mutableStateOf(false) }
    var message by remember(deck) { mutableStateOf("Run, jump, hit ? blocks, knock words out, land on opponents for lives.") }
    val activeWord = activeWordIndex?.let { deck[it % deck.size] }
    val choices = remember(activeWord, deck) {
        val word = activeWord ?: return@remember emptyList()
        (listOf(word.back) + deck
            .filterNot { it.id == word.id }
            .map { it.back }
            .distinct()
            .shuffled(Random(word.id + 251))
            .take(3))
            .distinct()
            .shuffled(Random(word.front.hashCode() + 269))
            .let { options -> if (options.size >= 4) options.take(4) else options + List(4 - options.size) { word.back } }
    }
    val transition = rememberInfiniteTransition(label = "riveRunnerLab")
    val glow by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1200), repeatMode = RepeatMode.Reverse),
        label = "riveGlow"
    )
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(2600), repeatMode = RepeatMode.Restart),
        label = "riveDrift"
    )
    fun jump() {
        if (playerAltitude <= 1f) {
            velocityY = 13.5f
            playerAltitude = 2f
            catAction = RiveCatAction.SPIN
            actionTicks = 42
            message = "Jump. Aim for ? blocks or land on an opponent."
        }
    }
    fun startCatAction(action: RiveCatAction) {
        catAction = action
        actionTicks = when (action) {
            RiveCatAction.SPIN -> 54
            RiveCatAction.DANCE -> 120
            RiveCatAction.CRAWL -> 100
            RiveCatAction.MEOW -> 72
            RiveCatAction.SCRATCH -> 92
            RiveCatAction.TALK -> 105
            RiveCatAction.STAND -> 80
            RiveCatAction.RUN -> 0
        }
        message = when (action) {
            RiveCatAction.SPIN -> "The cat spins into the next jump."
            RiveCatAction.DANCE -> "The cat dances on the platform."
            RiveCatAction.CRAWL -> "The cat crawls under the lesson tunnel."
            RiveCatAction.MEOW -> "The cat meows."
            RiveCatAction.SCRATCH -> "The cat stands and scratches its head."
            RiveCatAction.TALK -> "The cat talks out loud."
            RiveCatAction.STAND -> "The cat is listening."
            RiveCatAction.RUN -> "The cat runs."
        }
        if (action == RiveCatAction.MEOW) {
            onSpeak("Meow", speechLanguageTag)
        }
        if (action == RiveCatAction.TALK) {
            val phrase = activeWord?.back
                ?: deck.getOrNull((score + hitBlocks.size) % deck.size)?.back
                ?: "Hello"
            onSpeak(phrase, speechLanguageTag)
        }
    }
    fun answerWord(choice: String) {
        val word = activeWord ?: return
        val correct = normalizeAnswerText(choice) == normalizeAnswerText(word.back)
        if (correct) {
            score += 4
            boost = true
            message = "Correct: ${word.front} -> ${word.back}"
        } else {
            boost = false
            message = "Answer: ${word.back}"
        }
        activeWordIndex = null
    }
    fun resetRun() {
        worldOffset = 0f
        playerAltitude = 0f
        velocityY = 0f
        hitBlocks = emptySet()
        stompedOpponents = emptySet()
        activeWordIndex = null
        lives = 3
        score = 0
        invulnerableTicks = 0
        running = true
        boost = false
        message = "New run. Hit ? blocks and jump on opponents."
    }
    LaunchedEffect(deck, running, boost) {
        while (true) {
            delay(16L)
            if (running && lives > 0) {
                val speed = if (boost) 4.2f else 2.8f
                worldOffset += speed
                facingRight = true
                if (playerAltitude > 0f || velocityY != 0f) {
                    playerAltitude = (playerAltitude + velocityY).coerceAtLeast(0f)
                    velocityY -= 0.68f
                    if (playerAltitude <= 0f) {
                        playerAltitude = 0f
                        velocityY = 0f
                    }
                }
                if (invulnerableTicks > 0) invulnerableTicks -= 1
                if (actionTicks > 0) {
                    actionTicks -= 1
                    if (actionTicks == 0 && playerAltitude <= 1f) catAction = RiveCatAction.RUN
                } else if (playerAltitude <= 1f && catAction == RiveCatAction.SPIN) {
                    catAction = RiveCatAction.RUN
                }
                val playerWorldX = worldOffset + 150f
                questionBlocks.forEachIndexed { index, blockX ->
                    if (index !in hitBlocks &&
                        abs(playerWorldX - blockX) < 38f &&
                        playerAltitude in 86f..172f &&
                        velocityY > 0f
                    ) {
                        hitBlocks = hitBlocks + index
                        activeWordIndex = index % deck.size
                        score += 1
                        velocityY = -2.2f
                        boost = true
                        message = "Word popped out: ${deck[index % deck.size].front}"
                    }
                }
                opponents.forEachIndexed { index, opponentX ->
                    if (index !in stompedOpponents && abs(playerWorldX - opponentX) < 42f) {
                        if (playerAltitude in 16f..94f && velocityY <= 0f) {
                            stompedOpponents = stompedOpponents + index
                            lives += 1
                            score += 3
                            velocityY = 9.5f
                            playerAltitude = playerAltitude.coerceAtLeast(30f)
                            boost = true
                            message = "Clean landing: +1 life."
                        } else if (playerAltitude < 12f && invulnerableTicks == 0) {
                            lives = (lives - 1).coerceAtLeast(0)
                            invulnerableTicks = 70
                            boost = false
                            message = if (lives == 0) "Run ended. Reset to try again." else "Hit from the side. Jump next time."
                        }
                    }
                }
                val finishLine = questionBlocks.lastOrNull()?.plus(560f) ?: 3200f
                if (worldOffset > finishLine) {
                    worldOffset = 0f
                    hitBlocks = emptySet()
                    stompedOpponents = emptySet()
                    activeWordIndex = null
                    message = "New lap. Blocks and opponents refreshed."
                }
            }
        }
    }
    LaunchedEffect(lives) {
        if (lives <= 0) {
            running = false
            activeWordIndex = null
        }
    }
    DisposableEffect(Unit) {
        onDispose { running = false }
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF3B1269), Color(0xFF091F3B), Color(0xFF041418))
                )
            )
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFFDD4A),
                border = BorderStroke(2.dp, Color.White.copy(alpha = 0.65f)),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("R", fontWeight = FontWeight.Black, fontSize = 26.sp, color = Color(0xFF3B1269))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Rive Runner Lab", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                Text("Lives $lives   Score $score   Words ${hitBlocks.size}/${questionBlocks.size}", color = Color.White.copy(alpha = 0.78f), fontSize = 12.sp)
                Text("Rive character + Compose platformer logic", color = Color(0xFFFFDD4A), fontSize = 11.sp)
            }
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.Transparent,
            border = BorderStroke(2.dp, Color(0xFFFFDD4A).copy(alpha = 0.55f + glow * 0.35f)),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .shadow(10.dp, RoundedCornerShape(8.dp))
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val groundY = h * 0.78f
                    val playerScreenX = w * 0.24f
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFFFF3DB8), Color(0xFF402BD8), Color(0xFF06131F)),
                            center = Offset(playerScreenX, h * 0.42f),
                            radius = w * (0.62f + glow * 0.18f)
                        )
                    )
                    repeat(18) { i ->
                        val x = w * (((i * 0.137f) + drift) % 1f)
                        val y = h * (0.12f + ((i * 0.211f) % 0.72f))
                        val color = when (i % 4) {
                            0 -> Color(0xFFFFDD4A)
                            1 -> Color(0xFFFF3DB8)
                            2 -> Color(0xFF67F5FF)
                            else -> Color(0xFF92FF72)
                        }
                        drawCircle(color.copy(alpha = 0.40f + glow * 0.25f), 6f + (i % 5) * 3f, Offset(x, y))
                    }
                    repeat(8) { lane ->
                        val y = h * (0.72f + lane * 0.025f)
                        drawLine(
                            color = Color.White.copy(alpha = 0.10f + lane * 0.025f),
                            start = Offset(0f, y),
                            end = Offset(w, y + sin(drift * 6.28f + lane) * 10f),
                            strokeWidth = 2f + lane
                        )
                    }
                    drawRoundRect(
                        color = Color(0xFF4B2A1D),
                        topLeft = Offset(0f, groundY),
                        size = Size(w, h - groundY),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(0f, 0f)
                    )
                    repeat(16) { tile ->
                        val x = ((tile * 78f - (worldOffset % 78f)) % (w + 78f)) - 40f
                        drawRoundRect(
                            color = if (tile % 2 == 0) Color(0xFFFFA83D) else Color(0xFFFFCF5E),
                            topLeft = Offset(x, groundY + 8f),
                            size = Size(62f, 18f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                        )
                    }
                    questionBlocks.forEachIndexed { index, blockWorldX ->
                        val x = playerScreenX + (blockWorldX - (worldOffset + 150f))
                        if (x in -90f..(w + 90f)) {
                            val y = groundY - 162f
                            val hit = index in hitBlocks
                            drawRoundRect(
                                color = if (hit) Color(0xFF7A5C30) else Color(0xFFFFDD4A),
                                topLeft = Offset(x - 24f, y - 24f),
                                size = Size(48f, 48f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
                            )
                            drawRoundRect(
                                color = Color.White.copy(alpha = if (hit) 0.12f else 0.32f),
                                topLeft = Offset(x - 18f, y - 18f),
                                size = Size(36f, 12f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                            )
                            drawContext.canvas.nativeCanvas.apply {
                                val paint = android.graphics.Paint().apply {
                                    isAntiAlias = true
                                    color = if (hit) android.graphics.Color.rgb(255, 221, 74) else android.graphics.Color.rgb(59, 18, 105)
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    textSize = 31f
                                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                                }
                                drawText(if (hit) "+" else "?", x, y + 11f, paint)
                            }
                            if (hit) {
                                val word = deck[index % deck.size].front.shortGameText()
                                drawContext.canvas.nativeCanvas.apply {
                                    val paint = android.graphics.Paint().apply {
                                        isAntiAlias = true
                                        color = android.graphics.Color.rgb(255, 247, 232)
                                        textAlign = android.graphics.Paint.Align.CENTER
                                        textSize = 21f
                                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                                    }
                                    drawText(word, x, y - 36f - sin(drift * 6.28f + index) * 7f, paint)
                                }
                            }
                        }
                    }
                    opponents.forEachIndexed { index, opponentWorldX ->
                        val x = playerScreenX + (opponentWorldX - (worldOffset + 150f))
                        if (x in -90f..(w + 90f) && index !in stompedOpponents) {
                            val y = groundY - 22f
                            drawOval(
                                color = Color(0xFFFF3D4E),
                                topLeft = Offset(x - 24f, y - 22f),
                                size = Size(48f, 34f)
                            )
                            drawCircle(Color(0xFFFFF7E8), 5f, Offset(x - 9f, y - 10f))
                            drawCircle(Color(0xFFFFF7E8), 5f, Offset(x + 9f, y - 10f))
                            drawCircle(Color(0xFF3B1269), 2f, Offset(x - 8f, y - 9f))
                            drawCircle(Color(0xFF3B1269), 2f, Offset(x + 8f, y - 9f))
                            repeat(3) { spike ->
                                val sx = x - 16f + spike * 16f
                                val path = Path().apply {
                                    moveTo(sx, y - 22f)
                                    lineTo(sx + 7f, y - 37f)
                                    lineTo(sx + 14f, y - 22f)
                                    close()
                                }
                                drawPath(path, Color(0xFFFFDD4A))
                            }
                        }
                    }
                    activeWord?.let { word ->
                        drawRoundRect(
                            color = Color(0xFFFFF7E8),
                            topLeft = Offset(w * 0.10f, h * 0.06f),
                            size = Size(w * 0.80f, h * 0.15f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f, 20f)
                        )
                        drawContext.canvas.nativeCanvas.apply {
                            val paint = android.graphics.Paint().apply {
                                isAntiAlias = true
                                color = android.graphics.Color.rgb(59, 18, 105)
                                textAlign = android.graphics.Paint.Align.CENTER
                                textSize = 30f
                                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                            }
                            drawText(word.front.take(32), w * 0.50f, h * 0.145f, paint)
                        }
                    }
                }
                Canvas(
                    modifier = Modifier
                        .width(140.dp)
                        .height(140.dp)
                        .align(Alignment.TopStart)
                        .offset(
                            x = maxWidth * 0.24f - 70.dp,
                            y = maxHeight * 0.78f - 145.dp - playerAltitude.dp
                        )
                        .graphicsLayer {
                            rotationZ = if (catAction == RiveCatAction.SPIN) drift * 360f else 0f
                            scaleX = if (facingRight) 1f else -1f
                            if (catAction == RiveCatAction.CRAWL) scaleY = 0.62f
                            if (catAction == RiveCatAction.DANCE) {
                                rotationZ = sin(drift * 6.28f) * 13f
                                translationY = -abs(sin(drift * 6.28f)) * 10f
                            }
                        }
                ) {
                    drawExpressiveGingerGameCat(
                        center = Offset(size.width / 2f, size.height * 0.56f),
                        radius = size.minDimension * 0.30f,
                        action = catAction,
                        frame = drift,
                        invulnerable = invulnerableTicks > 0
                    )
                }
            }
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.White.copy(alpha = 0.92f),
            border = BorderStroke(1.dp, Color(0xFFFFDD4A).copy(alpha = 0.8f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(activeWord?.front ?: message, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, color = Color(0xFF3B1269))
                if (activeWord != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        choices.forEach { choice ->
                            OutlinedButton(
                                onClick = { answerWord(choice) },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(choice.shortGameText(), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = {
                    worldOffset = (worldOffset - 55f).coerceAtLeast(0f)
                    facingRight = false
                    message = "Step back for timing."
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) { Text("Left") }
            Button(
                onClick = { jump() },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3DB8))
            ) { Text("Jump") }
            OutlinedButton(
                onClick = {
                    boost = !boost
                    running = true
                    facingRight = true
                    message = if (boost) "Run boosted." else "Run steady."
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) { Text(if (boost) "Steady" else "Boost") }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            listOf(
                "Spin" to RiveCatAction.SPIN,
                "Dance" to RiveCatAction.DANCE,
                "Crawl" to RiveCatAction.CRAWL,
                "Meow" to RiveCatAction.MEOW,
                "Scratch" to RiveCatAction.SCRATCH,
                "Talk" to RiveCatAction.TALK,
                "Stand" to RiveCatAction.STAND
            ).forEach { (label, action) ->
                OutlinedButton(
                    onClick = { startCatAction(action) },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text(label, fontSize = 12.sp)
                }
            }
        }
        Button(
            onClick = {
                if (lives <= 0) {
                    resetRun()
                } else {
                    running = !running
                    message = if (running) "Run resumed." else "Paused."
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFDD4A), contentColor = Color(0xFF3B1269))
        ) { Text(if (lives <= 0) "Reset" else if (running) "Pause" else "Run") }
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Close game", color = Color.White)
        }
    }
}

@Composable
private fun RiveLetterBlocksScreen(
    cards: List<LanguageGameEntry>,
    speechLanguageTag: String,
    onSpeak: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    data class LetterBlock(
        val id: Int,
        val label: String,
        val homeX: Float,
        val homeY: Float,
        val x: Float,
        val y: Float,
        val slot: Int?
    )
    fun cleanWords(value: String): List<String> {
        return value
            .split(Regex("\\s+"))
            .map { token ->
                token
                    .filter { it.isLetter() }
                    .uppercase(Locale.ROOT)
            }
            .filter { it.isNotBlank() }
    }
    fun cleanWord(value: String): String {
        return cleanWords(value)
            .firstOrNull()
            ?.take(10)
            .orEmpty()
    }
    fun cleanSentence(value: String): List<String> {
        return cleanWords(value)
            .filter { it.length <= 16 }
            .take(6)
    }
    fun cleanSpeech(value: String): String {
        return cleanWords(value).joinToString(" ")
    }
    val fallbackDeck = remember {
        listOf(
            LanguageGameEntry(1, "ginger cat", "KOT SKACZE"),
            LanguageGameEntry(2, "summer meadow", "LETNI LUG"),
            LanguageGameEntry(3, "new word", "NOWE SLOWO"),
            LanguageGameEntry(4, "play a game", "GRAM W GRE")
        )
    }
    val deck = remember(cards) {
        cards.ifEmpty { fallbackDeck }
            .filter { cleanWord(it.back).length >= 2 }
            .ifEmpty { fallbackDeck }
            .take(36)
    }
    var roundIndex by remember(deck) { mutableStateOf(0) }
    var score by remember(deck) { mutableStateOf(0) }
    var attempts by remember(deck) { mutableStateOf(0) }
    var resetNonce by remember(deck) { mutableStateOf(0) }
    var stage by remember(deck) { mutableStateOf(0) }
    var blocks by remember(deck) { mutableStateOf<List<LetterBlock>>(emptyList()) }
    var catAction by remember(deck) { mutableStateOf(RiveCatAction.STAND) }
    var catMessage by remember(deck) { mutableStateOf("Tap or drag blocks") }
    val activeEntry = deck[roundIndex % deck.size]
    val targetWord = cleanWord(activeEntry.back).ifBlank { cleanWord(activeEntry.front).ifBlank { "CAT" } }
    val sentenceWords = cleanSentence(activeEntry.back).let { words ->
        if (words.size >= 2) words else cleanSentence("${activeEntry.front} ${activeEntry.back}").takeIf { it.size >= 2 }
            ?: listOf(targetWord, "OK")
    }
    val targetLabels = if (stage == 0) targetWord.map { it.toString() } else sentenceWords
    val targetSpeech = if (stage == 0) targetWord else sentenceWords.joinToString(" ")
    val clue = cleanSentence(activeEntry.front).joinToString(" ").ifBlank { "BUILD THE CARD" }
    val isWordStage = stage == 1
    val density = LocalDensity.current
    val transition = rememberInfiniteTransition(label = "letterBlocksCat")
    val frame by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1500), repeatMode = RepeatMode.Restart),
        label = "letterBlocksFrame"
    )
    fun slotCenter(index: Int, count: Int, blockWidth: Float): Float {
        if (count <= 1) return 0.50f
        val safeWidth = blockWidth.coerceAtMost(0.86f / count.coerceAtLeast(1))
        val step = (0.88f - safeWidth) / (count - 1)
        val left = 0.50f - ((count - 1) * step + safeWidth) / 2f
        return left + safeWidth / 2f + index * step
    }
    fun buildBlocks(fieldWidthPx: Float, fieldHeightPx: Float, blockWidthPx: Float, blockHeightPx: Float): List<LetterBlock> {
        val blockW = blockWidthPx / fieldWidthPx.coerceAtLeast(1f)
        val blockH = blockHeightPx / fieldHeightPx.coerceAtLeast(1f)
        val items = targetLabels.mapIndexed { index, label -> index to label }
            .shuffled(Random(targetLabels.joinToString("").hashCode() + resetNonce * 19 + roundIndex * 37 + stage * 53))
        val gap = 0.025f
        val columns = ((0.86f / (blockW + gap)).toInt()).coerceIn(1, 8)
        return items.mapIndexed { index, pair ->
            val row = index / columns
            val column = index % columns
            val x = (0.07f + column * (blockW + gap)).coerceAtMost(0.94f - blockW)
            val y = (0.68f + row * (blockH + 0.04f)).coerceAtMost(0.94f - blockH)
            LetterBlock(
                id = pair.first,
                label = pair.second,
                homeX = x,
                homeY = y,
                x = x,
                y = y,
                slot = null
            )
        }
    }
    fun snapBlock(blockId: Int, targetSlot: Int?, fieldWidthPx: Float, blockWidthPx: Float) {
        val moving = blocks.firstOrNull { it.id == blockId } ?: return
        val blockW = blockWidthPx / fieldWidthPx.coerceAtLeast(1f)
        val slotY = 0.24f
        val occupant = targetSlot?.let { slot -> blocks.firstOrNull { it.id != blockId && it.slot == slot } }
        blocks = blocks.map { block ->
            when (block.id) {
                blockId -> {
                    if (targetSlot == null) {
                        block.copy(x = block.homeX, y = block.homeY, slot = null)
                    } else {
                        block.copy(
                            x = slotCenter(targetSlot, targetLabels.size, blockW) - blockW / 2f,
                            y = slotY,
                            slot = targetSlot
                        )
                    }
                }
                occupant?.id -> {
                    val oldSlot = moving.slot
                    if (oldSlot == null) {
                        block.copy(x = block.homeX, y = block.homeY, slot = null)
                    } else {
                        block.copy(
                            x = slotCenter(oldSlot, targetLabels.size, blockW) - blockW / 2f,
                            y = slotY,
                            slot = oldSlot
                        )
                    }
                }
                else -> block
            }
        }
    }
    fun tapBlock(blockId: Int, fieldWidthPx: Float, blockWidthPx: Float) {
        val current = blocks.firstOrNull { it.id == blockId } ?: return
        if (current.slot != null) {
            snapBlock(blockId, null, fieldWidthPx, blockWidthPx)
        } else {
            val freeSlot = (0 until targetLabels.size).firstOrNull { slot -> blocks.none { it.slot == slot } }
            if (freeSlot != null) {
                snapBlock(blockId, freeSlot, fieldWidthPx, blockWidthPx)
            }
        }
    }
    fun checkAnswer() {
        attempts += 1
        val assembled = (0 until targetLabels.size).map { slot ->
            blocks.firstOrNull { it.slot == slot }?.label.orEmpty()
        }
        if (assembled == targetLabels) {
            catAction = RiveCatAction.DANCE
            catMessage = if (stage == 0) "OK now sentence" else "OK next card"
            onSpeak("OK", speechLanguageTag)
            if (stage == 0) {
                stage = 1
            } else {
                score += 1
                stage = 0
                roundIndex = (roundIndex + 1) % deck.size
            }
            resetNonce += 1
        } else {
            catAction = RiveCatAction.SCRATCH
            catMessage = "Not OK try again"
            onSpeak("Not OK", speechLanguageTag)
        }
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFB8F777), Color(0xFF61D36E), Color(0xFF206B45))))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GingerFluffyCatMark(modifier = Modifier.size(46.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Rive Letter Blocks", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF173923))
                Text(
                    "Score $score   Attempts $attempts   Card ${roundIndex + 1}/${deck.size}   ${if (stage == 0) "Letters" else "Words"}",
                    color = Color(0xFF173923).copy(alpha = 0.78f),
                    fontSize = 12.sp
                )
            }
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.White.copy(alpha = 0.34f),
            border = BorderStroke(2.dp, Color(0xFFFAE97C).copy(alpha = 0.92f)),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val fieldWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
                val fieldHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
                val baseBlockWidthPx = with(density) { if (isWordStage) 96.dp.toPx() else 50.dp.toPx() }
                val maxBlockWidthPx = fieldWidthPx * 0.86f / targetLabels.size.coerceAtLeast(1)
                val minBlockWidthPx = with(density) { if (isWordStage) 30.dp.toPx() else 24.dp.toPx() }
                val blockWidthPx = baseBlockWidthPx.coerceAtMost(maxBlockWidthPx).coerceAtLeast(minBlockWidthPx)
                val blockHeightPx = with(density) { if (isWordStage) 46.dp.toPx() else 50.dp.toPx() }.coerceAtMost(blockWidthPx + with(density) { 10.dp.toPx() })
                val blockW = blockWidthPx / fieldWidthPx
                val blockH = blockHeightPx / fieldHeightPx
                val blockWidthDp = with(density) { blockWidthPx.toDp() }
                val blockHeightDp = with(density) { blockHeightPx.toDp() }
                val slotY = 0.24f
                LaunchedEffect(targetLabels, fieldWidthPx, fieldHeightPx, resetNonce, stage) {
                    blocks = buildBlocks(fieldWidthPx, fieldHeightPx, blockWidthPx, blockHeightPx)
                    catAction = RiveCatAction.STAND
                    catMessage = if (stage == 0) "Build word $targetWord" else "Build sentence"
                }
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    drawRect(
                        Brush.verticalGradient(
                            listOf(Color(0xFFE8FFD4), Color(0xFFB8F777), Color(0xFF69D46E), Color(0xFF2E8B57))
                        )
                    )
                    drawCircle(Color(0xFFFFF38A).copy(alpha = 0.86f), w * 0.09f, Offset(w * 0.12f, h * 0.14f))
                    repeat(28) { index ->
                        val x = w * ((index * 0.137f + frame * 0.12f) % 1f)
                        val y = h * (0.54f + ((index * 0.071f) % 0.38f))
                        drawLine(
                            color = Color(0xFF1F8A45).copy(alpha = 0.42f),
                            start = Offset(x, y + 16f),
                            end = Offset(x + ((index % 3) - 1) * 8f, y),
                            strokeWidth = 3f
                        )
                    }
                    repeat(18) { index ->
                        val x = w * ((index * 0.171f + 0.08f) % 1f)
                        val y = h * (0.52f + ((index * 0.113f) % 0.34f))
                        val color = if (index % 2 == 0) Color(0xFFFFF38A) else Color(0xFFFFA6C8)
                        drawCircle(color.copy(alpha = 0.74f), 4f + (index % 3) * 2f, Offset(x, y))
                    }
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.42f),
                        topLeft = Offset(w * 0.06f, h * 0.05f),
                        size = Size(w * 0.88f, h * 0.13f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f)
                    )
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            color = android.graphics.Color.rgb(23, 57, 35)
                            textAlign = android.graphics.Paint.Align.CENTER
                            textSize = 28f
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        }
                        drawText(if (stage == 0) clue.take(34) else targetWord.take(24), w * 0.50f, h * 0.125f, paint)
                    }
                    repeat(targetLabels.size) { index ->
                        val centerX = w * slotCenter(index, targetLabels.size, blockW)
                        val topLeft = Offset(centerX - blockWidthPx / 2f, h * slotY)
                        drawRoundRect(
                            color = Color(0xFFFFF7E8).copy(alpha = 0.36f),
                            topLeft = topLeft,
                            size = Size(blockWidthPx, blockHeightPx),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f),
                            style = Stroke(width = 3.5f)
                        )
                    }
                    drawExpressiveGingerGameCat(
                        center = Offset(w * 0.78f, h * 0.61f),
                        radius = minOf(w, h) * 0.105f,
                        action = catAction,
                        frame = frame,
                        invulnerable = false
                    )
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            color = android.graphics.Color.rgb(23, 57, 35)
                            textAlign = android.graphics.Paint.Align.CENTER
                            textSize = 22f
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        }
                        drawText(catMessage.take(32), w * 0.78f, h * 0.84f, paint)
                    }
                }
                blocks.forEach { block ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (block.slot == null) Color(0xFFFAE97C) else Color(0xFFB4F6FF),
                        border = BorderStroke(2.dp, Color.White.copy(alpha = 0.82f)),
                        shadowElevation = 7.dp,
                        modifier = Modifier
                            .size(width = blockWidthDp, height = blockHeightDp)
                            .offset {
                                IntOffset(
                                    (block.x * fieldWidthPx).roundToInt(),
                                    (block.y * fieldHeightPx).roundToInt()
                                )
                            }
                            .zIndex(if (block.slot == null) 2f else 3f)
                            .pointerInput(block.id, targetLabels, fieldWidthPx, blockWidthPx) {
                                detectTapGestures(
                                    onTap = {
                                        catAction = RiveCatAction.STAND
                                        catMessage = if (block.slot == null) "Placed ${block.label}" else "Returned ${block.label}"
                                        tapBlock(block.id, fieldWidthPx, blockWidthPx)
                                    }
                                )
                            }
                            .pointerInput(block.id, targetLabels, fieldWidthPx, fieldHeightPx, blockWidthPx, blockHeightPx) {
                                detectDragGestures(
                                    onDragStart = {
                                        catAction = RiveCatAction.STAND
                                        catMessage = "Move ${block.label}"
                                    },
                                    onDragEnd = {
                                        val current = blocks.firstOrNull { it.id == block.id } ?: return@detectDragGestures
                                        val centerX = current.x + blockW / 2f
                                        val centerY = current.y + blockH / 2f
                                        val nearest = (0 until targetLabels.size).minByOrNull { slot ->
                                            abs(centerX - slotCenter(slot, targetLabels.size, blockW)) + abs(centerY - (slotY + blockH / 2f))
                                        }
                                        if (nearest != null &&
                                            abs(centerX - slotCenter(nearest, targetLabels.size, blockW)) < (blockW * 0.74f).coerceAtLeast(0.055f) &&
                                            abs(centerY - (slotY + blockH / 2f)) < 0.17f
                                        ) {
                                            snapBlock(block.id, nearest, fieldWidthPx, blockWidthPx)
                                        } else {
                                            snapBlock(block.id, null, fieldWidthPx, blockWidthPx)
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        blocks = blocks.map { item ->
                                            if (item.id == block.id) {
                                                item.copy(
                                                    x = (item.x + dragAmount.x / fieldWidthPx).coerceIn(0.02f, 0.96f - blockW),
                                                    y = (item.y + dragAmount.y / fieldHeightPx).coerceIn(0.02f, 0.96f - blockH),
                                                    slot = null
                                                )
                                            } else {
                                                item
                                            }
                                        }
                                    }
                                )
                        }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                block.label,
                                color = Color(0xFF173923),
                                fontSize = if (isWordStage) 14.sp else 23.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { checkAnswer() },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFDD4A), contentColor = Color(0xFF3B1269))
            ) { Text("Check") }
            OutlinedButton(
                onClick = {
                    resetNonce += 1
                    catAction = RiveCatAction.CRAWL
                    catMessage = "Blocks returned"
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF173923))
            ) { Text("Reset") }
            OutlinedButton(
                onClick = {
                    catAction = RiveCatAction.TALK
                    catMessage = "Saying target"
                    onSpeak(cleanSpeech(targetSpeech), speechLanguageTag)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF173923))
            ) { Text("Say") }
        }
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Close game", color = Color(0xFF173923))
        }
    }
}

@Composable
private fun FlutterRunnerLabScreen(
    cards: List<LanguageGameEntry>,
    onDismiss: () -> Unit
) {
    val fallbackDeck = remember {
        listOf(
            LanguageGameEntry(1, "widget", "widzet"),
            LanguageGameEntry(2, "state", "stan"),
            LanguageGameEntry(3, "jump", "skok"),
            LanguageGameEntry(4, "life", "zycie")
        )
    }
    val deck = remember(cards) {
        cards.ifEmpty { fallbackDeck }.shuffled(Random(cards.size * 107 + 83)).take(24)
    }
    var playerX by remember(deck) { mutableStateOf(0.18f) }
    var playerY by remember(deck) { mutableStateOf(0.68f) }
    var velocityY by remember(deck) { mutableStateOf(0f) }
    var facingRight by remember(deck) { mutableStateOf(true) }
    var lives by remember(deck) { mutableStateOf(3) }
    var score by remember(deck) { mutableStateOf(0) }
    var taskUsed by remember(deck) { mutableStateOf(false) }
    var activeTaskIndex by remember(deck) { mutableStateOf<Int?>(null) }
    var message by remember(deck) { mutableStateOf("Flutter-style prototype hosted by Compose. Нажми Задание.") }
    val activeTask = activeTaskIndex?.let { deck[it % deck.size] }
    val choices = remember(activeTask, deck) {
        val task = activeTask ?: return@remember emptyList()
        (listOf(task.back) + deck
            .filterNot { it.id == task.id }
            .map { it.back }
            .distinct()
            .shuffled(Random(task.id + 211))
            .take(3))
            .distinct()
            .shuffled(Random(task.front.hashCode() + 229))
            .let { options -> if (options.size >= 4) options.take(4) else options + List(4 - options.size) { task.back } }
    }
    val transition = rememberInfiniteTransition(label = "flutterRunnerLab")
    val clock by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1600), repeatMode = RepeatMode.Restart),
        label = "flutterRunnerClock"
    )
    LaunchedEffect(deck) {
        while (true) {
            delay(16L)
            velocityY = (velocityY + 0.018f).coerceAtMost(0.035f)
            playerY = (playerY + velocityY).coerceAtMost(0.68f)
            if (playerY >= 0.68f) {
                playerY = 0.68f
                velocityY = 0f
            }
        }
    }
    fun move(dx: Float) {
        playerX = (playerX + dx).coerceIn(0.10f, 0.90f)
        if (dx != 0f) facingRight = dx > 0f
        message = "setState: x=${"%.2f".format(playerX)}"
    }
    fun jump() {
        if (playerY >= 0.67f) {
            velocityY = -0.065f
            message = "AnimatedPositioned: jump"
        }
    }
    fun startTask() {
        if (taskUsed || activeTaskIndex != null) {
            message = if (activeTaskIndex != null) "Задание уже открыто." else "Задание уже использовано."
            return
        }
        val nextIndex = (score + lives + (playerX * 100).toInt()) % deck.size
        activeTaskIndex = nextIndex
        taskUsed = true
        message = deck[nextIndex].front
    }
    fun answerTask(choice: String) {
        val task = activeTask ?: return
        val correct = normalizeAnswerText(choice) == normalizeAnswerText(task.back)
        if (correct) {
            lives += 1
            score += 1
            message = "Задание выполнено: +1 жизнь."
        } else {
            message = "Ответ: ${task.back}"
        }
        activeTaskIndex = null
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GingerFluffyCatMark(modifier = Modifier.size(46.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Flutter Runner Lab", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("Lives $lives   Score $score   Task ${if (taskUsed) "0" else "1"}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Text("No Flutter SDK found: native Compose host, Flutter-style game loop.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF0F2F3A),
            border = BorderStroke(2.dp, Color(0xFF55C5E8).copy(alpha = 0.55f)),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawRect(Color(0xFF0D2B36))
                repeat(3) { layer ->
                    val top = h * (0.10f + layer * 0.16f)
                    drawRoundRect(
                        color = Color(0xFF55C5E8).copy(alpha = 0.08f + layer * 0.035f),
                        topLeft = Offset(w * (0.06f + layer * 0.05f), top),
                        size = Size(w * (0.88f - layer * 0.10f), h * 0.16f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(26f, 26f)
                    )
                }
                repeat(9) { i ->
                    val x = w * (0.08f + i * 0.11f)
                    val y = h * (0.78f + sin(clock * 6.28f + i) * 0.012f)
                    drawRoundRect(
                        color = if (i % 2 == 0) Color(0xFF55C5E8) else Color(0xFFFFC66D),
                        topLeft = Offset(x - 18f, y),
                        size = Size(36f, 16f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                    )
                }
                val px = w * playerX
                val py = h * playerY + sin(clock * 6.28f) * 3f
                drawCircle(Color(0xFF061A20).copy(alpha = 0.34f), 34f, Offset(px + 4f, py + 36f))
                drawPixelGingerCat(
                    topLeft = Offset(px - 23f, py - 42f),
                    pixel = 7f,
                    facingRight = facingRight
                )
                drawRoundRect(
                    color = Color(0xFFFFF7E8).copy(alpha = 0.95f),
                    topLeft = Offset(w * 0.06f, h * 0.05f),
                    size = Size(w * 0.34f, h * 0.20f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f)
                )
                drawContext.canvas.nativeCanvas.apply {
                    val titlePaint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        color = android.graphics.Color.rgb(13, 43, 54)
                        textSize = 22f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }
                    val smallPaint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        color = android.graphics.Color.rgb(63, 83, 91)
                        textSize = 18f
                    }
                    drawText("Widget tree", w * 0.10f, h * 0.12f, titlePaint)
                    drawText("Stack > Cat > Task", w * 0.10f, h * 0.18f, smallPaint)
                }
                activeTask?.let { task ->
                    drawRoundRect(
                        color = Color(0xFFFFF7E8),
                        topLeft = Offset(w * 0.44f, h * 0.06f),
                        size = Size(w * 0.48f, h * 0.18f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f)
                    )
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            color = android.graphics.Color.rgb(13, 43, 54)
                            textAlign = android.graphics.Paint.Align.CENTER
                            textSize = 27f
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        }
                        drawText(task.front.take(30), w * 0.68f, h * 0.16f, paint)
                    }
                }
            }
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFFFF7E8),
            border = BorderStroke(1.dp, Color(0xFF55C5E8).copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(activeTask?.front ?: message, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (activeTask != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        choices.forEach { choice ->
                            OutlinedButton(
                                onClick = { answerTask(choice) },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(choice.shortGameText(), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { move(-0.07f) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("Left") }
            OutlinedButton(onClick = { jump() }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("Jump") }
            OutlinedButton(onClick = { move(0.07f) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("Right") }
        }
        Button(
            onClick = { startTask() },
            enabled = !taskUsed && activeTaskIndex == null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) { Text("Задание") }
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Close game")
        }
    }
}

@Composable
private fun SixteenBitGingerSpinRunnerScreen(
    cards: List<LanguageGameEntry>,
    onDismiss: () -> Unit
) {
    val fallbackDeck = remember {
        listOf(
            LanguageGameEntry(1, "speed", "szybkosc"),
            LanguageGameEntry(2, "jump", "skok"),
            LanguageGameEntry(3, "life", "zycie"),
            LanguageGameEntry(4, "word", "slowo")
        )
    }
    val deck = remember(cards) {
        cards.ifEmpty { fallbackDeck }.shuffled(Random(cards.size * 79 + 41)).take(28)
    }
    var runnerY by remember(deck) { mutableStateOf(214f) }
    var velocityY by remember(deck) { mutableStateOf(0f) }
    var worldOffset by remember(deck) { mutableStateOf(0f) }
    var boost by remember(deck) { mutableStateOf(false) }
    var spinning by remember(deck) { mutableStateOf(false) }
    var spinAngle by remember(deck) { mutableStateOf(0f) }
    var lives by remember(deck) { mutableStateOf(3) }
    var score by remember(deck) { mutableStateOf(0) }
    var rings by remember(deck) { mutableStateOf(0) }
    var invulnerableTicks by remember(deck) { mutableStateOf(0) }
    var message by remember(deck) { mutableStateOf("Нажми Задание, чтобы открыть единственную карточку.") }
    var lifeQuestionIndex by remember(deck) { mutableStateOf<Int?>(null) }
    var taskUsed by remember(deck) { mutableStateOf(false) }
    val runnerX = 74f
    val groundY = 232f
    val runnerRadius = 18f
    val speed = if (boost) 5.2f else 3.35f
    val currentQuestion = lifeQuestionIndex?.let { deck[it % deck.size] }
    val currentChoices = remember(currentQuestion, deck) {
        val current = currentQuestion ?: return@remember emptyList()
        (listOf(current.back) + deck
            .filterNot { it.id == current.id }
            .map { it.back }
            .distinct()
            .shuffled(Random(current.id + 113))
            .take(3))
            .distinct()
            .shuffled(Random(current.front.hashCode() + 71))
            .let { options -> if (options.size >= 4) options.take(4) else options + List(4 - options.size) { current.back } }
    }
    fun terrainY(x: Float): Float {
        return groundY + sin(x / 88f) * 14f + sin(x / 37f) * 4f
    }
    fun resetRunner(hurt: Boolean) {
        runnerY = terrainY(worldOffset + runnerX) - runnerRadius
        velocityY = 0f
        spinning = false
        if (hurt) {
            lives = (lives - 1).coerceAtLeast(0)
            invulnerableTicks = 70
            message = if (lives <= 1) "Last life. Нажми Задание, если оно еще доступно." else "Hit. Keep moving."
        }
    }
    fun startLifeQuestion() {
        if (taskUsed || lifeQuestionIndex != null) {
            message = if (lifeQuestionIndex != null) "Задание уже открыто." else "Задание уже использовано."
            return
        }
        val nextIndex = (score + rings + lives) % deck.size
        lifeQuestionIndex = nextIndex
        taskUsed = true
        message = deck[nextIndex].front
    }
    fun answerLifeQuestion(choice: String) {
        val card = currentQuestion ?: return
        val correct = normalizeAnswerText(choice) == normalizeAnswerText(card.back)
        if (correct) {
            lives += 1
            score += 2
            message = "Задание выполнено: +1 жизнь."
        } else {
            message = "Ответ: ${card.back}"
        }
        lifeQuestionIndex = null
    }
    LaunchedEffect(deck, boost, lifeQuestionIndex) {
        while (true) {
            delay(16L)
            if (lifeQuestionIndex != null) continue
            worldOffset += speed
            val worldX = worldOffset + runnerX
            val floor = terrainY(worldX)
            velocityY = (velocityY + 0.36f).coerceAtMost(9f)
            runnerY += velocityY
            if (runnerY + runnerRadius >= floor) {
                runnerY = floor - runnerRadius
                velocityY = 0f
                spinning = false
            }
            if (spinning || velocityY != 0f) {
                spinAngle = (spinAngle + speed * 10f + 10f) % 360f
            }
            if (invulnerableTicks > 0) invulnerableTicks -= 1
            val runnerWorldX = worldOffset + runnerX
            List(10) { 360f + it * 250f }.forEachIndexed { enemyIndex, enemyX ->
                val wrappedX = enemyX + ((worldOffset / 2600f).toInt() * 2600f)
                val enemyGround = terrainY(wrappedX)
                val close = abs(runnerWorldX - wrappedX) < 28f && abs((runnerY + runnerRadius) - enemyGround) < 35f
                if (close && invulnerableTicks <= 0) {
                    if (spinning && velocityY > -1.5f) {
                        score += 1
                        rings += 3
                        invulnerableTicks = 25
                        message = "Spin hit opponent +3"
                    } else {
                        resetRunner(hurt = true)
                    }
                }
                if (enemyIndex == -1) Unit
            }
            if (lives == 0) {
                lives = 3
                rings = 0
                score = 0
                worldOffset = 0f
                resetRunner(hurt = false)
                message = "Restarted. Three lives again."
            }
        }
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GingerPixelCatMark(modifier = Modifier.size(46.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("16-bit Ginger Spin Runner", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("Lives $lives   Rings $rings   Score $score   Task ${if (taskUsed) "0" else "1"}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF143857),
            border = BorderStroke(2.dp, Color(0xFF25382F)),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val scale = (h / 310f).coerceIn(1f, 2.2f)
                fun sx(worldX: Float) = (worldX - worldOffset) * scale
                fun sy(worldY: Float) = worldY * scale
                drawRect(Color(0xFF1D7CC0))
                repeat(7) { layer ->
                    val layerSpeed = 0.12f + layer * 0.05f
                    val baseY = h * (0.32f + layer * 0.035f)
                    var x = -((worldOffset * layerSpeed) % (160f * scale))
                    while (x < w + 180f * scale) {
                        val hill = Path().apply {
                            moveTo(x, baseY + 65f * scale)
                            lineTo(x + 84f * scale, baseY - (20f + layer * 5f) * scale)
                            lineTo(x + 168f * scale, baseY + 65f * scale)
                            close()
                        }
                        drawPath(hill, Color(0xFF2F7A62).copy(alpha = 0.22f + layer * 0.045f))
                        x += 155f * scale
                    }
                }
                repeat(9) { stripe ->
                    val x = ((stripe * 120f - worldOffset * 0.8f) % (w + 140f)) - 70f
                    drawLine(
                        color = Color.White.copy(alpha = 0.12f),
                        start = Offset(x, h * 0.15f),
                        end = Offset(x + 80f, h * 0.04f),
                        strokeWidth = 5f
                    )
                }
                val terrain = Path().apply {
                    moveTo(0f, h)
                    var px = 0f
                    while (px <= w + 12f) {
                        val worldX = worldOffset + px / scale
                        lineTo(px, sy(terrainY(worldX)))
                        px += 12f
                    }
                    lineTo(w, h)
                    close()
                }
                drawPath(terrain, Color(0xFF2C704A))
                var tileX = -((worldOffset * scale) % (32f * scale))
                while (tileX < w) {
                    val top = sy(terrainY(worldOffset + tileX / scale))
                    drawRect(
                        color = if (((tileX / (32f * scale)).toInt() % 2) == 0) Color(0xFFFFC66D) else Color(0xFFB66A35),
                        topLeft = Offset(tileX, top + 8f * scale),
                        size = Size(34f * scale, 20f * scale)
                    )
                    tileX += 32f * scale
                }
                List(10) { 360f + it * 250f }.forEachIndexed { enemyIndex, enemyBaseX ->
                    val enemyX = enemyBaseX + ((worldOffset / 2600f).toInt() * 2600f)
                    val x = sx(enemyX)
                    if (x > -60f && x < w + 60f) {
                        val y = sy(terrainY(enemyX)) - 30f * scale
                        drawOpponentSprite(Offset(x, y), scale, enemyIndex)
                    }
                }
                List(8) { 520f + it * 330f }.forEachIndexed { capsuleIndex, capsuleBaseX ->
                    val capsuleX = capsuleBaseX + ((worldOffset / 2700f).toInt() * 2700f)
                    val x = sx(capsuleX)
                    if (x > -70f && x < w + 70f) {
                        val y = sy(terrainY(capsuleX) - 68f - (capsuleIndex % 2) * 22f)
                        val pulse = 1f + 0.12f * sin((worldOffset / 15f + capsuleIndex).toFloat())
                        drawCircle(Color(0xFFFFE066), 12f * scale * pulse, Offset(x, y))
                        drawCircle(Color.White.copy(alpha = 0.75f), 4f * scale, Offset(x - 3f * scale, y - 4f * scale))
                        drawCircle(Color(0xFF25382F).copy(alpha = 0.24f), 12f * scale * pulse, Offset(x, y), style = Stroke(width = 2f * scale))
                    }
                }
                val flashHidden = invulnerableTicks > 0 && invulnerableTicks % 8 < 4
                if (!flashHidden) {
                    if (spinning || velocityY != 0f) {
                        drawSpinGingerBall(
                            center = Offset(runnerX * scale, sy(runnerY)),
                            radius = runnerRadius * scale,
                            angle = spinAngle
                        )
                    } else {
                        drawPixelGingerCat(
                            topLeft = Offset(runnerX * scale - 15f * scale, sy(runnerY) - 24f * scale),
                            pixel = 4.2f * scale,
                            facingRight = true
                        )
                    }
                }
            }
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFFFF7E8),
            border = BorderStroke(1.dp, Color(0xFFFFB45F).copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(currentQuestion?.front ?: message, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (currentQuestion != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        currentChoices.forEach { choice ->
                            OutlinedButton(
                                onClick = { answerLifeQuestion(choice) },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(choice.shortGameText(), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { boost = !boost },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) { Text(if (boost) "Cruise" else "Boost") }
            Button(
                onClick = {
                    if (velocityY == 0f) {
                        velocityY = -9.6f
                        spinning = true
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Jump") }
            OutlinedButton(
                onClick = { spinning = true; spinAngle = (spinAngle + 120f) % 360f },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Spin") }
        }
        Button(
            onClick = { startLifeQuestion() },
            enabled = !taskUsed && lifeQuestionIndex == null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) { Text("Задание") }
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Close game")
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSpinGingerBall(
    center: Offset,
    radius: Float,
    angle: Float
) {
    val outline = Color(0xFF5C3218)
    drawCircle(Color(0xFFFFA64A), radius, center)
    drawCircle(Color(0xFFFFD39A), radius * 0.52f, Offset(center.x - radius * 0.08f, center.y + radius * 0.06f))
    drawCircle(outline, radius, center, style = Stroke(width = radius * 0.16f))
    repeat(4) { stripe ->
        val a = Math.toRadians((angle + stripe * 90f).toDouble()).toFloat()
        drawLine(
            color = outline.copy(alpha = 0.58f),
            start = Offset(center.x + cos(a) * radius * 0.18f, center.y + sin(a) * radius * 0.18f),
            end = Offset(center.x + cos(a) * radius * 0.9f, center.y + sin(a) * radius * 0.9f),
            strokeWidth = radius * 0.12f
        )
    }
    drawCircle(Color.White.copy(alpha = 0.8f), radius * 0.13f, Offset(center.x - radius * 0.32f, center.y - radius * 0.36f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawOpponentSprite(
    center: Offset,
    scale: Float,
    variant: Int
) {
    val body = if (variant % 2 == 0) Color(0xFF7B58D6) else Color(0xFFE45757)
    val dark = Color(0xFF25382F)
    val size = 22f * scale
    drawRoundRect(
        color = body,
        topLeft = Offset(center.x - size / 2f, center.y - size / 2f),
        size = Size(size, size),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(7f * scale, 7f * scale)
    )
    repeat(6) { spike ->
        val angle = (spike / 6f) * Math.PI.toFloat() * 2f
        drawLine(
            color = dark.copy(alpha = 0.62f),
            start = Offset(center.x + cos(angle) * size * 0.36f, center.y + sin(angle) * size * 0.36f),
            end = Offset(center.x + cos(angle) * size * 0.72f, center.y + sin(angle) * size * 0.72f),
            strokeWidth = 3f * scale
        )
    }
    drawCircle(Color.White, 3.5f * scale, Offset(center.x - 4f * scale, center.y - 2f * scale))
    drawCircle(Color.White, 3.5f * scale, Offset(center.x + 5f * scale, center.y - 2f * scale))
    drawCircle(dark, 1.7f * scale, Offset(center.x - 3f * scale, center.y - 2f * scale))
    drawCircle(dark, 1.7f * scale, Offset(center.x + 6f * scale, center.y - 2f * scale))
}

@Composable
private fun GingerPixelCatMark(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawPixelGingerCat(
            topLeft = Offset(size.width * 0.18f, size.height * 0.12f),
            pixel = (size.minDimension / 9f).coerceAtLeast(3f),
            facingRight = true
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPixelGingerCat(
    topLeft: Offset,
    pixel: Float,
    facingRight: Boolean
) {
    val fur = Color(0xFFFFA64A)
    val furDark = Color(0xFFD16D2E)
    val outline = Color(0xFF5C3218)
    val cream = Color(0xFFFFE0B0)
    val eye = Color(0xFF17251F)
    fun rect(x: Int, y: Int, w: Int, h: Int, color: Color) {
        drawRect(color, Offset(topLeft.x + x * pixel, topLeft.y + y * pixel), Size(w * pixel, h * pixel))
    }
    rect(1, 1, 2, 2, furDark)
    rect(6, 1, 2, 2, furDark)
    rect(2, 2, 5, 1, fur)
    rect(1, 3, 7, 4, fur)
    rect(2, 5, 5, 2, cream)
    rect(if (facingRight) 5 else 3, 4, 1, 1, eye)
    rect(if (facingRight) 7 else 1, 6, 1, 1, outline)
    rect(3, 7, 4, 3, fur)
    rect(2, 8, 6, 2, fur)
    rect(2, 10, 2, 2, outline)
    rect(6, 10, 2, 2, outline)
    rect(if (facingRight) 8 else 0, 8, 2, 1, furDark)
    rect(if (facingRight) 9 else -1, 7, 1, 1, furDark)
    rect(1, 3, 1, 1, outline)
    rect(7, 3, 1, 1, outline)
    rect(1, 6, 1, 1, outline)
    rect(7, 6, 1, 1, outline)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawExpressiveGingerGameCat(
    center: Offset,
    radius: Float,
    action: RiveCatAction,
    frame: Float,
    invulnerable: Boolean
) {
    val wave = sin(frame * 6.28f)
    val hop = when (action) {
        RiveCatAction.DANCE -> -abs(wave) * radius * 0.24f
        RiveCatAction.MEOW,
        RiveCatAction.TALK -> -abs(wave) * radius * 0.08f
        else -> 0f
    }
    val c = Offset(center.x, center.y + hop)
    val outline = Color(0xFF54290F).copy(alpha = if (invulnerable) 0.46f else 1f)
    val fur = Color(0xFFFF9F32).copy(alpha = if (invulnerable) 0.55f else 1f)
    val furDark = Color(0xFFD66620).copy(alpha = if (invulnerable) 0.55f else 1f)
    val furLight = Color(0xFFFFD49A).copy(alpha = if (invulnerable) 0.55f else 1f)
    val cheek = Color(0xFFFFB7AA).copy(alpha = if (invulnerable) 0.55f else 1f)
    val bodyW = if (action == RiveCatAction.CRAWL) radius * 1.72f else radius * 1.20f
    val bodyH = if (action == RiveCatAction.CRAWL) radius * 0.58f else radius * 1.05f
    val bodyCenter = Offset(c.x, c.y + radius * 0.30f)
    val headCenter = Offset(c.x, c.y - if (action == RiveCatAction.CRAWL) radius * 0.16f else radius * 0.48f)
    val tail = Path().apply {
        moveTo(bodyCenter.x + bodyW * 0.42f, bodyCenter.y + bodyH * 0.02f)
        cubicTo(
            bodyCenter.x + bodyW * 0.92f,
            bodyCenter.y - bodyH * (0.42f + wave * 0.08f),
            bodyCenter.x + bodyW * 0.74f,
            bodyCenter.y - bodyH * 0.96f,
            bodyCenter.x + bodyW * 0.44f,
            bodyCenter.y - bodyH * 0.70f
        )
    }
    drawPath(tail, furDark, style = Stroke(width = radius * 0.18f))
    drawPath(tail, outline, style = Stroke(width = radius * 0.05f))
    drawOval(
        color = fur,
        topLeft = Offset(bodyCenter.x - bodyW / 2f, bodyCenter.y - bodyH / 2f),
        size = Size(bodyW, bodyH)
    )
    drawOval(
        color = outline,
        topLeft = Offset(bodyCenter.x - bodyW / 2f, bodyCenter.y - bodyH / 2f),
        size = Size(bodyW, bodyH),
        style = Stroke(width = radius * 0.07f)
    )
    val legLift = if (action == RiveCatAction.RUN || action == RiveCatAction.DANCE) wave * radius * 0.12f else 0f
    listOf(-0.36f, 0.32f).forEachIndexed { index, xFactor ->
        val x = bodyCenter.x + bodyW * xFactor
        val y = bodyCenter.y + bodyH * 0.43f + if (index == 0) legLift else -legLift
        drawRoundRect(
            color = furDark,
            topLeft = Offset(x - radius * 0.14f, y - radius * 0.05f),
            size = Size(radius * 0.28f, radius * 0.38f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius * 0.10f, radius * 0.10f)
        )
    }
    val leftEar = Path().apply {
        moveTo(headCenter.x - radius * 0.45f, headCenter.y - radius * 0.40f)
        lineTo(headCenter.x - radius * 0.75f, headCenter.y - radius * 0.92f)
        lineTo(headCenter.x - radius * 0.10f, headCenter.y - radius * 0.62f)
        close()
    }
    val rightEar = Path().apply {
        moveTo(headCenter.x + radius * 0.45f, headCenter.y - radius * 0.40f)
        lineTo(headCenter.x + radius * 0.75f, headCenter.y - radius * 0.92f)
        lineTo(headCenter.x + radius * 0.10f, headCenter.y - radius * 0.62f)
        close()
    }
    drawPath(leftEar, fur)
    drawPath(rightEar, fur)
    drawPath(leftEar, outline, style = Stroke(width = radius * 0.055f))
    drawPath(rightEar, outline, style = Stroke(width = radius * 0.055f))
    drawCircle(fur, radius * 0.66f, headCenter)
    drawCircle(furLight, radius * 0.36f, Offset(headCenter.x, headCenter.y + radius * 0.10f))
    drawCircle(outline, radius * 0.66f, headCenter, style = Stroke(width = radius * 0.065f))
    drawCircle(outline, radius * 0.055f, Offset(headCenter.x - radius * 0.24f, headCenter.y - radius * 0.10f))
    drawCircle(outline, radius * 0.055f, Offset(headCenter.x + radius * 0.24f, headCenter.y - radius * 0.10f))
    drawCircle(cheek, radius * 0.10f, Offset(headCenter.x - radius * 0.38f, headCenter.y + radius * 0.12f))
    drawCircle(cheek, radius * 0.10f, Offset(headCenter.x + radius * 0.38f, headCenter.y + radius * 0.12f))
    drawCircle(outline, radius * 0.045f, Offset(headCenter.x, headCenter.y + radius * 0.05f))
    val mouthOpen = action == RiveCatAction.MEOW || action == RiveCatAction.TALK
    if (mouthOpen) {
        drawOval(
            color = Color(0xFF3B1269),
            topLeft = Offset(headCenter.x - radius * 0.13f, headCenter.y + radius * 0.16f),
            size = Size(radius * 0.26f, radius * (0.18f + abs(wave) * 0.12f))
        )
    } else {
        drawLine(outline, Offset(headCenter.x, headCenter.y + radius * 0.10f), Offset(headCenter.x - radius * 0.13f, headCenter.y + radius * 0.22f), strokeWidth = radius * 0.035f)
        drawLine(outline, Offset(headCenter.x, headCenter.y + radius * 0.10f), Offset(headCenter.x + radius * 0.13f, headCenter.y + radius * 0.22f), strokeWidth = radius * 0.035f)
    }
    val pawY = bodyCenter.y - bodyH * 0.16f
    if (action == RiveCatAction.SCRATCH) {
        drawLine(outline, Offset(bodyCenter.x + radius * 0.25f, pawY), Offset(headCenter.x + radius * 0.46f, headCenter.y - radius * 0.30f), strokeWidth = radius * 0.08f)
        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.rgb(255, 247, 232)
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = radius * 0.50f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            drawText("?", headCenter.x + radius * 0.82f, headCenter.y - radius * 0.55f, paint)
        }
    } else {
        val pawLift = if (action == RiveCatAction.DANCE || action == RiveCatAction.MEOW || action == RiveCatAction.TALK) radius * (0.34f + abs(wave) * 0.12f) else 0f
        drawLine(outline, Offset(bodyCenter.x - radius * 0.38f, pawY), Offset(bodyCenter.x - radius * 0.62f, pawY - pawLift), strokeWidth = radius * 0.08f)
        drawLine(outline, Offset(bodyCenter.x + radius * 0.38f, pawY), Offset(bodyCenter.x + radius * 0.62f, pawY - pawLift * 0.82f), strokeWidth = radius * 0.08f)
    }
    if (action == RiveCatAction.DANCE || action == RiveCatAction.TALK) {
        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.rgb(255, 221, 74)
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = radius * 0.42f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            drawText(if (action == RiveCatAction.TALK) "..." else "♪", center.x - radius * 0.90f, center.y - radius * 0.88f, paint)
            drawText(if (action == RiveCatAction.TALK) "!" else "♫", center.x + radius * 0.88f, center.y - radius * 0.70f, paint)
        }
    }
    if (action == RiveCatAction.MEOW) {
        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.WHITE
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = radius * 0.30f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            drawText("MEOW", center.x, center.y - radius * 1.18f, paint)
        }
    }
}

@Composable
private fun GingerFluffyCatMark(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawGingerFluffyCat(
            center = Offset(size.width / 2f, size.height * 0.55f),
            bodyRadius = size.minDimension * 0.33f,
            bob = 0f
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGingerFluffyCat(
    center: Offset,
    bodyRadius: Float,
    bob: Float
) {
    val c = Offset(center.x, center.y + bob)
    val outline = Color(0xFF5C3218)
    val fur = Color(0xFFFFA64A)
    val furLight = Color(0xFFFFD39A)
    val cheek = Color(0xFFFFC0A0)
    val earLeft = Path().apply {
        moveTo(c.x - bodyRadius * 0.68f, c.y - bodyRadius * 0.58f)
        lineTo(c.x - bodyRadius * 1.06f, c.y - bodyRadius * 1.22f)
        lineTo(c.x - bodyRadius * 0.22f, c.y - bodyRadius * 0.92f)
        close()
    }
    val earRight = Path().apply {
        moveTo(c.x + bodyRadius * 0.68f, c.y - bodyRadius * 0.58f)
        lineTo(c.x + bodyRadius * 1.06f, c.y - bodyRadius * 1.22f)
        lineTo(c.x + bodyRadius * 0.22f, c.y - bodyRadius * 0.92f)
        close()
    }
    drawPath(earLeft, fur)
    drawPath(earRight, fur)
    drawPath(earLeft, outline, style = Stroke(width = bodyRadius * 0.08f))
    drawPath(earRight, outline, style = Stroke(width = bodyRadius * 0.08f))
    drawCircle(fur, bodyRadius, c)
    drawCircle(furLight, bodyRadius * 0.55f, Offset(c.x, c.y + bodyRadius * 0.12f))
    drawCircle(outline, bodyRadius, c, style = Stroke(width = bodyRadius * 0.08f))
    repeat(9) { tuft ->
        val angle = -2.8f + tuft * 0.7f
        val start = Offset(c.x + sin(angle) * bodyRadius * 0.72f, c.y - bodyRadius * 0.72f)
        val end = Offset(start.x + sin(angle) * bodyRadius * 0.28f, start.y - bodyRadius * 0.18f)
        drawLine(outline.copy(alpha = 0.42f), start, end, strokeWidth = bodyRadius * 0.04f)
    }
    drawCircle(outline, bodyRadius * 0.08f, Offset(c.x - bodyRadius * 0.34f, c.y - bodyRadius * 0.12f))
    drawCircle(outline, bodyRadius * 0.08f, Offset(c.x + bodyRadius * 0.34f, c.y - bodyRadius * 0.12f))
    drawCircle(cheek, bodyRadius * 0.13f, Offset(c.x - bodyRadius * 0.5f, c.y + bodyRadius * 0.13f))
    drawCircle(cheek, bodyRadius * 0.13f, Offset(c.x + bodyRadius * 0.5f, c.y + bodyRadius * 0.13f))
    drawLine(outline, Offset(c.x, c.y + bodyRadius * 0.02f), Offset(c.x, c.y + bodyRadius * 0.18f), strokeWidth = bodyRadius * 0.04f)
    drawLine(outline, Offset(c.x, c.y + bodyRadius * 0.18f), Offset(c.x - bodyRadius * 0.18f, c.y + bodyRadius * 0.30f), strokeWidth = bodyRadius * 0.04f)
    drawLine(outline, Offset(c.x, c.y + bodyRadius * 0.18f), Offset(c.x + bodyRadius * 0.18f, c.y + bodyRadius * 0.30f), strokeWidth = bodyRadius * 0.04f)
    drawLine(outline.copy(alpha = 0.62f), Offset(c.x - bodyRadius * 0.62f, c.y + bodyRadius * 0.06f), Offset(c.x - bodyRadius * 1.1f, c.y - bodyRadius * 0.02f), strokeWidth = bodyRadius * 0.035f)
    drawLine(outline.copy(alpha = 0.62f), Offset(c.x + bodyRadius * 0.62f, c.y + bodyRadius * 0.06f), Offset(c.x + bodyRadius * 1.1f, c.y - bodyRadius * 0.02f), strokeWidth = bodyRadius * 0.035f)
    drawCircle(Color.White.copy(alpha = 0.8f), bodyRadius * 0.05f, Offset(c.x - bodyRadius * 0.38f, c.y - bodyRadius * 0.18f))
    drawCircle(Color.White.copy(alpha = 0.8f), bodyRadius * 0.05f, Offset(c.x + bodyRadius * 0.30f, c.y - bodyRadius * 0.18f))
}

private fun String.shortGameText(): String {
    val clean = replace(Regex("\\s+"), " ").trim()
    return if (clean.length <= 16) clean else clean.take(14).trimEnd() + ".."
}

private fun gingerHtml5GameDocument(cardsJson: String): String {
    return """
        <!doctype html>
        <html>
        <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
        <style>
          html, body { margin:0; padding:0; width:100%; height:100%; overflow:hidden; background:#fff7e8; font-family:Arial, sans-serif; }
          #game { width:100vw; height:100vh; display:block; touch-action:none; }
        </style>
        </head>
        <body>
        <canvas id="game"></canvas>
        <script>
        const cards = $cardsJson;
        const canvas = document.getElementById('game');
        const ctx = canvas.getContext('2d');
        let index = 0;
        let score = 0;
        let tick = 0;
        let message = 'Tap the correct floating card.';
        function resize(){ canvas.width = Math.floor(window.innerWidth * devicePixelRatio); canvas.height = Math.floor(window.innerHeight * devicePixelRatio); }
        window.addEventListener('resize', resize); resize();
        function card(){ return cards.length ? cards[index % cards.length] : {front:'MurrLex', back:'cat'}; }
        function options(){
          const current = card().back;
          const values = [current];
          for(let i=0;i<cards.length && values.length<3;i++){
            const candidate = cards[(index+i+1)%cards.length].back;
            if(values.indexOf(candidate) < 0) values.push(candidate);
          }
          return values.sort((a,b)=> ((a.length + index) % 3) - ((b.length + index) % 3));
        }
        function drawCat(cx, cy, r){
          ctx.fillStyle='#ffa64a'; ctx.strokeStyle='#5c3218'; ctx.lineWidth=r*.08; ctx.lineJoin='round';
          ctx.beginPath(); ctx.moveTo(cx-r*.7,cy-r*.55); ctx.lineTo(cx-r*1.05,cy-r*1.18); ctx.lineTo(cx-r*.22,cy-r*.9); ctx.close(); ctx.fill(); ctx.stroke();
          ctx.beginPath(); ctx.moveTo(cx+r*.7,cy-r*.55); ctx.lineTo(cx+r*1.05,cy-r*1.18); ctx.lineTo(cx+r*.22,cy-r*.9); ctx.close(); ctx.fill(); ctx.stroke();
          ctx.beginPath(); ctx.arc(cx,cy,r,0,Math.PI*2); ctx.fill(); ctx.stroke();
          ctx.fillStyle='#ffd39a'; ctx.beginPath(); ctx.arc(cx,cy+r*.1,r*.55,0,Math.PI*2); ctx.fill();
          ctx.fillStyle='#5c3218'; ctx.beginPath(); ctx.arc(cx-r*.34,cy-r*.12,r*.08,0,Math.PI*2); ctx.arc(cx+r*.34,cy-r*.12,r*.08,0,Math.PI*2); ctx.fill();
          ctx.strokeStyle='#5c3218'; ctx.lineWidth=r*.04; ctx.beginPath(); ctx.moveTo(cx,cy+r*.03); ctx.lineTo(cx,cy+r*.2); ctx.lineTo(cx-r*.18,cy+r*.31); ctx.moveTo(cx,cy+r*.2); ctx.lineTo(cx+r*.18,cy+r*.31); ctx.stroke();
          for(let i=0;i<5;i++){ const y=cy-r*.05+i*r*.09; ctx.beginPath(); ctx.moveTo(cx-r*.62,y); ctx.lineTo(cx-r*1.08,y-r*.08); ctx.moveTo(cx+r*.62,y); ctx.lineTo(cx+r*1.08,y-r*.08); ctx.stroke(); }
        }
        function tokenRect(i){
          const w=canvas.width, h=canvas.height, baseY=h*.44 + Math.sin(tick*.04+i)*h*.04;
          return {x:w*(.18+i*.27), y:baseY+i*h*.08, w:w*.24, h:h*.13};
        }
        function draw(){
          tick++;
          ctx.clearRect(0,0,canvas.width,canvas.height);
          const w=canvas.width, h=canvas.height;
          ctx.fillStyle='#fff7e8'; ctx.fillRect(0,0,w,h);
          ctx.fillStyle='#25382f'; ctx.font='bold '+Math.floor(w*.055)+'px Arial'; ctx.textAlign='center';
          ctx.fillText('HTML5 Canvas Flash-style', w/2, h*.09);
          ctx.font='bold '+Math.floor(w*.045)+'px Arial'; ctx.fillText(card().front, w/2, h*.19);
          ctx.font=Math.floor(w*.034)+'px Arial'; ctx.fillText('Score '+score+'  '+message, w/2, h*.28);
          const opts=options();
          opts.forEach((text,i)=>{
            const r=tokenRect(i);
            ctx.fillStyle=['#ffe0a8','#e9f7ef','#ffeaf0'][i%3]; ctx.strokeStyle='#d78842'; ctx.lineWidth=4;
            ctx.beginPath(); ctx.roundRect(r.x,r.y,r.w,r.h,22); ctx.fill(); ctx.stroke();
            ctx.fillStyle='#263b32'; ctx.font='bold '+Math.floor(w*.034)+'px Arial'; ctx.fillText(text.length>15?text.slice(0,13)+'..':text, r.x+r.w/2, r.y+r.h*.58);
          });
          drawCat(w/2, h*.8 + Math.sin(tick*.06)*8, Math.min(w,h)*.12);
          requestAnimationFrame(draw);
        }
        canvas.addEventListener('pointerdown', ev=>{
          const rect=canvas.getBoundingClientRect();
          const x=(ev.clientX-rect.left)*devicePixelRatio, y=(ev.clientY-rect.top)*devicePixelRatio;
          const opts=options();
          opts.forEach((text,i)=>{
            const r=tokenRect(i);
            if(x>=r.x && x<=r.x+r.w && y>=r.y && y<=r.y+r.h){
              if(text.toLowerCase().trim()===card().back.toLowerCase().trim()){ score++; message='purrfect'; index=(index+1)%Math.max(cards.length,1); }
              else { message='answer: '+card().back; }
            }
          });
        });
        draw();
        </script>
        </body>
        </html>
    """.trimIndent()
}

@Composable
private fun MessageBubble(
    text: String,
    visible: Boolean,
    positive: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(140)) + slideInVertically(
            animationSpec = tween(180),
            initialOffsetY = { -it / 2 }
        ),
        exit = fadeOut(animationSpec = tween(180)) + slideOutVertically(
            animationSpec = tween(180),
            targetOffsetY = { -it / 2 }
        ),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            color = if (positive) CompletedFrameColor.copy(alpha = 0.98f) else Color.White.copy(alpha = 0.98f)
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (positive) Color(0xFF17351F) else Color(0xFF202821)
            )
        }
    }
}

@Composable
private fun VoiceDraftField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    singleLine: Boolean = false,
    onVoiceInput: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            label = { Text(label) },
            singleLine = singleLine
        )
        if (onVoiceInput != null) {
            IconButton(onClick = onVoiceInput) {
                Icon(Icons.Default.Mic, contentDescription = "Voice input for $label")
            }
        }
    }
}
@Composable
private fun StudyCardEditorDialog(
    cardDraft: CardDraft,
    onCardDraftChange: (CardDraft) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    onVoiceInput: (VoiceInputTarget) -> Unit
) {
    val ui = rememberUiText()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(ui.deleteCardTitle) },
            text = { Text(ui.deleteCardText) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = ui.editCard,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = ui.editCardSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Surface(
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 1.dp,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(430.dp)
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    VoiceDraftField(
                        value = cardDraft.nativeValue,
                        onValueChange = { onCardDraftChange(cardDraft.copy(nativeValue = it)) },
                        label = ui.mistakeSource,
                        onVoiceInput = { onVoiceInput(VoiceInputTarget.CARD_NATIVE) }
                    )
                    VoiceDraftField(
                        value = cardDraft.correctValue,
                        onValueChange = { onCardDraftChange(cardDraft.copy(correctValue = it)) },
                        label = ui.makeItRight,
                        onVoiceInput = { onVoiceInput(VoiceInputTarget.CARD_CORRECT) }
                    )
                    VoiceDraftField(
                        value = cardDraft.hint,
                        onValueChange = { onCardDraftChange(cardDraft.copy(hint = it)) },
                        label = ui.hintOrRule,
                        onVoiceInput = { onVoiceInput(VoiceInputTarget.CARD_HINT) }
                    )
                    VoiceDraftField(
                        value = cardDraft.original,
                        onValueChange = { onCardDraftChange(cardDraft.copy(original = it)) },
                        label = "Original",
                        onVoiceInput = null
                    )
                    VoiceDraftField(
                        value = cardDraft.madeAt,
                        onValueChange = { onCardDraftChange(cardDraft.copy(madeAt = it)) },
                        label = ui.madeAt,
                        singleLine = true
                    )
                    VoiceDraftField(
                        value = cardDraft.where,
                        onValueChange = { onCardDraftChange(cardDraft.copy(where = it)) },
                        label = ui.where,
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Delete")
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@Composable
private fun SplashScreen(soundEffectsEnabled: Boolean, vibrationEnabled: Boolean) {
    val context = LocalContext.current
    var started by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(durationMillis = 900),
        label = "splashMark"
    )
    LaunchedEffect(Unit) {
        performFeedback(context, soundEffectsEnabled, vibrationEnabled, FeedbackCue.SPLASH)
        started = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ProfessorCatMark(
                progress = progress,
                modifier = Modifier
                    .size(128.dp)
                    .graphicsLayer {
                        translationY = 14f * (1f - progress)
                        scaleX = 0.86f + 0.14f * progress
                        scaleY = 0.86f + 0.14f * progress
                        alpha = progress
                    }
            )
            Text(
                text = "MurrLex",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = BrandSaladColor,
                modifier = Modifier.graphicsLayer {
                    translationY = 18f * (1f - progress)
                    alpha = progress
                }
            )
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    cardStartSide: CardStartSide,
    excludeMasteredCards: Boolean,
    showCardLog: Boolean,
    interfaceLanguage: String,
    displayTextSize: DisplayTextSize,
    controlSize: ControlSize,
    soundEffectsEnabled: Boolean,
    vibrationEnabled: Boolean,
    notificationIntervalDraft: String,
    notificationMaxDraft: String,
    quickVocabularySourceLanguage: String,
    quickVocabularyTargetLanguage: String,
    useLocalTranslation: Boolean,
    autoSaveTranslatorCards: Boolean,
    translationApiUrl: String,
    translationApiToken: String,
    useOpenAiModels: Boolean,
    openAiBaseUrl: String,
    openAiApiKey: String,
    serverUsername: String,
    openAiSpeechModel: String,
    openAiTextModel: String,
    openAiImageTextModel: String,
    openAiTtsModel: String,
    openAiTtsVoice: String,
    belarusianTtsProvider: String,
    elevenLabsApiKey: String,
    elevenLabsModel: String,
    elevenLabsVoiceId: String,
    elevenLabsTtsLanguageCodes: Set<String>,
    openAiCacheDurationMinutes: Long,
    openAiVoiceSilenceTimeoutMs: Long,
    cardStatusBlinkIntervalMs: Long,
    catDialogRetentionDays: Int,
    catReplySpeechRate: Float,
    openAiActivityLog: List<String>,
    offlineSpeechLanguageTag: String,
    offlineSpeechStatuses: Map<String, String>,
    offlineSpeechDownloadingTag: String?,
    downloadedTranslationLanguages: Set<String>,
    translationDownloadLanguage: String,
    translationDownloadingLabel: String?,
    onTranslationDownloadLanguageChange: (String) -> Unit,
    onDownloadSelectedTranslationLanguage: () -> Unit,
    onOfflineSpeechLanguageChange: (String) -> Unit,
    onDownloadOfflineSpeechModel: (String) -> Unit,
    onOpenLocalLanguages: () -> Unit,
    onCardStartSideChange: (CardStartSide) -> Unit,
    onExcludeMasteredCardsChange: (Boolean) -> Unit,
    onShowCardLogChange: (Boolean) -> Unit,
    onInterfaceLanguageChange: (String) -> Unit,
    onDisplayTextSizeChange: (DisplayTextSize) -> Unit,
    onControlSizeChange: (ControlSize) -> Unit,
    onSoundEffectsEnabledChange: (Boolean) -> Unit,
    onVibrationEnabledChange: (Boolean) -> Unit,
    onNotificationIntervalChange: (String) -> Unit,
    onNotificationMaxChange: (String) -> Unit,
    onQuickVocabularySourceChange: (String) -> Unit,
    onQuickVocabularyTargetChange: (String) -> Unit,
    onUseLocalTranslationChange: (Boolean) -> Unit,
    onAutoSaveTranslatorCardsChange: (Boolean) -> Unit,
    onTranslationApiUrlChange: (String) -> Unit,
    onTranslationApiTokenChange: (String) -> Unit,
    onUseOpenAiModelsChange: (Boolean) -> Unit,
    onOpenAiBaseUrlChange: (String) -> Unit,
    onOpenAiApiKeyChange: (String) -> Unit,
    onMurrLexServerLogin: (String, String, Boolean, String) -> Unit,
    onMurrLexServerLogout: () -> Unit,
    onOpenAiSpeechModelChange: (String) -> Unit,
    onOpenAiTextModelChange: (String) -> Unit,
    onOpenAiImageTextModelChange: (String) -> Unit,
    onOpenAiTtsModelChange: (String) -> Unit,
    onOpenAiTtsVoiceChange: (String) -> Unit,
    onBelarusianTtsProviderChange: (String) -> Unit,
    onElevenLabsApiKeyChange: (String) -> Unit,
    onElevenLabsModelChange: (String) -> Unit,
    onElevenLabsVoiceIdChange: (String) -> Unit,
    onElevenLabsTtsLanguageEnabledChange: (String, Boolean) -> Unit,
    onOpenAiCacheDurationChange: (Long) -> Unit,
    onOpenAiVoiceSilenceTimeoutChange: (Long) -> Unit,
    onCardStatusBlinkIntervalChange: (Long) -> Unit,
    onCatDialogRetentionDaysChange: (Int) -> Unit,
    onCatReplySpeechRateChange: (Float) -> Unit,
    onClearOpenAiCache: () -> Unit,
    onSaveNotificationInterval: () -> Unit,
    onSaveNotificationMax: () -> Unit,
    onDownloadTranslationLanguages: () -> Unit,
    onDownloadSampleJson: () -> Unit
) {
    val ui = rememberUiText()
    val context = LocalContext.current
    val downloadedTranslationLabel = remember(downloadedTranslationLanguages) {
        downloadedTranslationLanguages.sorted().joinToString().ifBlank { "none" }
    }
    val versionLogLines = remember { versionLogText().lines() }
    fun st(en: String, de: String, be: String, es: String, uk: String, ru: String, pl: String): String {
        return en
    }
    var offlineSpeechHint by remember { mutableStateOf<String?>(null) }
    var showCrashLogs by remember { mutableStateOf(false) }
    var showServerLoginDialog by remember { mutableStateOf(false) }
    var serverLogin by remember { mutableStateOf("") }
    var serverPassword by remember { mutableStateOf("") }
    var serverEmail by remember { mutableStateOf("") }
    var createServerAccount by remember { mutableStateOf(false) }
    var crashLogs by remember { mutableStateOf(CrashLogStore.load(context)) }
    fun refreshCrashLogs() {
        crashLogs = CrashLogStore.load(context)
    }
    LaunchedEffect(offlineSpeechHint) {
        if (offlineSpeechHint != null) {
            delay(1500)
            offlineSpeechHint = null
        }
    }
    if (showServerLoginDialog) {
        AlertDialog(
            onDismissRequest = {
                serverPassword = ""
                showServerLoginDialog = false
            },
            title = { Text(if (createServerAccount) "Create MurrLex account" else "MurrLex server login") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = serverLogin,
                        onValueChange = { serverLogin = it },
                        label = { Text("Username or email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (createServerAccount) {
                        OutlinedTextField(
                            value = serverEmail,
                            onValueChange = { serverEmail = it },
                            label = { Text("Email") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    OutlinedTextField(
                        value = serverPassword,
                        onValueChange = { serverPassword = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(onClick = { createServerAccount = !createServerAccount }) {
                        Text(if (createServerAccount) "I already have an account" else "Create account")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onMurrLexServerLogin(serverLogin, serverPassword, createServerAccount, serverEmail)
                        serverPassword = ""
                        showServerLoginDialog = false
                    },
                    enabled = serverLogin.isNotBlank() && serverPassword.isNotBlank() && (!createServerAccount || serverEmail.isNotBlank())
                ) { Text(if (createServerAccount) "Create" else "Login") }
            },
            dismissButton = {
                TextButton(onClick = {
                    serverPassword = ""
                    showServerLoginDialog = false
                }) { Text("Cancel") }
            }
        )
    }
    if (showCrashLogs) {
        AlertDialog(
            onDismissRequest = { showCrashLogs = false },
            title = { Text("Last crash logs") },
            text = {
                SelectionContainer {
                    Text(
                        text = crashLogs.ifEmpty { listOf("No crash logs saved yet.") }.joinToString("\n\n---\n\n"),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showCrashLogs = false }) { Text("Close") }
            }
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = ui.settings,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = "Version ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "MurrLex account",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (openAiApiKey.isBlank()) {
                    Text(
                        text = "Sign in to use online translation, speech recognition, and audio generation through MurrLex server.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { showServerLoginDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Sign in") }
                } else {
                    Text(
                        text = "Signed in as ${serverUsername.ifBlank { serverEmail.ifBlank { "MurrLex user" } }}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedButton(
                        onClick = onMurrLexServerLogout,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Log out") }
                }
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Crash diagnostics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Saved crashes: ${crashLogs.size}/2. Use Share when you want to send the latest technical error log.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            refreshCrashLogs()
                            showCrashLogs = true
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text("View")
                    }
                    OutlinedButton(
                        onClick = {
                            refreshCrashLogs()
                            shareCrashLogs(context, CrashLogStore.load(context))
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Share")
                    }
                    TextButton(
                        onClick = {
                            CrashLogStore.clear(context)
                            refreshCrashLogs()
                        },
                        modifier = Modifier.weight(0.8f)
                    ) {
                        Text("Clear")
                    }
                }
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = ui.interfaceLanguage,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                val languageOptions = listOf(
                    "en" to "EN - English",
                    "es" to "ES - Espanol",
                    "pl" to "PL - Polski",
                    "ru" to "\uD83C\uDFF3\uFE0F RU - Russian",
                    "be" to "\uD83C\uDFF3\uFE0F BY - Belarusian",
                    "uk" to "UA - Ukrainian",
                    "de" to "DE - Deutsch",
                    "lv" to "LV - Latvian",
                    "lt" to "LT - Lithuanian",
                    "pt" to "PT - Portuguese"
                )
                var languageMenuExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { languageMenuExpanded = true }) {
                        Text(languageOptions.firstOrNull { it.first == interfaceLanguage }?.second ?: "EN - English")
                    }
                    DropdownMenu(
                        expanded = languageMenuExpanded,
                        onDismissRequest = { languageMenuExpanded = false }
                    ) {
                        languageOptions.forEach { (code, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    languageMenuExpanded = false
                                    onInterfaceLanguageChange(code)
                                }
                            )
                        }
                    }
                }
            }
        }
        LocalLanguagesSettingsCard(
            downloadedTranslationLanguages = downloadedTranslationLanguages,
            offlineSpeechStatuses = offlineSpeechStatuses,
            translationDownloadingLabel = translationDownloadingLabel,
            offlineSpeechDownloadingTag = offlineSpeechDownloadingTag,
            onOpenLocalLanguages = onOpenLocalLanguages
        )
        if (false) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = st("Default card side", "Standard-Kartenseite", "ÃƒÆ’Ã‚ÂÃƒâ€¦Ã‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â±ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Âº ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“", "Lado inicial de la tarjeta", "ÃƒÆ’Ã‚ÂÃƒâ€¦Ã‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¹ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â±ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Âº ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸", "ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¡ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â° ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¿ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ ÃƒÆ’Ã¢â‚¬ËœÃƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¼ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã¢â‚¬ËœÃƒâ€¦Ã‚Â½", "DomyÃƒÆ’Ã¢â‚¬Â¦ÃƒÂ¢Ã¢â€šÂ¬Ã‚Âºlna strona karty"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = st(
                        "Choose which side is shown first when a card opens.",
                        "WÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¤hle, welche Seite beim ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ffnen zuerst erscheint.",
                        "ÃƒÆ’Ã‚ÂÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â±ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹, ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â±ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Âº ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¿ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã¢â‚¬ËœÃƒâ€¦Ã¢â‚¬â„¢ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¿ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã¢â‚¬ËœÃƒâ€¹Ã¢â‚¬Â ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¼ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¿ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â´ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“.",
                        "Elige quÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â© lado se muestra primero al abrir una tarjeta.",
                        "ÃƒÆ’Ã‚ÂÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â±ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸, ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¹ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â±ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Âº ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¿ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã¢â‚¬ËœÃƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¿ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã¢â‚¬ËœÃƒâ€¹Ã¢â‚¬Â ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¼ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¿ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â´ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸.",
                        "ÃƒÆ’Ã‚ÂÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â±ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸, ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚Â ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â° ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¿ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚Â ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¿ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¹ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¿ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸.",
                        "Wybierz, ktÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³ra strona pokazuje siÃƒÆ’Ã¢â‚¬Å¾ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ pierwsza po otwarciu karty."
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = cardStartSide == CardStartSide.POLISH,
                        onClick = { onCardStartSideChange(CardStartSide.POLISH) },
                        label = { Text("Mistake Made") }
                    )
                    FilterChip(
                        selected = cardStartSide == CardStartSide.TRANSLATION,
                        onClick = { onCardStartSideChange(CardStartSide.TRANSLATION) },
                        label = { Text("Correct form") }
                    )
                }
            }
        }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = st("Hide done cards", "Fertige Karten ausblenden", "ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¡ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã¢â‚¬ËœÃƒâ€¦Ã¢â‚¬â„¢ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â±ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚Â ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“", "Ocultar tarjetas hechas", "ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¡ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸", "ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬ËœÃƒâ€¦Ã¢â‚¬â„¢ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¿ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Âµ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸", "Ukryj zrobione karty"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = st(
                            "Do not show or count cards marked with 3 stars inside a lesson.",
                            "Karten mit 3 Sternen in der Lektion nicht anzeigen oder zÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¤hlen.",
                            "ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Âµ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¿ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã¢â‚¬ËœÃƒâ€¦Ã¢â‚¬â„¢ ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Âµ ÃƒÆ’Ã¢â‚¬ËœÃƒâ€¦Ã‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã¢â‚¬ËœÃƒâ€¦Ã¢â‚¬â„¢ ÃƒÆ’Ã¢â‚¬ËœÃƒâ€ Ã¢â‚¬â„¢ ÃƒÆ’Ã¢â‚¬ËœÃƒâ€¦Ã‚Â¾ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã¢â‚¬ËœÃƒâ€ Ã¢â‚¬â„¢ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â· 3 ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¼ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“.",
                            "No mostrar ni contar dentro de la lecciÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³n las tarjetas con 3 estrellas.",
                            "ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Âµ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¿ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã¢â‚¬ËœÃƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¹ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Âµ ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã¢â‚¬ËœÃƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â² ÃƒÆ’Ã¢â‚¬ËœÃƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â· 3 ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¼ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸.",
                            "ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Âµ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¿ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬ËœÃƒâ€¦Ã¢â‚¬â„¢ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Âµ ÃƒÆ’Ã¢â‚¬ËœÃƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬ËœÃƒâ€¦Ã¢â‚¬â„¢ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â² ÃƒÆ’Ã¢â‚¬ËœÃƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Âµ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚Â 3 ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â´ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¼ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸.",
                            "Nie pokazuj i nie licz w lekcji kart z 3 gwiazdkami."
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = excludeMasteredCards,
                    onCheckedChange = onExcludeMasteredCardsChange
                )
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Offline speech recognition",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Download a language model before using offline microphone input. Recognition uses Android on-device SpeechRecognizer only.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                offlineSpeechHint?.let { hint ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f)
                    ) {
                        Text(
                            text = hint,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                OfflineSpeechLanguages.forEach { language ->
                    val status = offlineSpeechStatuses[language.tag]
                        ?: CardRepository.OFFLINE_SPEECH_STATUS_NOT_DOWNLOADED
                    val isSelected = offlineSpeechLanguageTag == language.tag
                    val isDownloadingOther = offlineSpeechDownloadingTag != null &&
                        offlineSpeechDownloadingTag != language.tag
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                onOfflineSpeechLanguageChange(language.tag)
                                offlineSpeechHint = offlineSpeechLocalizedName(language.tag, interfaceLanguage)
                            },
                            label = { Text(language.tag) },
                            modifier = Modifier.weight(1.25f)
                        )
                        Text(
                            text = status,
                            style = MaterialTheme.typography.bodySmall,
                            color = when (status) {
                                CardRepository.OFFLINE_SPEECH_STATUS_READY -> MaterialTheme.colorScheme.primary
                                CardRepository.OFFLINE_SPEECH_STATUS_ERROR,
                                CardRepository.OFFLINE_SPEECH_STATUS_NOT_SUPPORTED -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.width(96.dp)
                        )
                        OutlinedButton(
                            onClick = {
                                onOfflineSpeechLanguageChange(language.tag)
                                onDownloadOfflineSpeechModel(language.tag)
                            },
                            enabled = !isDownloadingOther &&
                                status != CardRepository.OFFLINE_SPEECH_STATUS_READY &&
                                status != CardRepository.OFFLINE_SPEECH_STATUS_NOT_SUPPORTED,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Download", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                val selectedLanguage = OfflineSpeechLanguages.firstOrNull { it.tag == offlineSpeechLanguageTag }
                    ?: OfflineSpeechLanguages.first()
                Button(
                    onClick = { onDownloadOfflineSpeechModel(selectedLanguage.tag) },
                    enabled = offlineSpeechDownloadingTag == null &&
                        offlineSpeechStatuses[selectedLanguage.tag] != CardRepository.OFFLINE_SPEECH_STATUS_READY &&
                        offlineSpeechStatuses[selectedLanguage.tag] != CardRepository.OFFLINE_SPEECH_STATUS_NOT_SUPPORTED,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Download offline model")
                }
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = st("Card and feedback", "Karte und Feedback", "ÃƒÆ’Ã‚ÂÃƒâ€¦Ã‚Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â° ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â´ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â³ÃƒÆ’Ã¢â‚¬ËœÃƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Âº", "Tarjeta y respuesta", "ÃƒÆ’Ã‚ÂÃƒâ€¦Ã‚Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â° ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â´ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â³ÃƒÆ’Ã¢â‚¬ËœÃƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Âº", "ÃƒÆ’Ã‚ÂÃƒâ€¦Ã‚Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â° ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Âº", "Karta i reakcje"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Display size",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DisplayTextSize.entries.forEach { size ->
                        FilterChip(
                            selected = displayTextSize == size,
                            onClick = { onDisplayTextSizeChange(size) },
                            label = { Text(size.name.lowercase(Locale.ROOT).replace('_', ' ').replaceFirstChar { it.titlecase(Locale.ROOT) }) }
                        )
                    }
                }
                Text(
                    text = "Button and icon size",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ControlSize.entries.forEach { size ->
                        FilterChip(
                            selected = controlSize == size,
                            onClick = { onControlSizeChange(size) },
                            label = { Text(size.name.lowercase(Locale.ROOT).replace('_', ' ').replaceFirstChar { it.titlecase(Locale.ROOT) }) }
                        )
                    }
                }
                val blinkIntervalLabel = CardStatusBlinkIntervalOptions
                    .firstOrNull { it.second == cardStatusBlinkIntervalMs }
                    ?.first
                    ?: "2 seconds"
                ModelDropdown(
                    label = "Card status blink",
                    value = blinkIntervalLabel,
                    options = CardStatusBlinkIntervalOptions.map { it.first },
                    onValueChange = { selected ->
                        CardStatusBlinkIntervalOptions.firstOrNull { it.first == selected }?.second
                            ?.let(onCardStatusBlinkIntervalChange)
                    }
                )

                SettingsSwitchRow(
                    title = st("Show card log", "Kartenlog anzeigen", "ÃƒÆ’Ã‚ÂÃƒâ€¦Ã‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã¢â‚¬ËœÃƒâ€¦Ã¢â‚¬â„¢ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â³ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“", "Mostrar registro de tarjeta", "ÃƒÆ’Ã‚ÂÃƒâ€¦Ã‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã¢â‚¬ËœÃƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â³ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸", "ÃƒÆ’Ã‚ÂÃƒâ€¦Ã‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬ËœÃƒâ€¦Ã¢â‚¬â„¢ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â³ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸", "PokaÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¼ log karty"),
                    description = st(
                        "Show the M icon with mistakes and work log on study cards.",
                        "Zeigt das M-Symbol mit Fehlern und Arbeitslog auf Lernkarten.",
                        "ÃƒÆ’Ã‚ÂÃƒâ€¦Ã‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Âµ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Âº M ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â· ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¿ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¼ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¼ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â³ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¼ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¿ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â° ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦.",
                        "Muestra el icono M con errores y registro de trabajo en las tarjetas.",
                        "ÃƒÆ’Ã‚ÂÃƒâ€¦Ã‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã¢â‚¬ËœÃƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Âº M ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â· ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¿ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¼ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¼ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¹ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â³ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¼ ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â±ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â° ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦.",
                        "ÃƒÆ’Ã‚ÂÃƒâ€¦Ã‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Âº M ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚Â ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã¢â‚¬ËœÃƒâ€¹Ã¢â‚¬Â ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â±ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¼ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â³ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¼ ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â±ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â° ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦.",
                        "Pokazuje ikonÃƒÆ’Ã¢â‚¬Å¾ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ M z bÃƒÆ’Ã¢â‚¬Â¦ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¾ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢dami i logiem pracy na kartach."
                    ),
                    checked = showCardLog,
                    onCheckedChange = onShowCardLogChange
                )
                SettingsSwitchRow(
                    title = st("Startup purr", "Soundeffekte", "ÃƒÆ’Ã‚ÂÃƒÂ¢Ã¢â€šÂ¬Ã…â€œÃƒÆ’Ã¢â‚¬ËœÃƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚Â ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹", "Efectos de sonido", "ÃƒÆ’Ã‚ÂÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬ÂÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã¢â‚¬ËœÃƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸", "ÃƒÆ’Ã‚ÂÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬ÂÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã¢â‚¬ËœÃƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Âµ ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹", "Efekty dÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚ÂºwiÃƒÆ’Ã¢â‚¬Å¾ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢kowe"),
                    description = st(
                        "Play one soft purr on the startup screen. Other app actions stay silent.",
                        "Spielt kurze Sounds fÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¼r Aktionen und den Startbildschirm.",
                        "ÃƒÆ’Ã‚ÂÃƒâ€¦Ã‚Â¸ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¹ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â³ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Âµ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚Â ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â³ÃƒÆ’Ã¢â‚¬ËœÃƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â´ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚Â ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â´ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã¢â‚¬ËœÃƒâ€¦Ã‚Â¾ ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒâ€¦Ã‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“.",
                        "Reproduce sonidos cortos para acciones y la pantalla inicial.",
                        "ÃƒÆ’Ã‚ÂÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â´ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã¢â‚¬ËœÃƒâ€¦Ã‚Â½ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã¢â‚¬ËœÃƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â´ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚Â ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â´ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¹ ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸.",
                        "ÃƒÆ’Ã‚ÂÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¿ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â´ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Âµ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã¢â‚¬ËœÃƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â´ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚Â ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â´ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¹ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¹ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸.",
                        "Odtwarza krÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³tkie dÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚ÂºwiÃƒÆ’Ã¢â‚¬Å¾ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ki dla akcji i ekranu startowego."
                    ),
                    checked = soundEffectsEnabled,
                    onCheckedChange = onSoundEffectsEnabledChange
                )
                SettingsSwitchRow(
                    title = st("Vibration", "Vibration", "ÃƒÆ’Ã‚ÂÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â±ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚Â", "VibraciÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³n", "ÃƒÆ’Ã‚ÂÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â±ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚Â", "ÃƒÆ’Ã‚ÂÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â±ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚Â", "Wibracja"),
                    description = st(
                        "Use short haptic feedback for app actions.",
                        "Nutzt kurze haptische RÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¼ckmeldung fÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¼r Aktionen.",
                        "ÃƒÆ’Ã‚ÂÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã¢â‚¬ËœÃƒâ€¦Ã‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Âµ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â±ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â´ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â³ÃƒÆ’Ã¢â‚¬ËœÃƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Âº ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â´ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚Â ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â´ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã¢â‚¬ËœÃƒâ€¦Ã‚Â¾.",
                        "Usa respuesta hÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡ptica corta para acciones.",
                        "ÃƒÆ’Ã‚ÂÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã¢â‚¬ËœÃƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¹ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â±ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â´ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â³ÃƒÆ’Ã¢â‚¬ËœÃƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Âº ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â´ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚Â ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â´ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¹.",
                        "ÃƒÆ’Ã‚ÂÃƒâ€¹Ã…â€œÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¿ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã¢â‚¬ËœÃƒâ€¦Ã¢â‚¬â„¢ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã¢â‚¬ËœÃƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¹ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â±ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Âº ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â´ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚Â ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â´ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¹ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¹.",
                        "UÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¼ywa krÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³tkiej reakcji haptycznej dla akcji."
                    ),
                    checked = vibrationEnabled,
                    onCheckedChange = onVibrationEnabledChange
                )
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Quick vocabulary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Basic Language is your Native Language: quick vocabulary, Translate dictation, AI explanations, and generated documentation use it by default. Target Language is the language you are learning and the checked side of cards.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
           DictionaryLanguageDropdown(
               label = "Basic Language / Native Language",
               value = quickVocabularySourceLanguage,
               onValueChange = onQuickVocabularySourceChange
           )
           DictionaryLanguageDropdown(
               label = "Target Language / Learning Language",
               value = quickVocabularyTargetLanguage,
               onValueChange = onQuickVocabularyTargetChange
           )
                SettingsSwitchRow(
                    title = "Critical offline mode",
                    description = "Use downloaded on-device speech and Google Translate models when online recognition or free online translation is unavailable.",
                    checked = useLocalTranslation,
                    onCheckedChange = onUseLocalTranslationChange
                )
                SettingsSwitchRow(
                    title = "Auto-save translator cards",
                    description = "Automatically create or update a vocabulary card when Translate produces a result. Manual + still works when this is off.",
                    checked = autoSaveTranslatorCards,
                    onCheckedChange = onAutoSaveTranslatorCardsChange
                )
                OutlinedButton(
                    onClick = onDownloadTranslationLanguages,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Download current pair")
                }
                Text(
                    text = "Downloaded languages: $downloadedTranslationLabel",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                translationDownloadingLabel?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    DictionaryLanguageDropdown(
                        label = "Download language",
                        value = translationDownloadLanguage,
                        onValueChange = onTranslationDownloadLanguageChange,
                        modifier = Modifier.weight(1f),
                        codeOnly = true
                    )
                    OutlinedButton(
                        onClick = onDownloadSelectedTranslationLanguage,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Download language")
                    }
                }
                SettingsSwitchRow(
                    title = "Use MurrLex server online",
                    description = "When the phone is online and the server session is active, transcription, translation, image text, chat, and speech run only through MurrLex server. Offline mode stays local.",
                    checked = useOpenAiModels,
                    onCheckedChange = onUseOpenAiModelsChange
                )
                OutlinedTextField(
                    value = openAiBaseUrl,
                    onValueChange = {
                        onOpenAiBaseUrlChange(it)
                        onTranslationApiUrlChange(it)
                    },
                    label = { Text("MurrLex server URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (openAiApiKey.isBlank()) {
                    OutlinedButton(onClick = { showServerLoginDialog = true }) { Text("Login to MurrLex server") }
                } else {
                    Text("Server session: ${serverUsername.ifBlank { "active" }}", style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(onClick = onMurrLexServerLogout) { Text("Log out") }
                }
                ModelDropdown(
                    label = "Speech-to-text model",
                    value = openAiSpeechModel,
                    options = OpenAiSpeechModels,
                    onValueChange = onOpenAiSpeechModelChange
                )
                ModelDropdown(
                    label = "Translation / text model",
                    value = openAiTextModel,
                    options = OpenAiTextModels,
                    onValueChange = onOpenAiTextModelChange
                )
                ModelDropdown(
                    label = "Image text recognition model",
                    value = openAiImageTextModel,
                    options = OpenAiImageTextModels,
                    onValueChange = onOpenAiImageTextModelChange
                )
                ModelDropdown(
                    label = "Text-to-speech model",
                    value = openAiTtsModel,
                    options = OpenAiTtsModels,
                    onValueChange = onOpenAiTtsModelChange
                )
                ModelDropdown(
                    label = "Text-to-speech voice",
                    value = openAiTtsVoice,
                    options = OpenAiTtsVoices,
                    onValueChange = onOpenAiTtsVoiceChange
                )
                Text(
                    text = "Special ElevenLabs text-to-speech",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Enabled languages use ElevenLabs through MurrLex server. Its API key stays on the server.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ElevenLabsSpecialTtsLanguages.chunked(2).forEach { rowLanguages ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowLanguages.forEach { (code, label) ->
                            FilterChip(
                                selected = code in elevenLabsTtsLanguageCodes,
                                onClick = {
                                    onElevenLabsTtsLanguageEnabledChange(
                                        code,
                                        code !in elevenLabsTtsLanguageCodes
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowLanguages.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
                ModelDropdown(
                    label = "ElevenLabs model",
                    value = elevenLabsModel,
                    options = ElevenLabsModels,
                    onValueChange = onElevenLabsModelChange
                )
                OutlinedTextField(
                    value = elevenLabsVoiceId,
                    onValueChange = onElevenLabsVoiceIdChange,
                    label = { Text("ElevenLabs voice ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                val voiceSilenceLabel = OpenAiVoiceSilenceTimeoutOptions
                    .firstOrNull { it.second == openAiVoiceSilenceTimeoutMs }
                    ?.first
                    ?: "5 seconds"
                ModelDropdown(
                    label = "Voice silence timeout",
                    value = voiceSilenceLabel,
                    options = OpenAiVoiceSilenceTimeoutOptions.map { it.first },
                    onValueChange = { selected ->
                        OpenAiVoiceSilenceTimeoutOptions.firstOrNull { it.first == selected }?.second
                            ?.let(onOpenAiVoiceSilenceTimeoutChange)
                    }
                )
                val cacheLabel = OpenAiCacheDurationOptions.firstOrNull { it.second == openAiCacheDurationMinutes }?.first
                    ?: "5 minutes"
                ModelDropdown(
                    label = "OpenAI cache lifetime",
                    value = cacheLabel,
                    options = OpenAiCacheDurationOptions.map { it.first },
                    onValueChange = { selected ->
                        OpenAiCacheDurationOptions.firstOrNull { it.first == selected }?.second
                            ?.let(onOpenAiCacheDurationChange)
                    }
                )
                ModelDropdown(
                    label = "Cat reply speed",
                    value = "${catReplySpeechRate}x",
                    options = CatReplySpeechRateOptions.map { "${it}x" },
                    onValueChange = { selected ->
                        selected.removeSuffix("x").toFloatOrNull()?.let(onCatReplySpeechRateChange)
                    }
                )
                ModelDropdown(
                    label = "Cat dialog retention",
                    value = "$catDialogRetentionDays days",
                    options = CatDialogRetentionDayOptions.map { "$it days" },
                    onValueChange = { selected ->
                        selected.substringBefore(" ").toIntOrNull()?.let(onCatDialogRetentionDaysChange)
                    }
                )
                OutlinedButton(
                    onClick = onClearOpenAiCache,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Clear app cache")
                }
                Text(
                    text = "OpenAI activity log, last 2 days",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (openAiActivityLog.isEmpty()) {
                    Text(
                        text = "No OpenAI activity yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            openAiActivityLog.take(40).forEach { line ->
                                Text(
                                    text = line,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = st("JSON template", "JSON-Vorlage", "ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¨ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â±ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ JSON", "Plantilla JSON", "ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¨ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â±ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ JSON", "ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¨ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â±ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ JSON", "Szablon JSON"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = st(
                        "Download an instruction-ready JSON template for generating lessons from your mistakes.",
                        "Lade eine JSON-Vorlage mit Anweisungen herunter, um Lektionen aus deinen Fehlern zu erzeugen.",
                        "ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¿ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¼ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¿ÃƒÆ’Ã¢â‚¬ËœÃƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¹ JSON-ÃƒÆ’Ã¢â‚¬ËœÃƒâ€¹Ã¢â‚¬Â ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â±ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â· ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã¢â‚¬ËœÃƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¹ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â´ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚Â ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚Â ÃƒÆ’Ã¢â‚¬ËœÃƒâ€¦Ã‚Â¾ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒâ€¦Ã‚Â¾ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â· ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¿ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¼ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Âº.",
                        "Descarga una plantilla JSON lista como instrucciÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³n para generar lecciones desde tus errores.",
                        "ÃƒÆ’Ã‚ÂÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬ÂÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¶ JSON-ÃƒÆ’Ã¢â‚¬ËœÃƒâ€¹Ã¢â‚¬Â ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â±ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â· ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã¢â‚¬ËœÃƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚ÂÃƒÆ’Ã¢â‚¬ËœÃƒâ€¦Ã‚Â½ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â´ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚Â ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚Â ÃƒÆ’Ã¢â‚¬ËœÃƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â² ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â· ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬ÂÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¿ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¼ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Âº.",
                        "ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¹ JSON-ÃƒÆ’Ã¢â‚¬ËœÃƒâ€¹Ã¢â‚¬Â ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â±ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚Â ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã¢â‚¬ËœÃƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¹ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â´ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚Â ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â´ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚Â ÃƒÆ’Ã¢â‚¬ËœÃƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â² ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â· ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã¢â‚¬ËœÃƒâ€¹Ã¢â‚¬Â ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â±ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Âº.",
                        "Pobierz szablon JSON z instrukcjÃƒÆ’Ã¢â‚¬Å¾ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ do tworzenia lekcji z Twoich bÃƒÆ’Ã¢â‚¬Â¦ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¾ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢dÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³w."
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onDownloadSampleJson, shape = RoundedCornerShape(18.dp)) {
                    Icon(Icons.Default.FileDownload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Download sample JSON")
                }
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = st("Notifications", "Benachrichtigungen", "ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¿ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã¢â‚¬ËœÃƒâ€¹Ã¢â‚¬Â ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¡ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“", "Notificaciones", "ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¿ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚Â", "ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â£ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â´ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¼ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚Â", "Powiadomienia"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = st(
                        "Shows weighted reminders for cards with 0-2 stars. Cards with 0 stars appear most often; 3-star cards are excluded.",
                        "Zeigt gewichtete Erinnerungen fÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¼r Karten mit 0-2 Sternen. 0 Sterne erscheinen am hÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¤ufigsten; 3 Sterne sind ausgeschlossen.",
                        "ÃƒÆ’Ã‚ÂÃƒâ€¦Ã‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Âµ ÃƒÆ’Ã¢â‚¬ËœÃƒâ€¦Ã‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¶ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚Â ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¿ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¼ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â´ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚Â ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Âº ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â· 0-2 ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¼ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“. 0 ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Âº ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¿ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã¢â‚¬ËœÃƒâ€¦Ã‚Â½ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã¢â‚¬ËœÃƒâ€¦Ã¢â‚¬â„¢ ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¹; 3 ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã¢â‚¬ËœÃƒâ€¦Ã‚Â½ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒâ€¦Ã‚Â½ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°.",
                        "Muestra recordatorios ponderados para tarjetas con 0-2 estrellas. Las de 0 salen mÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡s; las de 3 no salen.",
                        "ÃƒÆ’Ã‚ÂÃƒâ€¦Ã‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã¢â‚¬ËœÃƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¶ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â³ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â´ÃƒÆ’Ã¢â‚¬ËœÃƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚Â ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â´ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚Â ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Âº ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â· 0-2 ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¼ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸. 0 ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Âº ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·'ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã¢â‚¬ËœÃƒâ€¦Ã‚Â½ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬ËœÃƒâ€¦Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚Â ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¹ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ÃƒÆ’Ã¢â‚¬ËœÃƒâ€¹Ã¢â‚¬Â ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Âµ; 3 ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã¢â‚¬ËœÃƒâ€¦Ã‚Â½ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“.",
                        "ÃƒÆ’Ã‚ÂÃƒâ€¦Ã‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã¢â‚¬ËœÃƒâ€¹Ã¢â‚¬Â ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Âµ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¿ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¼ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚Â ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â´ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚Â ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Âº ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚Â 0-2 ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â´ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¼ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸. ÃƒÆ’Ã‚ÂÃƒâ€¦Ã‚Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚Â 0 ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â´ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¼ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¿ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã¢â‚¬ËœÃƒâ€¦Ã‚Â½ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚Â ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Âµ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â³ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾; 3 ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â´ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã¢â‚¬ËœÃƒâ€¦Ã‚Â½ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹.",
                        "Pokazuje waÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¼one przypomnienia dla kart z 0-2 gwiazdkami. 0 gwiazdek pojawia siÃƒÆ’Ã¢â‚¬Å¾ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ najczÃƒÆ’Ã¢â‚¬Å¾ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã¢â‚¬Â¦ÃƒÂ¢Ã¢â€šÂ¬Ã‚Âºciej; 3 gwiazdki sÃƒÆ’Ã¢â‚¬Å¾ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ wykluczone."
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = notificationIntervalDraft,
                    onValueChange = onNotificationIntervalChange,
                    label = { Text(st("Interval minutes", "Intervall in Minuten", "ÃƒÆ’Ã‚ÂÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â» ÃƒÆ’Ã¢â‚¬ËœÃƒâ€ Ã¢â‚¬â„¢ ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦", "Intervalo en minutos", "ÃƒÆ’Ã‚ÂÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â» ÃƒÆ’Ã¢â‚¬ËœÃƒâ€ Ã¢â‚¬â„¢ ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦", "ÃƒÆ’Ã‚ÂÃƒâ€¹Ã…â€œÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â» ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â² ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¼ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã¢â‚¬ËœÃƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦", "InterwaÃƒÆ’Ã¢â‚¬Â¦ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ w minutach")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = onSaveNotificationInterval) {
                    Text("Save notification interval")
                }
                OutlinedTextField(
                    value = notificationMaxDraft,
                    onValueChange = onNotificationMaxChange,
                    label = { Text(st("Maximum active notifications", "Maximale aktive Benachrichtigungen", "ÃƒÆ’Ã‚ÂÃƒâ€¦Ã¢â‚¬Å“ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¼ÃƒÆ’Ã¢â‚¬ËœÃƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¼ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹ÃƒÆ’Ã¢â‚¬ËœÃƒâ€¦Ã‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¿ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã¢â‚¬ËœÃƒâ€¹Ã¢â‚¬Â ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¡ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã¢â‚¬ËœÃƒâ€¦Ã‚Â¾", "MÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡ximo de notificaciones activas", "ÃƒÆ’Ã‚ÂÃƒâ€¦Ã¢â‚¬Å“ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¼ÃƒÆ’Ã¢â‚¬ËœÃƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¼ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¿ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã¢â‚¬ËœÃƒâ€¦Ã¢â‚¬â„¢", "ÃƒÆ’Ã‚ÂÃƒâ€¦Ã¢â‚¬Å“ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¼ÃƒÆ’Ã¢â‚¬ËœÃƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¼ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ ÃƒÆ’Ã¢â‚¬ËœÃƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â´ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¼ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¹", "Maksimum aktywnych powiadomieÃƒÆ’Ã¢â‚¬Â¦ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = onSaveNotificationMax) {
                    Text("Save notification maximum")
                }
                Text(
                    text = st(
                        "Android may run background work with system scheduling delays, especially when the phone is idle.",
                        "Android kann Hintergrundaufgaben verzÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¶gert ausfÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¼hren, besonders wenn das Telefon inaktiv ist.",
                        "Android ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¼ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¶ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â° ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¿ÃƒÆ’Ã¢â‚¬ËœÃƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã¢â‚¬ËœÃƒâ€¦Ã¢â‚¬â„¢ ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚Â ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â´ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¡ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â· ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¼ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¹, ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â±ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â° ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹ÃƒÆ’Ã¢â‚¬ËœÃƒâ€¦Ã‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹.",
                        "Android puede retrasar tareas en segundo plano, sobre todo cuando el telÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©fono estÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡ inactivo.",
                        "Android ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¼ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¶ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Âµ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¿ÃƒÆ’Ã¢â‚¬ËœÃƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â´ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¡ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â· ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¼ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã¢â‚¬ËœÃƒâ€¦Ã‚Â½, ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â±ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¹.",
                        "Android ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¼ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¶ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¿ÃƒÆ’Ã¢â‚¬ËœÃƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬ËœÃƒâ€¦Ã¢â‚¬â„¢ ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Âµ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â´ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚Â ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â·ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â´ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¶ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¹, ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â±ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â³ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â´ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â° ÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â»ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â°ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂºÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â½.",
                        "Android moÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¼e opÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚ÂºniaÃƒÆ’Ã¢â‚¬Å¾ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¡ pracÃƒÆ’Ã¢â‚¬Å¾ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ w tle, szczegÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³lnie gdy telefon jest bezczynny."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = st("Version log", "Versionslog", "ÃƒÆ’Ã‚ÂÃƒÂ¢Ã¢â€šÂ¬Ã‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â³ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¹", "Registro de versiones", "ÃƒÆ’Ã‚ÂÃƒÂ¢Ã¢â€šÂ¬Ã‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â³ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¹", "ÃƒÆ’Ã‚ÂÃƒÂ¢Ã¢â€šÂ¬Ã‚ÂºÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¾ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â³ ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â²ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚ÂµÃƒÆ’Ã¢â‚¬ËœÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÆ’Ã¢â‚¬ËœÃƒâ€šÃ‚ÂÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¸ÃƒÆ’Ã‚ÂÃƒâ€šÃ‚Â¹", "Historia wersji"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                versionLogLines.forEach { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
private val DictionaryLanguageOptions = listOf(
    "English" to "EN - English",
    "Spanish" to "ES - espa\u00f1ol",
    "Polish" to "PL - polski",
    "Russian" to "RU - \u0440\u0443\u0441\u0441\u043a\u0438\u0439",
    "Belarusian" to "BY - \u0431\u0435\u043b\u0430\u0440\u0443\u0441\u043a\u0430\u044f",
    "Ukrainian" to "UA - \u0443\u043a\u0440\u0430\u0457\u043d\u0441\u044c\u043a\u0430",
    "German" to "DE - Deutsch",
    "Latvian" to "LV - latvie\u0161u",
    "Lithuanian" to "LT - lietuvi\u0173",
    "Portuguese" to "PT - portugu\u00eas"
)

private fun dictionaryLanguageCode(language: String): String {
    val label = DictionaryLanguageOptions.firstOrNull { it.first.equals(language, ignoreCase = true) }?.second.orEmpty()
    return label.substringBefore(" - ").ifBlank { language.take(2).uppercase(Locale.ROOT) }
}

private fun mlKitLanguageTagForName(language: String): String {
    return when (language.trim().lowercase(Locale.ROOT)) {
        "english", "en" -> "en"
        "spanish", "es", "espanol", "espa\u00f1ol" -> "es"
        "polish", "pl", "polski" -> "pl"
        "russian", "ru" -> "ru"
        "belarusian", "belarus", "by", "be" -> "be"
        "ukrainian", "uk", "ua" -> "uk"
        "german", "de", "deutsch" -> "de"
        "latvian", "lv" -> "lv"
        "lithuanian", "lt" -> "lt"
        "portuguese", "pt" -> "pt"
        else -> language.trim().takeIf { it.length in 2..3 }?.lowercase(Locale.ROOT).orEmpty()
    }
}

private fun dictionaryLanguageCodeForMlKitTag(tag: String): String? {
    return DictionaryLanguageOptions.firstOrNull { (language, _) ->
        mlKitLanguageTagForName(language).equals(tag, ignoreCase = true)
    }?.let { (language, _) -> dictionaryLanguageCode(language) }
}

private fun speechTagForDictionaryLanguage(language: String): String? {
    return when (dictionaryLanguageCode(language).uppercase(Locale.ROOT)) {
        "EN" -> "en-US"
        "PL" -> "pl-PL"
        "RU" -> "ru-RU"
        "BE", "BY" -> "be-BY"
        "DE" -> "de-DE"
        "ES" -> "es-ES"
        "LV" -> "lv-LV"
        "LT" -> "lt-LT"
        "PT" -> "pt-PT"
        else -> null
    }
}

private fun nativeLanguageLabel(language: String): String {
    return DictionaryLanguageOptions.firstOrNull { it.first.equals(language, ignoreCase = true) }?.second
        ?: language.ifBlank { "Language" }
}

private fun String.shortCodeForUi(): String {
    return nativeLanguageLabel(this).substringBefore(" - ").ifBlank { take(2).uppercase(Locale.ROOT) }
}

private fun String.trimToWordLimit(limit: Int): String {
    val words = trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return words.take(limit).joinToString(" ")
}

@Composable
private fun DictionaryLanguageDropdown(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    codeOnly: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = nativeLanguageLabel(value)
    val selectedCode = selectedLabel.substringBefore(" - ").ifBlank { selectedLabel }
    val buttonText = if (codeOnly) selectedCode else selectedCode
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label.isNotBlank()) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = buttonText,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start
                )
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DictionaryLanguageOptions.forEach { (language, optionLabel) ->
                    DropdownMenuItem(
                        text = { Text(if (codeOnly) optionLabel.substringBefore(" - ") else optionLabel.substringBefore(" - ")) },
                        onClick = {
                            expanded = false
                            onValueChange(language)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelDropdown(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = value,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(32.dp))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            expanded = false
                            onValueChange(option)
                        }
                    )
                }
            }
        }
    }

}

@Composable
private fun LocalLanguagesSettingsCard(
    downloadedTranslationLanguages: Set<String>,
    offlineSpeechStatuses: Map<String, String>,
    translationDownloadingLabel: String?,
    offlineSpeechDownloadingTag: String?,
    onOpenLocalLanguages: () -> Unit
) {
    val downloadedLanguages = remember(downloadedTranslationLanguages, offlineSpeechStatuses) {
        DictionaryLanguageOptions.filter { (language, _) ->
            val code = dictionaryLanguageCode(language)
            val speechTag = speechTagForDictionaryLanguage(language)
            downloadedTranslationLanguages.contains(code) ||
                speechTag?.let { offlineSpeechStatuses[it] == CardRepository.OFFLINE_SPEECH_STATUS_READY } == true
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Local languages",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Downloaded languages for critical offline mode. Translation uses ML Kit models; speech recognition uses Android on-device models.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (downloadedLanguages.isEmpty()) {
                Text(
                    text = "No local languages downloaded yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                downloadedLanguages.forEach { (language, label) ->
                    LocalLanguageStatusRow(
                        languageName = language,
                        label = label,
                        translationReady = downloadedTranslationLanguages.contains(dictionaryLanguageCode(language)),
                        speechStatus = speechTagForDictionaryLanguage(language)?.let { offlineSpeechStatuses[it] },
                        compact = true
                    )
                }
            }
            translationDownloadingLabel?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            offlineSpeechDownloadingTag?.let {
                Text(
                    text = "Speech recognition downloading: $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Button(
                onClick = onOpenLocalLanguages,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Download or remove languages")
            }
        }
    }
}

@Composable
private fun LocalLanguagesDialog(
    downloadedTranslationLanguages: Set<String>,
    offlineSpeechStatuses: Map<String, String>,
    offlineSpeechDownloadingTag: String?,
    translationDownloadingLabel: String?,
    onDownloadLanguage: (String) -> Unit,
    onDeleteTranslationLanguage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Local languages") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Choose languages to keep on the phone for critical offline mode.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                translationDownloadingLabel?.let {
                    Text(it, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
                DictionaryLanguageOptions.forEach { (language, label) ->
                    val code = dictionaryLanguageCode(language)
                    val speechTag = speechTagForDictionaryLanguage(language)
                    val speechStatus = speechTag?.let { offlineSpeechStatuses[it] }
                        ?: CardRepository.OFFLINE_SPEECH_STATUS_NOT_SUPPORTED
                    val translationReady = downloadedTranslationLanguages.contains(code)
                    val speechReady = speechStatus == CardRepository.OFFLINE_SPEECH_STATUS_READY
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LocalLanguageStatusRow(
                                languageName = language,
                                label = label,
                                translationReady = translationReady,
                                speechStatus = speechStatus,
                                compact = false
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { onDownloadLanguage(language) },
                                    enabled = offlineSpeechDownloadingTag == null || speechReady,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Download", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                OutlinedButton(
                                    onClick = { onDeleteTranslationLanguage(language) },
                                    enabled = translationReady,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Remove", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
                Text(
                    text = "Speech recognition models are managed by Android. MurrLex can request/download them and show their status; removal may be handled by the system.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
private fun LocalLanguageStatusRow(
    languageName: String,
    label: String,
    translationReady: Boolean,
    speechStatus: String?,
    compact: Boolean
) {
    var hint by remember { mutableStateOf<String?>(null) }
    val speechReady = speechStatus == CardRepository.OFFLINE_SPEECH_STATUS_READY
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = dictionaryLanguageCode(languageName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(
            onClick = {
                hint = if (translationReady) {
                    "Translation model is downloaded."
                } else {
                    "Translation model is not downloaded."
                }
            },
            modifier = Modifier.size(30.dp)
        ) {
            Icon(
                Icons.Default.Translate,
                contentDescription = "Translation status",
                modifier = Modifier.size(16.dp),
                tint = if (translationReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
            )
        }
        IconButton(
            onClick = {
                hint = when (speechStatus) {
                    CardRepository.OFFLINE_SPEECH_STATUS_READY -> "Offline speech recognition is downloaded."
                    CardRepository.OFFLINE_SPEECH_STATUS_DOWNLOADING -> "Offline speech recognition is downloading."
                    CardRepository.OFFLINE_SPEECH_STATUS_ERROR -> "Offline speech recognition download failed."
                    CardRepository.OFFLINE_SPEECH_STATUS_NOT_SUPPORTED -> "Offline speech recognition is not supported for this language on this device."
                    else -> "Offline speech recognition is not downloaded."
                }
            },
            modifier = Modifier.size(30.dp)
        ) {
            Icon(
                Icons.Default.Mic,
                contentDescription = "Speech recognition status",
                modifier = Modifier.size(16.dp),
                tint = if (speechReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
            )
        }
    }
    hint?.let { text ->
        AlertDialog(
            onDismissRequest = { hint = null },
            title = { Text(dictionaryLanguageCode(languageName)) },
            text = { Text(text) },
            confirmButton = {
                TextButton(onClick = { hint = null }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}


@Composable
private fun WorkModeToggleRow(
    workMode: WorkMode,
    onWorkModeChange: (WorkMode) -> Unit,
    onOpenCatDialogs: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        WorkMode.values().forEach { mode ->
            WorkModeIconButton(
                icon = mode.icon(),
                contentDescription = mode.label(),
                selected = workMode == mode,
                onClick = { onWorkModeChange(mode) }
            )
        }
        WorkModeIconButton(
            icon = Icons.Default.Chat,
            contentDescription = "Cat dialogs",
            selected = false,
            onClick = onOpenCatDialogs
        )
    }
}

@Composable
private fun WorkModeIconButton(
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.size(38.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(13.dp),
        border = BorderStroke(1.dp, if (selected) BrandSaladColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
        color = if (selected) BrandSaladColor.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surface
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(20.dp),
                tint = if (selected) Color(0xFF234231) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun WorkMode.icon(): ImageVector {
    return when (this) {
        WorkMode.CARDS -> Icons.Default.Style
        WorkMode.TESTS -> Icons.Default.Quiz
        WorkMode.TRANSLATE -> Icons.Default.Translate
        WorkMode.SPLIT -> Icons.Default.CallSplit
    }
}

private fun WorkMode.label(): String {
    return when (this) {
        WorkMode.CARDS -> "Cards"
        WorkMode.TESTS -> "Tests"
        WorkMode.TRANSLATE -> "Translate"
        WorkMode.SPLIT -> "Split"
    }
}

@Composable
private fun ProfessorCatMark(progress: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h * 0.52f
        val faceRadius = w * 0.31f
        val outline = Color(0xFF263B32)
        val fur = Color(0xFFFFF7E8)
        val cap = Color(0xFF263B32)
        val lens = Color(0xFFFFFFFF).copy(alpha = 0.82f)
        val earLeft = Path().apply {
            moveTo(cx - faceRadius * 0.78f, cy - faceRadius * 0.58f)
            lineTo(cx - faceRadius * 1.12f, cy - faceRadius * 1.18f)
            lineTo(cx - faceRadius * 0.36f, cy - faceRadius * 0.96f)
            close()
        }
        val earRight = Path().apply {
            moveTo(cx + faceRadius * 0.78f, cy - faceRadius * 0.58f)
            lineTo(cx + faceRadius * 1.12f, cy - faceRadius * 1.18f)
            lineTo(cx + faceRadius * 0.36f, cy - faceRadius * 0.96f)
            close()
        }
        drawPath(earLeft, fur)
        drawPath(earLeft, outline, style = Stroke(width = w * 0.035f))
        drawPath(earRight, fur)
        drawPath(earRight, outline, style = Stroke(width = w * 0.035f))
        drawCircle(fur, radius = faceRadius, center = Offset(cx, cy))
        drawCircle(outline, radius = faceRadius, center = Offset(cx, cy), style = Stroke(width = w * 0.035f))

        val capY = cy - faceRadius * (1.08f + 0.08f * (1f - progress))
        val capTop = Path().apply {
            moveTo(cx - faceRadius * 0.95f, capY)
            lineTo(cx, capY - faceRadius * 0.35f)
            lineTo(cx + faceRadius * 0.95f, capY)
            lineTo(cx, capY + faceRadius * 0.32f)
            close()
        }
        drawPath(capTop, cap)
        drawLine(cap, Offset(cx + faceRadius * 0.56f, capY + faceRadius * 0.08f), Offset(cx + faceRadius * 0.92f, capY + faceRadius * 0.48f), strokeWidth = w * 0.025f)
        drawCircle(BrandRedColor, radius = w * 0.025f, center = Offset(cx + faceRadius * 0.96f, capY + faceRadius * 0.52f))

        val eyeY = cy - faceRadius * 0.12f
        val lensRadius = faceRadius * 0.28f
        val glassesAlpha = (0.25f + progress * 0.75f).coerceIn(0f, 1f)
        drawCircle(lens.copy(alpha = glassesAlpha), lensRadius, Offset(cx - faceRadius * 0.36f, eyeY))
        drawCircle(lens.copy(alpha = glassesAlpha), lensRadius, Offset(cx + faceRadius * 0.36f, eyeY))
        drawCircle(outline, lensRadius, Offset(cx - faceRadius * 0.36f, eyeY), style = Stroke(width = w * 0.025f))
        drawCircle(outline, lensRadius, Offset(cx + faceRadius * 0.36f, eyeY), style = Stroke(width = w * 0.025f))
        drawLine(outline, Offset(cx - faceRadius * 0.08f, eyeY), Offset(cx + faceRadius * 0.08f, eyeY), strokeWidth = w * 0.02f)
        drawCircle(outline, radius = w * 0.018f, center = Offset(cx - faceRadius * 0.36f, eyeY))
        drawCircle(outline, radius = w * 0.018f, center = Offset(cx + faceRadius * 0.36f, eyeY))

        drawLine(outline, Offset(cx, cy + faceRadius * 0.06f), Offset(cx, cy + faceRadius * 0.20f), strokeWidth = w * 0.018f)
        drawLine(outline, Offset(cx, cy + faceRadius * 0.20f), Offset(cx - faceRadius * 0.16f, cy + faceRadius * 0.32f), strokeWidth = w * 0.018f)
        drawLine(outline, Offset(cx, cy + faceRadius * 0.20f), Offset(cx + faceRadius * 0.16f, cy + faceRadius * 0.32f), strokeWidth = w * 0.018f)
        drawLine(outline.copy(alpha = 0.58f), Offset(cx - faceRadius * 0.62f, cy + faceRadius * 0.14f), Offset(cx - faceRadius * 1.02f, cy + faceRadius * 0.04f), strokeWidth = w * 0.012f)
        drawLine(outline.copy(alpha = 0.58f), Offset(cx + faceRadius * 0.62f, cy + faceRadius * 0.14f), Offset(cx + faceRadius * 1.02f, cy + faceRadius * 0.04f), strokeWidth = w * 0.012f)
    }
}

private data class OnboardingLanguageOption(
    val code: String,
    val storageName: String,
    val label: String
)

private data class OnboardingCopy(
    val title: String,
    val guide: String,
    val interfaceLanguage: String,
    val knownLanguage: String,
    val learningLanguage: String,
    val explanationLanguage: String,
    val start: String
)

private val OnboardingLanguages = listOf(
    OnboardingLanguageOption("be", "Belarusian", "\uD83C\uDFF3\uFE0F BY - Belarusian"),
    OnboardingLanguageOption("en", "English", "\uD83C\uDDEC\uD83C\uDDE7 EN - English"),
    OnboardingLanguageOption("de", "German", "\uD83C\uDDE9\uD83C\uDDEA DE - Deutsch"),
    OnboardingLanguageOption("pl", "Polish", "\uD83C\uDDF5\uD83C\uDDF1 PL - polski"),
    OnboardingLanguageOption("ru", "Russian", "\uD83C\uDFF3\uFE0F RU - Russian"),
    OnboardingLanguageOption("es", "Spanish", "\uD83C\uDDEA\uD83C\uDDF8 ES - espaÃ±ol"),
    OnboardingLanguageOption("lv", "Latvian", "LV - Latvian"),
    OnboardingLanguageOption("lt", "Lithuanian", "LT - Lithuanian"),
    OnboardingLanguageOption("pt", "Portuguese", "PT - Portuguese"),
    OnboardingLanguageOption("uk", "Ukrainian", "\uD83C\uDDFA\uD83C\uDDE6 UA - ÑƒÐºÑ€Ð°Ñ—Ð½ÑÑŒÐºÐ°")
)

@Composable
private fun OnboardingDialog(
    interfaceLanguage: String,
    defaultKnownLanguage: String,
    defaultLearningLanguage: String,
    onComplete: (String, String, String, String) -> Unit
) {
    val systemKnown = OnboardingLanguages.firstOrNull { it.code == interfaceLanguage }?.storageName ?: "English"
    var knownLanguage by remember {
        mutableStateOf(
            OnboardingLanguages.firstOrNull { it.storageName.equals(defaultKnownLanguage, ignoreCase = true) }
                ?: OnboardingLanguages.firstOrNull { it.storageName == systemKnown }
                ?: OnboardingLanguages.first { it.storageName == "Russian" }
        )
    }
    val initialInterface = OnboardingLanguages.firstOrNull { it.code == knownLanguage.code }
        ?: OnboardingLanguages.firstOrNull { it.code == interfaceLanguage }
        ?: OnboardingLanguages.first { it.code == "en" }
    var selectedInterface by remember { mutableStateOf(initialInterface) }
    var learningLanguage by remember {
        mutableStateOf(
            OnboardingLanguages.firstOrNull { it.storageName.equals(defaultLearningLanguage, ignoreCase = true) }
                ?: OnboardingLanguages.first { it.storageName == "Polish" }
        )
    }
    var explanationLanguage by remember { mutableStateOf(knownLanguage) }
    val copy = onboardingCopy(selectedInterface.code)
    AlertDialog(
        onDismissRequest = {},
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ProfessorCatMark(progress = 1f, modifier = Modifier.size(82.dp))
                Text(copy.title, textAlign = TextAlign.Center)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(copy.guide, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OnboardingDropdown("Basic Language / Native Language", knownLanguage) {
                    knownLanguage = it
                    explanationLanguage = it
                    selectedInterface = it
                }
                OnboardingDropdown("Interface Language", selectedInterface) { selectedInterface = it }
                OnboardingDropdown("Target Language / Learning Language", learningLanguage) { learningLanguage = it }
                OnboardingDropdown("AI explanation language", explanationLanguage) { explanationLanguage = it }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onComplete(
                        selectedInterface.code,
                        knownLanguage.storageName,
                        learningLanguage.storageName,
                        explanationLanguage.storageName
                    )
                }
            ) {
                Text(copy.start)
            }
        }
    )
}

@Composable
private fun OnboardingDropdown(
    label: String,
    selected: OnboardingLanguageOption,
    onSelected: (OnboardingLanguageOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(selected.label, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(30.dp))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                OnboardingLanguages.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            expanded = false
                            onSelected(option)
                        }
                    )
                }
            }
        }
    }
}

private fun onboardingCopy(language: String): OnboardingCopy {
    return when (language.lowercase(Locale.ROOT)) {
        "de" -> OnboardingCopy(
            title = "MurrLex begrusst dich",
            guide = "Kurzer Start: wahle die Sprache der App, deine Sprache, die Lernsprache und die Sprache fur Erklarungen.",
            interfaceLanguage = "Sprache der App",
            knownLanguage = "Deine Sprache",
            learningLanguage = "Sprache, die du lernst",
            explanationLanguage = "Sprache fur Erklarungen",
            start = "Start"
        )
        "be" -> OnboardingCopy(
            title = "MurrLex Ð²Ñ–Ñ‚Ð°Ðµ",
            guide = "ÐšÐ°Ñ€Ð¾Ñ‚ÐºÑ– ÑÑ‚Ð°Ñ€Ñ‚: Ð²Ñ‹Ð±ÐµÑ€Ñ‹ Ð¼Ð¾Ð²Ñƒ Ñ–Ð½Ñ‚ÑÑ€Ñ„ÐµÐ¹ÑÑƒ, ÑÐ²Ð°ÑŽ Ð¼Ð¾Ð²Ñƒ, Ð¼Ð¾Ð²Ñƒ Ð½Ð°Ð²ÑƒÑ‡Ð°Ð½Ð½Ñ Ñ– Ð¼Ð¾Ð²Ñƒ Ñ‚Ð»ÑƒÐ¼Ð°Ñ‡ÑÐ½Ð½ÑÑž.",
            interfaceLanguage = "ÐœÐ¾Ð²Ð° Ñ–Ð½Ñ‚ÑÑ€Ñ„ÐµÐ¹ÑÑƒ",
            knownLanguage = "Ð’Ð°ÑˆÐ° Ð¼Ð¾Ð²Ð°",
            learningLanguage = "ÐœÐ¾Ð²Ð°, ÑÐºÑƒÑŽ Ð²Ñ‹Ð²ÑƒÑ‡Ð°ÐµÑ†Ðµ",
            explanationLanguage = "ÐœÐ¾Ð²Ð° Ñ‚Ð»ÑƒÐ¼Ð°Ñ‡ÑÐ½Ð½ÑÑž",
            start = "ÐŸÐ°Ñ‡Ð°Ñ†ÑŒ"
        )
        "es" -> OnboardingCopy(
            title = "MurrLex te da la bienvenida",
            guide = "Inicio rapido: elige el idioma de la app, tu idioma, el idioma que estudias y el idioma de las explicaciones.",
            interfaceLanguage = "Idioma de la app",
            knownLanguage = "Tu idioma",
            learningLanguage = "Idioma que estudias",
            explanationLanguage = "Idioma de explicaciones",
            start = "Empezar"
        )
        "uk" -> OnboardingCopy(
            title = "MurrLex Ð²Ñ–Ñ‚Ð°Ñ”",
            guide = "ÐšÐ¾Ñ€Ð¾Ñ‚ÐºÐ¸Ð¹ ÑÑ‚Ð°Ñ€Ñ‚: Ð¾Ð±ÐµÑ€Ñ–Ñ‚ÑŒ Ð¼Ð¾Ð²Ñƒ Ñ–Ð½Ñ‚ÐµÑ€Ñ„ÐµÐ¹ÑÑƒ, Ð²Ð°ÑˆÑƒ Ð¼Ð¾Ð²Ñƒ, Ð¼Ð¾Ð²Ñƒ Ð½Ð°Ð²Ñ‡Ð°Ð½Ð½Ñ Ñ– Ð¼Ð¾Ð²Ñƒ Ð¿Ð¾ÑÑÐ½ÐµÐ½ÑŒ.",
            interfaceLanguage = "ÐœÐ¾Ð²Ð° Ñ–Ð½Ñ‚ÐµÑ€Ñ„ÐµÐ¹ÑÑƒ",
            knownLanguage = "Ð’Ð°ÑˆÐ° Ð¼Ð¾Ð²Ð°",
            learningLanguage = "ÐœÐ¾Ð²Ð°, ÑÐºÑƒ Ð²Ð¸Ð²Ñ‡Ð°Ñ”Ñ‚Ðµ",
            explanationLanguage = "ÐœÐ¾Ð²Ð° Ð¿Ð¾ÑÑÐ½ÐµÐ½ÑŒ",
            start = "ÐŸÐ¾Ñ‡Ð°Ñ‚Ð¸"
        )
        "ru" -> OnboardingCopy(
            title = "MurrLex Ð¿Ñ€Ð¸Ð²ÐµÑ‚ÑÑ‚Ð²ÑƒÐµÑ‚",
            guide = "ÐšÑ€Ð°Ñ‚ÐºÐ¸Ð¹ ÑÑ‚Ð°Ñ€Ñ‚: Ð²Ñ‹Ð±ÐµÑ€Ð¸Ñ‚Ðµ ÑÐ·Ñ‹Ðº Ð¸Ð½Ñ‚ÐµÑ€Ñ„ÐµÐ¹ÑÐ°, Ð²Ð°Ñˆ ÑÐ·Ñ‹Ðº, ÑÐ·Ñ‹Ðº, ÐºÐ¾Ñ‚Ð¾Ñ€Ñ‹Ð¹ Ð¸Ð·ÑƒÑ‡Ð°ÐµÑ‚Ðµ, Ð¸ ÑÐ·Ñ‹Ðº Ð¾Ð±ÑŠÑÑÐ½ÐµÐ½Ð¸Ð¹.",
            interfaceLanguage = "Ð¯Ð·Ñ‹Ðº Ð¸Ð½Ñ‚ÐµÑ€Ñ„ÐµÐ¹ÑÐ°",
            knownLanguage = "Ð’Ð°Ñˆ ÑÐ·Ñ‹Ðº",
            learningLanguage = "Ð¯Ð·Ñ‹Ðº, ÐºÐ¾Ñ‚Ð¾Ñ€Ñ‹Ð¹ Ð²Ñ‹ Ð¸Ð·ÑƒÑ‡Ð°ÐµÑ‚Ðµ",
            explanationLanguage = "Ð¯Ð·Ñ‹Ðº Ð¾Ð±ÑŠÑÑÐ½ÐµÐ½Ð¸Ð¹",
            start = "ÐÐ°Ñ‡Ð°Ñ‚ÑŒ"
        )
        "pl" -> OnboardingCopy(
            title = "MurrLex wita",
            guide = "Krotki start: wybierz jezyk aplikacji, swoj jezyk, jezyk nauki i jezyk wyjasnien.",
            interfaceLanguage = "Jezyk aplikacji",
            knownLanguage = "Twoj jezyk",
            learningLanguage = "Jezyk, ktorego sie uczysz",
            explanationLanguage = "Jezyk wyjasnien",
            start = "Start"
        )
        else -> OnboardingCopy(
            title = "MurrLex welcomes you",
            guide = "Quick start: choose the app interface language, your language, the language you study, and the language for explanations.",
            interfaceLanguage = "App interface language",
            knownLanguage = "Your language",
            learningLanguage = "Language you study",
            explanationLanguage = "Explanation language",
            start = "Start"
        )
    }
}

private fun Context.isNetworkAvailable(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

private fun offlineSpeechLocalizedName(tag: String, interfaceLanguage: String): String {
    val key = tag.substringBefore("-").lowercase(Locale.ROOT)
    return when (interfaceLanguage.lowercase(Locale.ROOT)) {
        "ru" -> when (key) {
            "en" -> "ÐÐ½Ð³Ð»Ð¸Ð¹ÑÐºÐ¸Ð¹"
            "pl" -> "ÐŸÐ¾Ð»ÑŒÑÐºÐ¸Ð¹"
            "ru" -> "Ð ÑƒÑÑÐºÐ¸Ð¹"
            "de" -> "ÐÐµÐ¼ÐµÑ†ÐºÐ¸Ð¹"
            "es" -> "Ð˜ÑÐ¿Ð°Ð½ÑÐºÐ¸Ð¹"
            else -> tag
        }
        "pl" -> when (key) {
            "en" -> "Angielski"
            "pl" -> "Polski"
            "ru" -> "Rosyjski"
            "de" -> "Niemiecki"
            "es" -> "Hiszpanski"
            else -> tag
        }
        "de" -> when (key) {
            "en" -> "Englisch"
            "pl" -> "Polnisch"
            "ru" -> "Russisch"
            "de" -> "Deutsch"
            "es" -> "Spanisch"
            else -> tag
        }
        "es" -> when (key) {
            "en" -> "Ingles"
            "pl" -> "Polaco"
            "ru" -> "Ruso"
            "de" -> "Aleman"
            "es" -> "Espanol"
            else -> tag
        }
        "uk", "ua" -> when (key) {
            "en" -> "ÐÐ½Ð³Ð»Ñ–Ð¹ÑÑŒÐºÐ°"
            "pl" -> "ÐŸÐ¾Ð»ÑŒÑÑŒÐºÐ°"
            "ru" -> "Ð Ð¾ÑÑ–Ð¹ÑÑŒÐºÐ°"
            "de" -> "ÐÑ–Ð¼ÐµÑ†ÑŒÐºÐ°"
            "es" -> "Ð†ÑÐ¿Ð°Ð½ÑÑŒÐºÐ°"
            else -> tag
        }
        "be", "by" -> when (key) {
            "en" -> "ÐÐ½Ð³Ð»Ñ–Ð¹ÑÐºÐ°Ñ"
            "pl" -> "ÐŸÐ¾Ð»ÑŒÑÐºÐ°Ñ"
            "ru" -> "Ð ÑƒÑÐºÐ°Ñ"
            "de" -> "ÐÑÐ¼ÐµÑ†ÐºÐ°Ñ"
            "es" -> "Ð†ÑÐ¿Ð°Ð½ÑÐºÐ°Ñ"
            else -> tag
        }
        else -> when (key) {
            "en" -> "English"
            "pl" -> "Polish"
            "ru" -> "Russian"
            "de" -> "German"
            "es" -> "Spanish"
            else -> tag
        }
    }
}

@Composable
private fun TranslateScreen(
    state: StudyUiState,
    onSourceLanguageChange: (String) -> Unit,
    onTargetLanguageChange: (String) -> Unit,
    onInputChange: (String) -> Unit,
    onClear: () -> Unit,
    onAddCard: () -> Unit,
    onSwapLanguages: () -> Unit,
    translateAutoSpeakEnabled: Boolean,
    onTranslateAutoSpeakChange: (Boolean) -> Unit,
    onSpeakTranslation: () -> Unit,
    onSpeakInput: () -> Unit,
    onVoiceInput: () -> Unit,
    onTargetVoiceInput: () -> Unit
) {
    val splitMode = state.workMode == WorkMode.SPLIT
    var splitResultExpanded by remember { mutableStateOf(false) }
    var normalResultExpanded by remember { mutableStateOf(false) }
    var translationPulse by remember { mutableStateOf(false) }
    LaunchedEffect(state.translationOutput) {
        if (state.translationOutput.isNotBlank()) {
            translationPulse = true
            delay(900)
            translationPulse = false
        }
    }
    val translationBorderColor by animateColorAsState(
        targetValue = if (translationPulse) BrandSaladColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
        animationSpec = tween(durationMillis = 520),
        label = "translationBorderPulse"
    )
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (splitMode) {
            SplitTranslationPanel(
                title = nativeLanguageLabel(state.activeVocabularyTargetLanguage),
                text = state.translationOutput,
                flipped = true,
                modifier = Modifier.weight(1f),
                attribution = state.translationAttribution,
                onSpeak = onSpeakTranslation,
                onVoiceInput = onTargetVoiceInput,
                borderColor = translationBorderColor,
                borderHighlighted = translationPulse,
                onClick = { splitResultExpanded = !splitResultExpanded }
            )
            if (!splitResultExpanded) {
                SplitTranslationPanel(
                    title = nativeLanguageLabel(state.activeVocabularySourceLanguage),
                    text = state.translationInput,
                    flipped = false,
                    editable = true,
                    onValueChange = onInputChange,
                    modifier = Modifier.weight(1f),
                    onSpeak = onSpeakInput,
                    onVoiceInput = onVoiceInput
                )
            }
        } else {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable { normalResultExpanded = !normalResultExpanded },
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(if (translationPulse) 3.dp else 1.dp, translationBorderColor)
            ) {
                Box(Modifier.fillMaxSize().padding(18.dp)) {
                    LanguageSpeakerRow(
                        title = nativeLanguageLabel(state.activeVocabularyTargetLanguage),
                        enabled = state.translationOutput.isNotBlank(),
                        autoSpeakEnabled = translateAutoSpeakEnabled,
                        onSpeak = onSpeakTranslation,
                        onAutoSpeakChange = onTranslateAutoSpeakChange,
                        modifier = Modifier.align(Alignment.TopStart)
                    )
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(top = 42.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.Start
                    ) {
                        if (state.translationOutput.isNotBlank()) {
                            Text(
                                text = state.translationOutput,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Start
                            )
                            if (state.translationAttribution.isNotBlank()) {
                                Text(
                                    text = state.translationAttribution,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                )
                            }
                        }
                    }
                }
            }
            if (!normalResultExpanded) {
                Surface(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                ) {
                    Box(Modifier.fillMaxSize().padding(18.dp)) {
                        LanguageSpeakerRow(
                            title = nativeLanguageLabel(state.activeVocabularySourceLanguage),
                            enabled = state.translationInput.isNotBlank(),
                            onSpeak = onSpeakInput,
                            modifier = Modifier.align(Alignment.TopStart)
                        )
                        Column(
                            Modifier
                                .fillMaxSize()
                                .padding(top = 42.dp)
                        ) {
                            OutlinedTextField(
                                value = state.translationInput,
                                onValueChange = onInputChange,
                                modifier = Modifier.fillMaxSize(),
                                minLines = 8,
                                shape = RoundedCornerShape(18.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    disabledBorderColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DictionaryLanguageDropdown(
                    label = "",
                    value = state.activeVocabularySourceLanguage,
                    onValueChange = onSourceLanguageChange,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onSwapLanguages,
                    modifier = Modifier.size(58.dp)
                ) {
                    Icon(
                        Icons.Default.SwapHoriz,
                        contentDescription = "Swap languages",
                        modifier = Modifier.size(36.dp)
                    )
                }
                DictionaryLanguageDropdown(
                    label = "",
                    value = state.activeVocabularyTargetLanguage,
                    onValueChange = onTargetLanguageChange,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onAddCard,
                    modifier = Modifier.weight(0.82f).height(52.dp),
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 0.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add card", modifier = Modifier.size(22.dp))
                }
                Button(
                    onClick = onVoiceInput,
                    modifier = Modifier.weight(1.12f).height(58.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE4F6E8),
                        contentColor = Color(0xFF234231)
                    ),
                    contentPadding = PaddingValues(horizontal = 0.dp)
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "Speak",
                        modifier = Modifier.size(30.dp),
                        tint = Color(0xFF234231)
                    )
                }
                OutlinedButton(
                    onClick = onClear,
                    modifier = Modifier.weight(0.82f).height(52.dp),
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 0.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Clear", modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

@Composable
private fun LanguageSpeakerRow(
    title: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    autoSpeakEnabled: Boolean = true,
    onSpeak: (() -> Unit)? = null,
    onAutoSpeakChange: ((Boolean) -> Unit)? = null
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.46f)
        )
        if (onSpeak != null) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .pointerInput(autoSpeakEnabled, enabled) {
                        detectTapGestures(
                            onTap = {
                                if (autoSpeakEnabled) {
                                    if (enabled) onSpeak()
                                } else {
                                    onAutoSpeakChange?.invoke(true)
                                }
                            },
                            onLongPress = { onAutoSpeakChange?.invoke(false) }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (autoSpeakEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                    contentDescription = if (autoSpeakEnabled) "Speak" else "Auto speak off",
                    tint = if (enabled && autoSpeakEnabled) {
                        Color(0xFF234231)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    },
                    modifier = Modifier.size(21.dp)
                )
            }
        }
    }
}

@Composable
private fun SplitTranslationPanel(
    title: String,
    text: String,
    flipped: Boolean,
    modifier: Modifier = Modifier,
    editable: Boolean = false,
    onValueChange: (String) -> Unit = {},
    attribution: String = "",
    onSpeak: (() -> Unit)? = null,
    onVoiceInput: (() -> Unit)? = null,
    borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
    borderHighlighted: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (borderHighlighted) 3.dp else 1.dp, borderColor)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(18.dp), contentAlignment = Alignment.Center) {
            LanguageSpeakerRow(
                title = title,
                enabled = text.isNotBlank(),
                onSpeak = onSpeak,
                modifier = Modifier
                    .align(if (flipped) Alignment.BottomEnd else Alignment.TopStart)
                    .graphicsLayer { rotationZ = if (flipped) 180f else 0f }
            )
            if (onVoiceInput != null) {
                Surface(
                    modifier = Modifier
                        .align(if (flipped) Alignment.BottomStart else Alignment.TopEnd)
                        .graphicsLayer { rotationZ = if (flipped) 180f else 0f }
                        .size(54.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFE4F6E8)
                ) {
                    IconButton(onClick = onVoiceInput) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "Speak this language",
                            modifier = Modifier.size(28.dp),
                            tint = Color(0xFF234231)
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        if (flipped) {
                            PaddingValues(bottom = if (onVoiceInput != null) 64.dp else 42.dp)
                        } else {
                            PaddingValues(top = 42.dp)
                        }
                    )
                    .graphicsLayer { rotationZ = if (flipped) 180f else 0f },
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start
            ) {
                if (editable) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = onValueChange,
                        modifier = Modifier.fillMaxSize(),
                        minLines = 5,
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            disabledContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            disabledBorderColor = Color.Transparent
                        )
                    )
                } else if (text.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = text,
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Start
                            )
                            if (attribution.isNotBlank()) {
                                Text(
                                    text = attribution,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun TestAnswerOptions(
    card: Flashcard?,
    cards: List<Flashcard>,
    isBackVisible: Boolean,
    selectedAnswer: String,
    answerFeedbackVisible: Boolean,
    onAnswer: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (card == null) return
    val expectedAnswer = card.expectedAnswerText(isBackVisible)
    val normalizedExpectedAnswer = remember(expectedAnswer) { normalizeAnswerText(expectedAnswer) }
    val choices = remember(card.id, cards, isBackVisible) {
        testChoices(card, cards, isBackVisible)
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        choices.forEachIndexed { index, choice ->
            val selected = selectedAnswer == choice
            OutlinedButton(
                onClick = { onAnswer(choice) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(
                    1.dp,
                    when {
                        selected && normalizeAnswerText(choice) == normalizedExpectedAnswer -> BrandSaladColor
                        selected && answerFeedbackVisible -> BrandRedColor
                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
                    }
                )
            ) {
                Text(choice, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
        }
    }
}

private fun testChoices(card: Flashcard, cards: List<Flashcard>, isBackVisible: Boolean): List<String> {
    val correct = card.expectedAnswerText(isBackVisible).ifBlank { card.correctText().ifBlank { card.nativeText() } }.ifBlank { "Correct" }
    val normalizedCorrect = normalizeAnswerText(correct)
    val distractors = cards.asSequence()
        .filterNot { it.id == card.id }
        .map { it.expectedAnswerText(isBackVisible).ifBlank { it.correctText().ifBlank { it.nativeText() } } }
        .filter { it.isNotBlank() }
        .map { it to normalizeAnswerText(it) }
        .filter { (_, normalized) -> normalized != normalizedCorrect }
        .distinctBy { (_, normalized) -> normalized }
        .map { (text, _) -> text }
        .toList()
        .shuffled(Random(card.id + cards.size + correct.hashCode()))
        .take(3)
    val fallback = listOf("I am not sure", "Review later", "Skip this one")
        .map { it to normalizeAnswerText(it) }
        .filter { (_, normalized) -> normalized != normalizedCorrect && distractors.none { d -> normalizeAnswerText(d) == normalized } }
        .map { (text, _) -> text }
    return (listOf(correct) + distractors + fallback)
        .distinctBy { normalizeAnswerText(it) }
        .take(4)
        .shuffled(Random(card.id * 31 + cards.size + correct.hashCode()))
}
@Composable
private fun CatDialogsScreen(
    dialogs: List<CatDialog>,
    onOpenDialog: (CatDialog) -> Unit,
    onNewDialog: () -> Unit,
    onToggleFeatured: (CatDialog) -> Unit,
    onSetHidden: (CatDialog, Boolean) -> Unit,
    onDeleteDialog: (String) -> Unit,
    onMoveDialog: (String, String) -> Unit
) {
    var pendingDelete by remember { mutableStateOf<CatDialog?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var searchExpanded by remember { mutableStateOf(false) }
    val visibleDialogs = remember(dialogs, searchQuery) {
        dialogs.filter { it.matchesCatDialogQuery(searchQuery) }
    }
    pendingDelete?.let { dialog ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete dialog?") },
            text = { Text("This cat dialog will be removed from the app.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    onDeleteDialog(dialog.id)
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(searchExpanded, searchQuery) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Final)
                        if (event.changes.any { it.changedToDown() } && searchExpanded && searchQuery.isBlank()) {
                            searchExpanded = false
                        }
                    }
                }
            }
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (searchExpanded) {
                ExpandableSearchAction(
                    query = searchQuery,
                    expanded = true,
                    onQueryChange = { searchQuery = it },
                    onExpandedChange = { searchExpanded = it },
                    modifier = Modifier.weight(1f)
                )
            } else {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Cat dialogs",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Starred dialogs are kept; old unstarred dialogs are pruned automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ExpandableSearchAction(
                    query = searchQuery,
                    expanded = false,
                    onQueryChange = { searchQuery = it },
                    onExpandedChange = { searchExpanded = it }
                )
                Button(onClick = onNewDialog, shape = RoundedCornerShape(18.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("New")
                }
            }
        }
        if (visibleDialogs.isEmpty()) {
            EmptyState(if (searchQuery.isBlank()) "No cat dialogs yet." else "No dialogs match this filter.")
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(visibleDialogs, key = { it.id }) { dialog ->
                    val index = visibleDialogs.indexOfFirst { it.id == dialog.id }
                    CatDialogTile(
                        dialog = dialog,
                        onOpen = { onOpenDialog(dialog) },
                        onToggleFeatured = { onToggleFeatured(dialog) },
                        onSetHidden = { hidden -> onSetHidden(dialog, hidden) },
                        onMoveUp = {
                            if (index > 0) onMoveDialog(dialog.id, visibleDialogs[index - 1].id)
                        },
                        onMoveDown = {
                            if (index >= 0 && index < visibleDialogs.lastIndex) onMoveDialog(dialog.id, visibleDialogs[index + 1].id)
                        },
                        onDelete = { pendingDelete = dialog }
                    )
                }
            }
        }
    }
}

private fun CatDialog.matchesCatDialogQuery(query: String): Boolean {
    val normalized = query.trim()
    if (normalized.isBlank()) return true
    val haystack = listOf(
        title,
        basicLanguage,
        targetLanguage,
        createdAt,
        updatedAt,
        messages.joinToString(" ") { message ->
            listOf(message.text, message.analysis, message.featuredSelection, message.createdAt).joinToString(" ")
        }
    ).joinToString(" ")
    return haystack.contains(normalized, ignoreCase = true)
}

@Composable
private fun CatDialogTile(
    dialog: CatDialog,
    onOpen: () -> Unit,
    onToggleFeatured: () -> Unit,
    onSetHidden: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (dialog.featured) BorderStroke(2.dp, BrandSaladColor) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(24.dp),
                color = if (dialog.featured) BrandSaladColor.copy(alpha = 0.22f) else Color(0xFFFFF2F6)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Chat, contentDescription = null, tint = Color(0xFF25382F))
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = dialog.title.ifBlank { "Cat chat" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${dialog.basicLanguage.shortLanguageCode()} -> ${dialog.targetLanguage.shortLanguageCode()} | ${dialog.messages.size} messages",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Modified: ${dialog.updatedAt.ifBlank { dialog.createdAt.ifBlank { "not saved" } }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                dialog.messages.lastOrNull()?.text?.takeIf { it.isNotBlank() }?.let { preview ->
                    Text(
                        text = preview,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onToggleFeatured, modifier = Modifier.size(34.dp)) {
                    Icon(
                        if (dialog.featured) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Featured dialog",
                        tint = if (dialog.featured) BrandSaladColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { onSetHidden(!dialog.hidden) }, modifier = Modifier.size(34.dp)) {
                    Icon(
                        if (dialog.hidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Hide dialog",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onMoveUp, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up")
                }
                IconButton(onClick = onMoveDown, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down")
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete dialog")
                }
            }
        }
    }
}

@Composable
private fun LessonCatalogScreen(
    lessons: List<Lesson>,
    selectedLessonIds: Set<String>,
    searchQuery: String,
    sortMode: LessonSortMode,
    sortDescending: Boolean,
    listState: LazyListState,
    onSearchQueryChange: (String) -> Unit,
    onSortModeChange: (LessonSortMode) -> Unit,
    onSortDirectionChange: (Boolean) -> Unit,
    onOpenLesson: (Lesson) -> Unit,
    onEditLesson: (Lesson) -> Unit,
    onToggleLessonSelection: (String) -> Unit,
    onDeleteSelectedLessons: () -> Unit,
    onDeleteLesson: (String) -> Unit,
    onClearSelection: () -> Unit,
    onMoveLesson: (String, String) -> Unit,
    onSaveLessonOrder: () -> Unit,
    onCreateLesson: () -> Unit,
    onCopyLesson: (Lesson) -> Unit,
    onDownloadLesson: (Lesson) -> Unit,
    onShareLesson: (Lesson) -> Unit,
    onSetLessonHidden: (Lesson, Boolean) -> Unit,
    workMode: WorkMode,
    onWorkModeChange: (WorkMode) -> Unit,
    onOpenCatDialogs: () -> Unit,
    quickVocabularySourceLanguage: String,
    quickVocabularyTargetLanguage: String,
    onQuickVocabularySourceChange: (String) -> Unit,
    onQuickVocabularyTargetChange: (String) -> Unit,
    onSwapQuickVocabularyLanguages: () -> Unit,
    onQuickVoiceInput: () -> Unit,
    onImportLessons: () -> Unit
) {
    val lessonBounds = remember { mutableStateMapOf<String, IntRange>() }
    var draggingLessonId by remember { mutableStateOf<String?>(null) }
    var dragStartRootY by remember { mutableStateOf(0f) }
    var dragPointerOffsetY by remember { mutableStateOf(0f) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var configuringLessonId by remember { mutableStateOf<String?>(null) }
    var pendingDeleteLesson by remember { mutableStateOf<Lesson?>(null) }
    var searchExpanded by remember { mutableStateOf(searchQuery.isNotBlank()) }

    pendingDeleteLesson?.let { lesson ->
        AlertDialog(
            onDismissRequest = { pendingDeleteLesson = null },
            title = { Text("Delete lesson?") },
            text = { Text("This lesson will be removed from the app.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDeleteLesson = null
                    configuringLessonId = null
                    onDeleteLesson(lesson.id)
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteLesson = null }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(configuringLessonId, searchExpanded, searchQuery) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Final)
                        if (event.changes.any { it.changedToDown() }) {
                            if (configuringLessonId != null) configuringLessonId = null
                            if (searchExpanded && searchQuery.isBlank()) searchExpanded = false
                        }
                    }
                }
            }
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onImportLessons,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Upload JSON")
            }
            OutlinedButton(
                onClick = onCreateLesson,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("New lesson")
            }
        }

        val selectedLessons = remember(lessons, selectedLessonIds) {
            lessons.filter { it.id in selectedLessonIds }
        }
        val lessonIndexById = remember(lessons) {
            lessons.mapIndexed { index, lesson -> lesson.id to index }.toMap()
        }
        if (selectedLessonIds.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${selectedLessonIds.size} selected",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                selectedLessons.singleOrNull()?.let { selectedLesson ->
                    IconButton(onClick = { onCopyLesson(selectedLesson) }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy lesson")
                    }
                    IconButton(onClick = { onDownloadLesson(selectedLesson) }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Download lesson JSON")
                    }
                    IconButton(onClick = { onShareLesson(selectedLesson) }) {
                        Icon(Icons.Default.Share, contentDescription = "Share lesson JSON")
                    }
                }
                OutlinedButton(
                    onClick = {
                        onClearSelection()
                        configuringLessonId = null
                    },
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("Clear")
                }
                Button(
                    onClick = {
                        onDeleteSelectedLessons()
                        configuringLessonId = null
                    },
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Delete selected")
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Lessons",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            WorkModeToggleRow(
                workMode = workMode,
                onWorkModeChange = onWorkModeChange,
                onOpenCatDialogs = onOpenCatDialogs
            )
        }

        LessonCatalogFilterRow(
            searchQuery = searchQuery,
            sortMode = sortMode,
            sortDescending = sortDescending,
            searchExpanded = searchExpanded,
            onSearchQueryChange = onSearchQueryChange,
            onSearchExpandedChange = { searchExpanded = it },
            onSortModeChange = onSortModeChange,
            onSortDirectionChange = onSortDirectionChange
        )

        if (lessons.isEmpty()) {
            EmptyState(if (searchQuery.isBlank()) "No lessons yet. Import JSON files or create a lesson." else "No lessons match this filter.")
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                contentPadding = PaddingValues(top = 10.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(lessons, key = { it.id }) { lesson ->
                    LessonTile(
                        lesson = lesson,
                        selected = lesson.id in selectedLessonIds,
                        configuring = lesson.id == configuringLessonId,
                        configurationActive = configuringLessonId != null,
                        selectionMode = selectedLessonIds.isNotEmpty(),
                        dragging = draggingLessonId == lesson.id,
                        dragOffsetY = if (draggingLessonId == lesson.id) dragOffsetY else 0f,
                        cardSearchMatchCount = lesson.cardContentMatchCount(searchQuery),
                        onOpen = { onOpenLesson(lesson) },
                        onEdit = { onEditLesson(lesson) },
                        onEnterConfiguration = { configuringLessonId = lesson.id },
                        onClearConfiguration = { configuringLessonId = null },
                        onDelete = { pendingDeleteLesson = lesson },
                        onDownload = { onDownloadLesson(lesson) },
                        onShare = { onShareLesson(lesson) },
                        onSetHidden = { hidden ->
                            configuringLessonId = null
                            onSetLessonHidden(lesson, hidden)
                        },
                        onMoveUp = {
                            val index = lessonIndexById[lesson.id] ?: -1
                            if (index > 0) {
                                onMoveLesson(lesson.id, lessons[index - 1].id)
                                onSaveLessonOrder()
                            }
                        },
                        onMoveDown = {
                            val index = lessonIndexById[lesson.id] ?: -1
                            if (index >= 0 && index < lessons.lastIndex) {
                                onMoveLesson(lesson.id, lessons[index + 1].id)
                                onSaveLessonOrder()
                            }
                        },
                        onToggleSelection = { onToggleLessonSelection(lesson.id) },
                        onBoundsChanged = { bounds -> lessonBounds[lesson.id] = bounds },
                        onDragStart = { localY ->
                            configuringLessonId = lesson.id
                            draggingLessonId = lesson.id
                            dragStartRootY = (lessonBounds[lesson.id]?.first ?: 0).toFloat() + localY
                            dragPointerOffsetY = 0f
                            dragOffsetY = 0f
                        },
                        onDrag = { deltaY ->
                            val draggedId = draggingLessonId ?: return@LessonTile
                            val minY = lessonBounds.values.minOfOrNull { it.first }?.toFloat() ?: dragStartRootY
                            val maxY = lessonBounds.values.maxOfOrNull { it.last }?.toFloat() ?: dragStartRootY
                            val pointerY = (dragStartRootY + dragPointerOffsetY + deltaY).coerceIn(minY, maxY)
                            dragPointerOffsetY = pointerY - dragStartRootY
                            dragOffsetY = dragPointerOffsetY.coerceIn(-72f, 72f)
                            val pointerInt = pointerY.toInt()
                            val targetId = lessonBounds.entries
                                .firstOrNull { entry ->
                                    if (entry.key == draggedId || pointerInt !in entry.value) {
                                        false
                                    } else {
                                        val midpoint = (entry.value.first + entry.value.last) / 2
                                        if (deltaY > 0) pointerInt > midpoint else pointerInt < midpoint
                                    }
                                }
                                ?.key
                            if (targetId != null) {
                                onMoveLesson(draggedId, targetId)
                                dragStartRootY = pointerY
                                dragPointerOffsetY = 0f
                                dragOffsetY = 0f
                            }
                        },
                        onDragEnd = {
                            if (draggingLessonId != null) onSaveLessonOrder()
                            draggingLessonId = null
                            dragPointerOffsetY = 0f
                            dragOffsetY = 0f
                        }
                    )
                }
            }
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 18.dp)
                .height(58.dp)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onQuickVoiceInput() })
                },
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuickVocabularyLanguageCode(
                    language = quickVocabularySourceLanguage,
                    contentDescription = "Basic recognition language",
                    onTap = onSwapQuickVocabularyLanguages,
                    onLanguageChange = onQuickVocabularySourceChange
                )
                Icon(
                    Icons.Default.Mic,
                    contentDescription = "Add vocabulary by voice",
                    tint = BrandSaladColor,
                    modifier = Modifier.size(30.dp)
                )
                QuickVocabularyLanguageCode(
                    language = quickVocabularyTargetLanguage,
                    contentDescription = "Target translation language",
                    onTap = onSwapQuickVocabularyLanguages,
                    onLanguageChange = onQuickVocabularyTargetChange
                )
            }
        }
    }
}

@Composable
private fun LessonCatalogFilterRow(
    searchQuery: String,
    sortMode: LessonSortMode,
    sortDescending: Boolean,
    searchExpanded: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onSearchExpandedChange: (Boolean) -> Unit,
    onSortModeChange: (LessonSortMode) -> Unit,
    onSortDirectionChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExpandableSearchAction(
            query = searchQuery,
            expanded = searchExpanded,
            onQueryChange = onSearchQueryChange,
            onExpandedChange = onSearchExpandedChange,
            modifier = if (searchExpanded) Modifier.weight(1f) else Modifier
        )
        if (!searchExpanded) {
            Box {
                OutlinedButton(
                    onClick = { sortMenuExpanded = true },
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Default.SortByAlpha, contentDescription = "Sort lessons")
                }
                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false }
                ) {
                    LessonSortMode.values().forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = {
                                sortMenuExpanded = false
                                onSortModeChange(option)
                            },
                            leadingIcon = if (option == sortMode) {
                                { Icon(Icons.Default.DoneAll, contentDescription = null) }
                            } else {
                                null
                            }
                        )
                    }
                }
            }
            OutlinedButton(
                onClick = { onSortDirectionChange(!sortDescending) },
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp)
            ) {
                Icon(
                    if (sortDescending) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                    contentDescription = if (sortDescending) "Descending" else "Ascending"
                )
            }
        }
    }
}

@Composable
private fun ExpandableSearchAction(
    query: String,
    expanded: Boolean,
    onQueryChange: (String) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    if (expanded) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                onQueryChange(it)
                if (it.isNotBlank()) onExpandedChange(true)
            },
            modifier = modifier.height(52.dp),
            singleLine = true,
            leadingIcon = {
                IconButton(
                    onClick = {
                        if (query.isBlank()) {
                            onExpandedChange(false)
                        }
                    }
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Collapse search")
                }
            },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear search")
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            shape = RoundedCornerShape(18.dp)
        )
    } else {
        OutlinedButton(
            onClick = { onExpandedChange(true) },
            shape = RoundedCornerShape(18.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            modifier = modifier
        ) {
            Icon(Icons.Default.Search, contentDescription = "Search")
        }
    }
}

@Composable
private fun QuickVocabularyLanguageCode(
    language: String,
    contentDescription: String,
    onTap: () -> Unit,
    onLanguageChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            modifier = Modifier
                .size(width = 70.dp, height = 42.dp)
                .pointerInput(language) {
                    detectTapGestures(
                        onTap = { onTap() },
                        onLongPress = { expanded = true }
                    )
                },
            shape = RoundedCornerShape(14.dp),
            color = BrandSaladColor.copy(alpha = 0.16f),
            border = BorderStroke(1.dp, BrandSaladColor.copy(alpha = 0.55f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = dictionaryLanguageCode(language),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF234231)
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DictionaryLanguageOptions.forEach { (optionLanguage, optionLabel) ->
                DropdownMenuItem(
                    text = { Text(optionLabel.substringBefore(" - ")) },
                    onClick = {
                        expanded = false
                        onLanguageChange(optionLanguage)
                    },
                    leadingIcon = if (optionLanguage.equals(language, ignoreCase = true)) {
                        { Icon(Icons.Default.DoneAll, contentDescription = contentDescription) }
                    } else {
                        null
                    }
                )
            }
        }
    }
}

@Composable
private fun LessonTile(
    lesson: Lesson,
    selected: Boolean,
    configuring: Boolean,
    configurationActive: Boolean,
    selectionMode: Boolean,
    dragging: Boolean,
    dragOffsetY: Float,
    cardSearchMatchCount: Int,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onEnterConfiguration: () -> Unit,
    onClearConfiguration: () -> Unit,
    onDelete: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onSetHidden: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onToggleSelection: () -> Unit,
    onBoundsChanged: (IntRange) -> Unit,
    onDragStart: (Float) -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val ui = rememberUiText()
    var showLessonInfo by remember { mutableStateOf(false) }

    if (showLessonInfo && lesson.lessonInfo.isNotBlank()) {
        CardTextDialog(
            title = "Lesson info",
            text = lesson.lessonInfo,
            onDismiss = { showLessonInfo = false }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (dragging) 1f else 0f)
            .graphicsLayer {
                translationY = if (dragging) dragOffsetY else 0f
                shadowElevation = if (dragging) 18f else 0f
            }
            .onGloballyPositioned { coordinates ->
                val top = coordinates.positionInRoot().y.toInt()
                onBoundsChanged(top..(top + coordinates.size.height))
            }
            .then(
                Modifier.pointerInput(lesson.id, selectionMode, configurationActive) {
                    detectTapGestures(
                        onTap = {
                            when {
                                configurationActive -> onClearConfiguration()
                                selectionMode -> onToggleSelection()
                                else -> onOpen()
                            }
                        },
                        onLongPress = { onEnterConfiguration() }
                    )
                }
            )
            .then(
                if (configuring) {
                    Modifier.pointerInput(lesson.id) {
                        detectDragGestures(
                            onDragStart = { offset -> onDragStart(offset.y) },
                            onDragEnd = onDragEnd,
                            onDragCancel = onDragEnd,
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount.y)
                            }
                        )
                    }
                } else if (!configurationActive) {
                    Modifier.pointerInput(lesson.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { offset -> onDragStart(offset.y) },
                            onDragEnd = onDragEnd,
                            onDragCancel = onDragEnd,
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount.y)
                            }
                        )
                    }
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (configuring) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = when {
            dragging -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f))
            configuring -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            else -> null
        },
        elevation = CardDefaults.cardElevation(defaultElevation = if (dragging || configuring) 12.dp else 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(166.dp)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.align(Alignment.BottomEnd),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (lesson.hidden) {
                    Icon(
                        Icons.Default.VisibilityOff,
                        contentDescription = "Hidden lesson",
                        tint = BrandSaladColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (lesson.lessonInfo.isNotBlank()) {
                    IconButton(
                        onClick = { showLessonInfo = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Lesson info",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = lesson.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2
                )
                Text(
                    text = lesson.cardKindSummary(ui),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = ui.questionsText(lesson.cards.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (cardSearchMatchCount > 0) {
                    Text(
                        text = "$cardSearchMatchCount card matches",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE67E22)
                    )
                }
                Text(
                    text = ui.doneText(lesson.cards.count { it.starCount() == 3 }, lesson.cards.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = ui.completedText(lesson.timesCompleted),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val modifiedAt = lesson.updatedDisplayText()
                if (modifiedAt.isNotBlank()) {
                    Text(
                        text = "Modified: $modifiedAt",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (configuring) {
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                            shape = RoundedCornerShape(26.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDownload) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Download lesson JSON")
                    }
                    IconButton(onClick = { onSetHidden(!lesson.hidden) }) {
                        Icon(
                            if (lesson.hidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (lesson.hidden) "Make lesson visible" else "Hide lesson"
                        )
                    }
                    IconButton(onClick = onMoveUp) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move lesson up")
                    }
                    IconButton(onClick = onMoveDown) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move lesson down")
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit lesson")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete lesson")
                    }
                    IconButton(onClick = onShare) {
                        Icon(Icons.Default.Share, contentDescription = "Share lesson JSON")
                    }
                }
            }
        }
    }
}
@Composable
private fun StudyScreen(
    state: StudyUiState,
    isDeviceOnline: Boolean,
    statusBlinkOn: Boolean,
    onModeChange: (StudyMode) -> Unit,
    onShowAllCardsChange: (Boolean) -> Unit,
    onPreviousCard: () -> Unit,
    onNextCard: () -> Unit,
    onFirstCard: () -> Unit,
    onFirstRemainingCard: () -> Unit,
    onLastCompletedCard: () -> Unit,
    onNewPortion: () -> Unit,
    onNextLesson: () -> Unit,
    onOpenCatalog: () -> Unit,
    showMap: Boolean,
    onOpenCardFromMap: (Int) -> Unit,
    onRefreshOnlineState: () -> Unit,
    onToggleCard: () -> Unit,
    onToggleStar: (Int, Int) -> Unit,
    onQuickEditCard: () -> Unit,
    onEditCard: () -> Unit,
    quickEditMode: Boolean,
    showCardLog: Boolean,
    onTestAnswer: (String) -> Unit,
    onShareCard: (Lesson, Flashcard) -> Unit,
    onShareCardSideAudio: (Flashcard, Boolean) -> Unit,
    onPlayOriginalVoice: (Flashcard) -> Unit,
    onDeleteCard: () -> Unit,
    onCopyCard: () -> Unit,
    onMoveCard: () -> Unit,
    onToggleFeaturedCard: () -> Unit,
    onToggleOriginalCardText: (Int) -> Unit,
    onGenerateTrainCards: (Flashcard) -> Unit
) {
    val context = LocalContext.current
    val selectedLesson = state.selectedLesson

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {


        CountersRow(
            portionSize = state.portionSize,
            remainingCount = state.remainingCount,
            completedCount = state.completedCount,
            onPortionClick = onFirstCard,
            onRemainingClick = onFirstRemainingCard,
            onDoneClick = onLastCompletedCard
        )

        state.studyEmptyMessage?.let { message ->
            StudyEmptyState(message = message)
        } ?: if (state.isPortionFinished) {
            FinishedScreen(onNewPortion = onNewPortion, onNextLesson = onNextLesson, onOpenCatalog = onOpenCatalog)
        } else if (showMap) {
            StudyIslandMapScreen(
                state = state,
                onOpenCard = onOpenCardFromMap,
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        } else {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = if (state.workMode == WorkMode.TESTS) {
                    Arrangement.spacedBy(8.dp, Alignment.Top)
                } else {
                    Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
                },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val effectiveDisplayTextSize = if (state.workMode == WorkMode.TESTS && state.displayTextSize != DisplayTextSize.VERY_SMALL) {
                    DisplayTextSize.VERY_SMALL
                } else {
                    state.displayTextSize
                }
                AnimatedContent(
                    targetState = state.currentIndex,
                    transitionSpec = {
                        val direction = if (state.cardTransitionDirection >= 0) 1 else -1
                        slideInHorizontally(
                            animationSpec = tween(durationMillis = 260),
                            initialOffsetX = { width -> width * direction }
                        ) togetherWith slideOutHorizontally(
                            animationSpec = tween(durationMillis = 260),
                            targetOffsetX = { width -> -width * direction }
                        ) using SizeTransform(clip = false)
                    },
                    label = "studyCardSlide"
                ) { animatedIndex ->
                    val animatedCard = state.currentPortion.getOrNull(animatedIndex)
                    val showOriginalText = animatedCard.shouldShowOriginalText(
                        isBackVisible = state.isBackVisible,
                        translatedOriginalCardIds = state.translatedOriginalCardIds
                    )
                    val visibleCardText = animatedCard?.displayedCardText(state.isBackVisible, showOriginalText).orEmpty()
                    val visibleCardSpeechTag = animatedCard?.speechLanguageTagForSide(
                        state.isBackVisible,
                        state.interfaceLanguage,
                        state.selectedLesson,
                        showOriginalText = showOriginalText
                    ).orEmpty()
                    val cardSideAudioCached = remember(
                        animatedCard?.id,
                        state.isBackVisible,
                        visibleCardText,
                        visibleCardSpeechTag,
                        state.openAiTtsModel,
                        state.openAiTtsVoice,
                        state.elevenLabsTtsLanguageCodes,
                        state.elevenLabsModel,
                        state.elevenLabsVoiceId
                    ) {
                        animatedCard != null && findCachedCardSideAudioFile(
                            context = context,
                            text = visibleCardText,
                            languageTag = visibleCardSpeechTag,
                            state = state
                        ) != null
                    }
                    val cardAudioStatus = when {
                        !isDeviceOnline -> CardAudioStatus.OFFLINE
                        else -> CardAudioStatus.ONLINE
                    }
                    val canFlipTestCard = state.workMode != WorkMode.TESTS ||
                        animatedCard?.id in state.completedCardIds ||
                        animatedCard?.hasEmptySide() == true
                    StudyCard(
                        card = animatedCard,
                        lesson = state.selectedLesson,
                        isBackVisible = state.isBackVisible,
                        onClick = if (canFlipTestCard) onToggleCard else ({}),
                        onToggleStar = onToggleStar,
                        onQuickEditCard = onQuickEditCard,
                        onEditCard = onEditCard,
                        quickEditMode = quickEditMode,
                        displayTextSize = effectiveDisplayTextSize,
                        controlSize = state.controlSize,
                        isCompleted = animatedCard?.id in state.completedCardIds,
                        positionLabel = animatedCard.sideLanguageCode(
                            state.isBackVisible,
                            state.selectedLesson,
                            showOriginalText
                        ),
                        showOriginalText = showOriginalText,
                        cardAudioStatus = cardAudioStatus,
                        cardSideAudioCached = cardSideAudioCached,
                        statusBlinkOn = isDeviceOnline && statusBlinkOn,
                        onRefreshOnlineState = onRefreshOnlineState,
                        onSwipePrevious = {
                            if (state.currentIndex > 0) {
                                onPreviousCard()
                            }
                        },
                        onSwipeNext = {
                            if (state.currentIndex < state.currentPortion.lastIndex) {
                                onNextCard()
                            }
                        },
                        showCardLog = showCardLog,
                    onShareCard = { card -> state.selectedLesson?.let { lesson -> onShareCard(lesson, card) } },
                    onShareCardSideAudio = { card -> onShareCardSideAudio(card, state.isBackVisible) },
                    onPlayOriginalVoice = onPlayOriginalVoice,
                    onDeleteCard = onDeleteCard,
                    onCopyCard = onCopyCard,
                    onMoveCard = onMoveCard,
                    onToggleFeaturedCard = onToggleFeaturedCard,
                    onToggleOriginalCardText = onToggleOriginalCardText,
                    onGenerateTrainCards = onGenerateTrainCards,
                    modifier = Modifier.fillMaxWidth()
                )
                }
                CardNavigation(
                    position = state.currentPositionLabel,
                    canGoBack = state.currentIndex > 0,
                    canGoForward = state.currentIndex < state.currentPortion.lastIndex,
                    onPreviousCard = onPreviousCard,
                    onNextCard = onNextCard
                )
                if (state.workMode == WorkMode.TESTS) {
                   TestAnswerOptions(
                       card = state.currentCard,
                       cards = state.currentPortion,
                       isBackVisible = state.isBackVisible,
                       selectedAnswer = state.answer,
                        answerFeedbackVisible = state.answerFeedbackVisible,
                        onAnswer = onTestAnswer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopControls(
    mode: StudyMode,
    showAllCards: Boolean,
    onModeChange: (StudyMode) -> Unit,
    onShowAllCardsChange: (Boolean) -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            color = MaterialTheme.colorScheme.surface
        ) {
            IconButton(onClick = { onModeChange(mode.nextMode()) }) {
                Icon(
                    imageVector = mode.displayIcon(),
                    contentDescription = mode.displayLabel(),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Surface(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            color = MaterialTheme.colorScheme.surface
        ) {
            IconButton(onClick = onReset) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Start over",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Surface(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            color = if (showAllCards) Color(0xFFE4F6E8) else MaterialTheme.colorScheme.surface
        ) {
            IconButton(onClick = { onShowAllCardsChange(!showAllCards) }) {
                StudyHideStarIcon(active = showAllCards)
            }
        }
    }
}

private fun StudyMode.displayIcon(): ImageVector {
    return when (this) {
        StudyMode.ORIGINAL -> Icons.Default.FormatListNumbered
        StudyMode.ALPHABETICAL -> Icons.Default.SortByAlpha
        StudyMode.RANDOM -> Icons.Default.Shuffle
    }
}

private fun StudyMode.nextMode(): StudyMode {
    return when (this) {
        StudyMode.ORIGINAL -> StudyMode.ALPHABETICAL
        StudyMode.ALPHABETICAL -> StudyMode.RANDOM
        StudyMode.RANDOM -> StudyMode.ORIGINAL
    }
}

private fun StudyMode.displayLabel(): String {
    return when (this) {
        StudyMode.ORIGINAL -> "Original"
        StudyMode.ALPHABETICAL -> "Alphabetical"
        StudyMode.RANDOM -> "Random"
    }
}

@Composable
private fun StudyHideStarIcon(active: Boolean, modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Default.Star,
        contentDescription = if (active) "Starred cards visible" else "Starred cards hidden",
        modifier = modifier.size(24.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (active) 0.96f else 0.34f)
    )
}

@Composable
private fun StudyHideDoneIcon(active: Boolean, modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Default.DoneAll,
        contentDescription = if (active) "Done cards visible" else "Done cards hidden",
        modifier = modifier.size(24.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (active) 0.96f else 0.34f)
    )
}

@Composable
private fun CatalogIslandMapScreen(
    lessons: List<Lesson>,
    searchQuery: String,
    sortMode: LessonSortMode,
    sortDescending: Boolean,
    scrollState: ScrollState,
    onSearchQueryChange: (String) -> Unit,
    onSortModeChange: (LessonSortMode) -> Unit,
    onSortDirectionChange: (Boolean) -> Unit,
    onOpenLesson: (Lesson) -> Unit,
    onBackToList: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchExpanded by remember { mutableStateOf(searchQuery.isNotBlank()) }
    val transition = rememberInfiniteTransition(label = "catalogIslandMap")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lessonIslandPulse"
    )
    Column(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(searchExpanded, searchQuery) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Final)
                        if (event.changes.any { it.changedToDown() } && searchExpanded && searchQuery.isBlank()) {
                            searchExpanded = false
                        }
                    }
                }
            }
            .verticalScroll(scrollState)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onBackToList,
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("List")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Lesson map",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Tap an island to open its card path",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        LessonCatalogFilterRow(
            searchQuery = searchQuery,
            sortMode = sortMode,
            sortDescending = sortDescending,
            searchExpanded = searchExpanded,
            onSearchQueryChange = onSearchQueryChange,
            onSearchExpandedChange = { searchExpanded = it },
            onSortModeChange = onSortModeChange,
            onSortDirectionChange = onSortDirectionChange
        )
        Spacer(Modifier.height(10.dp))
        if (lessons.isEmpty()) {
            EmptyState(if (searchQuery.isBlank()) "No lessons yet. Create or import a lesson first." else "No lessons match this filter.")
        } else {
            lessons.forEachIndexed { index, lesson ->
                if (index > 0) {
                    IslandConnector(
                        reverse = index % 2 == 0,
                        pulse = pulse,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    )
                }
                LessonIslandRow(
                    lesson = lesson,
                    index = index,
                    alignEnd = index % 2 == 1,
                    pulse = pulse,
                    onClick = { onOpenLesson(lesson) }
                )
            }
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun LessonIslandRow(
    lesson: Lesson,
    index: Int,
    alignEnd: Boolean,
    pulse: Float,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp),
        horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val completed = lesson.timesCompleted > 0
        val scale = 1f + sin(((pulse + index) * 1.7f).toDouble()).toFloat().coerceAtLeast(0f) * 0.035f
        val islandColor = when {
            lesson.hidden -> Color(0xFFF1EFEA)
            completed -> Color(0xFFE4F6E8)
            lesson.cards.isEmpty() -> Color(0xFFEDEDED)
            else -> Color(0xFFFFE4EC)
        }
        Surface(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .size(width = 226.dp, height = 78.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationY = -3f * pulse
                }
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(34.dp),
            color = islandColor,
            tonalElevation = 4.dp,
            shadowElevation = 6.dp,
            border = BorderStroke(
                width = 1.dp,
                color = if (completed) BrandSaladColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.36f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.84f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = (index + 1).toString(),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF25382F)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = lesson.title.ifBlank { "Lesson ${index + 1}" },
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = buildList {
                            add("${lesson.cards.size} cards")
                            val pair = lesson.languagePairLabel().takeUnless { it == "XX" }.orEmpty()
                            if (pair.isNotBlank()) add(pair)
                            if (lesson.hidden) add("hidden")
                        }.joinToString(" | "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    lesson.updatedDisplayText().takeIf { it.isNotBlank() }?.let { modifiedAt ->
                        Text(
                            text = "Modified: $modifiedAt",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StudyIslandMapScreen(
    state: StudyUiState,
    onOpenCard: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "studyIslandMap")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "islandPulse"
    )
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(top = 10.dp, bottom = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = state.selectedLesson?.title?.ifBlank { "Lesson map" } ?: "Lesson map",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        state.currentPortion.forEachIndexed { index, card ->
            if (index > 0) {
                IslandConnector(
                    reverse = index % 2 == 0,
                    pulse = pulse,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                )
            }
            IslandRow(
                card = card,
                index = index,
                current = index == state.currentIndex,
                completed = card.id in state.completedCardIds || card.starCount() == 3,
                alignEnd = index % 2 == 1,
                pulse = pulse,
                onClick = { onOpenCard(card.id) }
            )
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun IslandConnector(reverse: Boolean, pulse: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val path = Path()
        val startX = if (reverse) size.width * 0.68f else size.width * 0.32f
        val endX = if (reverse) size.width * 0.32f else size.width * 0.68f
        path.moveTo(startX, 0f)
        path.cubicTo(
            size.width * 0.50f,
            size.height * (0.18f + pulse * 0.08f),
            size.width * 0.50f,
            size.height * (0.82f - pulse * 0.08f),
            endX,
            size.height
        )
        drawPath(
            path = path,
            color = BrandSaladColor.copy(alpha = 0.36f),
            style = Stroke(width = 8f)
        )
        drawPath(
            path = path,
            color = Color.White.copy(alpha = 0.72f),
            style = Stroke(width = 3f)
        )
    }
}

@Composable
private fun IslandRow(
    card: Flashcard,
    index: Int,
    current: Boolean,
    completed: Boolean,
    alignEnd: Boolean,
    pulse: Float,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(74.dp),
        horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val scale = if (current) 1f + pulse * 0.08f else 1f
        val islandColor = when {
            current -> Color(0xFFFFEBCB)
            completed -> Color(0xFFE4F6E8)
            else -> Color(0xFFFFE4EC)
        }
        Surface(
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .size(width = 132.dp, height = 58.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationY = if (current) -4f * pulse else 0f
                }
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(30.dp),
            color = islandColor,
            tonalElevation = if (current) 5.dp else 2.dp,
            shadowElevation = if (current) 8.dp else 3.dp,
            border = BorderStroke(
                width = if (current) 2.dp else 1.dp,
                color = if (current) BrandSaladColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.36f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier.size(34.dp),
                    shape = RoundedCornerShape(17.dp),
                    color = if (completed) BrandSaladColor.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.82f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = (index + 1).toString(),
                            fontWeight = FontWeight.Bold,
                            color = if (completed) Color(0xFF234231) else Color(0xFF111111)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = card.kindCode(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = card.nativeText().ifBlank { card.correctText() },
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = if (current) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun CardNavigation(
    position: String,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onPreviousCard: () -> Unit,
    onNextCard: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onPreviousCard,
            enabled = canGoBack,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(18.dp)
        ) { Text("Back") }
        Text(
            text = position,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(
            onClick = onNextCard,
            enabled = canGoForward,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(18.dp)
        ) { Text("Next") }
    }
}
@Composable
private fun CountersRow(
    portionSize: Int,
    remainingCount: Int,
    completedCount: Int,
    onPortionClick: () -> Unit,
    onRemainingClick: () -> Unit,
    onDoneClick: () -> Unit
) {
    val ui = rememberUiText()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        CounterText("${ui.inPortion}: $portionSize", modifier = Modifier.clickable(enabled = portionSize > 0) { onPortionClick() })
        CounterText(
            "${ui.left}: $remainingCount",
            modifier = Modifier.clickable(enabled = portionSize > 0) { onRemainingClick() }
        )
        CounterText("${ui.done}: $completedCount", modifier = Modifier.clickable(enabled = portionSize > 0) { onDoneClick() })
    }
}

@Composable
private fun CounterText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private fun DisplayTextSize.cardMainTextSize() = when (this) {
    DisplayTextSize.VERY_SMALL -> 22.sp
    DisplayTextSize.SMALL -> 27.sp
    DisplayTextSize.MEDIUM -> 32.sp
    DisplayTextSize.LARGE -> 38.sp
}

private fun DisplayTextSize.studyCardHeight() = when (this) {
    DisplayTextSize.VERY_SMALL -> 288.dp
    DisplayTextSize.SMALL -> 350.dp
    DisplayTextSize.MEDIUM -> 382.dp
    DisplayTextSize.LARGE -> 420.dp
}

private fun ControlSize.cardIconButtonSize() = when (this) {
    ControlSize.VERY_SMALL -> 34.dp
    ControlSize.SMALL -> 38.dp
    ControlSize.MEDIUM -> 44.dp
}
private fun ControlSize.answerIconButtonSize() = when (this) {
    ControlSize.VERY_SMALL -> 40.dp
    ControlSize.SMALL -> 44.dp
    ControlSize.MEDIUM -> 48.dp
}
private fun ControlSize.micButtonSize() = when (this) {
    ControlSize.VERY_SMALL -> 52.dp
    ControlSize.SMALL -> 58.dp
    ControlSize.MEDIUM -> 66.dp
}
private fun ControlSize.micIconSize() = when (this) {
    ControlSize.VERY_SMALL -> 24.dp
    ControlSize.SMALL -> 28.dp
    ControlSize.MEDIUM -> 32.dp
}
private fun ControlSize.answerRowHeight() = when (this) {
    ControlSize.VERY_SMALL -> 56.dp
    ControlSize.SMALL -> 62.dp
    ControlSize.MEDIUM -> 70.dp
}
private fun ControlSize.okButtonSize() = when (this) {
    ControlSize.VERY_SMALL -> 52.dp
    ControlSize.SMALL -> 58.dp
    ControlSize.MEDIUM -> 66.dp
}
@Composable
private fun StudyCard(
    card: Flashcard?,
    lesson: Lesson?,
    isBackVisible: Boolean,
    onClick: () -> Unit,
    onToggleStar: (Int, Int) -> Unit,
    onQuickEditCard: () -> Unit,
    onEditCard: () -> Unit,
    quickEditMode: Boolean,
    displayTextSize: DisplayTextSize,
    controlSize: ControlSize,
    isCompleted: Boolean,
    positionLabel: String,
    showOriginalText: Boolean,
    cardAudioStatus: CardAudioStatus,
    cardSideAudioCached: Boolean,
    statusBlinkOn: Boolean,
    onRefreshOnlineState: () -> Unit,
    onSwipePrevious: () -> Unit,
    onSwipeNext: () -> Unit,
    showCardLog: Boolean,
    onShareCard: (Flashcard) -> Unit,
    onShareCardSideAudio: (Flashcard) -> Unit,
    onPlayOriginalVoice: (Flashcard) -> Unit,
    onDeleteCard: () -> Unit,
    onCopyCard: () -> Unit,
    onMoveCard: () -> Unit,
    onToggleFeaturedCard: () -> Unit,
    onToggleOriginalCardText: (Int) -> Unit,
    onGenerateTrainCards: (Flashcard) -> Unit,
    modifier: Modifier = Modifier
) {
    var showRule by remember { mutableStateOf(false) }
    var showMistakes by remember { mutableStateOf(false) }
    var showCardMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var swipeDistance by remember(card?.id) { mutableStateOf(0f) }
    val rotation by animateFloatAsState(
        targetValue = if (isBackVisible) 180f else 0f,
        animationSpec = tween(durationMillis = 420),
        label = "cardFlip"
    )
    val showingFront = rotation <= 90f
    val frontColor = card?.let { cardFrontColor(it) } ?: Color(0xFFFFE4EC)
    val backColor = Color(0xFFE8F5E9)

    if (showRule && card != null) {
        CardTextDialog(
            title = "Info / rule",
            text = buildRuleInfo(card),
            onDismiss = { showRule = false }
        )
    }
    if (showMistakes && card != null) {
        CardTextDialog(
            title = "Log / progress",
            text = buildMistakesAndLog(card),
            onDismiss = { showMistakes = false }
        )
    }
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete card?") },
            text = { Text("This card will be removed from the lesson.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDeleteCard()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(displayTextSize.studyCardHeight())
                .pointerInput(card?.id) {
                    detectHorizontalDragGestures(
                        onDragStart = { swipeDistance = 0f },
                        onDragCancel = { swipeDistance = 0f },
                        onDragEnd = {
                            when {
                                swipeDistance > 90f -> onSwipePrevious()
                                swipeDistance < -90f -> onSwipeNext()
                            }
                            swipeDistance = 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            swipeDistance += dragAmount
                            if (abs(swipeDistance) > 24f) {
                                change.consume()
                            }
                        }
                    )
                }
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 12f * density
                }
                .shadow(
                    elevation = if (isCompleted) 28.dp else 0.dp,
                    shape = RoundedCornerShape(18.dp),
                    ambientColor = BrandSaladColor.copy(alpha = 0.95f),
                    spotColor = BrandSaladColor.copy(alpha = 0.95f)
                )
                .clip(RoundedCornerShape(18.dp))
                .pointerInput(card?.id, onClick) {
                    detectTapGestures(
                        onTap = { onClick() },
                        onLongPress = {
                            val currentCard = card
                            if (currentCard?.kindCode() == "MK") {
                                onGenerateTrainCards(currentCard)
                            }
                        }
                    )
                },
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (showingFront) frontColor else backColor
            ),
            border = when {
                isCompleted -> BorderStroke(4.dp, CompletedFrameColor)
                card?.featured == true -> BorderStroke(3.dp, BrandSaladColor)
                else -> null
            },
            elevation = CardDefaults.cardElevation(defaultElevation = if (isCompleted) 9.dp else 3.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (controlSize == ControlSize.SMALL) 14.dp else 16.dp)
                    .graphicsLayer {
                        rotationY = if (rotation > 90f) 180f else 0f
                    }
            ) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .clickable(onClick = onRefreshOnlineState),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = positionLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f)
                    )
                    Spacer(Modifier.width(6.dp))
                    CardAudioStatusDot(
                        status = cardAudioStatus,
                        cached = cardSideAudioCached,
                        blinkOn = statusBlinkOn
                    )
                }

                Box(
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (card?.originalText()?.isNotBlank() == true && !isBackVisible) {
                            CircleTextButton(
                                text = card.originalToggleLanguageCode(
                                    showOriginalText = showOriginalText,
                                    lesson = lesson
                                ),
                                onClick = { onToggleOriginalCardText(card.id) }
                            )
                        }
                        IconButton(
                            onClick = { showRule = true },
                            enabled = card != null,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color.White.copy(alpha = 0.92f))
                        ) {
                            Icon(Icons.Default.Info, contentDescription = "Info / rule", tint = Color(0xFF111111))
                        }
                        IconButton(
                            onClick = { if (card != null) onQuickEditCard() },
                            enabled = card != null,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color.White.copy(alpha = 0.92f))
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit card", tint = Color(0xFF111111))
                        }
                        IconButton(
                            onClick = { showCardMenu = true },
                            enabled = card != null,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color.White.copy(alpha = 0.92f))
                        ) {
                            Text(
                                text = "🐱",
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = showCardMenu,
                        onDismissRequest = { showCardMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Full edit") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                showCardMenu = false
                                if (card != null) onEditCard()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Share") },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                            onClick = {
                                showCardMenu = false
                                card?.let(onShareCard)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Copy to lesson end") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                            onClick = {
                                showCardMenu = false
                                onCopyCard()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Move to lesson") },
                            leadingIcon = { Icon(Icons.Default.UploadFile, contentDescription = null) },
                            onClick = {
                                showCardMenu = false
                                onMoveCard()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (card?.featured == true) "Remove featured" else "Mark featured") },
                            leadingIcon = {
                                Icon(
                                    if (card?.featured == true) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showCardMenu = false
                                onToggleFeaturedCard()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Cached audio") },
                            leadingIcon = {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.FileDownload,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Icon(
                                        Icons.Default.MusicNote,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(12.dp)
                                            .align(Alignment.BottomEnd)
                                    )
                                }
                            },
                            onClick = {
                                showCardMenu = false
                                card?.let(onShareCardSideAudio)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Original voice") },
                            leadingIcon = { Icon(Icons.Default.Mic, contentDescription = null) },
                            onClick = {
                                showCardMenu = false
                                card?.let(onPlayOriginalVoice)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Log / progress") },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                            onClick = {
                                showCardMenu = false
                                showMistakes = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            onClick = {
                                showCardMenu = false
                                showDeleteConfirm = true
                            }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(top = 48.dp, bottom = 42.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (showingFront) {
                            Text(
                                text = card?.frontDisplayLabel(showOriginalText).orEmpty(),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = card?.let { flashcard ->
                                    if (flashcard.isLearningKind()) {
                                        flashcard.frontDisplayText(showOriginalText)
                                    } else {
                                        flashcard.mistakeText().ifBlank { "No mistake recorded yet" }
                                    }
                                }.orEmpty(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontSize = displayTextSize.cardMainTextSize(),
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            Text(
                                text = card?.backLabel().orEmpty(),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF315E3B)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = card?.correctText().orEmpty(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontSize = displayTextSize.cardMainTextSize(),
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                color = Color(0xFF183D22)
                            )
                        }
                    }
                }

                card?.let { flashcard ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .background(
                                color = Color.White.copy(alpha = 0.78f),
                                shape = RoundedCornerShape(22.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        StarRating(
                            stars = flashcard.stars,
                            onToggleStar = { starIndex -> onToggleStar(flashcard.id, starIndex) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CardAudioStatusDot(status: CardAudioStatus, cached: Boolean, blinkOn: Boolean) {
    val color = when (status) {
        CardAudioStatus.ONLINE -> OnlineStatusColor
        CardAudioStatus.OFFLINE -> BrandRedColor
    }
    val shouldBlink = status == CardAudioStatus.ONLINE && blinkOn
    val alpha by animateFloatAsState(
        targetValue = if (shouldBlink) 0.28f else 1f,
        animationSpec = tween(durationMillis = 220),
        label = "cardAudioStatusBlink"
    )
    Box(
        modifier = Modifier
            .size(9.dp)
            .graphicsLayer { this.alpha = alpha }
            .clip(RoundedCornerShape(5.dp))
            .background(color)
            .then(
                if (cached) {
                    Modifier.border(1.dp, Color.Black, RoundedCornerShape(5.dp))
                } else {
                    Modifier
                }
            )
    )
}

@Composable
private fun CircleTextButton(text: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.92f))
    ) {
        Text(
            text = text,
            fontSize = if (text.length > 2) 10.sp else 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            color = Color(0xFF111111)
        )
    }
}

@Composable
private fun CardTextDialog(
    title: String,
    text: String,
    onDismiss: () -> Unit,
    onResetProgress: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onResetProgress != null) {
                    TextButton(onClick = onResetProgress) { Text("Reset progress") }
                }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
        title = { Text(title) },
        text = {
            Text(
                text = text,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (text.length > 420) 300.dp else 160.dp)
                    .verticalScroll(rememberScrollState())
            )
        }
    )
}

@Composable
private fun MoveStudyCardDialog(
    currentLesson: Lesson?,
    currentCard: Flashcard?,
    lessons: List<Lesson>,
    onDismiss: () -> Unit,
    onMove: (String?) -> Unit
) {
    val sourceCode = currentCard?.sourceLanguage?.toCardLanguageCode()
        ?: currentLesson?.sourceLanguage.toCardLanguageCode()
    val targetCode = currentCard?.targetLanguage?.toCardLanguageCode()
        ?: currentLesson?.targetLanguage.toCardLanguageCode()
    val candidates = remember(currentLesson?.id, currentCard?.id, lessons) {
        lessons
            .filter { lesson -> lesson.id != currentLesson?.id }
            .sortedWith(
                compareByDescending<Lesson> { lesson ->
                    lesson.sourceLanguage.toCardLanguageCode().equals(sourceCode, ignoreCase = true) &&
                        lesson.targetLanguage.toCardLanguageCode().equals(targetCode, ignoreCase = true)
                }.thenBy { it.title.lowercase(Locale.ROOT) }
            )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move card") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Choose lesson for $sourceCode -> $targetCode.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                candidates.forEach { lesson ->
                    OutlinedButton(
                        onClick = { onMove(lesson.id) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "${lesson.sourceLanguage.toCardLanguageCode()} -> ${lesson.targetLanguage.toCardLanguageCode()}  ${lesson.title}",
                            maxLines = 2
                        )
                    }
                }
                if (candidates.isEmpty()) {
                    Text("No other lessons yet.")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onMove(null) }, shape = RoundedCornerShape(14.dp)) {
                Text("Create new and move")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun cardFrontColor(card: Flashcard): Color {
    return when (card.starCount()) {
        0 -> Color(0xFFFFE4EC)
        1 -> Color(0xFFFFEBCB)
        2 -> Color(0xFFDFF0FF)
        else -> Color(0xFFE4F6E8)
    }
}

private fun Lesson.cardKindSummary(ui: UiText): String {
    val kinds = cards.map { it.kindCode() }.distinct()
    return when {
        kinds.isEmpty() -> ui.kindText("MK")
        kinds.size == 1 -> ui.kindText(kinds.first())
        else -> ui.mixedKindText()
    }
}

private fun UiText.languageKey(): String = "en"

private fun UiText.kindText(kindCode: String): String = when (kindCode) {
    "LN" -> "Lesson"
    "TR" -> "Train"
    else -> "Mistakes"
}

private fun UiText.mixedKindText(): String = "Mixed"

private fun UiText.questionsText(count: Int): String = "$count questions"

private fun UiText.doneText(done: Int, total: Int): String = "$done of $total done"

private fun UiText.completedText(count: Int): String = "Completed $count times"

private fun Flashcard?.shouldShowOriginalText(
    isBackVisible: Boolean,
    translatedOriginalCardIds: Set<Int>
): Boolean {
    val card = this ?: return false
    return !isBackVisible && card.originalText().isNotBlank() && card.id !in translatedOriginalCardIds
}

private fun Flashcard.displayedCardText(isBackVisible: Boolean, showOriginalText: Boolean = false): String {
    return if (isBackVisible) {
        correctText()
    } else if (showOriginalText && originalText().isNotBlank()) {
        originalText()
    } else if (isLearningKind()) {
        frontDisplayText(showOriginalText)
    } else {
        mistakeText().ifBlank { nativeText().ifBlank { correctText() } }
    }
}

private fun Flashcard?.sideLanguageCode(
    isBackVisible: Boolean,
    lesson: Lesson? = null,
    showOriginalText: Boolean = false
): String {
    if (this != null && !isLearningKind()) return "Mistake"
    val language = if (isBackVisible) {
        this?.targetLanguage.asLessonLanguage()
            ?: lesson?.targetLanguage.asLessonLanguage()
            ?: this?.backLabel()
    } else if (showOriginalText && this?.originalText()?.isNotBlank() == true) {
        this.sourceLanguage.asLessonLanguage()
            ?: this.originalText().speechLanguageTagFromText()?.speechLanguageDisplayName()
            ?: "Mixed"
    } else {
        lesson?.sourceLanguage.asLessonLanguage()
            ?: this?.sourceLanguage.asLessonLanguage()
            ?: this?.frontLabel()
    }
    val code = language.toCardLanguageCode()
    return if (showOriginalText && this?.originalText()?.isNotBlank() == true) "$code Original" else code
}

private fun Flashcard.originalToggleLanguageCode(showOriginalText: Boolean, lesson: Lesson?): String {
    val language = if (showOriginalText) {
        lesson?.sourceLanguage.asLessonLanguage() ?: frontLabel()
    } else {
        sourceLanguage.asLessonLanguage()
            ?: originalText().speechLanguageTagFromText()?.speechLanguageDisplayName()
            ?: "Mixed"
    }
    return language.toCardLanguageCode()
}

private fun String?.toCardLanguageCode(): String {
    val value = this?.trim().orEmpty()
    if (value.isBlank() || value.equals("Mixed", ignoreCase = true)) return "MX"
    val code = dictionaryLanguageCode(value).trim().uppercase(Locale.ROOT)
    return if (code.isBlank() || code == "MI") "MX" else code
}

private fun Flashcard.expectedAnswerText(isBackVisible: Boolean): String {
    return if (isBackVisible) nativeText() else correctText()
}

private fun Flashcard.frontDisplayLabel(showOriginalText: Boolean = false): String {
    return if (showOriginalText && originalText().isNotBlank()) "Original" else frontLabel()
}

private fun Flashcard.frontDisplayText(showOriginalText: Boolean = false): String {
    return if (showOriginalText && originalText().isNotBlank()) originalText() else nativeText()
}

private fun Flashcard.hasPendingNativeTranslation(): Boolean {
    if (!isLearningKind() || correctText().isBlank()) return false
    val native = nativeText().trim()
    return native.isBlank() || native.isEmptyPlaceholder() || native.contains("translation pending", ignoreCase = true)
}

private fun String.isEmptyPlaceholder(): Boolean = trim().equals("Empty", ignoreCase = true)

private fun Flashcard.hasEmptySide(): Boolean = nativeText().isEmptyPlaceholder() || correctText().isEmptyPlaceholder()

private fun Flashcard.answerInputLabel(isBackVisible: Boolean, lesson: Lesson? = null): String {
    val language = if (isBackVisible) {
        sourceLanguage.asLessonLanguage() ?: lesson?.sourceLanguage.asLessonLanguage() ?: frontLabel()
    } else {
        targetLanguage.asLessonLanguage() ?: lesson?.targetLanguage.asLessonLanguage() ?: backLabel()
    }
    return language.ifBlank { if (isBackVisible) frontLabel() else backLabel() }
}

private fun Flashcard.visibleCardSideLanguage(
    isBackVisible: Boolean,
    lesson: Lesson? = null,
    showOriginalText: Boolean = false
): String {
    return if (isBackVisible) {
        targetLanguage.asLessonLanguage() ?: lesson?.targetLanguage.asLessonLanguage() ?: backLabel()
    } else if (showOriginalText && originalText().isNotBlank()) {
        sourceLanguage.asLessonLanguage()
            ?: originalText().speechLanguageTagFromText()?.speechLanguageDisplayName()
            ?: frontLabel()
    } else {
        sourceLanguage.asLessonLanguage() ?: lesson?.sourceLanguage.asLessonLanguage() ?: frontLabel()
    }.ifBlank { if (isBackVisible) backLabel() else frontLabel() }
}

private fun Flashcard.answerTargetLanguage(isBackVisible: Boolean, lesson: Lesson? = null): String {
    return if (isBackVisible) {
        sourceLanguage.asLessonLanguage() ?: lesson?.sourceLanguage.asLessonLanguage() ?: frontLabel()
    } else {
        targetLanguage.asLessonLanguage() ?: lesson?.targetLanguage.asLessonLanguage() ?: backLabel()
    }.ifBlank { if (isBackVisible) frontLabel() else backLabel() }
}

private fun Flashcard?.speechLanguageTag(interfaceLanguage: String, lesson: Lesson? = null): String {
    if (this == null) return interfaceLanguage.speechLanguageTagFromName()
        ?: Locale.getDefault().toLanguageTag()

    (targetLanguage.asLessonLanguage() ?: lesson?.targetLanguage.asLessonLanguage())
        ?.speechLanguageTagFromName()
        ?.let { return it }
    if (isLearningKind()) {
        backLabel().speechLanguageTagFromName()?.let { return it }
    }
    correctText().speechLanguageTagFromText()?.let { return it }
    (sourceLanguage.asLessonLanguage() ?: lesson?.sourceLanguage.asLessonLanguage())
        ?.speechLanguageTagFromName()
        ?.let { return it }
    return interfaceLanguage.speechLanguageTagFromName()
        ?: Locale.getDefault().toLanguageTag()
}

private fun Flashcard.speechLanguageTagForSide(
    isBackVisible: Boolean,
    interfaceLanguage: String,
    lesson: Lesson? = null,
    showOriginalText: Boolean = false
): String {
    val explicitLanguage = if (isBackVisible || hasPendingNativeTranslation()) {
        targetLanguage.asLessonLanguage() ?: lesson?.targetLanguage.asLessonLanguage() ?: backLabel()
    } else if (showOriginalText && originalText().isNotBlank()) {
        sourceLanguage.asLessonLanguage() ?: originalText().speechLanguageTagFromText()?.speechLanguageDisplayName().orEmpty()
    } else if (isLearningKind()) {
        lesson?.sourceLanguage.asLessonLanguage() ?: sourceLanguage.asLessonLanguage() ?: frontLabel()
    } else {
        targetLanguage.asLessonLanguage() ?: lesson?.targetLanguage.asLessonLanguage()
            ?: sourceLanguage.asLessonLanguage() ?: lesson?.sourceLanguage.asLessonLanguage().orEmpty()
    }
    explicitLanguage.speechLanguageTagFromName()?.let { return it }
    displayedCardText(isBackVisible, showOriginalText).speechLanguageTagFromText()?.let { return it }
    return interfaceLanguage.speechLanguageTagFromName()
        ?: Locale.getDefault().toLanguageTag()
}

private fun Flashcard?.voiceLanguageForDisplayedSide(
    isBackVisible: Boolean,
    interfaceLanguage: String,
    lesson: Lesson? = null
): Pair<String, String> {
    val card = this ?: return languageForVoice(interfaceLanguage, "", interfaceLanguage)
    val languageName = if (isBackVisible) {
        card.targetLanguage.asLessonLanguage() ?: lesson?.targetLanguage.asLessonLanguage() ?: card.backLabel()
    } else {
        card.sourceLanguage.asLessonLanguage() ?: lesson?.sourceLanguage.asLessonLanguage() ?: card.frontLabel()
    }
    return languageForVoice(languageName, card.displayedCardText(isBackVisible), interfaceLanguage)
}

private fun Flashcard?.voiceLanguageForCorrectSide(interfaceLanguage: String, lesson: Lesson? = null): Pair<String, String> {
    val card = this ?: return languageForVoice(interfaceLanguage, "", interfaceLanguage)
    val languageName = when {
        card.nativeText().isEmptyPlaceholder() -> card.sourceLanguage.asLessonLanguage()
            ?: lesson?.sourceLanguage.asLessonLanguage() ?: card.frontLabel()
        card.correctText().isEmptyPlaceholder() -> card.targetLanguage.asLessonLanguage()
            ?: lesson?.targetLanguage.asLessonLanguage() ?: card.backLabel()
        else -> card.targetLanguage.asLessonLanguage() ?: lesson?.targetLanguage.asLessonLanguage() ?: card.backLabel()
    }
    val sampleText = when {
        card.nativeText().isEmptyPlaceholder() -> card.nativeText()
        card.correctText().isEmptyPlaceholder() -> card.correctText()
        else -> card.correctText()
    }
    return languageForVoice(languageName, sampleText, interfaceLanguage)
}

private fun String?.asLessonLanguage(): String? {
    val cleaned = orEmpty().trim()
    return cleaned.takeIf { it.isNotBlank() && !it.equals("Mixed", ignoreCase = true) }
}

private fun languageForVoice(languageName: String, sampleText: String, interfaceLanguage: String): Pair<String, String> {
    val tag = languageName.speechLanguageTagFromName()
        ?: sampleText.speechLanguageTagFromText()
        ?: interfaceLanguage.speechLanguageTagFromName()
        ?: Locale.getDefault().toLanguageTag()
    val label = languageName.trim().ifBlank { tag.speechLanguageDisplayName() }
    return tag to label
}

private fun String.speechLanguageDisplayName(): String {
    return when (substringBefore('-').lowercase(Locale.ROOT)) {
        "en" -> "English"
        "de" -> "German"
        "be" -> "Belarusian"
        "es" -> "Spanish"
        "uk" -> "Ukrainian"
        "ru" -> "Russian"
        "pl" -> "Polish"
        "lv" -> "Latvian"
        "lt" -> "Lithuanian"
        "pt" -> "Portuguese"
        else -> "System language"
    }
}

private fun String.isBelarusianSpeechTag(): Boolean {
    val normalized = trim().lowercase(Locale.ROOT)
    return normalized == "be" || normalized == "by" || normalized.startsWith("be-")
}

private fun String.speechLanguageTagFromName(): String? {
    val normalized = trim().lowercase(Locale.ROOT)
    return when {
        normalized in listOf("en", "eng", "english", "angielski", "\u0430\u043d\u0433\u043b\u0438\u0439\u0441\u043a\u0438\u0439", "\u0430\u043d\u0433\u043b\u0456\u0439\u0441\u043a\u0430\u044f") -> "en-US"
        normalized in listOf("de", "deu", "ger", "german", "deutsch", "niemiecki", "\u043d\u0435\u043c\u0435\u0446\u043a\u0438\u0439", "\u043d\u044f\u043c\u0435\u0446\u043a\u0430\u044f") -> "de-DE"
        normalized in listOf("be", "by", "belarusian", "belaruska", "\u0431\u0435\u043b\u0430\u0440\u0443\u0441\u043a\u0430\u044f", "\u0431\u0435\u043b\u0430\u0440\u0443\u0441\u043a\u0456", "\u0431\u0435\u043b\u043e\u0440\u0443\u0441\u0441\u043a\u0438\u0439") -> "be-BY"
        normalized in listOf("es", "spa", "spanish", "espanol", "espa\u00f1ol", "\u0438\u0441\u043f\u0430\u043d\u0441\u043a\u0438\u0439", "\u0456\u0441\u043f\u0430\u043d\u0441\u043a\u0430\u044f") -> "es-ES"
        normalized in listOf("uk", "ua", "ukrainian", "\u0443\u043a\u0440\u0430\u0438\u043d\u0441\u043a\u0438\u0439", "\u0443\u043a\u0440\u0430\u0457\u043d\u0441\u044c\u043a\u0430", "\u0443\u043a\u0440\u0430\u0456\u043d\u0441\u043a\u0430\u044f") -> "uk-UA"
        normalized in listOf("ru", "rus", "russian", "rosyjski", "\u0440\u0443\u0441\u0441\u043a\u0438\u0439", "\u0440\u0443\u0441\u0441\u043a\u0430\u044f") -> "ru-RU"
        normalized in listOf("pl", "pol", "polish", "polski", "\u043f\u043e\u043b\u044c\u0441\u043a\u0438\u0439", "\u043f\u043e\u043b\u044c\u0441\u043a\u0430\u044f") -> "pl-PL"
        normalized in listOf("lv", "lav", "latvian", "latviesu", "latvie\u0161u") -> "lv-LV"
        normalized in listOf("lt", "lit", "lithuanian", "lietuviu", "lietuvi\u0173") -> "lt-LT"
        normalized in listOf("pt", "por", "portuguese", "portugues", "portugu\u00eas") -> "pt-PT"
        else -> null
    }
}

private fun String.speechLanguageTagFromText(): String? {
    val text = lowercase(Locale.ROOT)
    return when {
        text.any { it in "\u045e\u0456" } -> "be-BY"
        text.any { it in "\u0456\u0457\u0454\u0491" } -> "uk-UA"
        text.any { it in "\u0105\u0107\u0119\u0142\u0144\u00f3\u015b\u017a\u017c" } -> "pl-PL"
        text.any { it in "\u00e4\u00f6\u00fc\u00df" } -> "de-DE"
        text.any { it in "\u00e1\u00e9\u00ed\u00f1\u00f3\u00fa\u00fc\u00bf\u00a1" } -> "es-ES"
        text.any { it in "\u0101\u010d\u0113\u0123\u012b\u0137\u013c\u0146\u0161\u016b\u017e" } -> "lv-LV"
        text.any { it in "\u0105\u010d\u0117\u0119\u012f\u0161\u0173\u016b\u017e" } -> "lt-LT"
        text.any { it in "\u00e3\u00f5\u00e7" } -> "pt-PT"
        text.any { it in '\u0430'..'\u044f' || it == '\u0451' } -> "ru-RU"
        text.any { it in 'a'..'z' } -> "en-US"
        else -> null
    }
}
private fun normalizeAnswerText(value: String): String {
    return value
        .replace('\u00A0', ' ')
        .replace('\u2007', ' ')
        .replace('\u202F', ' ')
        .replace(Regex("[\\p{P}\\s]+"), "")
        .lowercase(Locale.getDefault())
}

private fun buildRuleInfo(card: Flashcard): String {
    val rule = card.hintText().ifBlank { "No rule or hint for this card." }
    val original = card.originalText().trim().takeIf { it.isNotBlank() }
        ?.let { "\n\nOriginal:\n$it" }
        .orEmpty()
    return "Native value:\n${card.nativeText().ifBlank { "Not specified" }}$original\n\nMake it right:\n${card.correctText().ifBlank { "Not specified" }}\n\nRule:\n$rule"
}

private fun buildMistakesAndLog(card: Flashcard): String {
    val records = card.mistakeRecords()
    val mistakes = if (records.isEmpty()) {
        "No wrong answers recorded yet."
    } else {
        records.joinToString("\n\n") { record ->
            "${record.date.ifBlank { "No date" }}\n${record.answer.ifBlank { "(empty answer)" }}"
        }
    }
    val created = "Created: ${card.madeAtText()}\nWhere: ${card.whereText()}"
    val logText = if (card.log.isEmpty()) {
        "No work log yet."
    } else {
        card.log.joinToString("\n")
    }
    return "$created\n\nWrong answers:\n$mistakes\n\nWork log (${card.log.size}):\n$logText"
}

@Composable
private fun VoiceInputDialog(
    status: String,
    languageLabel: String,
    elapsedMs: Long,
    isRecording: Boolean,
    signalLevel: Float,
    onHoldStart: () -> Unit,
    onHoldEnd: () -> Unit,
    onTapStop: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {},
        title = { Text(if (isRecording) "Recording voice" else "Recognizing") },
        text = {
            Column(
                modifier = Modifier.pointerInput(isRecording) {
                    detectTapGestures(
                        onPress = {
                            if (!isRecording) return@detectTapGestures
                            val startedAt = System.currentTimeMillis()
                            onHoldStart()
                            try {
                                tryAwaitRelease()
                            } finally {
                                val duration = System.currentTimeMillis() - startedAt
                                onHoldEnd()
                                if (duration < 260L) onTapStop()
                            }
                        }
                    )
                },
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = formatVoiceElapsed(elapsedMs),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Expected speech: $languageLabel",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                VoiceWaveform(
                    active = isRecording && signalLevel > 0.03f,
                    elapsedMs = elapsedMs,
                    signalLevel = signalLevel,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                )
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = if (isRecording) "Tap to stop. Hold this window to keep recording through silence." else "Please wait...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@Composable
private fun TrainCardOptionsDialog(
    sourceCard: Flashcard,
    onDismiss: () -> Unit,
    onGenerate: (List<String>) -> Unit
) {
    var selectedOptions by remember(sourceCard.id) { mutableStateOf(TrainCardGenerationOptions.toSet()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Train cards") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = sourceCard.mistakeText().ifBlank { sourceCard.nativeText() }.take(180),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                TrainCardGenerationOptions.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedOptions = if (option in selectedOptions) {
                                    selectedOptions - option
                                } else {
                                    selectedOptions + option
                                }
                            }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = option in selectedOptions,
                            onCheckedChange = { checked ->
                                selectedOptions = if (checked) selectedOptions + option else selectedOptions - option
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(option, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onGenerate(TrainCardGenerationOptions.filter { it in selectedOptions }) },
                enabled = selectedOptions.isNotEmpty()
            ) { Text("Generate") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun SharedPostImportDialog(
    sharedText: String,
    sourceLanguage: String,
    targetLanguage: String,
    onDismiss: () -> Unit,
    onCreate: () -> Unit
) {
    val preview = remember(sharedText) { sharedText.trimToWordLimit(500) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import shared post") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${sourceLanguage.shortCodeForUi()} -> ${targetLanguage.shortCodeForUi()}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Up to 500 words will be sent to OpenAI. MurrLex will create one Basic -> Target retelling card for each meaningful sentence.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onCreate) { Text("Create cards") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun UrlImportDialog(
    sourceLanguage: String,
    targetLanguage: String,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardUrl = remember { context.clipboardUrlOrBlank() }
    var url by remember { mutableStateOf(clipboardUrl) }
    val cleanUrl = url.trim()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import URL") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${sourceLanguage.shortCodeForUi()} -> ${targetLanguage.shortCodeForUi()}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Paste a Telegram, YouTube, Instagram, Viber, or web link. MurrLex will try to read up to 2000 words and create Basic -> Target thesis cards.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { url = "" },
                        enabled = url.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear")
                    }
                    OutlinedButton(
                        onClick = { url = context.clipboardUrlOrBlank() },
                        enabled = context.clipboardUrlOrBlank().isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Paste")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(cleanUrl) },
                enabled = cleanUrl.isNotBlank()
            ) { Text("Create lesson") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun VoiceWaveform(
    active: Boolean,
    elapsedMs: Long,
    signalLevel: Float,
    modifier: Modifier = Modifier
) {
    val phase = (elapsedMs / 90L).toFloat()
    val color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    val level = signalLevel.coerceIn(0f, 1f)
    Canvas(modifier = modifier) {
        val bars = 28
        val gap = size.width / (bars * 1.75f)
        val barWidth = gap * 0.72f
        val centerY = size.height / 2f
        for (index in 0 until bars) {
            val wave = if (active) abs(sin((index * 0.72f + phase).toDouble())).toFloat() else 0f
            val height = (size.height * (0.08f + level * (0.22f + wave * 0.7f))).coerceAtLeast(4f)
            val x = index * gap * 1.75f
            drawRoundRect(
                color = color.copy(alpha = if (active) 0.5f + wave * 0.5f else 0.28f),
                topLeft = Offset(x, centerY - height / 2f),
                size = androidx.compose.ui.geometry.Size(barWidth, height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

private fun formatVoiceElapsed(elapsedMs: Long): String {
    val totalSeconds = (elapsedMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(Locale.ROOT, minutes, seconds)
}

private fun Context.clipboardUrlOrBlank(): String {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return ""
    val text = clipboard.primaryClip
        ?.takeIf { it.itemCount > 0 }
        ?.getItemAt(0)
        ?.coerceToText(this)
        ?.toString()
        ?.trim()
        .orEmpty()
    return text.takeIf { it.isLikelyUrlText() }.orEmpty()
}

private fun String.isLikelyUrlText(): Boolean {
    val clean = trim()
    if (clean.length < 4 || clean.any { it.isWhitespace() }) return false
    return clean.startsWith("http://", ignoreCase = true) ||
        clean.startsWith("https://", ignoreCase = true) ||
        clean.startsWith("t.me/", ignoreCase = true) ||
        Regex("""^[A-Za-z0-9.-]+\.[A-Za-z]{2,}(/.*)?$""").matches(clean)
}

private fun buildAnswerFeedback(answer: String, expected: String): AnnotatedString {
    val typed = answer.trim()
    val target = expected.trim()
    return buildAnnotatedString {
        val maxLength = maxOf(typed.length, target.length)
        for (index in 0 until maxLength) {
            val typedChar = typed.getOrNull(index)
            val targetChar = target.getOrNull(index)
            when {
                targetChar == null && typedChar != null -> {
                    pushStyle(SpanStyle(color = BrandRedColor, fontWeight = FontWeight.Bold))
                    append(typedChar)
                    pop()
                }
                typedChar == null && targetChar != null -> {
                    if (targetChar.isLetterOrDigit()) {
                        append("*")
                    } else {
                        append(targetChar)
                    }
                }
                typedChar != null && targetChar != null &&
                    typedChar.lowercaseChar() == targetChar.lowercaseChar() -> append(typedChar)
                typedChar != null -> {
                    pushStyle(SpanStyle(color = BrandRedColor, fontWeight = FontWeight.Bold))
                    append(typedChar)
                    pop()
                }
            }
        }
    }
}

@Composable
private fun AnswerBar(
    answer: String,
    answerLabel: String,
    currentCard: Flashcard?,
    isBackVisible: Boolean,
    showOriginalText: Boolean,
    isCurrentCardDone: Boolean,
    answerFeedbackVisible: Boolean,
    isVoiceRecording: Boolean,
    quickEditMode: Boolean,
    translateTargetCode: String,
    onAnswerChange: (String) -> Unit,
    onCheck: () -> Unit,
    onOk: () -> Unit,
    onSaveEmptySide: () -> Unit,
    onTranslateEmptySide: () -> Unit,
    onClearAnswer: () -> Unit,
    onCopy: (String) -> Unit,
    onVoiceToggle: () -> Unit,
    onSpeak: () -> Unit,
    onDismissAnswerFeedback: () -> Unit,
    controlSize: ControlSize = ControlSize.MEDIUM,
) {
    val isDisplayedEmptySide = currentCard
        ?.displayedCardText(isBackVisible, showOriginalText)
        ?.let { text -> text.isBlank() || text.isEmptyPlaceholder() } == true
val usesEditActionBar = quickEditMode || isDisplayedEmptySide
val typedAnswerCorrect = currentCard?.let { card ->
    answer.isNotBlank() && normalizeAnswerText(answer) == normalizeAnswerText(card.expectedAnswerText(isBackVisible))
} == true
    val doneLocked = currentCard != null && isCurrentCardDone && answer.none { it.isLetter() }
    val canSubmit = currentCard != null && ((!doneLocked && !usesEditActionBar) || answer.isBlank())
    Surface(
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = answerFeedbackVisible) { onDismissAnswerFeedback() }
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = answer,
                onValueChange = onAnswerChange,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp, max = 150.dp),
                label = { Text(answerLabel) },
                singleLine = false,
                minLines = 1,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (canSubmit) onOk() })
            )
            if (answerFeedbackVisible && currentCard != null) {
                Text(
                text = buildAnswerFeedback(answer, currentCard.expectedAnswerText(isBackVisible)),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(controlSize.answerRowHeight()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (usesEditActionBar) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        OutlinedButton(
                            onClick = onClearAnswer,
                            enabled = quickEditMode || answer.isNotBlank(),
                            shape = RoundedCornerShape(18.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Text("Cancel")
                        }
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        OutlinedButton(
                            onClick = onTranslateEmptySide,
                            enabled = currentCard != null,
                            shape = RoundedCornerShape(18.dp),
                            contentPadding = PaddingValues(horizontal = 0.dp),
                            modifier = Modifier.size(controlSize.answerIconButtonSize())
                        ) {
                            Text(translateTargetCode.ifBlank { "T" }, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Surface(
                            modifier = Modifier
                                .size(controlSize.micButtonSize())
                                .clickable { onVoiceToggle() },
                            shape = RoundedCornerShape(23.dp),
                            border = BorderStroke(
                                width = if (isVoiceRecording) 3.dp else 1.dp,
                                color = if (isVoiceRecording) BrandSaladColor else MaterialTheme.colorScheme.outline
                            ),
                            color = when {
                                isVoiceRecording -> BrandSaladColor.copy(alpha = 0.24f)
                                else -> MaterialTheme.colorScheme.surface
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = "Voice input",
                                    modifier = Modifier.size(36.dp),
                                    tint = if (isVoiceRecording) Color(0xFF234231) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Button(
                            onClick = onSaveEmptySide,
                            enabled = true,
                            shape = RoundedCornerShape(18.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE4F6E8),
                                contentColor = Color(0xFF234231),
                                disabledContainerColor = Color(0xFFC8CEC4),
                                disabledContentColor = Color(0xFF5F685D)
                            )
                        ) {
                            Text("Save")
                        }
                    }
                } else {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        AnswerIconButton(
                            icon = Icons.Default.ContentCopy,
                            contentDescription = "Copy visible card text",
                            enabled = currentCard != null,
                            onClick = { currentCard?.displayedCardText(isBackVisible, showOriginalText)?.let(onCopy) },
                            size = controlSize.answerIconButtonSize()
                        )
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        AnswerIconButton(
                            icon = Icons.Default.Refresh,
                            contentDescription = "Clear input",
                            enabled = answer.isNotBlank(),
                            onClick = onClearAnswer,
                            size = controlSize.answerIconButtonSize()
                        )
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Surface(
                            modifier = Modifier
                                .size(controlSize.micButtonSize())
                                .clickable { onVoiceToggle() },
                            shape = RoundedCornerShape(23.dp),
                            border = BorderStroke(
                                width = if (isVoiceRecording) 3.dp else 1.dp,
                                color = if (isVoiceRecording) BrandSaladColor else MaterialTheme.colorScheme.outline
                            ),
                            color = when {
                                isVoiceRecording -> BrandSaladColor.copy(alpha = 0.24f)
                                else -> MaterialTheme.colorScheme.surface
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = "Voice input",
                                    modifier = Modifier.size(36.dp),
                                    tint = if (isVoiceRecording) Color(0xFF234231) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        AnswerIconButton(
                            icon = Icons.Default.VolumeUp,
                            contentDescription = "Read aloud",
                            enabled = currentCard != null,
                            onClick = onSpeak,
                            size = controlSize.answerIconButtonSize()
                        )
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Button(
                            onClick = onOk,
                            modifier = Modifier
                                .size(controlSize.okButtonSize()),
                            shape = RoundedCornerShape(18.dp),
                            enabled = canSubmit,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE4F6E8),
                                contentColor = Color(0xFF234231),
                                disabledContainerColor = Color(0xFFC8CEC4),
                                disabledContentColor = Color(0xFF5F685D)
                            ),
                            contentPadding = PaddingValues(horizontal = 0.dp)
                        ) {
                            Text("OK")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnswerIconButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp = 48.dp
) {
    Surface(
        modifier = Modifier
            .size(size)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        color = if (enabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = contentDescription,
                tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline
            )
        }
    }
}
@Composable
private fun StudyEmptyState(message: String) {
    val ui = rememberUiText()
    val translatedMessage = when {
        message.startsWith("All done") -> ui.allDone
        else -> ui.nothingToShow
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = translatedMessage,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = ui.emptyHint,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FinishedScreen(onNewPortion: () -> Unit, onNextLesson: () -> Unit, onOpenCatalog: () -> Unit) {
    val ui = rememberUiText()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = ui.lessonCompleteTitle,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = ui.lessonCompleteBody,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onNewPortion,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Repeat")
                }
                Button(
                    onClick = onNextLesson,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("Next lesson")
                }
            }
            OutlinedButton(
                onClick = onOpenCatalog,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("Lessons")
            }
        }
    }
}

@Composable
private fun LessonEditorScreen(
    state: StudyUiState,
    onTitleChange: (String) -> Unit,
    onLessonInfoChange: (String) -> Unit,
    onLessonSourceLanguageChange: (String) -> Unit,
    onLessonTargetLanguageChange: (String) -> Unit,
    onCardDraftChange: (CardDraft) -> Unit,
    onAddCard: () -> Unit,
    onDeleteCard: (Int) -> Unit,
    onCopyCard: (Int) -> Unit,
    onMoveCard: (Int, Int) -> Unit,
    onEditCard: (Flashcard) -> Unit,
    onCancelCardEditing: () -> Unit,
    onToggleCardStar: (Int, Int) -> Unit,
    onSave: () -> Unit,
    onDeleteLesson: (String) -> Unit,
    onVoiceInputForCardField: (VoiceInputTarget) -> Unit,
    onDeleteCards: (Set<Int>) -> Unit,
    onCopyCardsToLesson: (Set<Int>, String?) -> Unit
) {
    val lesson = state.editorLesson ?: return
    val cardDraft = state.cardDraft
    var showDeleteLessonDialog by remember { mutableStateOf(false) }
    var pendingDeleteCardId by remember { mutableStateOf<Int?>(null) }
    var showCardDialog by remember { mutableStateOf(false) }
    var selectedCardIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var pendingCopyCardIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var showCopyCardsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.returnToStudyAfterEdit, cardDraft.editingCardId) {
        if (state.returnToStudyAfterEdit && cardDraft.editingCardId != null) {
            showCardDialog = true
        }
    }

    if (showCopyCardsDialog) {
        AlertDialog(
            onDismissRequest = { showCopyCardsDialog = false },
            title = { Text("Copy cards to...") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose an existing lesson or create a new one.")
                    OutlinedButton(
                        onClick = {
                            onCopyCardsToLesson(pendingCopyCardIds, null)
                            selectedCardIds = emptySet()
                            pendingCopyCardIds = emptySet()
                            showCopyCardsDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("New lesson") }
                    state.lessons.filterNot { it.hidden }.forEach { targetLesson ->
                        OutlinedButton(
                            onClick = {
                                onCopyCardsToLesson(pendingCopyCardIds, targetLesson.id)
                                selectedCardIds = emptySet()
                                pendingCopyCardIds = emptySet()
                                showCopyCardsDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(targetLesson.title.ifBlank { "Untitled lesson" }) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCopyCardsDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteLessonDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteLessonDialog = false },
            title = { Text("Delete lesson?") },
            text = { Text("This lesson will be removed from the app.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteLessonDialog = false
                    onDeleteLesson(lesson.id)
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteLessonDialog = false }) { Text("Cancel") }
            }
        )
    }
    pendingDeleteCardId?.let { cardId ->
        AlertDialog(
            onDismissRequest = { pendingDeleteCardId = null },
            title = { Text("Delete card?") },
            text = { Text("This card will be removed from the lesson.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDeleteCardId = null
                    onDeleteCard(cardId)
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteCardId = null }) { Text("Cancel") }
            }
        )
    }
    if (showCardDialog) {
        AlertDialog(
            onDismissRequest = {
                showCardDialog = false
                onCancelCardEditing()
            },
            title = { Text(if (cardDraft.editingCardId == null) "Add card" else "Edit card") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    VoiceDraftField(
                        value = cardDraft.nativeValue,
                        onValueChange = { onCardDraftChange(cardDraft.copy(nativeValue = it)) },
                        label = "Native value",
                        onVoiceInput = { onVoiceInputForCardField(VoiceInputTarget.CARD_NATIVE) }
                    )
                    VoiceDraftField(
                        value = cardDraft.correctValue,
                        onValueChange = { onCardDraftChange(cardDraft.copy(correctValue = it)) },
                        label = "Make it right",
                        onVoiceInput = { onVoiceInputForCardField(VoiceInputTarget.CARD_CORRECT) }
                    )
                    VoiceDraftField(
                        value = cardDraft.hint,
                        onValueChange = { onCardDraftChange(cardDraft.copy(hint = it)) },
                        label = "Hint or rule",
                        onVoiceInput = { onVoiceInputForCardField(VoiceInputTarget.CARD_HINT) }
                    )
                    VoiceDraftField(
                        value = cardDraft.madeAt,
                        onValueChange = { onCardDraftChange(cardDraft.copy(madeAt = it)) },
                        label = "Made at",
                        singleLine = true
                    )
                    VoiceDraftField(
                        value = cardDraft.where,
                        onValueChange = { onCardDraftChange(cardDraft.copy(where = it)) },
                        label = "Where",
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onAddCard()
                    showCardDialog = false
                }) {
                    Text(if (cardDraft.editingCardId == null) "Add" else "Update")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    cardDraft.editingCardId?.let { editingCardId ->
                        TextButton(onClick = {
                            showCardDialog = false
                            pendingDeleteCardId = editingCardId
                        }) {
                            Text("Delete")
                        }
                    }
                    TextButton(onClick = {
                        showCardDialog = false
                        onCancelCardEditing()
                    }) { Text("Cancel") }
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp)
    ) {
        item {
            OutlinedTextField(
                value = lesson.title,
                onValueChange = onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Lesson title") },
                singleLine = true
            )
        }
        item {
            OutlinedTextField(
                value = lesson.lessonInfo,
                onValueChange = onLessonInfoChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Lesson info") },
                minLines = 2,
                maxLines = 6
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DictionaryLanguageDropdown(
                    label = "Front side input language",
                    value = lesson.sourceLanguage,
                    onValueChange = onLessonSourceLanguageChange,
                    modifier = Modifier.weight(1f)
                )
                DictionaryLanguageDropdown(
                    label = "Back side input language",
                    value = lesson.targetLanguage,
                    onValueChange = onLessonTargetLanguageChange,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("Save lesson")
                }
                OutlinedButton(
                    onClick = { showDeleteLessonDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Delete")
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cards (${lesson.cards.size})",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = {
                    onCancelCardEditing()
                    showCardDialog = true
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add card")
                }
            }
        }
        if (lesson.cards.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                pendingCopyCardIds = selectedCardIds
                                showCopyCardsDialog = true
                            },
                            enabled = selectedCardIds.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) { Text("Copy selected") }
                        OutlinedButton(
                            onClick = {
                                pendingCopyCardIds = lesson.cards.map { it.id }.toSet()
                                showCopyCardsDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) { Text("Copy all") }
                    }
                    OutlinedButton(
                        onClick = {
                            onDeleteCards(selectedCardIds)
                            selectedCardIds = emptySet()
                        },
                        enabled = selectedCardIds.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("Delete selected") }
                }
            }
        }
        if (lesson.cards.isEmpty()) {
            item { EmptyState("This lesson has no cards yet.") }
        } else {
            items(lesson.cards, key = { it.id }) { card ->
                EditableCardRow(
                    card = card,
                    selected = card.id in selectedCardIds,
                    onToggleSelection = { checked ->
                        selectedCardIds = if (checked) selectedCardIds + card.id else selectedCardIds - card.id
                    },
                    onMoveUp = { onMoveCard(card.id, -1) },
                    onMoveDown = { onMoveCard(card.id, 1) },
                    onEdit = {
                        onEditCard(card)
                        showCardDialog = true
                    },
                    onCopy = { onCopyCard(card.id) },
                    onToggleStar = { starIndex -> onToggleCardStar(card.id, starIndex) },
                    onDelete = { pendingDeleteCardId = card.id }
                )
            }
        }
    }
}

@Composable
private fun EditableCardRow(
    card: Flashcard,
    selected: Boolean,
    onToggleSelection: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onCopy: () -> Unit,
    onToggleStar: (Int) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Checkbox(checked = selected, onCheckedChange = onToggleSelection)
                Spacer(Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = card.nativeText().ifBlank { "Empty" },
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = card.correctText().ifBlank { "Empty" },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StarRating(stars = card.stars, onToggleStar = onToggleStar)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onMoveUp) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move card up")
                }
                IconButton(onClick = onMoveDown) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move card down")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Edit")
                }
                IconButton(onClick = onCopy) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy card")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete card")
                }
            }
        }
    }
}
@Composable
private fun StarRating(
    stars: Int,
    onToggleStar: (Int) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        val starCount = (0 until 3).count { index -> stars and (1 shl index) != 0 }.coerceIn(0, 3)
        repeat(3) { index ->
            val filled = index < starCount
            IconButton(
                onClick = { onToggleStar(index) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (filled) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = if (filled) "Clear star ${index + 1}" else "Set star ${index + 1}",
                    tint = if (filled) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
@Composable
private fun EmptyState(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun String.exportFileName(): String {
    return lowercase()
        .replace(Regex("[^a-z0-9\\u0430-\\u044f\\u0451\\u0105\\u0107\\u0119\\u0142\\u0144\\u00f3\\u015b\\u017a\\u017c]+"), "_")
        .trim('_')
        .ifBlank { "lesson" }
}

private fun Flashcard.audioFileBaseName(isBackVisible: Boolean): String {
    val side = if (isBackVisible) backLabel() else frontLabel()
    val text = displayedCardText(isBackVisible)
    return "murrlex_${side}_${text.take(32)}".exportFileName().ifBlank { "murrlex_card_audio" }
}

@Composable
private fun MakeMistakeTheme(content: @Composable () -> Unit) {
    val colorScheme = androidx.compose.material3.lightColorScheme(
        primary = Color(0xFF2F3A36),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE8ECE8),
        onPrimaryContainer = Color(0xFF202824),
        secondaryContainer = Color(0xFFE8F5E9),
        onSecondaryContainer = Color(0xFF111111),
        background = Color(0xFFFAF8F3),
        surface = Color(0xFFFFFEFB),
        onSurface = Color(0xFF111111),
        onSurfaceVariant = Color(0xFF66706B)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}













private enum class FeedbackCue {
    TAP,
    SUCCESS,
    SWIPE,
    SPLASH
}

private fun performFeedback(context: Context, soundEnabled: Boolean, vibrationEnabled: Boolean, cue: FeedbackCue) {
    if (soundEnabled && cue == FeedbackCue.SPLASH) {
        playStartupPurr(context)
    }
    if (vibrationEnabled) {
        vibrate(context, cue)
    }
}

private fun prewarmFeedbackSound(context: Context) {
    runCatching {
        MediaPlayer.create(context, R.raw.startup_purr)?.release()
    }
}

private fun playStartupPurr(context: Context) {
    AppAudioPlayer.playResource(context, R.raw.startup_purr)
}

private fun vibrate(context: Context, cue: FeedbackCue) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
    val duration = when (cue) {
        FeedbackCue.SUCCESS -> 35L
        FeedbackCue.SPLASH -> 90L
        FeedbackCue.SWIPE -> 10L
        FeedbackCue.TAP -> 18L
    }
    if (duration <= 0L) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(duration)
    }
}
private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Card value", text))
}

private fun shareSingleCard(context: Context, json: Json, lesson: Lesson, card: Flashcard) {
    val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
    val exportedCard = card.copy(
        id = 1,
        log = (card.log + "$timestamp - copied from lesson: ${lesson.title}").takeLast(100)
    )
    val kindTitle = if (exportedCard.isLearningKind()) exportedCard.kindLabel() else "Mistakes"
    val exportedLesson = Lesson(
        id = "shared_${lesson.id}_${card.id}_${System.currentTimeMillis()}",
        title = "$kindTitle - ${lesson.title} - 1 card",
        lessonInfo = lesson.lessonInfo,
        cards = listOf(exportedCard),
        timesCompleted = 0,
        editable = true,
        hidden = false
    )
    shareJson(context, "${exportedLesson.title.exportFileName()}.json", json.encodeToString(exportedLesson))
}

private fun createCameraImageUri(context: Context): Uri {
    val cameraDir = File(context.cacheDir, "camera").apply { mkdirs() }
    val imageFile = File.createTempFile("murrlex_camera_", ".jpg", cameraDir)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
}

private fun shareJson(context: Context, fileName: String, jsonText: String) {
    val safeFileName = fileName.ifBlank { "make_mistake_lesson.json" }
    val shareDir = File(context.cacheDir, "shared_json").apply { mkdirs() }
    val shareFile = File(shareDir, safeFileName).apply { writeText(jsonText, Charsets.UTF_8) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", shareFile)
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_TITLE, safeFileName)
        putExtra(Intent.EXTRA_SUBJECT, safeFileName)
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newUri(context.contentResolver, safeFileName, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share lesson JSON"))
}

private fun shareAudioFile(context: Context, sourceFile: File, fileName: String) {
    val safeFileName = fileName.takeIf { it.endsWith(".mp3", ignoreCase = true) }
        ?: "${fileName.ifBlank { "murrlex_card_audio" }}.mp3"
    val shareDir = File(context.cacheDir, "shared_audio").apply { mkdirs() }
    val shareFile = File(shareDir, safeFileName)
    sourceFile.inputStream().use { input ->
        shareFile.outputStream().use { output -> input.copyTo(output) }
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", shareFile)
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "audio/mpeg"
        putExtra(Intent.EXTRA_TITLE, safeFileName)
        putExtra(Intent.EXTRA_SUBJECT, safeFileName)
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newUri(context.contentResolver, safeFileName, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share or save cached audio"))
}

private fun shareCrashLogs(context: Context, logs: List<String>) {
    val text = logs.ifEmpty { listOf("No crash logs saved yet.") }.joinToString("\n\n---\n\n")
    val shareDir = File(context.cacheDir, "crash_logs").apply { mkdirs() }
    val shareFile = File(shareDir, "murrlex_last_crashes.txt").apply { writeText(text, Charsets.UTF_8) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", shareFile)
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TITLE, "murrlex_last_crashes.txt")
        putExtra(Intent.EXTRA_SUBJECT, "MurrLex crash logs")
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newUri(context.contentResolver, "murrlex_last_crashes.txt", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share MurrLex crash logs"))
}
