package com.lexaprograms.polishcards

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
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
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.FileProvider
import java.io.File

private val BrandSaladColor = Color(0xFF8EDB67)
private val BrandRedColor = Color(0xFFD64B42)
private val CompletedFrameColor = Color(0xFFE8F5E9)

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
    val language = when (languageCode.lowercase(Locale.ROOT)) {
        "by", "be" -> "be"
        "ua", "uk" -> "uk"
        "de" -> "de"
        "es" -> "es"
        "ru" -> "ru"
        "pl" -> "pl"
        else -> "en"
    }
    return when (language) {
        "de" -> UiText("Zuruck", "Ausgeblendete Lektionen ausblenden", "Ausgeblendete Lektionen zeigen", "Einstellungen", "In Eingabe kopiert", "Richtig", "Richtig machen", "Alphabetisch", "Zufallig", "Alle zeigen", "Im Satz", "Ubrig", "Fertig", "Weiter", "Wiederholen", "Nachste Lektion", "Stark! Diese Lektion ist abgeschlossen.", "Du hast jede sichtbare Karte geschafft.", "Alles erledigt. Alle sichtbaren Karten haben drei Sterne.", "Nichts zu zeigen.", "Aktiviere Alle zeigen oder fuge Karten hinzu.", "Kopieren", "OK", "Karte bearbeiten", "Bearbeite diese Karte, ohne die Ubung zu verlassen.", "Fehler / Ausgangswert", "Hinweis oder Regel", "Gemacht am", "Wo", "Speichern", "Loschen", "Abbrechen", "Karte loschen?", "Diese Karte wird aus der Lektion entfernt.", "Sprache der Benutzeroberflache")
        "be" -> UiText("Назад", "Схаваць схаваныя ўрокі", "Паказаць схаваныя ўрокі", "Налады", "Скапіявана ва ўвод", "Правільна", "Выпраў", "Па алфавіце", "Выпадкова", "Паказаць усе", "У порцыі", "Засталося", "Гатова", "Далей", "Паўтарыць", "Наступны ўрок", "Выдатна! Гэты ўрок завершаны.", "Ты адказаў на ўсе бачныя карткі.", "Усё зроблена. Усе бачныя карткі маюць тры зоркі.", "Няма чаго паказаць.", "Уключы Паказаць усе або дадай карткі.", "Капіяваць", "OK", "Рэдагаваць картку", "Змяні картку, не пакідаючы трэніроўку.", "Памылка / зыходнае значэнне", "Падказка або правіла", "Калі зроблена", "Дзе", "Захаваць", "Выдаліць", "Адмена", "Выдаліць картку?", "Гэта картка будзе выдалена з урока.", "Мова інтэрфейсу")
        "es" -> UiText("Atrás", "Ocultar lecciones ocultas", "Mostrar lecciones ocultas", "Ajustes", "Copiado al campo", "Correcto", "Corrígelo", "Alfabético", "Aleatorio", "Mostrar todo", "En bloque", "Quedan", "Hechas", "Siguiente", "Repetir", "Siguiente lección", "¡Excelente! Esta lección está completa.", "Respondiste todas las tarjetas visibles.", "Todo listo. Todas las tarjetas visibles tienen tres estrellas.", "Nada que mostrar.", "Activa Mostrar todo o añade tarjetas.", "Copiar", "OK", "Editar tarjeta", "Edita esta tarjeta sin salir de la práctica.", "Error / valor origen", "Pista o regla", "Fecha", "Dónde", "Guardar", "Eliminar", "Cancelar", "¿Eliminar tarjeta?", "Esta tarjeta se eliminará de la lección.", "Idioma de la interfaz")
        "uk" -> UiText("Назад", "Сховати приховані уроки", "Показати приховані уроки", "Налаштування", "Скопійовано у поле", "Правильно", "Виправ", "За алфавітом", "Випадково", "Показати всі", "У порції", "Залишилось", "Готово", "Далі", "Повторити", "Наступний урок", "Чудово! Урок завершено.", "Ти відповів на всі видимі картки.", "Усе зроблено. Усі видимі картки мають три зірки.", "Нічого показати.", "Увімкни Показати всі або додай картки.", "Копіювати", "OK", "Редагувати картку", "Редагуй картку, не виходячи з практики.", "Помилка / вихідне значення", "Підказка або правило", "Коли зроблено", "Де", "Зберегти", "Видалити", "Скасувати", "Видалити картку?", "Цю картку буде видалено з уроку.", "Мова інтерфейсу")
        "ru" -> UiText("Назад", "Скрыть скрытые уроки", "Показать скрытые уроки", "Настройки", "Скопировано в поле", "Верно", "Исправь", "По алфавиту", "Случайно", "Показать все", "В порции", "Осталось", "Готово", "Далее", "Повторить", "Следующий урок", "Отлично! Урок завершен.", "Ты ответил на все видимые карточки.", "Все готово. У всех видимых карточек три звезды.", "Нечего показать.", "Включи Показать все или добавь карточки.", "Копировать", "OK", "Редактировать карточку", "Редактируй карточку, не выходя из тренировки.", "Ошибка / исходное значение", "Подсказка или правило", "Когда сделано", "Где", "Сохранить", "Удалить", "Отмена", "Удалить карточку?", "Эта карточка будет удалена из урока.", "Язык интерфейса")
        "pl" -> UiText("Wstecz", "Ukryj ukryte lekcje", "Pokaż ukryte lekcje", "Ustawienia", "Skopiowano do pola", "Dobrze", "Popraw", "Alfabetycznie", "Losowo", "Pokaż wszystko", "W porcji", "Zostało", "Zrobione", "Dalej", "Powtórz", "Następna lekcja", "Świetnie! Lekcja zakończona.", "Odpowiedziano na wszystkie widoczne karty.", "Gotowe. Wszystkie widoczne karty mają trzy gwiazdki.", "Nic do pokazania.", "Włącz Pokaż wszystko albo dodaj karty.", "Kopiuj", "OK", "Edytuj kartę", "Edytuj tę kartę bez wychodzenia z nauki.", "Błąd / wartość źródłowa", "Podpowiedź lub reguła", "Data", "Gdzie", "Zapisz", "Usuń", "Anuluj", "Usunąć kartę?", "Ta karta zostanie usunięta z lekcji.", "Język interfejsu")
        else -> UiText("Back", "Hide hidden lessons", "Show hidden lessons", "Settings", "Copied to input", "Correct", "Make it right", "Alphabetical", "Random", "Show all", "In portion", "Left", "Done", "Next", "Repeat", "Next lesson", "Excellent work. This lesson is complete!", "You answered every visible card.", "All done. Every visible card already has three stars.", "Nothing to show yet.", "Turn on Show all or add cards to keep practicing.", "Copy", "OK", "Edit card", "Update this card without leaving practice.", "Mistake made / source value", "Hint or rule", "Made at", "Where", "Save", "Delete", "Cancel", "Delete card?", "This card will be removed from the lesson.", "Interface language")
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MakeMistakeTheme {
                MakeMistakeApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MakeMistakeApp(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val ui = remember(state.interfaceLanguage) { uiTextFor(state.interfaceLanguage) }
    var showSplash by remember { mutableStateOf(true) }
    var splashReady by remember { mutableStateOf(false) }
    var titleActivated by remember { mutableStateOf(false) }
    var correctBubbleVisible by remember { mutableStateOf(false) }
    val appTitleColor by animateColorAsState(
        targetValue = if (titleActivated) BrandSaladColor else BrandRedColor,
        animationSpec = tween(durationMillis = 3000),
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
            if (message == "Correct") {
                correctBubbleVisible = true
                delay(1000)
                correctBubbleVisible = false
                viewModel.consumeMessage()
            } else {
                snackbarHostState.showSnackbar(message)
                viewModel.consumeMessage()
            }
        }
    }

    CompositionLocalProvider(LocalUiText provides ui) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    if (state.screen != AppScreen.CATALOG) {
                        IconButton(onClick = {
                            titleActivated = true
                            viewModel.openCatalog()
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = ui.back)
                        }
                    }
                },
                title = {
                    if (state.screen == AppScreen.STUDY) {
                        Text("")
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Make Mistake", fontWeight = FontWeight.SemiBold, color = appTitleColor)
                            Text("v0.47", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            if (state.screen == AppScreen.STUDY && !state.isPortionFinished && state.currentCard != null) {
                AnswerBar(
                    answer = state.answer,
                    answerLabel = state.currentCard?.backLabel().orEmpty().ifBlank { ui.makeItRight },
                    currentCard = state.currentCard,
                    isBackVisible = state.isBackVisible,
                    isCurrentCardDone = state.currentCard?.id in state.completedCardIds,
                    onAnswerChange = viewModel::updateAnswer,
                    onOk = {
                        val typedCorrect = state.answer.isNotBlank() &&
                            state.currentCard?.let { card ->
                                normalizeAnswerText(state.answer) == normalizeAnswerText(card.correctText())
                            } == true
                        if (typedCorrect) {
                            performFeedback(context, state.soundEffectsEnabled, state.vibrationEnabled, FeedbackCue.SUCCESS)
                        }
                        viewModel.acceptCurrentCard()
                    },
                    onCopy = { text ->
                        viewModel.updateAnswer(text)
                        viewModel.showMessage(ui.copiedToInput)
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
                    onImportLessons = {
                        titleActivated = true
                        importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                    }
                )
                AppScreen.STUDY -> StudyScreen(
                    state = state,
                    onModeChange = viewModel::setMode,
                    onShowAllCardsChange = viewModel::setShowAllCards,
                    onPreviousCard = viewModel::previousCard,
                    onNextCard = viewModel::nextCard,
                    onFirstRemainingCard = viewModel::goToFirstRemainingCard,
                    onNewPortion = viewModel::startNewPortion,
                    onNextLesson = viewModel::openNextVisibleLesson,
                    onOpenCatalog = viewModel::openCatalog,
                    onToggleCard = {
                        performFeedback(context, state.soundEffectsEnabled, state.vibrationEnabled, FeedbackCue.TAP)
                        viewModel.toggleCard()
                    },
                    onToggleStar = { cardId, starIndex ->
                        performFeedback(context, state.soundEffectsEnabled, state.vibrationEnabled, FeedbackCue.TAP)
                        viewModel.toggleCardStar(cardId, starIndex)
                    },
                    onEditCard = {
                        performFeedback(context, state.soundEffectsEnabled, state.vibrationEnabled, FeedbackCue.TAP)
                        viewModel.editCurrentStudyCard()
                    },
                    showCardLog = state.showCardLog,
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
                    soundEffectsEnabled = state.soundEffectsEnabled,
                    vibrationEnabled = state.vibrationEnabled,
                    notificationIntervalDraft = state.notificationIntervalDraft,
                    notificationMaxDraft = state.notificationMaxDraft,
                    onCardStartSideChange = viewModel::setCardStartSide,
                    onExcludeMasteredCardsChange = viewModel::setExcludeMasteredCards,
                    onShowCardLogChange = viewModel::setShowCardLog,
                    onInterfaceLanguageChange = viewModel::setInterfaceLanguage,
                    onSoundEffectsEnabledChange = viewModel::setSoundEffectsEnabled,
                    onVibrationEnabledChange = viewModel::setVibrationEnabled,
                    onNotificationIntervalChange = viewModel::updateNotificationIntervalDraft,
                    onNotificationMaxChange = viewModel::updateNotificationMaxDraft,
                    onSaveNotificationInterval = { viewModel.saveNotificationInterval(context) },
                    onSaveNotificationMax = viewModel::saveNotificationMax,
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
                    onCardDraftChange = viewModel::updateCardDraft,
                    onAddCard = viewModel::addCardToLesson,
                    onDeleteCard = viewModel::deleteCardFromLesson,
                    onCopyCard = viewModel::copyCardInLesson,
                    onMoveCard = viewModel::moveCard,
                    onEditCard = viewModel::editCardInLesson,
                    onCancelCardEditing = viewModel::cancelCardEditing,
                    onToggleCardStar = viewModel::toggleCardStar,
                    onSave = viewModel::saveEditedLesson,
                    onDeleteLesson = viewModel::deleteLesson
                )
            }
            if (state.screen == AppScreen.STUDY && state.cardDraft.editingCardId != null) {
                StudyCardEditorDialog(
                    cardDraft = state.cardDraft,
                    onCardDraftChange = viewModel::updateCardDraft,
                    onSave = viewModel::saveCurrentStudyCard,
                    onDelete = viewModel::deleteCurrentStudyCard,
                    onDismiss = viewModel::cancelCardEditing
                )
            }
            CorrectBubble(
                visible = correctBubbleVisible,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 22.dp)
                    .zIndex(20f)
            )
        }
    }
}
}

