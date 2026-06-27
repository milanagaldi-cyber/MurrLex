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
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.random.Random
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.ToneGenerator
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.FileProvider
import java.io.File

private val BrandSaladColor = Color(0xFF8FDCC4)
private val BrandRedColor = Color(0xFFC45F59)
private val OnlineStatusColor = Color(0xFF72D66A)
private val CompletedFrameColor = Color(0xFFE8F5E9)

private enum class VoiceInputTarget {
    ANSWER,
    QUICK_VOCABULARY,
    TRANSLATE_INPUT,
    CARD_NATIVE,
    CARD_CORRECT,
    CARD_HINT,
    CARD_MADE_AT,
    CARD_WHERE
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.action == ACTION_START_QUICK_VOICE) {
            quickVoiceLaunchSignal++
        }
        enableEdgeToEdge()
        setContent {
            MakeMistakeTheme {
                MakeMistakeApp(quickVoiceLaunchSignal = quickVoiceLaunchSignal)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == ACTION_START_QUICK_VOICE) {
            quickVoiceLaunchSignal++
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MakeMistakeApp(viewModel: MainViewModel = viewModel(), quickVoiceLaunchSignal: Int = 0) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
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
    var voiceNoMatchRetried by remember { mutableStateOf(false) }
    var voiceHoldActive by remember { mutableStateOf(false) }
    var voiceHoldReleasedAt by remember { mutableStateOf(0L) }
    var lastVoiceActivityAt by remember { mutableStateOf(0L) }
    var quickEditCardId by remember { mutableStateOf<Int?>(null) }
    val quickEditActive = quickEditCardId != null && state.currentCard?.id == quickEditCardId
    var showHeaderLessonInfo by remember { mutableStateOf(false) }
    var showResetProgressConfirm by remember { mutableStateOf(false) }
    var showTranslationDownloadDialog by remember { mutableStateOf(false) }
    var downloadedTranslationLanguages by remember { mutableStateOf(setOf<String>()) }
var translationDownloadLanguage by remember { mutableStateOf(state.quickVocabularyTargetLanguage) }
var translationDownloadingLabel by remember { mutableStateOf<String?>(null) }
var translateAutoSpeakEnabled by remember { mutableStateOf(true) }
var suppressTranslateAutoSpeak by remember { mutableStateOf(false) }
var isDeviceOnline by remember { mutableStateOf(context.isNetworkAvailable()) }
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
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 60000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 60000L)
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
            }

            override fun onRmsChanged(rmsdB: Float) {
                if (rmsdB > 1.5f) {
                    lastVoiceActivityAt = System.currentTimeMillis()
                }
            }
            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() {
                voiceRecording = false
                voiceHoldActive = false
                voiceHoldReleasedAt = 0L
                voiceStatus = "Recognizing..."
            }

            override fun onError(error: Int) {
                voiceRecording = false
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
                viewModel.showMessage(message)
            }
            override fun onResults(results: Bundle?) {
                voiceRecording = false
                voiceHoldActive = false
                voiceHoldReleasedAt = 0L
                voiceDialogVisible = false
                releaseSpeechRecognizer()
                val spokenText = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                    .trim()
                if (spokenText.isNotBlank()) {
                    when (voiceTarget) {
                        VoiceInputTarget.ANSWER -> {
                            viewModel.updateAnswer(spokenText)
                            viewModel.showMessage("Voice input added")
                        }
                        VoiceInputTarget.QUICK_VOCABULARY -> {
                            viewModel.addQuickVocabularyCard(spokenText)
                            if (state.useLocalTranslation) {
                                requestGoogleOfflineTranslation(
                                    spokenText,
                                    state.quickVocabularyTargetLanguage,
                                    state.quickVocabularySourceLanguage,
                                    { translated: String -> viewModel.applyQuickVocabularyGoogleTranslation(spokenText, translated) },
                                    {}
                                )
                            }
                        }
                        VoiceInputTarget.TRANSLATE_INPUT -> viewModel.updateTranslationInput(spokenText)
                        VoiceInputTarget.CARD_NATIVE -> viewModel.updateCardDraft(state.cardDraft.copy(nativeValue = spokenText))
                        VoiceInputTarget.CARD_CORRECT -> viewModel.updateCardDraft(state.cardDraft.copy(correctValue = spokenText))
                        VoiceInputTarget.CARD_HINT -> viewModel.updateCardDraft(state.cardDraft.copy(hint = spokenText))
                        VoiceInputTarget.CARD_MADE_AT -> viewModel.updateCardDraft(state.cardDraft.copy(madeAt = spokenText))
                        VoiceInputTarget.CARD_WHERE -> viewModel.updateCardDraft(state.cardDraft.copy(where = spokenText))
                    }
                } else {
                    viewModel.showMessage("No speech recognized")
                }
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
            withContext(Dispatchers.Default) { prewarmFeedbackSound() }
        }
        splashReady = true
        delay(1300)
        showSplash = false
    }

    fun startVoiceInput(target: VoiceInputTarget = VoiceInputTarget.ANSWER) {
        if (voiceRecording) {
            voiceRecording = false
            voiceStatus = "Recognizing..."
            activeSpeechRecognizer?.stopListening()
            return
        }
        if (!SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            viewModel.showMessage("On-device speech recognition is not available on this phone.")
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
            VoiceInputTarget.QUICK_VOCABULARY -> languageForVoice(state.quickVocabularyTargetLanguage, "", state.interfaceLanguage)
            VoiceInputTarget.TRANSLATE_INPUT -> languageForVoice(state.quickVocabularyTargetLanguage, state.translationInput, state.interfaceLanguage)
            VoiceInputTarget.CARD_NATIVE -> languageForVoice(
                draftCard?.sourceLanguage.asLessonLanguage()
                    ?: state.editorLesson?.sourceLanguage.asLessonLanguage()
                    ?: state.selectedLesson?.sourceLanguage.asLessonLanguage()
                    ?: lessonSampleCard?.sourceLanguage.asLessonLanguage()
                    ?: draftCard?.frontLabel()
                    ?: lessonSampleCard?.frontLabel()
                    ?: state.quickVocabularySourceLanguage,
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
                    ?: state.quickVocabularyTargetLanguage,
                state.cardDraft.correctValue,
                state.interfaceLanguage
            )
            VoiceInputTarget.CARD_HINT,
            VoiceInputTarget.CARD_MADE_AT,
            VoiceInputTarget.CARD_WHERE -> languageForVoice(state.interfaceLanguage, "", state.interfaceLanguage)
        }
        val requestedSpeechTag = speechTagForDictionaryLanguage(language.second)
            ?: state.offlineSpeechLanguageTag
        val selectedSpeechLanguage = OfflineSpeechLanguages.firstOrNull { it.tag == requestedSpeechTag }
            ?: OfflineSpeechLanguages.first()
        val selectedSpeechStatus = state.offlineSpeechStatuses[selectedSpeechLanguage.tag]
            ?: CardRepository.OFFLINE_SPEECH_STATUS_NOT_DOWNLOADED
        if (selectedSpeechStatus != CardRepository.OFFLINE_SPEECH_STATUS_READY) {
            viewModel.showMessage("Download this language first for offline recognition.")
            return
        }
        voiceTarget = target
        voiceLanguageTag = selectedSpeechLanguage.tag
        voiceExpectedLanguage = selectedSpeechLanguage.tag
        viewModel.showMessage("Speak ${selectedSpeechLanguage.name}")
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            voicePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        pendingVoiceStart = true
    }

    fun stopVoiceInput() {
        if (!voiceRecording) return
        voiceRecording = false
        voiceHoldActive = false
        voiceHoldReleasedAt = 0L
        voiceStatus = "Recognizing..."
        activeSpeechRecognizer?.stopListening()
    }

    LaunchedEffect(quickVoiceLaunchSignal, showSplash) {
        if (quickVoiceLaunchSignal > 0 && !showSplash) {
            titleActivated = true
            startVoiceInput(VoiceInputTarget.QUICK_VOCABULARY)
        }
    }
    fun speakText(text: String, languageTag: String) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) {
            viewModel.showMessage("Nothing to read")
            return
        }
        if (!textToSpeechReady) {
            viewModel.showMessage("Speech engine is not ready yet")
            return
        }
        val languageResult = textToSpeech.setLanguage(Locale.forLanguageTag(languageTag))
        if (languageResult == TextToSpeech.LANG_MISSING_DATA || languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            viewModel.showMessage("Speech language is not supported on this device")
            return
        }
        textToSpeech.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "speech-${System.currentTimeMillis()}")
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
                    .addOnFailureListener { onFailure() }
                    .addOnCompleteListener { translator.close() }
            }
            .addOnFailureListener {
                viewModel.showMessage("Download translation languages")
                onFailure()
                translator.close()
            }
    }
    fun translateWithGoogleOffline(text: String = state.translationInput) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) {
            viewModel.updateTranslationOutput("")
            return
        }
        if (!state.useLocalTranslation) return
        translateGoogleOfflineText(
            text = cleanText,
            sourceLanguageName = state.quickVocabularyTargetLanguage,
            targetLanguageName = state.quickVocabularySourceLanguage,
            onSuccess = { translated -> viewModel.updateTranslationOutput(translated) }
        )
    }

    fun downloadGoogleTranslationModels() {
        val sourceName = state.quickVocabularyTargetLanguage
        val targetName = state.quickVocabularySourceLanguage
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

    LaunchedEffect("downloadedTranslationLanguages") {
        refreshDownloadedTranslationLanguages()
    }

    LaunchedEffect(
        state.screen,
        state.translationInput,
        state.quickVocabularySourceLanguage,
        state.quickVocabularyTargetLanguage,
        state.useLocalTranslation
    ) {
        if (state.screen != AppScreen.TRANSLATE) return@LaunchedEffect
        if (state.translationInput.isBlank()) {
            viewModel.updateTranslationOutput("")
            return@LaunchedEffect
        }
        if (!state.useLocalTranslation) return@LaunchedEffect
        delay(650)
        translateWithGoogleOffline(state.translationInput)
    }

    LaunchedEffect("translateAutoSpeak", state.screen, state.translationOutput, translateAutoSpeakEnabled) {
        if (state.screen == AppScreen.TRANSLATE && translateAutoSpeakEnabled && state.translationOutput.isNotBlank()) {
            if (suppressTranslateAutoSpeak) {
                suppressTranslateAutoSpeak = false
                return@LaunchedEffect
            }
            speakText(
                state.translationOutput,
                languageForVoice(state.quickVocabularySourceLanguage, state.translationOutput, state.interfaceLanguage).first
            )
        }
    }
    LaunchedEffect(pendingVoiceStart) {
        if (!pendingVoiceStart) return@LaunchedEffect
        pendingVoiceStart = false
        if (!SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            viewModel.showMessage("On-device speech recognition is not available on this phone.")
            return@LaunchedEffect
        }
        val selectedSpeechStatus = state.offlineSpeechStatuses[voiceLanguageTag]
            ?: CardRepository.OFFLINE_SPEECH_STATUS_NOT_DOWNLOADED
        if (selectedSpeechStatus != CardRepository.OFFLINE_SPEECH_STATUS_READY) {
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
        try {
            releaseSpeechRecognizer()
            activeSpeechRecognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context).also { recognizer ->
                recognizer.setRecognitionListener(buildRecognitionListener())
                recognizer.startListening(offlineSpeechIntent(voiceLanguageTag))
            }
        } catch (_: Exception) {
            voiceRecording = false
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
            when {
                voiceHoldActive -> Unit
                voiceHoldReleasedAt > 0L && now - voiceHoldReleasedAt >= 2000L -> stopVoiceInput()
                voiceHoldReleasedAt == 0L && now - lastVoiceActivityAt >= 10000L -> stopVoiceInput()
            }
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
        if (state.screen != AppScreen.CATALOG) {
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
            onComplete = viewModel::completeOnboarding
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
            languageLabel = if (voiceTarget == VoiceInputTarget.TRANSLATE_INPUT) "Translate offline - $voiceExpectedLanguage" else voiceExpectedLanguage,
            elapsedMs = voiceElapsedMs,
            isRecording = voiceRecording,
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
    val headerLessonInfo = state.selectedLesson?.let { lesson ->
        buildString {
            append("Lesson title:\n")
            append(lesson.title.ifBlank { "Untitled lesson" })
            if (lesson.lessonInfo.isNotBlank()) {
                append("\n\nInfo:\n")
                append(lesson.lessonInfo)
            }
        }
    }.orEmpty()
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
                    Text("Download offline translation models for ${nativeLanguageLabel(state.quickVocabularyTargetLanguage)} -> ${nativeLanguageLabel(state.quickVocabularySourceLanguage)}.")
                    Text("Downloaded: ${downloadedTranslationLanguages.sorted().joinToString().ifBlank { "none" }}")
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
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    if (state.screen != AppScreen.CATALOG) {
                        IconButton(onClick = {
                            titleActivated = true
                            viewModel.navigateBack()
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = ui.back)
                        }
                    }
                },
                title = {
                    if (state.screen == AppScreen.STUDY) {
                        Text("")
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("MurrLex", fontWeight = FontWeight.SemiBold, color = appTitleColor)
                            Box(
                                modifier = Modifier
                                    .padding(start = 7.dp, top = 2.dp)
                                    .size(9.dp)
                                    .clip(RoundedCornerShape(percent = 50))
                                    .background(if (isDeviceOnline) OnlineStatusColor else BrandRedColor)
                                    .clickable(enabled = state.screen == AppScreen.TRANSLATE) {
                                        titleActivated = true
                                        showTranslationDownloadDialog = true
                                    }
                            )
                        }
                    }
                },
                actions = {
                    if (state.screen == AppScreen.CATALOG) {
                        IconButton(onClick = {
                            titleActivated = true
                            viewModel.toggleShowHiddenLessons()
                        }) {
                            Icon(
                                Icons.Default.Visibility,
                                contentDescription = if (state.showHiddenLessons) ui.hideHiddenLessons else ui.showHiddenLessons,
                                tint = if (state.showHiddenLessons) BrandSaladColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (state.screen == AppScreen.STUDY) {
                        IconButton(onClick = {
                            titleActivated = true
                            val nextMode = state.mode.nextMode()
                            viewModel.setMode(nextMode)
                            viewModel.showMessage(nextMode.displayLabel())
                        }) {
                            Icon(
                                imageVector = state.mode.displayIcon(),
                                contentDescription = state.mode.displayLabel(),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = {
                            titleActivated = true
                            val nextSide = if (state.cardStartSide == CardStartSide.POLISH) {
                                CardStartSide.TRANSLATION
                            } else {
                                CardStartSide.POLISH
                            }
                            viewModel.setCardStartSide(nextSide)
                            viewModel.showMessage(state.currentCard.sideLanguageCode(nextSide == CardStartSide.TRANSLATION, state.selectedLesson))
                        }) {
                            Text(
                                text = state.currentCard.sideLanguageCode(state.cardStartSide == CardStartSide.TRANSLATION, state.selectedLesson),
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = {
                            titleActivated = true
                            viewModel.setHideCompletedCards(!state.hideCompletedCards)
                            viewModel.showMessage(if (!state.hideCompletedCards) "Hide done" else "Show done")
                        }) {
                            StudyHideDoneIcon(active = !state.hideCompletedCards)
                        }
                        IconButton(onClick = {
                            titleActivated = true
                            viewModel.addEmptyCardToCurrentLesson()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add empty card",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (state.screen == AppScreen.STUDY && headerLessonInfo.isNotBlank()) {
                        IconButton(onClick = {
                            titleActivated = true
                            showHeaderLessonInfo = true
                        }) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "Lesson info"
                            )
                        }
                    }
                    IconButton(onClick = {
                        titleActivated = true
                        viewModel.openSettings()
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = ui.settings)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (state.screen == AppScreen.STUDY && (state.workMode == WorkMode.CARDS || quickEditActive) && !state.isPortionFinished && state.currentCard != null) {
                AnswerBar(
                    answer = state.answer,
               answerLabel = state.currentCard?.answerInputLabel(state.isBackVisible).orEmpty().ifBlank { ui.makeItRight },
                    currentCard = state.currentCard,
                    isBackVisible = state.isBackVisible,
                    isCurrentCardDone = state.currentCard?.id in state.completedCardIds,
                    answerFeedbackVisible = state.answerFeedbackVisible,
                    isVoiceRecording = voiceRecording,
                    quickEditMode = quickEditActive,
                    onAnswerChange = viewModel::updateAnswer,
                    onCheck = viewModel::previewAnswer,
                    onOk = {
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
                        val fillBackSide = cardBeforeSave?.correctText()?.isEmptyPlaceholder() == true
                        viewModel.saveVisibleSideFromAnswer(state.isBackVisible)
                        quickEditCardId = null
                        if (state.useLocalTranslation && cardBeforeSave != null && enteredText.isNotBlank()) {
                            val sourceLanguage = if (fillBackSide) cardBeforeSave.sourceLanguage.ifBlank { state.selectedLesson?.sourceLanguage.orEmpty() } else cardBeforeSave.targetLanguage.ifBlank { state.selectedLesson?.targetLanguage.orEmpty() }
                            val targetLanguage = if (fillBackSide) cardBeforeSave.targetLanguage.ifBlank { state.selectedLesson?.targetLanguage.orEmpty() } else cardBeforeSave.sourceLanguage.ifBlank { state.selectedLesson?.sourceLanguage.orEmpty() }
                            translateGoogleOfflineText(
                                text = enteredText,
                                sourceLanguageName = sourceLanguage,
                                targetLanguageName = targetLanguage,
                                onSuccess = { translated -> viewModel.applyGoogleTranslationToCard(cardBeforeSave.id, translated, fillBackSide) },
                                onFailure = {}
                            )
                        }
                    },
                    onGoogleTranslateEmptySide = {
                        val card = state.currentCard ?: return@AnswerBar
                        val sourceText = if (state.isBackVisible) card.nativeText() else card.correctText()
                        val sourceLanguage = if (state.isBackVisible) card.sourceLanguage.ifBlank { state.selectedLesson?.sourceLanguage.orEmpty() } else card.targetLanguage.ifBlank { state.selectedLesson?.targetLanguage.orEmpty() }
                        val targetLanguage = if (state.isBackVisible) card.targetLanguage.ifBlank { state.selectedLesson?.targetLanguage.orEmpty() } else card.sourceLanguage.ifBlank { state.selectedLesson?.sourceLanguage.orEmpty() }
                        if (!state.useLocalTranslation) {
                            viewModel.showMessage("Enable local translation")
                        } else {
                            translateGoogleOfflineText(
                                text = sourceText,
                                sourceLanguageName = sourceLanguage,
                                targetLanguageName = targetLanguage,
                                onSuccess = { translated ->
                                    viewModel.updateAnswer(translated)
                                    viewModel.showMessage("Powered by Google Translator")
                                }
                            )
                        }
                    },
                    onClearAnswer = {
                        viewModel.updateAnswer("")
                        quickEditCardId = null
                    },
                    onCopy = { text ->
                        viewModel.updateAnswer(text)
                        viewModel.showMessage(ui.copiedToInput)
                    },
                    onVoiceToggle = { startVoiceInput(VoiceInputTarget.ANSWER) },
                    onDismissAnswerFeedback = viewModel::dismissAnswerFeedback,
                    controlSize = state.controlSize,
                    onSpeak = {
                        val card = state.currentCard
                        if (card != null) {
                            val inputText = state.answer.trim()
                            val textToRead = inputText.ifBlank { card.displayedCardText(state.isBackVisible) }
                            val languageTag = if (inputText.isNotBlank()) {
                                inputText.speechLanguageTagFromText() ?: card.speechLanguageTag(state.interfaceLanguage, state.selectedLesson)
                            } else {
                                card.speechLanguageTagForSide(state.isBackVisible, state.interfaceLanguage, state.selectedLesson)
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
                .padding(padding)
        ) {
            when (state.screen) {
                AppScreen.CATALOG -> LessonCatalogScreen(
                    lessons = state.lessons,
                    selectedLessonIds = state.selectedLessonIds,
                    workMode = state.workMode,
                    onOpenLesson = { titleActivated = true; viewModel.openLesson(it) },
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
                    quickVocabularySourceLanguage = state.quickVocabularySourceLanguage,
                    quickVocabularyTargetLanguage = state.quickVocabularyTargetLanguage,
                    onQuickVocabularySourceChange = { language ->
                        titleActivated = true
                        viewModel.setQuickVocabularySourceLanguage(language)
                    },
                    onQuickVocabularyTargetChange = { language ->
                        titleActivated = true
                        viewModel.setQuickVocabularyTargetLanguage(language)
                        speechTagForDictionaryLanguage(language)?.let(viewModel::setOfflineSpeechLanguage)
                    },
                    onSwapQuickVocabularyLanguages = {
                        titleActivated = true
                        val nextRecognitionLanguage = state.quickVocabularySourceLanguage
                        viewModel.swapQuickVocabularyLanguages()
                        speechTagForDictionaryLanguage(nextRecognitionLanguage)?.let(viewModel::setOfflineSpeechLanguage)
                    },
                    onQuickVoiceInput = {
                        titleActivated = true
                        startVoiceInput(VoiceInputTarget.QUICK_VOCABULARY)
                    },
                    onWorkModeChange = { mode -> titleActivated = true; viewModel.setWorkMode(mode) },
                    onImportLessons = {
                        titleActivated = true
                        importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                    }
                )
                AppScreen.TRANSLATE -> TranslateScreen(
                    state = state,
                    onSourceLanguageChange = { language ->
                        suppressTranslateAutoSpeak = true
                        viewModel.setQuickVocabularyTargetLanguage(language)
                        speechTagForDictionaryLanguage(language)?.let(viewModel::setOfflineSpeechLanguage)
                    },
                    onTargetLanguageChange = { language ->
                        suppressTranslateAutoSpeak = true
                        viewModel.setQuickVocabularySourceLanguage(language)
                    },
                    onInputChange = viewModel::updateTranslationInput,
                    onClear = viewModel::clearTranslationInput,
                    onAddCard = viewModel::addTranslationCard,
                    onSwapLanguages = {
                        suppressTranslateAutoSpeak = true
                        val nextRecognitionLanguage = state.quickVocabularySourceLanguage
                        viewModel.swapQuickVocabularyLanguages()
                        speechTagForDictionaryLanguage(nextRecognitionLanguage)?.let(viewModel::setOfflineSpeechLanguage)
                    },
                    translateAutoSpeakEnabled = translateAutoSpeakEnabled,
                    onTranslateAutoSpeakChange = { translateAutoSpeakEnabled = it },
                    onSpeakTranslation = {
                        speakText(state.translationOutput, languageForVoice(state.quickVocabularySourceLanguage, state.translationOutput, state.interfaceLanguage).first)
                    },
                    onSpeakInput = {
                        speakText(state.translationInput, languageForVoice(state.quickVocabularyTargetLanguage, state.translationInput, state.interfaceLanguage).first)
                    },
                    onVoiceInput = { startVoiceInput(VoiceInputTarget.TRANSLATE_INPUT) }
                )
                AppScreen.STUDY -> StudyScreen(
                    state = state,
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
                    onToggleCard = {
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
                            quickEditCardId = state.currentCard?.id
                            viewModel.updateAnswer("")
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
                    }
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
    onSaveNotificationInterval: () -> Unit,
    onSaveNotificationMax: () -> Unit,
    onDownloadTranslationLanguages: () -> Unit,
    onDownloadSampleJson: () -> Unit
) {
    val ui = rememberUiText()
    fun st(en: String, de: String, be: String, es: String, uk: String, ru: String, pl: String): String {
        return en
    }
    var offlineSpeechHint by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(offlineSpeechHint) {
        if (offlineSpeechHint != null) {
            delay(1500)
            offlineSpeechHint = null
        }
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
                    "ru" to "RU - Russian",
                    "be" to "BY - Belarusian",
                    "uk" to "UA - Ukrainian",
                    "de" to "DE - Deutsch"
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
                    text = st("Default card side", "Standard-Kartenseite", "ÃƒÂÃ…Â¸ÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â‚¬Â¡ÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚ÂºÃƒÂÃ‚Â¾ÃƒÂÃ‚Â²Ãƒâ€˜Ã¢â‚¬Â¹ ÃƒÂÃ‚Â±ÃƒÂÃ‚Â¾ÃƒÂÃ‚Âº ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚ÂºÃƒâ€˜Ã¢â‚¬â€œ", "Lado inicial de la tarjeta", "ÃƒÂÃ…Â¸ÃƒÂÃ‚Â¾Ãƒâ€˜Ã¢â‚¬Â¡ÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚ÂºÃƒÂÃ‚Â¾ÃƒÂÃ‚Â²ÃƒÂÃ‚Â¸ÃƒÂÃ‚Â¹ ÃƒÂÃ‚Â±Ãƒâ€˜Ã¢â‚¬â€œÃƒÂÃ‚Âº ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚ÂºÃƒÂÃ‚Â¸", "ÃƒÂÃ‚Â¡Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚Â¾Ãƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â½ÃƒÂÃ‚Â° ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚Â¾Ãƒâ€˜Ã¢â‚¬Â¡ÃƒÂÃ‚ÂºÃƒÂÃ‚Â¸ ÃƒÂÃ‚Â¿ÃƒÂÃ‚Â¾ Ãƒâ€˜Ã†â€™ÃƒÂÃ‚Â¼ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â»Ãƒâ€˜Ã¢â‚¬Â¡ÃƒÂÃ‚Â°ÃƒÂÃ‚Â½ÃƒÂÃ‚Â¸Ãƒâ€˜Ã…Â½", "DomyÃƒâ€¦Ã¢â‚¬Âºlna strona karty"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = st(
                        "Choose which side is shown first when a card opens.",
                        "WÃƒÆ’Ã‚Â¤hle, welche Seite beim ÃƒÆ’Ã¢â‚¬â€œffnen zuerst erscheint.",
                        "ÃƒÂÃ¢â‚¬â„¢Ãƒâ€˜Ã¢â‚¬Â¹ÃƒÂÃ‚Â±ÃƒÂÃ‚ÂµÃƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã¢â‚¬Â¹, Ãƒâ€˜Ã‚ÂÃƒÂÃ‚ÂºÃƒâ€˜Ã¢â‚¬â€œ ÃƒÂÃ‚Â±ÃƒÂÃ‚Â¾ÃƒÂÃ‚Âº ÃƒÂÃ‚Â¿ÃƒÂÃ‚Â°ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°ÃƒÂÃ‚Â·ÃƒÂÃ‚Â²ÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â‚¬Â Ãƒâ€˜Ã…â€™ ÃƒÂÃ‚Â¿ÃƒÂÃ‚ÂµÃƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã‹â€ Ãƒâ€˜Ã¢â‚¬Â¹ÃƒÂÃ‚Â¼ ÃƒÂÃ‚Â¿Ãƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã¢â‚¬Â¹ ÃƒÂÃ‚Â°ÃƒÂÃ‚Â´ÃƒÂÃ‚ÂºÃƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã¢â‚¬Â¹Ãƒâ€˜Ã¢â‚¬Â Ãƒâ€˜Ã¢â‚¬Â Ãƒâ€˜Ã¢â‚¬â€œ ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚ÂºÃƒâ€˜Ã¢â‚¬â€œ.",
                        "Elige quÃƒÆ’Ã‚Â© lado se muestra primero al abrir una tarjeta.",
                        "ÃƒÂÃ¢â‚¬â„¢ÃƒÂÃ‚Â¸ÃƒÂÃ‚Â±ÃƒÂÃ‚ÂµÃƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚Â¸, Ãƒâ€˜Ã‚ÂÃƒÂÃ‚ÂºÃƒÂÃ‚Â¸ÃƒÂÃ‚Â¹ ÃƒÂÃ‚Â±Ãƒâ€˜Ã¢â‚¬â€œÃƒÂÃ‚Âº ÃƒÂÃ‚Â¿ÃƒÂÃ‚Â¾ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°ÃƒÂÃ‚Â·Ãƒâ€˜Ã†â€™ÃƒÂÃ‚Â²ÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚Â¸ ÃƒÂÃ‚Â¿ÃƒÂÃ‚ÂµÃƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã‹â€ ÃƒÂÃ‚Â¸ÃƒÂÃ‚Â¼ ÃƒÂÃ‚Â¿Ãƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚Â¸ ÃƒÂÃ‚Â²Ãƒâ€˜Ã¢â‚¬â€œÃƒÂÃ‚Â´ÃƒÂÃ‚ÂºÃƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚Â¸Ãƒâ€˜Ã¢â‚¬Å¡Ãƒâ€˜Ã¢â‚¬Å¡Ãƒâ€˜Ã¢â‚¬â€œ ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚ÂºÃƒÂÃ‚Â¸.",
                        "ÃƒÂÃ¢â‚¬â„¢Ãƒâ€˜Ã¢â‚¬Â¹ÃƒÂÃ‚Â±ÃƒÂÃ‚ÂµÃƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚Â¸, ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°Ãƒâ€˜Ã‚Â Ãƒâ€˜Ã‚ÂÃƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚Â¾Ãƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â½ÃƒÂÃ‚Â° ÃƒÂÃ‚Â¿ÃƒÂÃ‚Â¾ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°ÃƒÂÃ‚Â·Ãƒâ€˜Ã¢â‚¬Â¹ÃƒÂÃ‚Â²ÃƒÂÃ‚Â°ÃƒÂÃ‚ÂµÃƒâ€˜Ã¢â‚¬Å¡Ãƒâ€˜Ã‚ÂÃƒâ€˜Ã‚Â ÃƒÂÃ‚Â¿ÃƒÂÃ‚ÂµÃƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚Â²ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â¹ ÃƒÂÃ‚Â¿Ãƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚Â¸ ÃƒÂÃ‚Â¾Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚ÂºÃƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã¢â‚¬Â¹Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚Â¸ÃƒÂÃ‚Â¸ ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚Â¾Ãƒâ€˜Ã¢â‚¬Â¡ÃƒÂÃ‚ÂºÃƒÂÃ‚Â¸.",
                        "Wybierz, ktÃƒÆ’Ã‚Â³ra strona pokazuje siÃƒâ€žÃ¢â€žÂ¢ pierwsza po otwarciu karty."
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
                        text = st("Hide done cards", "Fertige Karten ausblenden", "ÃƒÂÃ‚Â¡Ãƒâ€˜Ã¢â‚¬Â¦ÃƒÂÃ‚Â°ÃƒÂÃ‚Â²ÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â‚¬Â Ãƒâ€˜Ã…â€™ ÃƒÂÃ‚Â·Ãƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â±ÃƒÂÃ‚Â»ÃƒÂÃ‚ÂµÃƒÂÃ‚Â½Ãƒâ€˜Ã¢â‚¬Â¹Ãƒâ€˜Ã‚Â ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚ÂºÃƒâ€˜Ã¢â‚¬â€œ", "Ocultar tarjetas hechas", "ÃƒÂÃ‚Â¡Ãƒâ€˜Ã¢â‚¬Â¦ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â²ÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚Â¸ ÃƒÂÃ‚Â²ÃƒÂÃ‚Â¸ÃƒÂÃ‚ÂºÃƒÂÃ‚Â¾ÃƒÂÃ‚Â½ÃƒÂÃ‚Â°ÃƒÂÃ‚Â½Ãƒâ€˜Ã¢â‚¬â€œ ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚ÂºÃƒÂÃ‚Â¸", "ÃƒÂÃ‚Â¡ÃƒÂÃ‚ÂºÃƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã¢â‚¬Â¹ÃƒÂÃ‚Â²ÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â‚¬Å¡Ãƒâ€˜Ã…â€™ ÃƒÂÃ‚Â²Ãƒâ€˜Ã¢â‚¬Â¹ÃƒÂÃ‚Â¿ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â»ÃƒÂÃ‚Â½ÃƒÂÃ‚ÂµÃƒÂÃ‚Â½ÃƒÂÃ‚Â½Ãƒâ€˜Ã¢â‚¬Â¹ÃƒÂÃ‚Âµ ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚Â¾Ãƒâ€˜Ã¢â‚¬Â¡ÃƒÂÃ‚ÂºÃƒÂÃ‚Â¸", "Ukryj zrobione karty"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = st(
                            "Do not show or count cards marked with 3 stars inside a lesson.",
                            "Karten mit 3 Sternen in der Lektion nicht anzeigen oder zÃƒÆ’Ã‚Â¤hlen.",
                            "ÃƒÂÃ‚ÂÃƒÂÃ‚Âµ ÃƒÂÃ‚Â¿ÃƒÂÃ‚Â°ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°ÃƒÂÃ‚Â·ÃƒÂÃ‚Â²ÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â‚¬Â Ãƒâ€˜Ã…â€™ Ãƒâ€˜Ã¢â‚¬â€œ ÃƒÂÃ‚Â½ÃƒÂÃ‚Âµ Ãƒâ€˜Ã…Â¾ÃƒÂÃ‚Â»Ãƒâ€˜Ã¢â‚¬â€œÃƒâ€˜Ã¢â‚¬Â¡ÃƒÂÃ‚Â²ÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â‚¬Â Ãƒâ€˜Ã…â€™ Ãƒâ€˜Ã†â€™ Ãƒâ€˜Ã…Â¾Ãƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚Â¾ÃƒÂÃ‚ÂºÃƒâ€˜Ã†â€™ ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚ÂºÃƒâ€˜Ã¢â‚¬â€œ ÃƒÂÃ‚Â· 3 ÃƒÂÃ‚Â·ÃƒÂÃ‚Â¾Ãƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°ÃƒÂÃ‚Â¼Ãƒâ€˜Ã¢â‚¬â€œ.",
                            "No mostrar ni contar dentro de la lecciÃƒÆ’Ã‚Â³n las tarjetas con 3 estrellas.",
                            "ÃƒÂÃ‚ÂÃƒÂÃ‚Âµ ÃƒÂÃ‚Â¿ÃƒÂÃ‚Â¾ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°ÃƒÂÃ‚Â·Ãƒâ€˜Ã†â€™ÃƒÂÃ‚Â²ÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚Â¸ ÃƒÂÃ‚Â¹ ÃƒÂÃ‚Â½ÃƒÂÃ‚Âµ Ãƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â‚¬Â¦Ãƒâ€˜Ã†â€™ÃƒÂÃ‚Â²ÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚Â¸ ÃƒÂÃ‚Â² Ãƒâ€˜Ã†â€™Ãƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚Â¾Ãƒâ€˜Ã¢â‚¬Â Ãƒâ€˜Ã¢â‚¬â€œ ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚ÂºÃƒÂÃ‚Â¸ ÃƒÂÃ‚Â· 3 ÃƒÂÃ‚Â·Ãƒâ€˜Ã¢â‚¬â€œÃƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°ÃƒÂÃ‚Â¼ÃƒÂÃ‚Â¸.",
                            "ÃƒÂÃ‚ÂÃƒÂÃ‚Âµ ÃƒÂÃ‚Â¿ÃƒÂÃ‚Â¾ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°ÃƒÂÃ‚Â·Ãƒâ€˜Ã¢â‚¬Â¹ÃƒÂÃ‚Â²ÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â‚¬Å¡Ãƒâ€˜Ã…â€™ ÃƒÂÃ‚Â¸ ÃƒÂÃ‚Â½ÃƒÂÃ‚Âµ Ãƒâ€˜Ã†â€™Ãƒâ€˜Ã¢â‚¬Â¡ÃƒÂÃ‚Â¸Ãƒâ€˜Ã¢â‚¬Å¡Ãƒâ€˜Ã¢â‚¬Â¹ÃƒÂÃ‚Â²ÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â‚¬Å¡Ãƒâ€˜Ã…â€™ ÃƒÂÃ‚Â² Ãƒâ€˜Ã†â€™Ãƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚Â¾ÃƒÂÃ‚ÂºÃƒÂÃ‚Âµ ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚Â¾Ãƒâ€˜Ã¢â‚¬Â¡ÃƒÂÃ‚ÂºÃƒÂÃ‚Â¸ Ãƒâ€˜Ã‚Â 3 ÃƒÂÃ‚Â·ÃƒÂÃ‚Â²ÃƒÂÃ‚ÂµÃƒÂÃ‚Â·ÃƒÂÃ‚Â´ÃƒÂÃ‚Â°ÃƒÂÃ‚Â¼ÃƒÂÃ‚Â¸.",
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
                    text = st("Card and feedback", "Karte und Feedback", "ÃƒÂÃ…Â¡ÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚ÂºÃƒÂÃ‚Â° Ãƒâ€˜Ã¢â‚¬â€œ ÃƒÂÃ‚Â²ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â´ÃƒÂÃ‚Â³Ãƒâ€˜Ã†â€™ÃƒÂÃ‚Âº", "Tarjeta y respuesta", "ÃƒÂÃ…Â¡ÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚ÂºÃƒÂÃ‚Â° Ãƒâ€˜Ã¢â‚¬â€œ ÃƒÂÃ‚Â²Ãƒâ€˜Ã¢â‚¬â€œÃƒÂÃ‚Â´ÃƒÂÃ‚Â³Ãƒâ€˜Ã†â€™ÃƒÂÃ‚Âº", "ÃƒÂÃ…Â¡ÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚Â¾Ãƒâ€˜Ã¢â‚¬Â¡ÃƒÂÃ‚ÂºÃƒÂÃ‚Â° ÃƒÂÃ‚Â¸ ÃƒÂÃ‚Â¾Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚ÂºÃƒÂÃ‚Â»ÃƒÂÃ‚Â¸ÃƒÂÃ‚Âº", "Karta i reakcje"),
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

                SettingsSwitchRow(
                    title = st("Show card log", "Kartenlog anzeigen", "ÃƒÂÃ…Â¸ÃƒÂÃ‚Â°ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°ÃƒÂÃ‚Â·ÃƒÂÃ‚Â²ÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â‚¬Â Ãƒâ€˜Ã…â€™ ÃƒÂÃ‚Â»ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â³ ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚ÂºÃƒâ€˜Ã¢â‚¬â€œ", "Mostrar registro de tarjeta", "ÃƒÂÃ…Â¸ÃƒÂÃ‚Â¾ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°ÃƒÂÃ‚Â·Ãƒâ€˜Ã†â€™ÃƒÂÃ‚Â²ÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚Â¸ ÃƒÂÃ‚Â»ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â³ ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚ÂºÃƒÂÃ‚Â¸", "ÃƒÂÃ…Â¸ÃƒÂÃ‚Â¾ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°ÃƒÂÃ‚Â·Ãƒâ€˜Ã¢â‚¬Â¹ÃƒÂÃ‚Â²ÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â‚¬Å¡Ãƒâ€˜Ã…â€™ ÃƒÂÃ‚Â»ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â³ ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚Â¾Ãƒâ€˜Ã¢â‚¬Â¡ÃƒÂÃ‚ÂºÃƒÂÃ‚Â¸", "PokaÃƒâ€¦Ã‚Â¼ log karty"),
                    description = st(
                        "Show the M icon with mistakes and work log on study cards.",
                        "Zeigt das M-Symbol mit Fehlern und Arbeitslog auf Lernkarten.",
                        "ÃƒÂÃ…Â¸ÃƒÂÃ‚Â°ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°ÃƒÂÃ‚Â·ÃƒÂÃ‚Â²ÃƒÂÃ‚Â°ÃƒÂÃ‚Âµ ÃƒÂÃ‚Â·ÃƒÂÃ‚Â½ÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â‚¬Â¡ÃƒÂÃ‚Â¾ÃƒÂÃ‚Âº M ÃƒÂÃ‚Â· ÃƒÂÃ‚Â¿ÃƒÂÃ‚Â°ÃƒÂÃ‚Â¼Ãƒâ€˜Ã¢â‚¬Â¹ÃƒÂÃ‚Â»ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°ÃƒÂÃ‚Â¼Ãƒâ€˜Ã¢â‚¬â€œ Ãƒâ€˜Ã¢â‚¬â€œ ÃƒÂÃ‚Â»ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â³ÃƒÂÃ‚Â°ÃƒÂÃ‚Â¼ ÃƒÂÃ‚Â¿Ãƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â‚¬Â Ãƒâ€˜Ã¢â‚¬Â¹ ÃƒÂÃ‚Â½ÃƒÂÃ‚Â° ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â‚¬Â¦.",
                        "Muestra el icono M con errores y registro de trabajo en las tarjetas.",
                        "ÃƒÂÃ…Â¸ÃƒÂÃ‚Â¾ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°ÃƒÂÃ‚Â·Ãƒâ€˜Ã†â€™Ãƒâ€˜Ã¢â‚¬Â ÃƒÂÃ‚Â·ÃƒÂÃ‚Â½ÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â‚¬Â¡ÃƒÂÃ‚Â¾ÃƒÂÃ‚Âº M ÃƒÂÃ‚Â· ÃƒÂÃ‚Â¿ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â¼ÃƒÂÃ‚Â¸ÃƒÂÃ‚Â»ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°ÃƒÂÃ‚Â¼ÃƒÂÃ‚Â¸ ÃƒÂÃ‚Â¹ ÃƒÂÃ‚Â»ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â³ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â¼ Ãƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â±ÃƒÂÃ‚Â¾Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚Â¸ ÃƒÂÃ‚Â½ÃƒÂÃ‚Â° ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â‚¬Â¦.",
                        "ÃƒÂÃ…Â¸ÃƒÂÃ‚Â¾ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°ÃƒÂÃ‚Â·Ãƒâ€˜Ã¢â‚¬Â¹ÃƒÂÃ‚Â²ÃƒÂÃ‚Â°ÃƒÂÃ‚ÂµÃƒâ€˜Ã¢â‚¬Å¡ ÃƒÂÃ‚Â·ÃƒÂÃ‚Â½ÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â‚¬Â¡ÃƒÂÃ‚Â¾ÃƒÂÃ‚Âº M Ãƒâ€˜Ã‚Â ÃƒÂÃ‚Â¾Ãƒâ€˜Ã‹â€ ÃƒÂÃ‚Â¸ÃƒÂÃ‚Â±ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°ÃƒÂÃ‚Â¼ÃƒÂÃ‚Â¸ ÃƒÂÃ‚Â¸ ÃƒÂÃ‚Â»ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â³ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â¼ Ãƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚Â°ÃƒÂÃ‚Â±ÃƒÂÃ‚Â¾Ãƒâ€˜Ã¢â‚¬Å¡Ãƒâ€˜Ã¢â‚¬Â¹ ÃƒÂÃ‚Â½ÃƒÂÃ‚Â° ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚Â¾Ãƒâ€˜Ã¢â‚¬Â¡ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â‚¬Â¦.",
                        "Pokazuje ikonÃƒâ€žÃ¢â€žÂ¢ M z bÃƒâ€¦Ã¢â‚¬Å¡Ãƒâ€žÃ¢â€žÂ¢dami i logiem pracy na kartach."
                    ),
                    checked = showCardLog,
                    onCheckedChange = onShowCardLogChange
                )
                SettingsSwitchRow(
                    title = st("Sound effects", "Soundeffekte", "ÃƒÂÃ¢â‚¬Å“Ãƒâ€˜Ã†â€™ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°ÃƒÂÃ‚Â²Ãƒâ€˜Ã¢â‚¬Â¹Ãƒâ€˜Ã‚Â Ãƒâ€˜Ã‚ÂÃƒâ€˜Ã¢â‚¬Å¾ÃƒÂÃ‚ÂµÃƒÂÃ‚ÂºÃƒâ€˜Ã¢â‚¬Å¡Ãƒâ€˜Ã¢â‚¬Â¹", "Efectos de sonido", "ÃƒÂÃ¢â‚¬â€ÃƒÂÃ‚Â²Ãƒâ€˜Ã†â€™ÃƒÂÃ‚ÂºÃƒÂÃ‚Â¾ÃƒÂÃ‚Â²Ãƒâ€˜Ã¢â‚¬â€œ ÃƒÂÃ‚ÂµÃƒâ€˜Ã¢â‚¬Å¾ÃƒÂÃ‚ÂµÃƒÂÃ‚ÂºÃƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚Â¸", "ÃƒÂÃ¢â‚¬â€ÃƒÂÃ‚Â²Ãƒâ€˜Ã†â€™ÃƒÂÃ‚ÂºÃƒÂÃ‚Â¾ÃƒÂÃ‚Â²Ãƒâ€˜Ã¢â‚¬Â¹ÃƒÂÃ‚Âµ Ãƒâ€˜Ã‚ÂÃƒâ€˜Ã¢â‚¬Å¾Ãƒâ€˜Ã¢â‚¬Å¾ÃƒÂÃ‚ÂµÃƒÂÃ‚ÂºÃƒâ€˜Ã¢â‚¬Å¡Ãƒâ€˜Ã¢â‚¬Â¹", "Efekty dÃƒâ€¦Ã‚ÂºwiÃƒâ€žÃ¢â€žÂ¢kowe"),
                    description = st(
                        "Play short sounds for app actions and the splash screen.",
                        "Spielt kurze Sounds fÃƒÆ’Ã‚Â¼r Aktionen und den Startbildschirm.",
                        "ÃƒÂÃ…Â¸Ãƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚Â°ÃƒÂÃ‚Â¹ÃƒÂÃ‚Â³Ãƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚Â°ÃƒÂÃ‚Âµ ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚Â¾Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚ÂºÃƒâ€˜Ã¢â‚¬â€œÃƒâ€˜Ã‚Â ÃƒÂÃ‚Â³Ãƒâ€˜Ã†â€™ÃƒÂÃ‚ÂºÃƒâ€˜Ã¢â‚¬â€œ ÃƒÂÃ‚Â´ÃƒÂÃ‚Â»Ãƒâ€˜Ã‚Â ÃƒÂÃ‚Â´ÃƒÂÃ‚Â·ÃƒÂÃ‚ÂµÃƒâ€˜Ã‚ÂÃƒÂÃ‚Â½ÃƒÂÃ‚Â½Ãƒâ€˜Ã‚ÂÃƒâ€˜Ã…Â¾ Ãƒâ€˜Ã¢â‚¬â€œ ÃƒÂÃ‚Â·ÃƒÂÃ‚Â°Ãƒâ€˜Ã‚ÂÃƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚Â°Ãƒâ€˜Ã…Â¾ÃƒÂÃ‚ÂºÃƒâ€˜Ã¢â‚¬â€œ.",
                        "Reproduce sonidos cortos para acciones y la pantalla inicial.",
                        "ÃƒÂÃ¢â‚¬â„¢Ãƒâ€˜Ã¢â‚¬â€œÃƒÂÃ‚Â´Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚Â²ÃƒÂÃ‚Â¾Ãƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã…Â½Ãƒâ€˜Ã¢â‚¬Â ÃƒÂÃ‚ÂºÃƒÂÃ‚Â¾Ãƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚Â¾Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚ÂºÃƒâ€˜Ã¢â‚¬â€œ ÃƒÂÃ‚Â·ÃƒÂÃ‚Â²Ãƒâ€˜Ã†â€™ÃƒÂÃ‚ÂºÃƒÂÃ‚Â¸ ÃƒÂÃ‚Â´ÃƒÂÃ‚Â»Ãƒâ€˜Ã‚Â ÃƒÂÃ‚Â´Ãƒâ€˜Ã¢â‚¬â€œÃƒÂÃ‚Â¹ Ãƒâ€˜Ã¢â‚¬â€œ ÃƒÂÃ‚Â·ÃƒÂÃ‚Â°Ãƒâ€˜Ã‚ÂÃƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚Â°ÃƒÂÃ‚Â²ÃƒÂÃ‚ÂºÃƒÂÃ‚Â¸.",
                        "ÃƒÂÃ¢â‚¬â„¢ÃƒÂÃ‚Â¾Ãƒâ€˜Ã‚ÂÃƒÂÃ‚Â¿Ãƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â¸ÃƒÂÃ‚Â·ÃƒÂÃ‚Â²ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â´ÃƒÂÃ‚Â¸Ãƒâ€˜Ã¢â‚¬Å¡ ÃƒÂÃ‚ÂºÃƒÂÃ‚Â¾Ãƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚Â¾Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚ÂºÃƒÂÃ‚Â¸ÃƒÂÃ‚Âµ ÃƒÂÃ‚Â·ÃƒÂÃ‚Â²Ãƒâ€˜Ã†â€™ÃƒÂÃ‚ÂºÃƒÂÃ‚Â¸ ÃƒÂÃ‚Â´ÃƒÂÃ‚Â»Ãƒâ€˜Ã‚Â ÃƒÂÃ‚Â´ÃƒÂÃ‚ÂµÃƒÂÃ‚Â¹Ãƒâ€˜Ã‚ÂÃƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚Â²ÃƒÂÃ‚Â¸ÃƒÂÃ‚Â¹ ÃƒÂÃ‚Â¸ ÃƒÂÃ‚Â·ÃƒÂÃ‚Â°Ãƒâ€˜Ã‚ÂÃƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚Â°ÃƒÂÃ‚Â²ÃƒÂÃ‚ÂºÃƒÂÃ‚Â¸.",
                        "Odtwarza krÃƒÆ’Ã‚Â³tkie dÃƒâ€¦Ã‚ÂºwiÃƒâ€žÃ¢â€žÂ¢ki dla akcji i ekranu startowego."
                    ),
                    checked = soundEffectsEnabled,
                    onCheckedChange = onSoundEffectsEnabledChange
                )
                SettingsSwitchRow(
                    title = st("Vibration", "Vibration", "ÃƒÂÃ¢â‚¬â„¢Ãƒâ€˜Ã¢â‚¬â€œÃƒÂÃ‚Â±Ãƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â‚¬Â Ãƒâ€˜Ã¢â‚¬Â¹Ãƒâ€˜Ã‚Â", "VibraciÃƒÆ’Ã‚Â³n", "ÃƒÂÃ¢â‚¬â„¢Ãƒâ€˜Ã¢â‚¬â€œÃƒÂÃ‚Â±Ãƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â‚¬Â Ãƒâ€˜Ã¢â‚¬â€œÃƒâ€˜Ã‚Â", "ÃƒÂÃ¢â‚¬â„¢ÃƒÂÃ‚Â¸ÃƒÂÃ‚Â±Ãƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â‚¬Â ÃƒÂÃ‚Â¸Ãƒâ€˜Ã‚Â", "Wibracja"),
                    description = st(
                        "Use short haptic feedback for app actions.",
                        "Nutzt kurze haptische RÃƒÆ’Ã‚Â¼ckmeldung fÃƒÆ’Ã‚Â¼r Aktionen.",
                        "ÃƒÂÃ¢â‚¬â„¢Ãƒâ€˜Ã¢â‚¬Â¹ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã¢â‚¬Â¹Ãƒâ€˜Ã‚ÂÃƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚Â¾Ãƒâ€˜Ã…Â¾ÃƒÂÃ‚Â²ÃƒÂÃ‚Â°ÃƒÂÃ‚Âµ ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚Â¾Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚ÂºÃƒâ€˜Ã¢â‚¬â€œ ÃƒÂÃ‚Â²Ãƒâ€˜Ã¢â‚¬â€œÃƒÂÃ‚Â±Ãƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚Â°ÃƒÂÃ‚Â°ÃƒÂÃ‚Â´ÃƒÂÃ‚Â³Ãƒâ€˜Ã†â€™ÃƒÂÃ‚Âº ÃƒÂÃ‚Â´ÃƒÂÃ‚Â»Ãƒâ€˜Ã‚Â ÃƒÂÃ‚Â´ÃƒÂÃ‚Â·ÃƒÂÃ‚ÂµÃƒâ€˜Ã‚ÂÃƒÂÃ‚Â½ÃƒÂÃ‚Â½Ãƒâ€˜Ã‚ÂÃƒâ€˜Ã…Â¾.",
                        "Usa respuesta hÃƒÆ’Ã‚Â¡ptica corta para acciones.",
                        "ÃƒÂÃ¢â‚¬â„¢ÃƒÂÃ‚Â¸ÃƒÂÃ‚ÂºÃƒÂÃ‚Â¾Ãƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚Â¸Ãƒâ€˜Ã‚ÂÃƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â²Ãƒâ€˜Ã†â€™Ãƒâ€˜Ã¢â‚¬Â ÃƒÂÃ‚ÂºÃƒÂÃ‚Â¾Ãƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚Â¾Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚ÂºÃƒÂÃ‚Â¸ÃƒÂÃ‚Â¹ ÃƒÂÃ‚Â²Ãƒâ€˜Ã¢â‚¬â€œÃƒÂÃ‚Â±Ãƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â²Ãƒâ€˜Ã¢â‚¬â€œÃƒÂÃ‚Â´ÃƒÂÃ‚Â³Ãƒâ€˜Ã†â€™ÃƒÂÃ‚Âº ÃƒÂÃ‚Â´ÃƒÂÃ‚Â»Ãƒâ€˜Ã‚Â ÃƒÂÃ‚Â´Ãƒâ€˜Ã¢â‚¬â€œÃƒÂÃ‚Â¹.",
                        "ÃƒÂÃ‹Å“Ãƒâ€˜Ã‚ÂÃƒÂÃ‚Â¿ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â»Ãƒâ€˜Ã…â€™ÃƒÂÃ‚Â·Ãƒâ€˜Ã†â€™ÃƒÂÃ‚ÂµÃƒâ€˜Ã¢â‚¬Å¡ ÃƒÂÃ‚ÂºÃƒÂÃ‚Â¾Ãƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚Â¾Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚ÂºÃƒÂÃ‚Â¸ÃƒÂÃ‚Â¹ ÃƒÂÃ‚Â²ÃƒÂÃ‚Â¸ÃƒÂÃ‚Â±Ãƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â¾Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚ÂºÃƒÂÃ‚Â»ÃƒÂÃ‚Â¸ÃƒÂÃ‚Âº ÃƒÂÃ‚Â´ÃƒÂÃ‚Â»Ãƒâ€˜Ã‚Â ÃƒÂÃ‚Â´ÃƒÂÃ‚ÂµÃƒÂÃ‚Â¹Ãƒâ€˜Ã‚ÂÃƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚Â²ÃƒÂÃ‚Â¸ÃƒÂÃ‚Â¹.",
                        "UÃƒâ€¦Ã‚Â¼ywa krÃƒÆ’Ã‚Â³tkiej reakcji haptycznej dla akcji."
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
                    text = "Choose the language pair for phrases captured with the microphone and used by Translate. Offline translation uses Google Translate models downloaded on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
           DictionaryLanguageDropdown(
               label = "Target language",
               value = quickVocabularySourceLanguage,
               onValueChange = onQuickVocabularySourceChange
           )
           DictionaryLanguageDropdown(
               label = "Source language",
               value = quickVocabularyTargetLanguage,
               onValueChange = onQuickVocabularyTargetChange
           )
                SettingsSwitchRow(
                    title = "Local translation",
                    description = "Use on-device Google Translate after downloading the selected language pair. Translations are powered by Google Translate and may be inaccurate.",
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
                    text = "Downloaded languages: ${downloadedTranslationLanguages.sorted().joinToString().ifBlank { "none" }}",
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
                OutlinedTextField(
                    value = translationApiUrl,
                    onValueChange = onTranslationApiUrlChange,
                    label = { Text("Translation API URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = translationApiToken,
                    onValueChange = onTranslationApiTokenChange,
                    label = { Text("Translation API token") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = st("JSON template", "JSON-Vorlage", "ÃƒÂÃ‚Â¨ÃƒÂÃ‚Â°ÃƒÂÃ‚Â±ÃƒÂÃ‚Â»ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â½ JSON", "Plantilla JSON", "ÃƒÂÃ‚Â¨ÃƒÂÃ‚Â°ÃƒÂÃ‚Â±ÃƒÂÃ‚Â»ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â½ JSON", "ÃƒÂÃ‚Â¨ÃƒÂÃ‚Â°ÃƒÂÃ‚Â±ÃƒÂÃ‚Â»ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â½ JSON", "Szablon JSON"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = st(
                        "Download an instruction-ready JSON template for generating lessons from your mistakes.",
                        "Lade eine JSON-Vorlage mit Anweisungen herunter, um Lektionen aus deinen Fehlern zu erzeugen.",
                        "ÃƒÂÃ‚Â¡ÃƒÂÃ‚Â¿ÃƒÂÃ‚Â°ÃƒÂÃ‚Â¼ÃƒÂÃ‚Â¿Ãƒâ€˜Ã†â€™ÃƒÂÃ‚Â¹ JSON-Ãƒâ€˜Ã‹â€ ÃƒÂÃ‚Â°ÃƒÂÃ‚Â±ÃƒÂÃ‚Â»ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â½ ÃƒÂÃ‚Â· Ãƒâ€˜Ã¢â‚¬â€œÃƒÂÃ‚Â½Ãƒâ€˜Ã‚ÂÃƒâ€˜Ã¢â‚¬Å¡Ãƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã†â€™ÃƒÂÃ‚ÂºÃƒâ€˜Ã¢â‚¬Â Ãƒâ€˜Ã¢â‚¬Â¹Ãƒâ€˜Ã‚ÂÃƒÂÃ‚Â¹ ÃƒÂÃ‚Â´ÃƒÂÃ‚Â»Ãƒâ€˜Ã‚Â Ãƒâ€˜Ã‚ÂÃƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚Â²ÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã‚ÂÃƒÂÃ‚Â½ÃƒÂÃ‚Â½Ãƒâ€˜Ã‚Â Ãƒâ€˜Ã…Â¾Ãƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚Â¾ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°Ãƒâ€˜Ã…Â¾ ÃƒÂÃ‚Â· Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚Â²ÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â‚¬â€œÃƒâ€˜Ã¢â‚¬Â¦ ÃƒÂÃ‚Â¿ÃƒÂÃ‚Â°ÃƒÂÃ‚Â¼Ãƒâ€˜Ã¢â‚¬Â¹ÃƒÂÃ‚Â»ÃƒÂÃ‚Â°ÃƒÂÃ‚Âº.",
                        "Descarga una plantilla JSON lista como instrucciÃƒÆ’Ã‚Â³n para generar lecciones desde tus errores.",
                        "ÃƒÂÃ¢â‚¬â€ÃƒÂÃ‚Â°ÃƒÂÃ‚Â²ÃƒÂÃ‚Â°ÃƒÂÃ‚Â½Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚Â°ÃƒÂÃ‚Â¶ JSON-Ãƒâ€˜Ã‹â€ ÃƒÂÃ‚Â°ÃƒÂÃ‚Â±ÃƒÂÃ‚Â»ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â½ ÃƒÂÃ‚Â· Ãƒâ€˜Ã¢â‚¬â€œÃƒÂÃ‚Â½Ãƒâ€˜Ã‚ÂÃƒâ€˜Ã¢â‚¬Å¡Ãƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã†â€™ÃƒÂÃ‚ÂºÃƒâ€˜Ã¢â‚¬Â Ãƒâ€˜Ã¢â‚¬â€œÃƒâ€˜Ã¢â‚¬ÂÃƒâ€˜Ã…Â½ ÃƒÂÃ‚Â´ÃƒÂÃ‚Â»Ãƒâ€˜Ã‚Â Ãƒâ€˜Ã‚ÂÃƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚Â²ÃƒÂÃ‚Â¾Ãƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚ÂµÃƒÂÃ‚Â½ÃƒÂÃ‚Â½Ãƒâ€˜Ã‚Â Ãƒâ€˜Ã†â€™Ãƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚Â¾ÃƒÂÃ‚ÂºÃƒâ€˜Ã¢â‚¬â€œÃƒÂÃ‚Â² Ãƒâ€˜Ã¢â‚¬â€œÃƒÂÃ‚Â· Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚Â²ÃƒÂÃ‚Â¾Ãƒâ€˜Ã¢â‚¬â€Ãƒâ€˜Ã¢â‚¬Â¦ ÃƒÂÃ‚Â¿ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â¼ÃƒÂÃ‚Â¸ÃƒÂÃ‚Â»ÃƒÂÃ‚Â¾ÃƒÂÃ‚Âº.",
                        "ÃƒÂÃ‚Â¡ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â‚¬Â¡ÃƒÂÃ‚Â°ÃƒÂÃ‚Â¹ JSON-Ãƒâ€˜Ã‹â€ ÃƒÂÃ‚Â°ÃƒÂÃ‚Â±ÃƒÂÃ‚Â»ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â½ Ãƒâ€˜Ã‚Â ÃƒÂÃ‚Â¸ÃƒÂÃ‚Â½Ãƒâ€˜Ã‚ÂÃƒâ€˜Ã¢â‚¬Å¡Ãƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã†â€™ÃƒÂÃ‚ÂºÃƒâ€˜Ã¢â‚¬Â ÃƒÂÃ‚Â¸ÃƒÂÃ‚ÂµÃƒÂÃ‚Â¹ ÃƒÂÃ‚Â´ÃƒÂÃ‚Â»Ãƒâ€˜Ã‚Â Ãƒâ€˜Ã‚ÂÃƒÂÃ‚Â¾ÃƒÂÃ‚Â·ÃƒÂÃ‚Â´ÃƒÂÃ‚Â°ÃƒÂÃ‚Â½ÃƒÂÃ‚Â¸Ãƒâ€˜Ã‚Â Ãƒâ€˜Ã†â€™Ãƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚Â¾ÃƒÂÃ‚ÂºÃƒÂÃ‚Â¾ÃƒÂÃ‚Â² ÃƒÂÃ‚Â¸ÃƒÂÃ‚Â· Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚Â²ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â¸Ãƒâ€˜Ã¢â‚¬Â¦ ÃƒÂÃ‚Â¾Ãƒâ€˜Ã‹â€ ÃƒÂÃ‚Â¸ÃƒÂÃ‚Â±ÃƒÂÃ‚Â¾ÃƒÂÃ‚Âº.",
                        "Pobierz szablon JSON z instrukcjÃƒâ€žÃ¢â‚¬Â¦ do tworzenia lekcji z Twoich bÃƒâ€¦Ã¢â‚¬Å¡Ãƒâ€žÃ¢â€žÂ¢dÃƒÆ’Ã‚Â³w."
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
                    text = st("Notifications", "Benachrichtigungen", "ÃƒÂÃ‚ÂÃƒÂÃ‚Â¿ÃƒÂÃ‚Â°ÃƒÂÃ‚Â²Ãƒâ€˜Ã‚ÂÃƒâ€˜Ã‹â€ Ãƒâ€˜Ã¢â‚¬Â¡Ãƒâ€˜Ã‚ÂÃƒÂÃ‚Â½ÃƒÂÃ‚Â½Ãƒâ€˜Ã¢â‚¬â€œ", "Notificaciones", "ÃƒÂÃ‚Â¡ÃƒÂÃ‚Â¿ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â²Ãƒâ€˜Ã¢â‚¬â€œÃƒâ€˜Ã¢â‚¬Â°ÃƒÂÃ‚ÂµÃƒÂÃ‚Â½ÃƒÂÃ‚Â½Ãƒâ€˜Ã‚Â", "ÃƒÂÃ‚Â£ÃƒÂÃ‚Â²ÃƒÂÃ‚ÂµÃƒÂÃ‚Â´ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â¼ÃƒÂÃ‚Â»ÃƒÂÃ‚ÂµÃƒÂÃ‚Â½ÃƒÂÃ‚Â¸Ãƒâ€˜Ã‚Â", "Powiadomienia"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = st(
                        "Shows weighted reminders for cards with 0-2 stars. Cards with 0 stars appear most often; 3-star cards are excluded.",
                        "Zeigt gewichtete Erinnerungen fÃƒÆ’Ã‚Â¼r Karten mit 0-2 Sternen. 0 Sterne erscheinen am hÃƒÆ’Ã‚Â¤ufigsten; 3 Sterne sind ausgeschlossen.",
                        "ÃƒÂÃ…Â¸ÃƒÂÃ‚Â°ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°ÃƒÂÃ‚Â·ÃƒÂÃ‚Â²ÃƒÂÃ‚Â°ÃƒÂÃ‚Âµ Ãƒâ€˜Ã…Â¾ÃƒÂÃ‚Â·ÃƒÂÃ‚Â²ÃƒÂÃ‚Â°ÃƒÂÃ‚Â¶ÃƒÂÃ‚Â°ÃƒÂÃ‚Â½Ãƒâ€˜Ã¢â‚¬Â¹Ãƒâ€˜Ã‚Â ÃƒÂÃ‚Â½ÃƒÂÃ‚Â°ÃƒÂÃ‚Â¿ÃƒÂÃ‚Â°ÃƒÂÃ‚Â¼Ãƒâ€˜Ã¢â‚¬â€œÃƒÂÃ‚Â½Ãƒâ€˜Ã¢â‚¬Â¹ ÃƒÂÃ‚Â´ÃƒÂÃ‚Â»Ãƒâ€˜Ã‚Â ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚Â°ÃƒÂÃ‚Âº ÃƒÂÃ‚Â· 0-2 ÃƒÂÃ‚Â·ÃƒÂÃ‚Â¾Ãƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°ÃƒÂÃ‚Â¼Ãƒâ€˜Ã¢â‚¬â€œ. 0 ÃƒÂÃ‚Â·ÃƒÂÃ‚Â¾Ãƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚Â°ÃƒÂÃ‚Âº Ãƒâ€˜Ã¢â‚¬Å¡Ãƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚Â°ÃƒÂÃ‚Â¿ÃƒÂÃ‚Â»Ãƒâ€˜Ã‚ÂÃƒâ€˜Ã…Â½Ãƒâ€˜Ã¢â‚¬Â Ãƒâ€˜Ã…â€™ Ãƒâ€˜Ã¢â‚¬Â¡ÃƒÂÃ‚Â°Ãƒâ€˜Ã‚ÂÃƒâ€˜Ã¢â‚¬Â ÃƒÂÃ‚ÂµÃƒÂÃ‚Â¹; 3 ÃƒÂÃ‚Â·ÃƒÂÃ‚Â¾Ãƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚ÂºÃƒâ€˜Ã¢â‚¬â€œ ÃƒÂÃ‚Â²Ãƒâ€˜Ã¢â‚¬Â¹ÃƒÂÃ‚ÂºÃƒÂÃ‚Â»Ãƒâ€˜Ã…Â½Ãƒâ€˜Ã¢â‚¬Â¡ÃƒÂÃ‚Â°Ãƒâ€˜Ã…Â½Ãƒâ€˜Ã¢â‚¬Â Ãƒâ€˜Ã¢â‚¬Â ÃƒÂÃ‚Â°.",
                        "Muestra recordatorios ponderados para tarjetas con 0-2 estrellas. Las de 0 salen mÃƒÆ’Ã‚Â¡s; las de 3 no salen.",
                        "ÃƒÂÃ…Â¸ÃƒÂÃ‚Â¾ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°ÃƒÂÃ‚Â·Ãƒâ€˜Ã†â€™Ãƒâ€˜Ã¢â‚¬Â ÃƒÂÃ‚Â·ÃƒÂÃ‚Â²ÃƒÂÃ‚Â°ÃƒÂÃ‚Â¶ÃƒÂÃ‚ÂµÃƒÂÃ‚Â½Ãƒâ€˜Ã¢â‚¬â€œ ÃƒÂÃ‚Â½ÃƒÂÃ‚Â°ÃƒÂÃ‚Â³ÃƒÂÃ‚Â°ÃƒÂÃ‚Â´Ãƒâ€˜Ã†â€™ÃƒÂÃ‚Â²ÃƒÂÃ‚Â°ÃƒÂÃ‚Â½ÃƒÂÃ‚Â½Ãƒâ€˜Ã‚Â ÃƒÂÃ‚Â´ÃƒÂÃ‚Â»Ãƒâ€˜Ã‚Â ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚Â¾ÃƒÂÃ‚Âº Ãƒâ€˜Ã¢â‚¬â€œÃƒÂÃ‚Â· 0-2 ÃƒÂÃ‚Â·Ãƒâ€˜Ã¢â‚¬â€œÃƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°ÃƒÂÃ‚Â¼ÃƒÂÃ‚Â¸. 0 ÃƒÂÃ‚Â·Ãƒâ€˜Ã¢â‚¬â€œÃƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚Â¾ÃƒÂÃ‚Âº ÃƒÂÃ‚Â·'Ãƒâ€˜Ã‚ÂÃƒÂÃ‚Â²ÃƒÂÃ‚Â»Ãƒâ€˜Ã‚ÂÃƒâ€˜Ã…Â½Ãƒâ€˜Ã¢â‚¬Å¡Ãƒâ€˜Ã…â€™Ãƒâ€˜Ã‚ÂÃƒâ€˜Ã‚Â ÃƒÂÃ‚Â½ÃƒÂÃ‚Â°ÃƒÂÃ‚Â¹Ãƒâ€˜Ã¢â‚¬Â¡ÃƒÂÃ‚Â°Ãƒâ€˜Ã‚ÂÃƒâ€˜Ã¢â‚¬Å¡Ãƒâ€˜Ã¢â‚¬â€œÃƒâ€˜Ã‹â€ ÃƒÂÃ‚Âµ; 3 ÃƒÂÃ‚Â·Ãƒâ€˜Ã¢â‚¬â€œÃƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚ÂºÃƒÂÃ‚Â¸ ÃƒÂÃ‚Â²ÃƒÂÃ‚Â¸ÃƒÂÃ‚ÂºÃƒÂÃ‚Â»Ãƒâ€˜Ã…Â½Ãƒâ€˜Ã¢â‚¬Â¡ÃƒÂÃ‚ÂµÃƒÂÃ‚Â½Ãƒâ€˜Ã¢â‚¬â€œ.",
                        "ÃƒÂÃ…Â¸ÃƒÂÃ‚Â¾ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°ÃƒÂÃ‚Â·Ãƒâ€˜Ã¢â‚¬Â¹ÃƒÂÃ‚Â²ÃƒÂÃ‚Â°ÃƒÂÃ‚ÂµÃƒâ€˜Ã¢â‚¬Å¡ ÃƒÂÃ‚Â²ÃƒÂÃ‚Â·ÃƒÂÃ‚Â²ÃƒÂÃ‚ÂµÃƒâ€˜Ã‹â€ ÃƒÂÃ‚ÂµÃƒÂÃ‚Â½ÃƒÂÃ‚Â½Ãƒâ€˜Ã¢â‚¬Â¹ÃƒÂÃ‚Âµ ÃƒÂÃ‚Â½ÃƒÂÃ‚Â°ÃƒÂÃ‚Â¿ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â¼ÃƒÂÃ‚Â¸ÃƒÂÃ‚Â½ÃƒÂÃ‚Â°ÃƒÂÃ‚Â½ÃƒÂÃ‚Â¸Ãƒâ€˜Ã‚Â ÃƒÂÃ‚Â´ÃƒÂÃ‚Â»Ãƒâ€˜Ã‚Â ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚Â¾Ãƒâ€˜Ã¢â‚¬Â¡ÃƒÂÃ‚ÂµÃƒÂÃ‚Âº Ãƒâ€˜Ã‚Â 0-2 ÃƒÂÃ‚Â·ÃƒÂÃ‚Â²ÃƒÂÃ‚ÂµÃƒÂÃ‚Â·ÃƒÂÃ‚Â´ÃƒÂÃ‚Â°ÃƒÂÃ‚Â¼ÃƒÂÃ‚Â¸. ÃƒÂÃ…Â¡ÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚Â¾Ãƒâ€˜Ã¢â‚¬Â¡ÃƒÂÃ‚ÂºÃƒÂÃ‚Â¸ Ãƒâ€˜Ã‚Â 0 ÃƒÂÃ‚Â·ÃƒÂÃ‚Â²ÃƒÂÃ‚ÂµÃƒÂÃ‚Â·ÃƒÂÃ‚Â´ÃƒÂÃ‚Â°ÃƒÂÃ‚Â¼ÃƒÂÃ‚Â¸ ÃƒÂÃ‚Â¿ÃƒÂÃ‚Â¾Ãƒâ€˜Ã‚ÂÃƒÂÃ‚Â²ÃƒÂÃ‚Â»Ãƒâ€˜Ã‚ÂÃƒâ€˜Ã…Â½Ãƒâ€˜Ã¢â‚¬Å¡Ãƒâ€˜Ã‚ÂÃƒâ€˜Ã‚Â Ãƒâ€˜Ã¢â‚¬Â¡ÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â‚¬Â°ÃƒÂÃ‚Âµ ÃƒÂÃ‚Â²Ãƒâ€˜Ã‚ÂÃƒÂÃ‚ÂµÃƒÂÃ‚Â³ÃƒÂÃ‚Â¾; 3 ÃƒÂÃ‚Â·ÃƒÂÃ‚Â²ÃƒÂÃ‚ÂµÃƒÂÃ‚Â·ÃƒÂÃ‚Â´Ãƒâ€˜Ã¢â‚¬Â¹ ÃƒÂÃ‚Â¸Ãƒâ€˜Ã‚ÂÃƒÂÃ‚ÂºÃƒÂÃ‚Â»Ãƒâ€˜Ã…Â½Ãƒâ€˜Ã¢â‚¬Â¡ÃƒÂÃ‚ÂµÃƒÂÃ‚Â½Ãƒâ€˜Ã¢â‚¬Â¹.",
                        "Pokazuje waÃƒâ€¦Ã‚Â¼one przypomnienia dla kart z 0-2 gwiazdkami. 0 gwiazdek pojawia siÃƒâ€žÃ¢â€žÂ¢ najczÃƒâ€žÃ¢â€žÂ¢Ãƒâ€¦Ã¢â‚¬Âºciej; 3 gwiazdki sÃƒâ€žÃ¢â‚¬Â¦ wykluczone."
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = notificationIntervalDraft,
                    onValueChange = onNotificationIntervalChange,
                    label = { Text(st("Interval minutes", "Intervall in Minuten", "ÃƒÂÃ¢â‚¬Â ÃƒÂÃ‚Â½Ãƒâ€˜Ã¢â‚¬Å¡Ãƒâ€˜Ã‚ÂÃƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚Â²ÃƒÂÃ‚Â°ÃƒÂÃ‚Â» Ãƒâ€˜Ã†â€™ Ãƒâ€˜Ã¢â‚¬Â¦ÃƒÂÃ‚Â²Ãƒâ€˜Ã¢â‚¬â€œÃƒÂÃ‚Â»Ãƒâ€˜Ã¢â‚¬â€œÃƒÂÃ‚Â½ÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â‚¬Â¦", "Intervalo en minutos", "ÃƒÂÃ¢â‚¬Â ÃƒÂÃ‚Â½Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚ÂµÃƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚Â²ÃƒÂÃ‚Â°ÃƒÂÃ‚Â» Ãƒâ€˜Ã†â€™ Ãƒâ€˜Ã¢â‚¬Â¦ÃƒÂÃ‚Â²ÃƒÂÃ‚Â¸ÃƒÂÃ‚Â»ÃƒÂÃ‚Â¸ÃƒÂÃ‚Â½ÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â‚¬Â¦", "ÃƒÂÃ‹Å“ÃƒÂÃ‚Â½Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚ÂµÃƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚Â²ÃƒÂÃ‚Â°ÃƒÂÃ‚Â» ÃƒÂÃ‚Â² ÃƒÂÃ‚Â¼ÃƒÂÃ‚Â¸ÃƒÂÃ‚Â½Ãƒâ€˜Ã†â€™Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â‚¬Â¦", "InterwaÃƒâ€¦Ã¢â‚¬Å¡ w minutach")) },
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
                    label = { Text(st("Maximum active notifications", "Maximale aktive Benachrichtigungen", "ÃƒÂÃ…â€œÃƒÂÃ‚Â°ÃƒÂÃ‚ÂºÃƒâ€˜Ã‚ÂÃƒâ€˜Ã¢â‚¬â€œÃƒÂÃ‚Â¼Ãƒâ€˜Ã†â€™ÃƒÂÃ‚Â¼ ÃƒÂÃ‚Â°ÃƒÂÃ‚ÂºÃƒâ€˜Ã¢â‚¬Å¡Ãƒâ€˜Ã¢â‚¬Â¹Ãƒâ€˜Ã…Â¾ÃƒÂÃ‚Â½Ãƒâ€˜Ã¢â‚¬Â¹Ãƒâ€˜Ã¢â‚¬Â¦ ÃƒÂÃ‚Â°ÃƒÂÃ‚Â¿ÃƒÂÃ‚Â°ÃƒÂÃ‚Â²Ãƒâ€˜Ã‚ÂÃƒâ€˜Ã‹â€ Ãƒâ€˜Ã¢â‚¬Â¡Ãƒâ€˜Ã‚ÂÃƒÂÃ‚Â½ÃƒÂÃ‚Â½Ãƒâ€˜Ã‚ÂÃƒâ€˜Ã…Â¾", "MÃƒÆ’Ã‚Â¡ximo de notificaciones activas", "ÃƒÂÃ…â€œÃƒÂÃ‚Â°ÃƒÂÃ‚ÂºÃƒâ€˜Ã‚ÂÃƒÂÃ‚Â¸ÃƒÂÃ‚Â¼Ãƒâ€˜Ã†â€™ÃƒÂÃ‚Â¼ ÃƒÂÃ‚Â°ÃƒÂÃ‚ÂºÃƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚Â¸ÃƒÂÃ‚Â²ÃƒÂÃ‚Â½ÃƒÂÃ‚Â¸Ãƒâ€˜Ã¢â‚¬Â¦ Ãƒâ€˜Ã‚ÂÃƒÂÃ‚Â¿ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â²Ãƒâ€˜Ã¢â‚¬â€œÃƒâ€˜Ã¢â‚¬Â°ÃƒÂÃ‚ÂµÃƒÂÃ‚Â½Ãƒâ€˜Ã…â€™", "ÃƒÂÃ…â€œÃƒÂÃ‚Â°ÃƒÂÃ‚ÂºÃƒâ€˜Ã‚ÂÃƒÂÃ‚Â¸ÃƒÂÃ‚Â¼Ãƒâ€˜Ã†â€™ÃƒÂÃ‚Â¼ ÃƒÂÃ‚Â°ÃƒÂÃ‚ÂºÃƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚Â¸ÃƒÂÃ‚Â²ÃƒÂÃ‚Â½Ãƒâ€˜Ã¢â‚¬Â¹Ãƒâ€˜Ã¢â‚¬Â¦ Ãƒâ€˜Ã†â€™ÃƒÂÃ‚Â²ÃƒÂÃ‚ÂµÃƒÂÃ‚Â´ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â¼ÃƒÂÃ‚Â»ÃƒÂÃ‚ÂµÃƒÂÃ‚Â½ÃƒÂÃ‚Â¸ÃƒÂÃ‚Â¹", "Maksimum aktywnych powiadomieÃƒâ€¦Ã¢â‚¬Å¾")) },
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
                        "Android kann Hintergrundaufgaben verzÃƒÆ’Ã‚Â¶gert ausfÃƒÆ’Ã‚Â¼hren, besonders wenn das Telefon inaktiv ist.",
                        "Android ÃƒÂÃ‚Â¼ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â¶ÃƒÂÃ‚Â° ÃƒÂÃ‚Â·ÃƒÂÃ‚Â°ÃƒÂÃ‚Â¿Ãƒâ€˜Ã†â€™Ãƒâ€˜Ã‚ÂÃƒÂÃ‚ÂºÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â‚¬Â Ãƒâ€˜Ã…â€™ Ãƒâ€˜Ã¢â‚¬Å¾ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â½ÃƒÂÃ‚Â°ÃƒÂÃ‚Â²Ãƒâ€˜Ã¢â‚¬Â¹Ãƒâ€˜Ã‚Â ÃƒÂÃ‚Â·ÃƒÂÃ‚Â°ÃƒÂÃ‚Â´ÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â‚¬Â¡Ãƒâ€˜Ã¢â‚¬Â¹ ÃƒÂÃ‚Â· ÃƒÂÃ‚Â·ÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â‚¬Å¡Ãƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã¢â‚¬Â¹ÃƒÂÃ‚Â¼ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°ÃƒÂÃ‚Â¹, ÃƒÂÃ‚Â°Ãƒâ€˜Ã‚ÂÃƒÂÃ‚Â°ÃƒÂÃ‚Â±ÃƒÂÃ‚Â»Ãƒâ€˜Ã¢â‚¬â€œÃƒÂÃ‚Â²ÃƒÂÃ‚Â° ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°ÃƒÂÃ‚Â»Ãƒâ€˜Ã¢â‚¬â€œ Ãƒâ€˜Ã¢â‚¬Å¡Ãƒâ€˜Ã‚ÂÃƒÂÃ‚Â»ÃƒÂÃ‚ÂµÃƒâ€˜Ã¢â‚¬Å¾ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â½ ÃƒÂÃ‚Â½ÃƒÂÃ‚ÂµÃƒÂÃ‚Â°ÃƒÂÃ‚ÂºÃƒâ€˜Ã¢â‚¬Å¡Ãƒâ€˜Ã¢â‚¬Â¹Ãƒâ€˜Ã…Â¾ÃƒÂÃ‚Â½Ãƒâ€˜Ã¢â‚¬Â¹.",
                        "Android puede retrasar tareas en segundo plano, sobre todo cuando el telÃƒÆ’Ã‚Â©fono estÃƒÆ’Ã‚Â¡ inactivo.",
                        "Android ÃƒÂÃ‚Â¼ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â¶ÃƒÂÃ‚Âµ ÃƒÂÃ‚Â·ÃƒÂÃ‚Â°ÃƒÂÃ‚Â¿Ãƒâ€˜Ã†â€™Ãƒâ€˜Ã‚ÂÃƒÂÃ‚ÂºÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚Â¸ Ãƒâ€˜Ã¢â‚¬Å¾ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â½ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â²Ãƒâ€˜Ã¢â‚¬â€œ ÃƒÂÃ‚Â·ÃƒÂÃ‚Â°ÃƒÂÃ‚Â´ÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â‚¬Â¡Ãƒâ€˜Ã¢â‚¬â€œ Ãƒâ€˜Ã¢â‚¬â€œÃƒÂÃ‚Â· ÃƒÂÃ‚Â·ÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â‚¬Å¡Ãƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚Â¸ÃƒÂÃ‚Â¼ÃƒÂÃ‚ÂºÃƒÂÃ‚Â¾Ãƒâ€˜Ã…Â½, ÃƒÂÃ‚Â¾Ãƒâ€˜Ã‚ÂÃƒÂÃ‚Â¾ÃƒÂÃ‚Â±ÃƒÂÃ‚Â»ÃƒÂÃ‚Â¸ÃƒÂÃ‚Â²ÃƒÂÃ‚Â¾ ÃƒÂÃ‚ÂºÃƒÂÃ‚Â¾ÃƒÂÃ‚Â»ÃƒÂÃ‚Â¸ Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚ÂµÃƒÂÃ‚Â»ÃƒÂÃ‚ÂµÃƒâ€˜Ã¢â‚¬Å¾ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â½ ÃƒÂÃ‚Â½ÃƒÂÃ‚ÂµÃƒÂÃ‚Â°ÃƒÂÃ‚ÂºÃƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚Â¸ÃƒÂÃ‚Â²ÃƒÂÃ‚Â½ÃƒÂÃ‚Â¸ÃƒÂÃ‚Â¹.",
                        "Android ÃƒÂÃ‚Â¼ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â¶ÃƒÂÃ‚ÂµÃƒâ€˜Ã¢â‚¬Å¡ ÃƒÂÃ‚Â·ÃƒÂÃ‚Â°ÃƒÂÃ‚Â¿Ãƒâ€˜Ã†â€™Ãƒâ€˜Ã‚ÂÃƒÂÃ‚ÂºÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â‚¬Å¡Ãƒâ€˜Ã…â€™ Ãƒâ€˜Ã¢â‚¬Å¾ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â½ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â²Ãƒâ€˜Ã¢â‚¬Â¹ÃƒÂÃ‚Âµ ÃƒÂÃ‚Â·ÃƒÂÃ‚Â°ÃƒÂÃ‚Â´ÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â‚¬Â¡ÃƒÂÃ‚Â¸ Ãƒâ€˜Ã‚Â ÃƒÂÃ‚Â·ÃƒÂÃ‚Â°ÃƒÂÃ‚Â´ÃƒÂÃ‚ÂµÃƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚Â¶ÃƒÂÃ‚ÂºÃƒÂÃ‚Â¾ÃƒÂÃ‚Â¹, ÃƒÂÃ‚Â¾Ãƒâ€˜Ã‚ÂÃƒÂÃ‚Â¾ÃƒÂÃ‚Â±ÃƒÂÃ‚ÂµÃƒÂÃ‚Â½ÃƒÂÃ‚Â½ÃƒÂÃ‚Â¾ ÃƒÂÃ‚ÂºÃƒÂÃ‚Â¾ÃƒÂÃ‚Â³ÃƒÂÃ‚Â´ÃƒÂÃ‚Â° Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚ÂµÃƒÂÃ‚Â»ÃƒÂÃ‚ÂµÃƒâ€˜Ã¢â‚¬Å¾ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â½ ÃƒÂÃ‚Â½ÃƒÂÃ‚ÂµÃƒÂÃ‚Â°ÃƒÂÃ‚ÂºÃƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚Â¸ÃƒÂÃ‚Â²ÃƒÂÃ‚ÂµÃƒÂÃ‚Â½.",
                        "Android moÃƒâ€¦Ã‚Â¼e opÃƒÆ’Ã‚Â³Ãƒâ€¦Ã‚ÂºniaÃƒâ€žÃ¢â‚¬Â¡ pracÃƒâ€žÃ¢â€žÂ¢ w tle, szczegÃƒÆ’Ã‚Â³lnie gdy telefon jest bezczynny."
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
                    text = st("Version log", "Versionslog", "ÃƒÂÃ¢â‚¬ÂºÃƒÂÃ‚Â¾ÃƒÂÃ‚Â³ ÃƒÂÃ‚Â²ÃƒÂÃ‚ÂµÃƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã‚ÂÃƒâ€˜Ã¢â‚¬â€œÃƒÂÃ‚Â¹", "Registro de versiones", "ÃƒÂÃ¢â‚¬ÂºÃƒÂÃ‚Â¾ÃƒÂÃ‚Â³ ÃƒÂÃ‚Â²ÃƒÂÃ‚ÂµÃƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã‚ÂÃƒâ€˜Ã¢â‚¬â€œÃƒÂÃ‚Â¹", "ÃƒÂÃ¢â‚¬ÂºÃƒÂÃ‚Â¾ÃƒÂÃ‚Â³ ÃƒÂÃ‚Â²ÃƒÂÃ‚ÂµÃƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã‚ÂÃƒÂÃ‚Â¸ÃƒÂÃ‚Â¹", "Historia wersji"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                versionLogText().lines().forEach { line ->
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
    "German" to "DE - Deutsch"
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
        "DE" -> "de-DE"
        "ES" -> "es-ES"
        else -> null
    }
}

private fun nativeLanguageLabel(language: String): String {
    return DictionaryLanguageOptions.firstOrNull { it.first.equals(language, ignoreCase = true) }?.second
        ?: language.ifBlank { "Language" }
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
    onWorkModeChange: (WorkMode) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        WorkMode.values().forEach { mode ->
            Surface(
                modifier = Modifier.size(38.dp).clickable { onWorkModeChange(mode) },
                shape = RoundedCornerShape(13.dp),
                border = BorderStroke(1.dp, if (workMode == mode) BrandSaladColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                color = if (workMode == mode) BrandSaladColor.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surface
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = mode.icon(),
                        contentDescription = mode.label(),
                        modifier = Modifier.size(20.dp),
                        tint = if (workMode == mode) Color(0xFF234231) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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
    OnboardingLanguageOption("be", "Belarusian", "\uD83C\uDDE7\uD83C\uDDFE BY - беларуская"),
    OnboardingLanguageOption("en", "English", "\uD83C\uDDEC\uD83C\uDDE7 EN - English"),
    OnboardingLanguageOption("de", "German", "\uD83C\uDDE9\uD83C\uDDEA DE - Deutsch"),
    OnboardingLanguageOption("pl", "Polish", "\uD83C\uDDF5\uD83C\uDDF1 PL - polski"),
    OnboardingLanguageOption("ru", "Russian", "\uD83C\uDDF7\uD83C\uDDFA RU - русский"),
    OnboardingLanguageOption("es", "Spanish", "\uD83C\uDDEA\uD83C\uDDF8 ES - español"),
    OnboardingLanguageOption("uk", "Ukrainian", "\uD83C\uDDFA\uD83C\uDDE6 UA - українська")
)

@Composable
private fun OnboardingDialog(
    interfaceLanguage: String,
    defaultKnownLanguage: String,
    defaultLearningLanguage: String,
    onComplete: (String, String, String, String) -> Unit
) {
    val initialInterface = OnboardingLanguages.firstOrNull { it.code == interfaceLanguage }
        ?: OnboardingLanguages.first { it.code == "en" }
    val systemKnown = OnboardingLanguages.firstOrNull { it.code == interfaceLanguage }?.storageName ?: "English"
    var selectedInterface by remember { mutableStateOf(initialInterface) }
    var knownLanguage by remember {
        mutableStateOf(
            OnboardingLanguages.firstOrNull { it.storageName.equals(defaultKnownLanguage, ignoreCase = true) }
                ?: OnboardingLanguages.firstOrNull { it.storageName == systemKnown }
                ?: OnboardingLanguages.first { it.storageName == "English" }
        )
    }
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
                OnboardingDropdown(copy.interfaceLanguage, selectedInterface) { selectedInterface = it }
                OnboardingDropdown(copy.knownLanguage, knownLanguage) {
                    knownLanguage = it
                    explanationLanguage = it
                }
                OnboardingDropdown(copy.learningLanguage, learningLanguage) { learningLanguage = it }
                OnboardingDropdown(copy.explanationLanguage, explanationLanguage) { explanationLanguage = it }
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
            title = "MurrLex вітае",
            guide = "Кароткі старт: выберы мову інтэрфейсу, сваю мову, мову навучання і мову тлумачэнняў.",
            interfaceLanguage = "Мова інтэрфейсу",
            knownLanguage = "Ваша мова",
            learningLanguage = "Мова, якую вывучаеце",
            explanationLanguage = "Мова тлумачэнняў",
            start = "Пачаць"
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
            title = "MurrLex вітає",
            guide = "Короткий старт: оберіть мову інтерфейсу, вашу мову, мову навчання і мову пояснень.",
            interfaceLanguage = "Мова інтерфейсу",
            knownLanguage = "Ваша мова",
            learningLanguage = "Мова, яку вивчаєте",
            explanationLanguage = "Мова пояснень",
            start = "Почати"
        )
        "ru" -> OnboardingCopy(
            title = "MurrLex приветствует",
            guide = "Краткий старт: выберите язык интерфейса, ваш язык, язык, который изучаете, и язык объяснений.",
            interfaceLanguage = "Язык интерфейса",
            knownLanguage = "Ваш язык",
            learningLanguage = "Язык, который вы изучаете",
            explanationLanguage = "Язык объяснений",
            start = "Начать"
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
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

private fun offlineSpeechLocalizedName(tag: String, interfaceLanguage: String): String {
    val key = tag.substringBefore("-").lowercase(Locale.ROOT)
    return when (interfaceLanguage.lowercase(Locale.ROOT)) {
        "ru" -> when (key) {
            "en" -> "Английский"
            "pl" -> "Польский"
            "ru" -> "Русский"
            "de" -> "Немецкий"
            "es" -> "Испанский"
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
            "en" -> "Англійська"
            "pl" -> "Польська"
            "ru" -> "Російська"
            "de" -> "Німецька"
            "es" -> "Іспанська"
            else -> tag
        }
        "be", "by" -> when (key) {
            "en" -> "Англійская"
            "pl" -> "Польская"
            "ru" -> "Руская"
            "de" -> "Нямецкая"
            "es" -> "Іспанская"
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
    onVoiceInput: () -> Unit
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
                title = nativeLanguageLabel(state.quickVocabularySourceLanguage),
                text = state.translationOutput,
                flipped = true,
                modifier = Modifier.weight(1f),
                attribution = state.translationOutput.isNotBlank(),
                onSpeak = onSpeakTranslation,
                borderColor = translationBorderColor,
                borderHighlighted = translationPulse,
                onClick = { splitResultExpanded = !splitResultExpanded }
            )
            if (!splitResultExpanded) {
                SplitTranslationPanel(
                    title = nativeLanguageLabel(state.quickVocabularyTargetLanguage),
                    text = state.translationInput,
                    flipped = false,
                    editable = true,
                    onValueChange = onInputChange,
                    modifier = Modifier.weight(1f),
                    onSpeak = onSpeakInput
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
                        title = nativeLanguageLabel(state.quickVocabularySourceLanguage),
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
                            Text(
                                text = "Powered by Google Translator",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                            )
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
                        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            LanguageSpeakerRow(
                                title = nativeLanguageLabel(state.quickVocabularyTargetLanguage),
                                enabled = state.translationInput.isNotBlank(),
                                onSpeak = onSpeakInput
                            )
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
                    value = state.quickVocabularyTargetLanguage,
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
                    value = state.quickVocabularySourceLanguage,
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
    attribution: Boolean = false,
    onSpeak: (() -> Unit)? = null,
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (flipped) PaddingValues(bottom = 42.dp) else PaddingValues(top = 42.dp))
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
                            if (attribution) {
                                Text(
                                    text = "Powered by Google Translator",
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
    val choices = remember(card.id, card.expectedAnswerText(isBackVisible), cards.map { it.id to it.expectedAnswerText(isBackVisible) }) {
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
                        selected && normalizeAnswerText(choice) == normalizeAnswerText(card.expectedAnswerText(isBackVisible)) -> BrandSaladColor
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
    val distractors = cards
        .filterNot { it.id == card.id }
        .map { it.expectedAnswerText(isBackVisible).ifBlank { it.correctText().ifBlank { it.nativeText() } } }
        .filter { it.isNotBlank() && normalizeAnswerText(it) != normalizeAnswerText(correct) }
        .distinctBy { normalizeAnswerText(it) }
        .shuffled(Random(card.id + cards.size + correct.hashCode()))
        .take(3)
    val fallback = listOf("I am not sure", "Review later", "Skip this one")
        .filter { normalizeAnswerText(it) != normalizeAnswerText(correct) && distractors.none { d -> normalizeAnswerText(d) == normalizeAnswerText(it) } }
    return (listOf(correct) + distractors + fallback)
        .distinctBy { normalizeAnswerText(it) }
        .take(4)
        .shuffled(Random(System.nanoTime()))
}
@Composable
private fun LessonCatalogScreen(
    lessons: List<Lesson>,
    selectedLessonIds: Set<String>,
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
            .pointerInput(configuringLessonId) {
                detectTapGestures(onTap = { if (configuringLessonId != null) configuringLessonId = null })
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

        val selectedLessons = lessons.filter { it.id in selectedLessonIds }
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
            WorkModeToggleRow(workMode = workMode, onWorkModeChange = onWorkModeChange)
        }

        if (lessons.isEmpty()) {
            EmptyState("No lessons yet. Import JSON files or create a lesson.")
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
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
                            val index = lessons.indexOfFirst { it.id == lesson.id }
                            if (index > 0) {
                                onMoveLesson(lesson.id, lessons[index - 1].id)
                                onSaveLessonOrder()
                            }
                        },
                        onMoveDown = {
                            val index = lessons.indexOfFirst { it.id == lesson.id }
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
                    language = quickVocabularyTargetLanguage,
                    contentDescription = "Recognition language",
                    onTap = onSwapQuickVocabularyLanguages,
                    onLanguageChange = onQuickVocabularyTargetChange
                )
                Icon(
                    Icons.Default.Mic,
                    contentDescription = "Add vocabulary by voice",
                    tint = BrandSaladColor,
                    modifier = Modifier.size(30.dp)
                )
                QuickVocabularyLanguageCode(
                    language = quickVocabularySourceLanguage,
                    contentDescription = "Translation language",
                    onTap = onSwapQuickVocabularyLanguages,
                    onLanguageChange = onQuickVocabularySourceChange
                )
            }
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
                .height(150.dp)
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
    onToggleCard: () -> Unit,
    onToggleStar: (Int, Int) -> Unit,
    onQuickEditCard: () -> Unit,
    onEditCard: () -> Unit,
    quickEditMode: Boolean,
    showCardLog: Boolean,
    onTestAnswer: (String) -> Unit,
    onShareCard: (Lesson, Flashcard) -> Unit
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
                                        val canFlipTestCard = state.workMode != WorkMode.TESTS ||
                        animatedCard?.id in state.completedCardIds ||
                        animatedCard?.hasEmptySide() == true
StudyCard(
                        card = animatedCard,
                        isBackVisible = state.isBackVisible,
                        onClick = if (canFlipTestCard) onToggleCard else ({}),
                        onToggleStar = onToggleStar,
                        onQuickEditCard = onQuickEditCard,
                        onEditCard = onEditCard,
                        quickEditMode = quickEditMode,
                        displayTextSize = effectiveDisplayTextSize,
                        controlSize = state.controlSize,
                        isCompleted = animatedCard?.id in state.completedCardIds,
                        positionLabel = animatedCard.sideLanguageCode(state.isBackVisible, state.selectedLesson),
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
    onSwipePrevious: () -> Unit,
    onSwipeNext: () -> Unit,
    showCardLog: Boolean,
    onShareCard: (Flashcard) -> Unit,
    modifier: Modifier = Modifier
) {
    var showRule by remember { mutableStateOf(false) }
    var showMistakes by remember { mutableStateOf(false) }
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
    if (showCardLog && showMistakes && card != null) {
        CardTextDialog(
            title = "Mistakes and card log",
            text = buildMistakesAndLog(card),
            onDismiss = { showMistakes = false }
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
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (showingFront) frontColor else backColor
            ),
            border = if (isCompleted) BorderStroke(4.dp, CompletedFrameColor) else null,
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
                Text(
                    text = positionLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f),
                    modifier = Modifier.align(Alignment.TopStart)
                )

                Row(
                    modifier = Modifier.align(Alignment.TopEnd),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (showCardLog) {
                        CircleTextButton(text = "M", onClick = { showMistakes = true })
                    }
                    CircleTextButton(text = "?", onClick = { showRule = true })
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color.White.copy(alpha = 0.92f))
                            .pointerInput(card?.id, quickEditMode) {
                                detectTapGestures(
                                    onTap = { if (card != null) onQuickEditCard() },
                                    onLongPress = { if (card != null) onEditCard() }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit card", tint = Color(0xFF111111))
                    }
                                    IconButton(
                        onClick = { card?.let(onShareCard) },
                        enabled = card != null,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color.White.copy(alpha = 0.92f))
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share card JSON", tint = Color(0xFF111111))
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
                                text = card?.frontDisplayLabel().orEmpty(),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = card?.let { flashcard ->
                                    if (flashcard.kindCode() == "LN") {
                                        flashcard.frontDisplayText()
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
private fun CircleTextButton(text: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.92f))
    ) {
        Text(text = text, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color(0xFF111111))
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

private fun UiText.kindText(kindCode: String): String = if (kindCode == "LN") "Lesson" else "Mistakes"

private fun UiText.mixedKindText(): String = "Mixed"

private fun UiText.questionsText(count: Int): String = "$count questions"

private fun UiText.doneText(done: Int, total: Int): String = "$done of $total done"

private fun UiText.completedText(count: Int): String = "Completed $count times"

private fun Flashcard.displayedCardText(isBackVisible: Boolean): String {
    return if (isBackVisible) {
        correctText()
    } else if (kindCode() == "LN") {
        frontDisplayText()
    } else {
        mistakeText().ifBlank { nativeText().ifBlank { correctText() } }
    }
}

private fun Flashcard?.sideLanguageCode(isBackVisible: Boolean, lesson: Lesson? = null): String {
    if (this != null && kindCode() != "LN") return "Mistake"
    val language = if (isBackVisible) {
        this?.targetLanguage.asLessonLanguage()
            ?: lesson?.targetLanguage.asLessonLanguage()
            ?: this?.backLabel()
    } else {
        this?.sourceLanguage.asLessonLanguage()
            ?: lesson?.sourceLanguage.asLessonLanguage()
            ?: this?.frontLabel()
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

private fun Flashcard.frontDisplayLabel(): String {
    return frontLabel()
}

private fun Flashcard.frontDisplayText(): String {
    return nativeText()
}

private fun Flashcard.hasPendingNativeTranslation(): Boolean {
    if (kindCode() != "LN" || correctText().isBlank()) return false
    val native = nativeText().trim()
    return native.isBlank() || native.isEmptyPlaceholder() || native.contains("translation pending", ignoreCase = true)
}

private fun String.isEmptyPlaceholder(): Boolean = trim().equals("Empty", ignoreCase = true)

private fun Flashcard.hasEmptySide(): Boolean = nativeText().isEmptyPlaceholder() || correctText().isEmptyPlaceholder()

private fun Flashcard.answerInputLabel(isBackVisible: Boolean): String {
    return when {
        nativeText().isEmptyPlaceholder() -> sourceLanguage.ifBlank { frontLabel() }
        correctText().isEmptyPlaceholder() -> targetLanguage.ifBlank { backLabel() }
        isBackVisible -> frontLabel()
        else -> backLabel()
    }
}

private fun Flashcard?.speechLanguageTag(interfaceLanguage: String, lesson: Lesson? = null): String {
    if (this == null) return interfaceLanguage.speechLanguageTagFromName()
        ?: Locale.getDefault().toLanguageTag()

    (targetLanguage.asLessonLanguage() ?: lesson?.targetLanguage.asLessonLanguage())
        ?.speechLanguageTagFromName()
        ?.let { return it }
    if (kindCode() == "LN") {
        backLabel().speechLanguageTagFromName()?.let { return it }
    }
    correctText().speechLanguageTagFromText()?.let { return it }
    (sourceLanguage.asLessonLanguage() ?: lesson?.sourceLanguage.asLessonLanguage())
        ?.speechLanguageTagFromName()
        ?.let { return it }
    return interfaceLanguage.speechLanguageTagFromName()
        ?: Locale.getDefault().toLanguageTag()
}

private fun Flashcard.speechLanguageTagForSide(isBackVisible: Boolean, interfaceLanguage: String, lesson: Lesson? = null): String {
    val explicitLanguage = if (isBackVisible || hasPendingNativeTranslation()) {
        targetLanguage.asLessonLanguage() ?: lesson?.targetLanguage.asLessonLanguage() ?: backLabel()
    } else if (kindCode() == "LN") {
        sourceLanguage.asLessonLanguage() ?: lesson?.sourceLanguage.asLessonLanguage() ?: frontLabel()
    } else {
        targetLanguage.asLessonLanguage() ?: lesson?.targetLanguage.asLessonLanguage()
            ?: sourceLanguage.asLessonLanguage() ?: lesson?.sourceLanguage.asLessonLanguage().orEmpty()
    }
    explicitLanguage.speechLanguageTagFromName()?.let { return it }
    displayedCardText(isBackVisible).speechLanguageTagFromText()?.let { return it }
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
        else -> "System language"
    }
}
private fun String.speechLanguageTagFromName(): String? {
    val normalized = trim().lowercase(Locale.ROOT)
    return when {
        normalized in listOf("en", "eng", "english", "ÃƒÂÃ‚Â°ÃƒÂÃ‚Â½ÃƒÂÃ‚Â³ÃƒÂÃ‚Â»ÃƒÂÃ‚Â¸ÃƒÂÃ‚Â¹Ãƒâ€˜Ã‚ÂÃƒÂÃ‚ÂºÃƒÂÃ‚Â¸ÃƒÂÃ‚Â¹", "ÃƒÂÃ‚Â°ÃƒÂÃ‚Â½ÃƒÂÃ‚Â³ÃƒÂÃ‚Â»Ãƒâ€˜Ã¢â‚¬â€œÃƒÂÃ‚Â¹Ãƒâ€˜Ã‚ÂÃƒÂÃ‚ÂºÃƒÂÃ‚Â°Ãƒâ€˜Ã‚Â", "angielski") -> "en-US"
        normalized in listOf("de", "deu", "ger", "german", "deutsch", "ÃƒÂÃ‚Â½ÃƒÂÃ‚ÂµÃƒÂÃ‚Â¼ÃƒÂÃ‚ÂµÃƒâ€˜Ã¢â‚¬Â ÃƒÂÃ‚ÂºÃƒÂÃ‚Â¸ÃƒÂÃ‚Â¹", "ÃƒÂÃ‚Â½Ãƒâ€˜Ã‚ÂÃƒÂÃ‚Â¼ÃƒÂÃ‚ÂµÃƒâ€˜Ã¢â‚¬Â ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°Ãƒâ€˜Ã‚Â", "niemiecki") -> "de-DE"
        normalized in listOf("be", "by", "belarusian", "belaruska", "ÃƒÂÃ‚Â±ÃƒÂÃ‚ÂµÃƒÂÃ‚Â»ÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã†â€™Ãƒâ€˜Ã‚ÂÃƒÂÃ‚ÂºÃƒÂÃ‚Â°Ãƒâ€˜Ã‚Â", "ÃƒÂÃ‚Â±ÃƒÂÃ‚ÂµÃƒÂÃ‚Â»ÃƒÂÃ‚Â¾Ãƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã†â€™Ãƒâ€˜Ã‚ÂÃƒâ€˜Ã‚ÂÃƒÂÃ‚ÂºÃƒÂÃ‚Â¸ÃƒÂÃ‚Â¹", "ÃƒÂÃ‚Â±ÃƒÂÃ‚ÂµÃƒÂÃ‚Â»ÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã†â€™Ãƒâ€˜Ã‚ÂÃƒâ€˜Ã‚ÂÃƒÂÃ‚ÂºÃƒÂÃ‚Â¸ÃƒÂÃ‚Â¹") -> "be-BY"
        normalized in listOf("es", "spa", "spanish", "espanol", "espaÃƒÆ’Ã‚Â±ol", "ÃƒÂÃ‚Â¸Ãƒâ€˜Ã‚ÂÃƒÂÃ‚Â¿ÃƒÂÃ‚Â°ÃƒÂÃ‚Â½Ãƒâ€˜Ã‚ÂÃƒÂÃ‚ÂºÃƒÂÃ‚Â¸ÃƒÂÃ‚Â¹", "Ãƒâ€˜Ã¢â‚¬â€œÃƒâ€˜Ã‚ÂÃƒÂÃ‚Â¿ÃƒÂÃ‚Â°ÃƒÂÃ‚Â½Ãƒâ€˜Ã‚ÂÃƒÂÃ‚ÂºÃƒÂÃ‚Â°Ãƒâ€˜Ã‚Â") -> "es-ES"
        normalized in listOf("uk", "ua", "ukrainian", "Ãƒâ€˜Ã†â€™ÃƒÂÃ‚ÂºÃƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚Â°ÃƒÂÃ‚Â¸ÃƒÂÃ‚Â½Ãƒâ€˜Ã‚ÂÃƒÂÃ‚ÂºÃƒÂÃ‚Â¸ÃƒÂÃ‚Â¹", "Ãƒâ€˜Ã†â€™ÃƒÂÃ‚ÂºÃƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â‚¬â€ÃƒÂÃ‚Â½Ãƒâ€˜Ã‚ÂÃƒâ€˜Ã…â€™ÃƒÂÃ‚ÂºÃƒÂÃ‚Â°", "Ãƒâ€˜Ã†â€™ÃƒÂÃ‚ÂºÃƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â‚¬â€œÃƒÂÃ‚Â½Ãƒâ€˜Ã‚ÂÃƒÂÃ‚ÂºÃƒÂÃ‚Â°Ãƒâ€˜Ã‚Â") -> "uk-UA"
        normalized in listOf("ru", "rus", "russian", "Ãƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã†â€™Ãƒâ€˜Ã‚ÂÃƒâ€˜Ã‚ÂÃƒÂÃ‚ÂºÃƒÂÃ‚Â¸ÃƒÂÃ‚Â¹", "Ãƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã†â€™Ãƒâ€˜Ã‚ÂÃƒÂÃ‚ÂºÃƒÂÃ‚Â°Ãƒâ€˜Ã‚Â", "rosyjski") -> "ru-RU"
        normalized in listOf("pl", "pol", "polish", "polski", "ÃƒÂÃ‚Â¿ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â»Ãƒâ€˜Ã…â€™Ãƒâ€˜Ã‚ÂÃƒÂÃ‚ÂºÃƒÂÃ‚Â¸ÃƒÂÃ‚Â¹", "ÃƒÂÃ‚Â¿ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â»Ãƒâ€˜Ã…â€™Ãƒâ€˜Ã‚ÂÃƒÂÃ‚ÂºÃƒÂÃ‚Â°Ãƒâ€˜Ã‚Â") -> "pl-PL"
        else -> null
    }
}

private fun String.speechLanguageTagFromText(): String? {
    val text = lowercase(Locale.ROOT)
    return when {
        text.any { it in "Ãƒâ€˜Ã…Â¾Ãƒâ€˜Ã¢â‚¬â€œ" } -> "be-BY"
        text.any { it in "Ãƒâ€˜Ã¢â‚¬â€œÃƒâ€˜Ã¢â‚¬â€Ãƒâ€˜Ã¢â‚¬ÂÃƒâ€™Ã¢â‚¬Ëœ" } -> "uk-UA"
        text.any { it in "Ãƒâ€žÃ¢â‚¬Â¦Ãƒâ€žÃ¢â‚¬Â¡Ãƒâ€žÃ¢â€žÂ¢Ãƒâ€¦Ã¢â‚¬Å¡Ãƒâ€¦Ã¢â‚¬Å¾ÃƒÆ’Ã‚Â³Ãƒâ€¦Ã¢â‚¬ÂºÃƒâ€¦Ã‚ÂºÃƒâ€¦Ã‚Â¼" } -> "pl-PL"
        text.any { it in "ÃƒÆ’Ã‚Â¤ÃƒÆ’Ã‚Â¶ÃƒÆ’Ã‚Â¼ÃƒÆ’Ã…Â¸" } -> "de-DE"
        text.any { it in "ÃƒÆ’Ã‚Â¡ÃƒÆ’Ã‚Â©ÃƒÆ’Ã‚Â­ÃƒÆ’Ã‚Â±ÃƒÆ’Ã‚Â³ÃƒÆ’Ã‚ÂºÃƒÆ’Ã‚Â¼Ãƒâ€šÃ‚Â¿Ãƒâ€šÃ‚Â¡" } -> "es-ES"
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
    return "Native value:\n${card.nativeText().ifBlank { "Not specified" }}\n\nMake it right:\n${card.correctText().ifBlank { "Not specified" }}\n\nRule:\n$rule"
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

private fun formatVoiceElapsed(elapsedMs: Long): String {
    val totalSeconds = (elapsedMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(Locale.ROOT, minutes, seconds)
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
    isCurrentCardDone: Boolean,
    answerFeedbackVisible: Boolean,
    isVoiceRecording: Boolean,
    quickEditMode: Boolean,
    onAnswerChange: (String) -> Unit,
    onCheck: () -> Unit,
    onOk: () -> Unit,
    onSaveEmptySide: () -> Unit,
    onGoogleTranslateEmptySide: () -> Unit,
    onClearAnswer: () -> Unit,
    onCopy: (String) -> Unit,
    onVoiceToggle: () -> Unit,
    onSpeak: () -> Unit,
    onDismissAnswerFeedback: () -> Unit,
    controlSize: ControlSize = ControlSize.MEDIUM,
) {
    val isDisplayedEmptySide = currentCard
        ?.displayedCardText(isBackVisible)
        ?.let { text -> text.isBlank() || text.isEmptyPlaceholder() } == true
val usesEditActionBar = quickEditMode || isDisplayedEmptySide
val typedAnswerCorrect = currentCard?.let { card ->
    answer.isNotBlank() && normalizeAnswerText(answer) == normalizeAnswerText(card.expectedAnswerText(isBackVisible))
} == true
    val doneLocked = currentCard != null && isCurrentCardDone && answer.none { it.isLetter() }
    val canSubmit = currentCard != null && !doneLocked && !usesEditActionBar
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
                            onClick = onGoogleTranslateEmptySide,
                            enabled = currentCard != null,
                            shape = RoundedCornerShape(18.dp),
                            contentPadding = PaddingValues(horizontal = 0.dp),
                            modifier = Modifier.size(controlSize.answerIconButtonSize())
                        ) {
                            Text("G", fontWeight = FontWeight.Bold)
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
                            onClick = { currentCard?.displayedCardText(isBackVisible)?.let(onCopy) },
                            size = controlSize.answerIconButtonSize()
                        )
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
                            icon = Icons.Default.Refresh,
                            contentDescription = "Clear input",
                            enabled = answer.isNotBlank(),
                            onClick = onClearAnswer,
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
    if (soundEnabled) {
        playSound(cue)
    }
    if (vibrationEnabled) {
        vibrate(context, cue)
    }
}

private fun prewarmFeedbackSound() {
    runCatching {
        val generator = ToneGenerator(AudioManager.STREAM_MUSIC, 1)
        generator.startTone(ToneGenerator.TONE_PROP_BEEP2, 1)
        Thread.sleep(20)
        generator.release()
    }
}

private fun playSound(cue: FeedbackCue) {
    if (cue == FeedbackCue.SWIPE) {
        playSwipeRustle()
        return
    }
    val tone = when (cue) {
        FeedbackCue.SPLASH -> ToneGenerator.TONE_PROP_ACK
        FeedbackCue.SUCCESS -> ToneGenerator.TONE_PROP_BEEP
        FeedbackCue.TAP -> ToneGenerator.TONE_PROP_BEEP2
        FeedbackCue.SWIPE -> ToneGenerator.TONE_PROP_BEEP2
    }
    val duration = when (cue) {
        FeedbackCue.SPLASH -> 420
        FeedbackCue.SUCCESS -> 90
        FeedbackCue.TAP -> 45
        FeedbackCue.SWIPE -> 1
    }
    val generator = ToneGenerator(AudioManager.STREAM_MUSIC, if (cue == FeedbackCue.SPLASH) 62 else 38)
    generator.startTone(tone, duration)
    Handler(Looper.getMainLooper()).postDelayed({ generator.release() }, (duration + 80).toLong())
}

private fun playSwipeRustle() {
    val sampleRate = 22_050
    val durationMs = 72
    val sampleCount = sampleRate * durationMs / 1000
    val data = ByteArray(sampleCount * 2)
    var smoothedNoise = 0.0
    repeat(sampleCount) { index ->
        val envelope = 1.0 - (index.toDouble() / sampleCount.toDouble())
        val rawNoise = Random.nextDouble(-1.0, 1.0)
        smoothedNoise = smoothedNoise * 0.78 + rawNoise * 0.22
        val sample = (smoothedNoise * envelope * 2600.0)
            .toInt()
            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
        val byteIndex = index * 2
        data[byteIndex] = (sample and 0xFF).toByte()
        data[byteIndex + 1] = ((sample shr 8) and 0xFF).toByte()
    }
    val track = AudioTrack.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .setAudioFormat(
            AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
        )
        .setBufferSizeInBytes(data.size)
        .setTransferMode(AudioTrack.MODE_STATIC)
        .build()
    track.write(data, 0, data.size)
    track.play()
    Handler(Looper.getMainLooper()).postDelayed({
        runCatching { track.stop() }
        track.release()
    }, (durationMs + 80).toLong())
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
    val kindTitle = if (exportedCard.kindCode() == "LN") "Lesson" else "Mistakes"
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