@Composable
private fun CorrectBubble(visible: Boolean, modifier: Modifier = Modifier) {
    val ui = rememberUiText()
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
            color = BrandSaladColor.copy(alpha = 0.96f)
        ) {
            Text(
                text = ui.correct,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF17351F)
            )
        }
    }
}

@Composable
private fun StudyCardEditorDialog(
    cardDraft: CardDraft,
    onCardDraftChange: (CardDraft) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
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
                    OutlinedTextField(
                        value = cardDraft.nativeValue,
                        onValueChange = { onCardDraftChange(cardDraft.copy(nativeValue = it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(ui.mistakeSource) },
                        singleLine = false
                    )
                    OutlinedTextField(
                        value = cardDraft.correctValue,
                        onValueChange = { onCardDraftChange(cardDraft.copy(correctValue = it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(ui.makeItRight) },
                        singleLine = false
                    )
                    OutlinedTextField(
                        value = cardDraft.hint,
                        onValueChange = { onCardDraftChange(cardDraft.copy(hint = it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(ui.hintOrRule) },
                        singleLine = false
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = cardDraft.madeAt,
                            onValueChange = { onCardDraftChange(cardDraft.copy(madeAt = it)) },
                            modifier = Modifier.weight(1f),
                            label = { Text(ui.madeAt) },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = cardDraft.where,
                            onValueChange = { onCardDraftChange(cardDraft.copy(where = it)) },
                            modifier = Modifier.weight(1f),
                            label = { Text(ui.where) },
                            singleLine = true
                        )
                    }
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "M",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFE53935),
                    modifier = Modifier.graphicsLayer {
                        translationX = -56f * (1f - progress)
                        alpha = (1f - progress).coerceIn(0f, 1f)
                    }
                )
                Text(
                    text = "M",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFE53935),
                    modifier = Modifier.graphicsLayer {
                        translationX = 56f * (1f - progress)
                        alpha = (1f - progress).coerceIn(0f, 1f)
                    }
                )
                Text(
                    text = "M",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Black,
                    color = BrandSaladColor,
                    modifier = Modifier.graphicsLayer {
                        scaleX = 0.82f + 0.18f * progress
                        scaleY = 0.82f + 0.18f * progress
                        alpha = progress
                    }
                )
            }
            Text(
                text = "Make Mistake",
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
    soundEffectsEnabled: Boolean,
    vibrationEnabled: Boolean,
    notificationIntervalDraft: String,
    notificationMaxDraft: String,
    onCardStartSideChange: (CardStartSide) -> Unit,
    onExcludeMasteredCardsChange: (Boolean) -> Unit,
    onShowCardLogChange: (Boolean) -> Unit,
    onInterfaceLanguageChange: (String) -> Unit,
    onSoundEffectsEnabledChange: (Boolean) -> Unit,
    onVibrationEnabledChange: (Boolean) -> Unit,
    onNotificationIntervalChange: (String) -> Unit,
    onNotificationMaxChange: (String) -> Unit,
    onSaveNotificationInterval: () -> Unit,
    onSaveNotificationMax: () -> Unit,
    onDownloadSampleJson: () -> Unit
) {
    val ui = rememberUiText()
    fun st(en: String, de: String, be: String, es: String, uk: String, ru: String, pl: String): String {
        return when (interfaceLanguage.lowercase(Locale.ROOT)) {
            "de" -> de
            "be", "by" -> be
            "es" -> es
            "uk", "ua" -> uk
            "ru" -> ru
            "pl" -> pl
            else -> en
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
                    "en" to "EN",
                    "de" to "DE",
                    "be" to "BY",
                    "es" to "ES",
                    "uk" to "UA",
                    "ru" to "RU",
                    "pl" to "PL"
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    languageOptions.chunked(4).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { (code, label) ->
                                FilterChip(
                                    selected = interfaceLanguage == code,
                                    onClick = { onInterfaceLanguageChange(code) },
                                    label = { Text(label) }
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = st("Default card side", "Standard-Kartenseite", "Пачатковы бок карткі", "Lado inicial de la tarjeta", "Початковий бік картки", "Сторона карточки по умолчанию", "Domyślna strona karty"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = st(
                        "Choose which side is shown first when a card opens.",
                        "Wähle, welche Seite beim Öffnen zuerst erscheint.",
                        "Выберы, які бок паказваць першым пры адкрыцці карткі.",
                        "Elige qué lado se muestra primero al abrir una tarjeta.",
                        "Вибери, який бік показувати першим при відкритті картки.",
                        "Выбери, какая сторона показывается первой при открытии карточки.",
                        "Wybierz, która strona pokazuje się pierwsza po otwarciu karty."
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
                        text = st("Hide done cards", "Fertige Karten ausblenden", "Схаваць зробленыя карткі", "Ocultar tarjetas hechas", "Сховати виконані картки", "Скрывать выполненные карточки", "Ukryj zrobione karty"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = st(
                            "Do not show or count cards marked with 3 stars inside a lesson.",
                            "Karten mit 3 Sternen in der Lektion nicht anzeigen oder zählen.",
                            "Не паказваць і не ўлічваць у ўроку карткі з 3 зоркамі.",
                            "No mostrar ni contar dentro de la lección las tarjetas con 3 estrellas.",
                            "Не показувати й не рахувати в уроці картки з 3 зірками.",
                            "Не показывать и не учитывать в уроке карточки с 3 звездами.",
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
                    text = st("Card and feedback", "Karte und Feedback", "Картка і водгук", "Tarjeta y respuesta", "Картка і відгук", "Карточка и отклик", "Karta i reakcje"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                SettingsSwitchRow(
                    title = st("Show card log", "Kartenlog anzeigen", "Паказваць лог карткі", "Mostrar registro de tarjeta", "Показувати лог картки", "Показывать лог карточки", "Pokaż log karty"),
                    description = st(
                        "Show the M icon with mistakes and work log on study cards.",
                        "Zeigt das M-Symbol mit Fehlern und Arbeitslog auf Lernkarten.",
                        "Паказвае значок M з памылкамі і логам працы на картках.",
                        "Muestra el icono M con errores y registro de trabajo en las tarjetas.",
                        "Показує значок M з помилками й логом роботи на картках.",
                        "Показывает значок M с ошибками и логом работы на карточках.",
                        "Pokazuje ikonę M z błędami i logiem pracy na kartach."
                    ),
                    checked = showCardLog,
                    onCheckedChange = onShowCardLogChange
                )
                SettingsSwitchRow(
                    title = st("Sound effects", "Soundeffekte", "Гукавыя эфекты", "Efectos de sonido", "Звукові ефекти", "Звуковые эффекты", "Efekty dźwiękowe"),
                    description = st(
                        "Play short sounds for app actions and the splash screen.",
                        "Spielt kurze Sounds für Aktionen und den Startbildschirm.",
                        "Прайграе кароткія гукі для дзеянняў і застаўкі.",
                        "Reproduce sonidos cortos para acciones y la pantalla inicial.",
                        "Відтворює короткі звуки для дій і заставки.",
                        "Воспроизводит короткие звуки для действий и заставки.",
                        "Odtwarza krótkie dźwięki dla akcji i ekranu startowego."
                    ),
                    checked = soundEffectsEnabled,
                    onCheckedChange = onSoundEffectsEnabledChange
                )
                SettingsSwitchRow(
                    title = st("Vibration", "Vibration", "Вібрацыя", "Vibración", "Вібрація", "Вибрация", "Wibracja"),
                    description = st(
                        "Use short haptic feedback for app actions.",
                        "Nutzt kurze haptische Rückmeldung für Aktionen.",
                        "Выкарыстоўвае кароткі вібраадгук для дзеянняў.",
                        "Usa respuesta háptica corta para acciones.",
                        "Використовує короткий вібровідгук для дій.",
                        "Использует короткий виброотклик для действий.",
                        "Używa krótkiej reakcji haptycznej dla akcji."
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = st("JSON template", "JSON-Vorlage", "Шаблон JSON", "Plantilla JSON", "Шаблон JSON", "Шаблон JSON", "Szablon JSON"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = st(
                        "Download an instruction-ready JSON template for generating lessons from your mistakes.",
                        "Lade eine JSON-Vorlage mit Anweisungen herunter, um Lektionen aus deinen Fehlern zu erzeugen.",
                        "Спампуй JSON-шаблон з інструкцыяй для стварэння ўрокаў з тваіх памылак.",
                        "Descarga una plantilla JSON lista como instrucción para generar lecciones desde tus errores.",
                        "Завантаж JSON-шаблон з інструкцією для створення уроків із твоїх помилок.",
                        "Скачай JSON-шаблон с инструкцией для создания уроков из твоих ошибок.",
                        "Pobierz szablon JSON z instrukcją do tworzenia lekcji z Twoich błędów."
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
                    text = st("Notifications", "Benachrichtigungen", "Апавяшчэнні", "Notificaciones", "Сповіщення", "Уведомления", "Powiadomienia"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = st(
                        "Shows weighted reminders for cards with 0-2 stars. Cards with 0 stars appear most often; 3-star cards are excluded.",
                        "Zeigt gewichtete Erinnerungen für Karten mit 0-2 Sternen. 0 Sterne erscheinen am häufigsten; 3 Sterne sind ausgeschlossen.",
                        "Паказвае ўзважаныя напаміны для картак з 0-2 зоркамі. 0 зорак трапляюць часцей; 3 зоркі выключаюцца.",
                        "Muestra recordatorios ponderados para tarjetas con 0-2 estrellas. Las de 0 salen más; las de 3 no salen.",
                        "Показує зважені нагадування для карток із 0-2 зірками. 0 зірок з'являються найчастіше; 3 зірки виключені.",
                        "Показывает взвешенные напоминания для карточек с 0-2 звездами. Карточки с 0 звездами появляются чаще всего; 3 звезды исключены.",
                        "Pokazuje ważone przypomnienia dla kart z 0-2 gwiazdkami. 0 gwiazdek pojawia się najczęściej; 3 gwiazdki są wykluczone."
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = notificationIntervalDraft,
                    onValueChange = onNotificationIntervalChange,
                    label = { Text(st("Interval minutes", "Intervall in Minuten", "Інтэрвал у хвілінах", "Intervalo en minutos", "Інтервал у хвилинах", "Интервал в минутах", "Interwał w minutach")) },
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
                    label = { Text(st("Maximum active notifications", "Maximale aktive Benachrichtigungen", "Максімум актыўных апавяшчэнняў", "Máximo de notificaciones activas", "Максимум активних сповіщень", "Максимум активных уведомлений", "Maksimum aktywnych powiadomień")) },
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
                        "Android kann Hintergrundaufgaben verzögert ausführen, besonders wenn das Telefon inaktiv ist.",
                        "Android можа запускаць фонавыя задачы з затрымкай, асабліва калі тэлефон неактыўны.",
                        "Android puede retrasar tareas en segundo plano, sobre todo cuando el teléfono está inactivo.",
                        "Android може запускати фонові задачі із затримкою, особливо коли телефон неактивний.",
                        "Android может запускать фоновые задачи с задержкой, особенно когда телефон неактивен.",
                        "Android może opóźniać pracę w tle, szczególnie gdy telefon jest bezczynny."
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
                    text = st("Version log", "Versionslog", "Лог версій", "Registro de versiones", "Лог версій", "Лог версий", "Historia wersji"),
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

        Text(
            text = "Lessons",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp)
        )

        if (lessons.isEmpty()) {
            EmptyState("No lessons yet. Import JSON files or create a lesson.")
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(top = 10.dp, bottom = 96.dp),
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
                        modifier = Modifier.size(32.dp)
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
                    IconButton(onClick = onShare) {
                        Icon(Icons.Default.Share, contentDescription = "Share lesson JSON")
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
    onFirstRemainingCard: () -> Unit,
    onNewPortion: () -> Unit,
    onNextLesson: () -> Unit,
    onOpenCatalog: () -> Unit,
    onToggleCard: () -> Unit,
    onToggleStar: (Int, Int) -> Unit,
    onEditCard: () -> Unit,
    showCardLog: Boolean,
    onShareCard: (Lesson, Flashcard) -> Unit
) {
    val context = LocalContext.current
    var showLessonInfo by remember { mutableStateOf(false) }
    val selectedLesson = state.selectedLesson

    if (showLessonInfo && selectedLesson?.lessonInfo?.isNotBlank() == true) {
        CardTextDialog(
            title = "Lesson info",
            text = selectedLesson.lessonInfo,
            onDismiss = { showLessonInfo = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedLesson?.title.orEmpty(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (selectedLesson?.lessonInfo?.isNotBlank() == true) {
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

        TopControls(
            mode = state.mode,
            showAllCards = !state.excludeMasteredCards,
            onModeChange = onModeChange,
            onShowAllCardsChange = onShowAllCardsChange
        )

        CountersRow(
            portionSize = state.portionSize,
            remainingCount = state.remainingCount,
            completedCount = state.completedCount,
            onRemainingClick = onFirstRemainingCard
        )

        state.studyEmptyMessage?.let { message ->
            StudyEmptyState(message = message)
        } ?: if (state.isPortionFinished) {
            FinishedScreen(onNewPortion = onNewPortion, onNextLesson = onNextLesson, onOpenCatalog = onOpenCatalog)
        } else {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
                    StudyCard(
                        card = animatedCard,
                        isBackVisible = state.isBackVisible,
                        onClick = onToggleCard,
                        onToggleStar = onToggleStar,
                        onEditCard = onEditCard,
                        isCompleted = animatedCard?.id in state.completedCardIds,
                        positionLabel = (animatedIndex + 1).toString(),
                        onSwipePrevious = {
                            if (state.currentIndex > 0) {
                                performFeedback(context, state.soundEffectsEnabled, state.vibrationEnabled, FeedbackCue.SWIPE)
                                onPreviousCard()
                            }
                        },
                        onSwipeNext = {
                            if (state.currentIndex < state.currentPortion.lastIndex) {
                                performFeedback(context, state.soundEffectsEnabled, state.vibrationEnabled, FeedbackCue.SWIPE)
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
    onShowAllCardsChange: (Boolean) -> Unit
) {
    val ui = rememberUiText()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = mode == StudyMode.ALPHABETICAL,
            onClick = { onModeChange(StudyMode.ALPHABETICAL) },
            label = { Text("Alphabetical") }
        )
        FilterChip(
            selected = mode == StudyMode.RANDOM,
            onClick = { onModeChange(StudyMode.RANDOM) },
            label = { Text("Random") }
        )
        FilterChip(
            selected = showAllCards,
            onClick = { onShowAllCardsChange(!showAllCards) },
            label = { Text("Show all") }
        )
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
    onRemainingClick: () -> Unit
) {
    val ui = rememberUiText()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        CounterText("${ui.inPortion}: $portionSize")
        CounterText(
            "${ui.left}: $remainingCount",
            modifier = Modifier.clickable(enabled = remainingCount > 0) { onRemainingClick() }
        )
        CounterText("${ui.done}: $completedCount")
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

@Composable
private fun StudyCard(
    card: Flashcard?,
    isBackVisible: Boolean,
    onClick: () -> Unit,
    onToggleStar: (Int, Int) -> Unit,
    onEditCard: () -> Unit,
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
                .height(300.dp)
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
                    .padding(18.dp)
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
                    IconButton(
                        onClick = onEditCard,
                        enabled = card != null,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color.White.copy(alpha = 0.92f))
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
                        .padding(top = 54.dp, bottom = 56.dp)
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
                                text = card?.frontLabel().orEmpty(),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = card?.let { flashcard ->
                                    if (flashcard.kindCode() == "LN") {
                                        flashcard.nativeText()
                                    } else {
                                        flashcard.mistakeText().ifBlank { "No mistake recorded yet" }
                                    }
                                }.orEmpty(),
                                style = MaterialTheme.typography.headlineMedium,
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
                                shape = RoundedCornerShape(24.dp)
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
private fun CardTextDialog(title: String, text: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
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

private fun UiText.languageKey(): String = when (settings) {
    "Einstellungen" -> "de"
    "Налады" -> "be"
    "Ajustes" -> "es"
    "Налаштування" -> "uk"
    "Настройки" -> "ru"
    "Ustawienia" -> "pl"
    else -> "en"
}

private fun UiText.kindText(kindCode: String): String = when (languageKey()) {
    "de" -> if (kindCode == "LN") "Lektion" else "Fehler"
    "be" -> if (kindCode == "LN") "Урок" else "Памылкі"
    "es" -> if (kindCode == "LN") "Lección" else "Errores"
    "uk" -> if (kindCode == "LN") "Урок" else "Помилки"
    "ru" -> if (kindCode == "LN") "Урок" else "Ошибки"
    "pl" -> if (kindCode == "LN") "Lekcja" else "Błędy"
    else -> if (kindCode == "LN") "Lesson" else "Mistakes"
}

private fun UiText.mixedKindText(): String = when (languageKey()) {
    "de" -> "Gemischt"
    "be" -> "Змешана"
    "es" -> "Mixto"
    "uk" -> "Змішано"
    "ru" -> "Смешанный"
    "pl" -> "Mieszane"
    else -> "Mixed"
}

private fun UiText.questionsText(count: Int): String = when (languageKey()) {
    "de" -> "$count Fragen"
    "be" -> "$count пытанняў"
    "es" -> "$count preguntas"
    "uk" -> "$count питань"
    "ru" -> "$count вопросов"
    "pl" -> "$count pytań"
    else -> "$count questions"
}

private fun UiText.doneText(done: Int, total: Int): String = when (languageKey()) {
    "de" -> "$done von $total erledigt"
    "be" -> "$done з $total зроблена"
    "es" -> "$done de $total hechas"
    "uk" -> "$done із $total виконано"
    "ru" -> "$done из $total выполнены"
    "pl" -> "$done z $total zrobione"
    else -> "$done of $total done"
}

private fun UiText.completedText(count: Int): String = when (languageKey()) {
    "de" -> "$count-mal abgeschlossen"
    "be" -> "Пройдзена $count разоў"
    "es" -> "Completado $count veces"
    "uk" -> "Пройдено $count разів"
    "ru" -> "Пройдено $count раз"
    "pl" -> "Ukończono $count razy"
    else -> "Completed $count times"
}

private fun Flashcard.displayedCardText(isBackVisible: Boolean): String {
    return if (isBackVisible) {
        correctText()
    } else if (kindCode() == "LN") {
        nativeText()
    } else {
        mistakeText().ifBlank { nativeText().ifBlank { correctText() } }
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
private fun AnswerBar(
    answer: String,
    answerLabel: String,
    currentCard: Flashcard?,
    isBackVisible: Boolean,
    isCurrentCardDone: Boolean,
    onAnswerChange: (String) -> Unit,
    onOk: () -> Unit,
    onCopy: (String) -> Unit,
) {
    val doneLocked = currentCard != null && isCurrentCardDone && answer.none { it.isLetter() }
    val canSubmit = currentCard != null && !doneLocked
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = answer,
                onValueChange = onAnswerChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(answerLabel) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (canSubmit) onOk() })
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { currentCard?.displayedCardText(isBackVisible)?.let(onCopy) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    enabled = currentCard != null
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Copy")
                }
                Button(
                    onClick = onOk,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    enabled = canSubmit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE4F6E8),
                        contentColor = Color(0xFF234231),
                        disabledContainerColor = Color(0xFFD6DCD2),
                        disabledContentColor = Color(0xFF7D877B)
                    )
                ) {
                    Text(if (doneLocked) "Done" else "OK")
                }
            }
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
            shape = RoundedCornerShape(24.dp),
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
    onCardDraftChange: (CardDraft) -> Unit,
    onAddCard: () -> Unit,
    onDeleteCard: (Int) -> Unit,
    onCopyCard: (Int) -> Unit,
    onMoveCard: (Int, Int) -> Unit,
    onEditCard: (Flashcard) -> Unit,
    onCancelCardEditing: () -> Unit,
    onToggleCardStar: (Int, Int) -> Unit,
    onSave: () -> Unit,
    onDeleteLesson: (String) -> Unit
) {
    val lesson = state.editorLesson ?: return
    val cardDraft = state.cardDraft
    var showDeleteLessonDialog by remember { mutableStateOf(false) }
    var pendingDeleteCardId by remember { mutableStateOf<Int?>(null) }
    var showCardDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.returnToStudyAfterEdit, cardDraft.editingCardId) {
        if (state.returnToStudyAfterEdit && cardDraft.editingCardId != null) {
            showCardDialog = true
        }
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
                    OutlinedTextField(
                        value = cardDraft.nativeValue,
                        onValueChange = { onCardDraftChange(cardDraft.copy(nativeValue = it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Native value") },
                        singleLine = false
                    )
                    OutlinedTextField(
                        value = cardDraft.correctValue,
                        onValueChange = { onCardDraftChange(cardDraft.copy(correctValue = it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Make it right") },
                        singleLine = false
                    )
                    OutlinedTextField(
                        value = cardDraft.hint,
                        onValueChange = { onCardDraftChange(cardDraft.copy(hint = it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Hint or rule") },
                        singleLine = false
                    )
                    OutlinedTextField(
                        value = cardDraft.madeAt,
                        onValueChange = { onCardDraftChange(cardDraft.copy(madeAt = it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Made at") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = cardDraft.where,
                        onValueChange = { onCardDraftChange(cardDraft.copy(where = it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Where") },
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = lesson.title,
            onValueChange = onTitleChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            label = { Text("Lesson title") },
            singleLine = true
        )
        OutlinedTextField(
            value = lesson.lessonInfo,
            onValueChange = onLessonInfoChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Lesson info") },
            minLines = 2,
            maxLines = 5
        )

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

        if (lesson.cards.isEmpty()) {
            EmptyState("This lesson has no cards yet.")
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 18.dp)
            ) {
                items(lesson.cards, key = { it.id }) { card ->
                    EditableCardRow(card = card, onMoveUp = { onMoveCard(card.id, -1) }, onMoveDown = { onMoveCard(card.id, 1) }, onEdit = {
                        onEditCard(card)
                        showCardDialog = true
                    }, onCopy = { onCopyCard(card.id) }, onToggleStar = { starIndex -> onToggleCardStar(card.id, starIndex) }, onDelete = { pendingDeleteCardId = card.id })
                }
            }
        }
    }
}

@Composable
private fun EditableCardRow(
    card: Flashcard,
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
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(card.nativeText(), fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        card.correctText(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                OutlinedButton(
                    onClick = onEdit,
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
        .replace(Regex("[^a-z0-9Ð°-ÑÑ‘Ä…Ä‡Ä™Å‚Å„Ã³Å›ÅºÅ¼]+"), "_")
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
private fun sampleLessonJson(): String {
    return """
        {
          "prompt": "Export all my mistakes or lesson translation cards into this JSON format. Use cardKind MK for mistake cards and LN for normal lesson/translation cards. Fill lessonInfo with instructions for how the learner should work with this specific lesson or card type. For LN fill sourceLanguage and targetLanguage so the app can show language names on the card.",
          "id": "replace_with_unique_lesson_id",
          "title": "Lesson title shown on the main screen",
          "lessonInfo": "Instructions shown from the lesson info icon. Use this for the purpose of the lesson, how to answer, what to pay attention to, and any workflow notes for this set of cards.",
          "timesCompleted": 0,
          "editable": true,
          "cards": [
            {
              "id": 1,
              "nativeValue": "Meaning in the native language, for example Russian translation or explanation.",
              "correctValue": "The correct form the learner should type.",
              "wrongAnswers": [
                {
                  "answer": "The exact wrong answer the learner wrote.",
                  "date": "When the mistake was made, for example 2026-06-20 10:30."
                }
              ],
              "hint": "A rule, explanation, or memory hint. Long text is allowed.",
              "madeAt": "Date and time when this mistake card was created or when the mistake happened.",
              "where": "Source of the mistake, for example chat, lesson, exercise, video, or conversation.",
              "log": [
                "Work history for this card, for example created, answered correctly, skipped, or star changes."
              ],
              "mistake": "The current or most important wrong version shown on the front of the card.",
              "type": "card",
              "cardKind": "MK means Mistakes. LN means Lesson. If this field is missing or blank, the app treats it as MK.",
              "sourceLanguage": "For LN cards: language shown on the front side, for example Russian. For MK cards this may be blank.",
              "targetLanguage": "For LN cards: language shown on the back side, for example Polish. For MK cards this may be blank.",
              "stars": 0
            },
            {
              "id": 2,
              "nativeValue": "Text in the source language.",
              "correctValue": "Correct translation in the target language.",
              "wrongAnswers": [],
              "hint": "Optional grammar note, translation hint, or explanation.",
              "madeAt": "",
              "where": "lesson source",
              "log": [
                "created lesson card"
              ],
              "mistake": "",
              "type": "card",
              "cardKind": "LN",
              "sourceLanguage": "Russian",
              "targetLanguage": "Polish",
              "stars": 0
            }
          ]
        }
    """.trimIndent()
}

private fun versionLogText(): String {
    return """
        v0.48 - Added lesson-level lessonInfo JSON support with info icons on lesson tiles and the study screen, added lesson info editing, preserved lessonInfo in exports, and updated the sample JSON template.
        v0.47 - Localized settings descriptions and lesson tile summaries, kept action button labels in English for stable layout, added a Lessons action to the completed-lesson screen, and made the Left counter jump to the first remaining card.
        v0.46 - Added an in-app interface language selector for EN/DE/BY/ES/UA/RU/PL with saved preference and system-language fallback, and shortened the Make Mistake title color transition to three seconds.
        v0.45 - Added EN/DE/BE/ES/UK/RU/PL interface language support based on Android app/device locale, exposed Android app-language configuration, made the red Make Mistake title fade to the brand salad color over five seconds, and added Repeat / Next lesson actions on the completed-lesson screen while skipping hidden lessons.
        v0.44 - Fixed study counters so they only count cards in the visible portion, finished lessons when every visible card has been accepted with OK, added an in-lesson Show all chip for hiding three-star cards, and added All done / Nothing to show empty states.
        v0.43 - Made card slide transitions respect forward/back direction, moved study-card editing into an in-practice editor dialog, returned to the next or previous study card after deleting the edited card, and replaced the Correct snackbar with a one-second animated bubble.
        v0.42 - Kept OK advancing to the next card while adding a swipe-like visual transition, limited success sound to correct typed answers, showed Correct after typed success, recalculated lesson/card counters after card deletion, and added a Delete action inside the card edit dialog with confirmation.
        v0.41 - Made the app title start red after launch and smoothly transition to the soft salad brand color after the first action, moved hidden lesson markers to the lower-right corner, and recolored hidden eye indicators with the same brand accent.
        v0.40 - Showed only the current card number on study cards, moved the hidden lesson eye to the lower-left, made swipe feedback a soft rustle, matched completed-card framing to the Alphabetical chip color, used the card back label in the answer field, added a configurable active notification maximum, and closed notification practice after a correct answer.
        v0.39 - Made disabled OK read as Done, added a subtle card number in the top-left corner, strengthened completed-card salad highlighting, prewarmed splash audio, renamed the splash title to Make Mistake, added a soft swipe sound, and made MK card editing use the mistake JSON field instead of rule text.
        v0.38 - Replaced the completed-card outline with a soft salad glow, added left/right swipe navigation between study cards, and matched the app title and splash M to the same fresh accent color.
        v0.37 - Defaulted card log off while sound and vibration are on, marked completed cards with a green outline, disabled OK on already completed cards until new letters are typed, and made splash audio/haptics stronger.
        v0.36 - Shared JSON is sent as a file through FileProvider, added settings for showing the card log icon, added sound and vibration options defaulting off, and added an optional splash sound.
        v0.35 - Added a Share action directly on study cards that exports one card as a one-entry Lesson/Mistakes JSON with a copied-from-lesson log entry, and restored the answer bar to standard Scaffold bottom-bar keyboard behavior.
        v0.34 - Kept the study card visible while typing by floating the answer bar above the keyboard, changed the catalog eye to a plain/bright visibility state, added up/down order buttons to configured lesson cards, and added a small crossed-out eye marker on hidden lesson cards.
        v0.33 - Kept Edit only on the study card, made configured lessons drag immediately while unconfigured lessons still use long-press pickup, added Show Hidden in the catalog top bar and Hide/Unhide actions on configured lesson cards, excluded hidden lessons from notifications, cleaned LN/MK title suffixes on load/save, and added IME padding so the study card remains visible above the keyboard.
        v0.32 - Added edit current card actions from the study screen, returns to the same card after saving, removed the 20-card portion limit, cleaned LN/MK suffixes from built-in lesson titles, hid Lesson/Mistakes text on study cards while keeping language labels, made answer checking ignore punctuation/spaces/case, and extended the splash to 1.3 seconds.
        v0.31 - Restored long-press lesson edit/drag mode with centered active action icons, highlighted configured lesson cards, replaced bundled Belarusian lessons with the new MK/LN JSON files, allowed notification intervals down to 1 minute using one-time rescheduling work, shortened the splash to about one second, and made the app title green.
        v0.30 - Kept lesson action icons always visible but inactive until a press-and-hold on the icon zone, restored delayed drag pickup so list scrolling works, and added an animated splash where a red M joins into a green M with the Make Mistake title.
        v0.29 - Replaced hidden edit mode with a gear menu on each lesson card, kept direct drag reordering, added MK/LN card kinds with source/target language labels, and expanded settings to show the full version log.
        v0.28 - Made lesson dragging immediate and disabled it in edit mode, kept edit mode behind a one-second hold, and moved add/edit card fields into a dialog opened from the editor toolbar.
        v0.27 - Kept lesson cards the same color in edit mode, removed the selection checkbox, changed edit actions to a floating cloud overlay, separated drag pickup from one-second edit hold, and replaced bundled Belarusian JSON lessons.
        v0.26 - Fixed lesson tile hit areas by removing hidden action overlays, made the lesson list scroll to all built-in lessons, and added a delayed long-press pickup for lesson dragging.
        v0.25 - Improved lesson card layout with bookmark-style actions, kept completed cards available inside the current portion, added card copy and delete confirmations, made the editor card list visible, and bundled Belarusian mistake lessons by default.
        v0.24 - Made lesson dragging available without entering configuration mode, kept lesson cards visually stable while action icons appear, preserved card color during drag, and made star ratings fill sequentially from left to right.
        v0.23 - Stabilized lesson drag reordering so configured lesson cards stay within the list, and added automatic star progress after a correct typed answer.
        v0.22 - Made Share visible on configured lesson cards, improved download icons, made Copy fill the input with the currently visible card side, added outside-tap exit for lesson configuration, and improved lesson list spacing/highlight.
    """.trimIndent()
}





































