package com.lexaprograms.polishcards

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
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
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.FileProvider
import java.io.File

private val BrandSaladColor = Color(0xFF8FDCC4)
private val BrandRedColor = Color(0xFFC45F59)
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
        "be" -> UiText("ÃÂÃÂ°ÃÂ·ÃÂ°ÃÂ´", "ÃÂ¡Ã‘â€¦ÃÂ°ÃÂ²ÃÂ°Ã‘â€ Ã‘Å’ Ã‘ÂÃ‘â€¦ÃÂ°ÃÂ²ÃÂ°ÃÂ½Ã‘â€¹Ã‘Â Ã‘Å¾Ã‘â‚¬ÃÂ¾ÃÂºÃ‘â€“", "ÃÅ¸ÃÂ°ÃÂºÃÂ°ÃÂ·ÃÂ°Ã‘â€ Ã‘Å’ Ã‘ÂÃ‘â€¦ÃÂ°ÃÂ²ÃÂ°ÃÂ½Ã‘â€¹Ã‘Â Ã‘Å¾Ã‘â‚¬ÃÂ¾ÃÂºÃ‘â€“", "ÃÂÃÂ°ÃÂ»ÃÂ°ÃÂ´Ã‘â€¹", "ÃÂ¡ÃÂºÃÂ°ÃÂ¿Ã‘â€“Ã‘ÂÃÂ²ÃÂ°ÃÂ½ÃÂ° ÃÂ²ÃÂ° Ã‘Å¾ÃÂ²ÃÂ¾ÃÂ´", "ÃÅ¸Ã‘â‚¬ÃÂ°ÃÂ²Ã‘â€“ÃÂ»Ã‘Å’ÃÂ½ÃÂ°", "Ãâ€™Ã‘â€¹ÃÂ¿Ã‘â‚¬ÃÂ°Ã‘Å¾", "ÃÅ¸ÃÂ° ÃÂ°ÃÂ»Ã‘â€žÃÂ°ÃÂ²Ã‘â€“Ã‘â€ ÃÂµ", "Ãâ€™Ã‘â€¹ÃÂ¿ÃÂ°ÃÂ´ÃÂºÃÂ¾ÃÂ²ÃÂ°", "ÃÅ¸ÃÂ°ÃÂºÃÂ°ÃÂ·ÃÂ°Ã‘â€ Ã‘Å’ Ã‘Æ’Ã‘ÂÃÂµ", "ÃÂ£ ÃÂ¿ÃÂ¾Ã‘â‚¬Ã‘â€ Ã‘â€¹Ã‘â€“", "Ãâ€”ÃÂ°Ã‘ÂÃ‘â€šÃÂ°ÃÂ»ÃÂ¾Ã‘ÂÃ‘Â", "Ãâ€œÃÂ°Ã‘â€šÃÂ¾ÃÂ²ÃÂ°", "Ãâ€ÃÂ°ÃÂ»ÃÂµÃÂ¹", "ÃÅ¸ÃÂ°Ã‘Å¾Ã‘â€šÃÂ°Ã‘â‚¬Ã‘â€¹Ã‘â€ Ã‘Å’", "ÃÂÃÂ°Ã‘ÂÃ‘â€šÃ‘Æ’ÃÂ¿ÃÂ½Ã‘â€¹ Ã‘Å¾Ã‘â‚¬ÃÂ¾ÃÂº", "Ãâ€™Ã‘â€¹ÃÂ´ÃÂ°Ã‘â€šÃÂ½ÃÂ°! Ãâ€œÃ‘ÂÃ‘â€šÃ‘â€¹ Ã‘Å¾Ã‘â‚¬ÃÂ¾ÃÂº ÃÂ·ÃÂ°ÃÂ²ÃÂµÃ‘â‚¬Ã‘Ë†ÃÂ°ÃÂ½Ã‘â€¹.", "ÃÂ¢Ã‘â€¹ ÃÂ°ÃÂ´ÃÂºÃÂ°ÃÂ·ÃÂ°Ã‘Å¾ ÃÂ½ÃÂ° Ã‘Å¾Ã‘ÂÃÂµ ÃÂ±ÃÂ°Ã‘â€¡ÃÂ½Ã‘â€¹Ã‘Â ÃÂºÃÂ°Ã‘â‚¬Ã‘â€šÃÂºÃ‘â€“.", "ÃÂ£Ã‘ÂÃ‘â€˜ ÃÂ·Ã‘â‚¬ÃÂ¾ÃÂ±ÃÂ»ÃÂµÃÂ½ÃÂ°. ÃÂ£Ã‘ÂÃÂµ ÃÂ±ÃÂ°Ã‘â€¡ÃÂ½Ã‘â€¹Ã‘Â ÃÂºÃÂ°Ã‘â‚¬Ã‘â€šÃÂºÃ‘â€“ ÃÂ¼ÃÂ°Ã‘Å½Ã‘â€ Ã‘Å’ Ã‘â€šÃ‘â‚¬Ã‘â€¹ ÃÂ·ÃÂ¾Ã‘â‚¬ÃÂºÃ‘â€“.", "ÃÂÃ‘ÂÃÂ¼ÃÂ° Ã‘â€¡ÃÂ°ÃÂ³ÃÂ¾ ÃÂ¿ÃÂ°ÃÂºÃÂ°ÃÂ·ÃÂ°Ã‘â€ Ã‘Å’.", "ÃÂ£ÃÂºÃÂ»Ã‘Å½Ã‘â€¡Ã‘â€¹ ÃÅ¸ÃÂ°ÃÂºÃÂ°ÃÂ·ÃÂ°Ã‘â€ Ã‘Å’ Ã‘Æ’Ã‘ÂÃÂµ ÃÂ°ÃÂ±ÃÂ¾ ÃÂ´ÃÂ°ÃÂ´ÃÂ°ÃÂ¹ ÃÂºÃÂ°Ã‘â‚¬Ã‘â€šÃÂºÃ‘â€“.", "ÃÅ¡ÃÂ°ÃÂ¿Ã‘â€“Ã‘ÂÃÂ²ÃÂ°Ã‘â€ Ã‘Å’", "OK", "ÃÂ Ã‘ÂÃÂ´ÃÂ°ÃÂ³ÃÂ°ÃÂ²ÃÂ°Ã‘â€ Ã‘Å’ ÃÂºÃÂ°Ã‘â‚¬Ã‘â€šÃÂºÃ‘Æ’", "Ãâ€”ÃÂ¼Ã‘ÂÃÂ½Ã‘â€“ ÃÂºÃÂ°Ã‘â‚¬Ã‘â€šÃÂºÃ‘Æ’, ÃÂ½ÃÂµ ÃÂ¿ÃÂ°ÃÂºÃ‘â€“ÃÂ´ÃÂ°Ã‘Å½Ã‘â€¡Ã‘â€¹ Ã‘â€šÃ‘â‚¬Ã‘ÂÃÂ½Ã‘â€“Ã‘â‚¬ÃÂ¾Ã‘Å¾ÃÂºÃ‘Æ’.", "ÃÅ¸ÃÂ°ÃÂ¼Ã‘â€¹ÃÂ»ÃÂºÃÂ° / ÃÂ·Ã‘â€¹Ã‘â€¦ÃÂ¾ÃÂ´ÃÂ½ÃÂ°ÃÂµ ÃÂ·ÃÂ½ÃÂ°Ã‘â€¡Ã‘ÂÃÂ½ÃÂ½ÃÂµ", "ÃÅ¸ÃÂ°ÃÂ´ÃÂºÃÂ°ÃÂ·ÃÂºÃÂ° ÃÂ°ÃÂ±ÃÂ¾ ÃÂ¿Ã‘â‚¬ÃÂ°ÃÂ²Ã‘â€“ÃÂ»ÃÂ°", "ÃÅ¡ÃÂ°ÃÂ»Ã‘â€“ ÃÂ·Ã‘â‚¬ÃÂ¾ÃÂ±ÃÂ»ÃÂµÃÂ½ÃÂ°", "Ãâ€ÃÂ·ÃÂµ", "Ãâ€”ÃÂ°Ã‘â€¦ÃÂ°ÃÂ²ÃÂ°Ã‘â€ Ã‘Å’", "Ãâ€™Ã‘â€¹ÃÂ´ÃÂ°ÃÂ»Ã‘â€“Ã‘â€ Ã‘Å’", "ÃÂÃÂ´ÃÂ¼ÃÂµÃÂ½ÃÂ°", "Ãâ€™Ã‘â€¹ÃÂ´ÃÂ°ÃÂ»Ã‘â€“Ã‘â€ Ã‘Å’ ÃÂºÃÂ°Ã‘â‚¬Ã‘â€šÃÂºÃ‘Æ’?", "Ãâ€œÃ‘ÂÃ‘â€šÃÂ° ÃÂºÃÂ°Ã‘â‚¬Ã‘â€šÃÂºÃÂ° ÃÂ±Ã‘Æ’ÃÂ´ÃÂ·ÃÂµ ÃÂ²Ã‘â€¹ÃÂ´ÃÂ°ÃÂ»ÃÂµÃÂ½ÃÂ° ÃÂ· Ã‘Æ’Ã‘â‚¬ÃÂ¾ÃÂºÃÂ°.", "ÃÅ“ÃÂ¾ÃÂ²ÃÂ° Ã‘â€“ÃÂ½Ã‘â€šÃ‘ÂÃ‘â‚¬Ã‘â€žÃÂµÃÂ¹Ã‘ÂÃ‘Æ’")
        "es" -> UiText("AtrÃƒÂ¡s", "Ocultar lecciones ocultas", "Mostrar lecciones ocultas", "Ajustes", "Copiado al campo", "Correcto", "CorrÃƒÂ­gelo", "AlfabÃƒÂ©tico", "Aleatorio", "Mostrar todo", "En bloque", "Quedan", "Hechas", "Siguiente", "Repetir", "Siguiente lecciÃƒÂ³n", "Ã‚Â¡Excelente! Esta lecciÃƒÂ³n estÃƒÂ¡ completa.", "Respondiste todas las tarjetas visibles.", "Todo listo. Todas las tarjetas visibles tienen tres estrellas.", "Nada que mostrar.", "Activa Mostrar todo o aÃƒÂ±ade tarjetas.", "Copiar", "OK", "Editar tarjeta", "Edita esta tarjeta sin salir de la prÃƒÂ¡ctica.", "Error / valor origen", "Pista o regla", "Fecha", "DÃƒÂ³nde", "Guardar", "Eliminar", "Cancelar", "Ã‚Â¿Eliminar tarjeta?", "Esta tarjeta se eliminarÃƒÂ¡ de la lecciÃƒÂ³n.", "Idioma de la interfaz")
        "uk" -> UiText("ÃÂÃÂ°ÃÂ·ÃÂ°ÃÂ´", "ÃÂ¡Ã‘â€¦ÃÂ¾ÃÂ²ÃÂ°Ã‘â€šÃÂ¸ ÃÂ¿Ã‘â‚¬ÃÂ¸Ã‘â€¦ÃÂ¾ÃÂ²ÃÂ°ÃÂ½Ã‘â€“ Ã‘Æ’Ã‘â‚¬ÃÂ¾ÃÂºÃÂ¸", "ÃÅ¸ÃÂ¾ÃÂºÃÂ°ÃÂ·ÃÂ°Ã‘â€šÃÂ¸ ÃÂ¿Ã‘â‚¬ÃÂ¸Ã‘â€¦ÃÂ¾ÃÂ²ÃÂ°ÃÂ½Ã‘â€“ Ã‘Æ’Ã‘â‚¬ÃÂ¾ÃÂºÃÂ¸", "ÃÂÃÂ°ÃÂ»ÃÂ°Ã‘Ë†Ã‘â€šÃ‘Æ’ÃÂ²ÃÂ°ÃÂ½ÃÂ½Ã‘Â", "ÃÂ¡ÃÂºÃÂ¾ÃÂ¿Ã‘â€“ÃÂ¹ÃÂ¾ÃÂ²ÃÂ°ÃÂ½ÃÂ¾ Ã‘Æ’ ÃÂ¿ÃÂ¾ÃÂ»ÃÂµ", "ÃÅ¸Ã‘â‚¬ÃÂ°ÃÂ²ÃÂ¸ÃÂ»Ã‘Å’ÃÂ½ÃÂ¾", "Ãâ€™ÃÂ¸ÃÂ¿Ã‘â‚¬ÃÂ°ÃÂ²", "Ãâ€”ÃÂ° ÃÂ°ÃÂ»Ã‘â€žÃÂ°ÃÂ²Ã‘â€“Ã‘â€šÃÂ¾ÃÂ¼", "Ãâ€™ÃÂ¸ÃÂ¿ÃÂ°ÃÂ´ÃÂºÃÂ¾ÃÂ²ÃÂ¾", "ÃÅ¸ÃÂ¾ÃÂºÃÂ°ÃÂ·ÃÂ°Ã‘â€šÃÂ¸ ÃÂ²Ã‘ÂÃ‘â€“", "ÃÂ£ ÃÂ¿ÃÂ¾Ã‘â‚¬Ã‘â€ Ã‘â€“Ã‘â€”", "Ãâ€”ÃÂ°ÃÂ»ÃÂ¸Ã‘Ë†ÃÂ¸ÃÂ»ÃÂ¾Ã‘ÂÃ‘Å’", "Ãâ€œÃÂ¾Ã‘â€šÃÂ¾ÃÂ²ÃÂ¾", "Ãâ€ÃÂ°ÃÂ»Ã‘â€“", "ÃÅ¸ÃÂ¾ÃÂ²Ã‘â€šÃÂ¾Ã‘â‚¬ÃÂ¸Ã‘â€šÃÂ¸", "ÃÂÃÂ°Ã‘ÂÃ‘â€šÃ‘Æ’ÃÂ¿ÃÂ½ÃÂ¸ÃÂ¹ Ã‘Æ’Ã‘â‚¬ÃÂ¾ÃÂº", "ÃÂ§Ã‘Æ’ÃÂ´ÃÂ¾ÃÂ²ÃÂ¾! ÃÂ£Ã‘â‚¬ÃÂ¾ÃÂº ÃÂ·ÃÂ°ÃÂ²ÃÂµÃ‘â‚¬Ã‘Ë†ÃÂµÃÂ½ÃÂ¾.", "ÃÂ¢ÃÂ¸ ÃÂ²Ã‘â€“ÃÂ´ÃÂ¿ÃÂ¾ÃÂ²Ã‘â€“ÃÂ² ÃÂ½ÃÂ° ÃÂ²Ã‘ÂÃ‘â€“ ÃÂ²ÃÂ¸ÃÂ´ÃÂ¸ÃÂ¼Ã‘â€“ ÃÂºÃÂ°Ã‘â‚¬Ã‘â€šÃÂºÃÂ¸.", "ÃÂ£Ã‘ÂÃÂµ ÃÂ·Ã‘â‚¬ÃÂ¾ÃÂ±ÃÂ»ÃÂµÃÂ½ÃÂ¾. ÃÂ£Ã‘ÂÃ‘â€“ ÃÂ²ÃÂ¸ÃÂ´ÃÂ¸ÃÂ¼Ã‘â€“ ÃÂºÃÂ°Ã‘â‚¬Ã‘â€šÃÂºÃÂ¸ ÃÂ¼ÃÂ°Ã‘Å½Ã‘â€šÃ‘Å’ Ã‘â€šÃ‘â‚¬ÃÂ¸ ÃÂ·Ã‘â€“Ã‘â‚¬ÃÂºÃÂ¸.", "ÃÂÃ‘â€“Ã‘â€¡ÃÂ¾ÃÂ³ÃÂ¾ ÃÂ¿ÃÂ¾ÃÂºÃÂ°ÃÂ·ÃÂ°Ã‘â€šÃÂ¸.", "ÃÂ£ÃÂ²Ã‘â€“ÃÂ¼ÃÂºÃÂ½ÃÂ¸ ÃÅ¸ÃÂ¾ÃÂºÃÂ°ÃÂ·ÃÂ°Ã‘â€šÃÂ¸ ÃÂ²Ã‘ÂÃ‘â€“ ÃÂ°ÃÂ±ÃÂ¾ ÃÂ´ÃÂ¾ÃÂ´ÃÂ°ÃÂ¹ ÃÂºÃÂ°Ã‘â‚¬Ã‘â€šÃÂºÃÂ¸.", "ÃÅ¡ÃÂ¾ÃÂ¿Ã‘â€“Ã‘Å½ÃÂ²ÃÂ°Ã‘â€šÃÂ¸", "OK", "ÃÂ ÃÂµÃÂ´ÃÂ°ÃÂ³Ã‘Æ’ÃÂ²ÃÂ°Ã‘â€šÃÂ¸ ÃÂºÃÂ°Ã‘â‚¬Ã‘â€šÃÂºÃ‘Æ’", "ÃÂ ÃÂµÃÂ´ÃÂ°ÃÂ³Ã‘Æ’ÃÂ¹ ÃÂºÃÂ°Ã‘â‚¬Ã‘â€šÃÂºÃ‘Æ’, ÃÂ½ÃÂµ ÃÂ²ÃÂ¸Ã‘â€¦ÃÂ¾ÃÂ´Ã‘ÂÃ‘â€¡ÃÂ¸ ÃÂ· ÃÂ¿Ã‘â‚¬ÃÂ°ÃÂºÃ‘â€šÃÂ¸ÃÂºÃÂ¸.", "ÃÅ¸ÃÂ¾ÃÂ¼ÃÂ¸ÃÂ»ÃÂºÃÂ° / ÃÂ²ÃÂ¸Ã‘â€¦Ã‘â€“ÃÂ´ÃÂ½ÃÂµ ÃÂ·ÃÂ½ÃÂ°Ã‘â€¡ÃÂµÃÂ½ÃÂ½Ã‘Â", "ÃÅ¸Ã‘â€“ÃÂ´ÃÂºÃÂ°ÃÂ·ÃÂºÃÂ° ÃÂ°ÃÂ±ÃÂ¾ ÃÂ¿Ã‘â‚¬ÃÂ°ÃÂ²ÃÂ¸ÃÂ»ÃÂ¾", "ÃÅ¡ÃÂ¾ÃÂ»ÃÂ¸ ÃÂ·Ã‘â‚¬ÃÂ¾ÃÂ±ÃÂ»ÃÂµÃÂ½ÃÂ¾", "Ãâ€ÃÂµ", "Ãâ€”ÃÂ±ÃÂµÃ‘â‚¬ÃÂµÃÂ³Ã‘â€šÃÂ¸", "Ãâ€™ÃÂ¸ÃÂ´ÃÂ°ÃÂ»ÃÂ¸Ã‘â€šÃÂ¸", "ÃÂ¡ÃÂºÃÂ°Ã‘ÂÃ‘Æ’ÃÂ²ÃÂ°Ã‘â€šÃÂ¸", "Ãâ€™ÃÂ¸ÃÂ´ÃÂ°ÃÂ»ÃÂ¸Ã‘â€šÃÂ¸ ÃÂºÃÂ°Ã‘â‚¬Ã‘â€šÃÂºÃ‘Æ’?", "ÃÂ¦Ã‘Å½ ÃÂºÃÂ°Ã‘â‚¬Ã‘â€šÃÂºÃ‘Æ’ ÃÂ±Ã‘Æ’ÃÂ´ÃÂµ ÃÂ²ÃÂ¸ÃÂ´ÃÂ°ÃÂ»ÃÂµÃÂ½ÃÂ¾ ÃÂ· Ã‘Æ’Ã‘â‚¬ÃÂ¾ÃÂºÃ‘Æ’.", "ÃÅ“ÃÂ¾ÃÂ²ÃÂ° Ã‘â€“ÃÂ½Ã‘â€šÃÂµÃ‘â‚¬Ã‘â€žÃÂµÃÂ¹Ã‘ÂÃ‘Æ’")
        "ru" -> UiText("ÃÂÃÂ°ÃÂ·ÃÂ°ÃÂ´", "ÃÂ¡ÃÂºÃ‘â‚¬Ã‘â€¹Ã‘â€šÃ‘Å’ Ã‘ÂÃÂºÃ‘â‚¬Ã‘â€¹Ã‘â€šÃ‘â€¹ÃÂµ Ã‘Æ’Ã‘â‚¬ÃÂ¾ÃÂºÃÂ¸", "ÃÅ¸ÃÂ¾ÃÂºÃÂ°ÃÂ·ÃÂ°Ã‘â€šÃ‘Å’ Ã‘ÂÃÂºÃ‘â‚¬Ã‘â€¹Ã‘â€šÃ‘â€¹ÃÂµ Ã‘Æ’Ã‘â‚¬ÃÂ¾ÃÂºÃÂ¸", "ÃÂÃÂ°Ã‘ÂÃ‘â€šÃ‘â‚¬ÃÂ¾ÃÂ¹ÃÂºÃÂ¸", "ÃÂ¡ÃÂºÃÂ¾ÃÂ¿ÃÂ¸Ã‘â‚¬ÃÂ¾ÃÂ²ÃÂ°ÃÂ½ÃÂ¾ ÃÂ² ÃÂ¿ÃÂ¾ÃÂ»ÃÂµ", "Ãâ€™ÃÂµÃ‘â‚¬ÃÂ½ÃÂ¾", "ÃËœÃ‘ÂÃÂ¿Ã‘â‚¬ÃÂ°ÃÂ²Ã‘Å’", "ÃÅ¸ÃÂ¾ ÃÂ°ÃÂ»Ã‘â€žÃÂ°ÃÂ²ÃÂ¸Ã‘â€šÃ‘Æ’", "ÃÂ¡ÃÂ»Ã‘Æ’Ã‘â€¡ÃÂ°ÃÂ¹ÃÂ½ÃÂ¾", "ÃÅ¸ÃÂ¾ÃÂºÃÂ°ÃÂ·ÃÂ°Ã‘â€šÃ‘Å’ ÃÂ²Ã‘ÂÃÂµ", "Ãâ€™ ÃÂ¿ÃÂ¾Ã‘â‚¬Ã‘â€ ÃÂ¸ÃÂ¸", "ÃÅ¾Ã‘ÂÃ‘â€šÃÂ°ÃÂ»ÃÂ¾Ã‘ÂÃ‘Å’", "Ãâ€œÃÂ¾Ã‘â€šÃÂ¾ÃÂ²ÃÂ¾", "Ãâ€ÃÂ°ÃÂ»ÃÂµÃÂµ", "ÃÅ¸ÃÂ¾ÃÂ²Ã‘â€šÃÂ¾Ã‘â‚¬ÃÂ¸Ã‘â€šÃ‘Å’", "ÃÂ¡ÃÂ»ÃÂµÃÂ´Ã‘Æ’Ã‘Å½Ã‘â€°ÃÂ¸ÃÂ¹ Ã‘Æ’Ã‘â‚¬ÃÂ¾ÃÂº", "ÃÅ¾Ã‘â€šÃÂ»ÃÂ¸Ã‘â€¡ÃÂ½ÃÂ¾! ÃÂ£Ã‘â‚¬ÃÂ¾ÃÂº ÃÂ·ÃÂ°ÃÂ²ÃÂµÃ‘â‚¬Ã‘Ë†ÃÂµÃÂ½.", "ÃÂ¢Ã‘â€¹ ÃÂ¾Ã‘â€šÃÂ²ÃÂµÃ‘â€šÃÂ¸ÃÂ» ÃÂ½ÃÂ° ÃÂ²Ã‘ÂÃÂµ ÃÂ²ÃÂ¸ÃÂ´ÃÂ¸ÃÂ¼Ã‘â€¹ÃÂµ ÃÂºÃÂ°Ã‘â‚¬Ã‘â€šÃÂ¾Ã‘â€¡ÃÂºÃÂ¸.", "Ãâ€™Ã‘ÂÃÂµ ÃÂ³ÃÂ¾Ã‘â€šÃÂ¾ÃÂ²ÃÂ¾. ÃÂ£ ÃÂ²Ã‘ÂÃÂµÃ‘â€¦ ÃÂ²ÃÂ¸ÃÂ´ÃÂ¸ÃÂ¼Ã‘â€¹Ã‘â€¦ ÃÂºÃÂ°Ã‘â‚¬Ã‘â€šÃÂ¾Ã‘â€¡ÃÂµÃÂº Ã‘â€šÃ‘â‚¬ÃÂ¸ ÃÂ·ÃÂ²ÃÂµÃÂ·ÃÂ´Ã‘â€¹.", "ÃÂÃÂµÃ‘â€¡ÃÂµÃÂ³ÃÂ¾ ÃÂ¿ÃÂ¾ÃÂºÃÂ°ÃÂ·ÃÂ°Ã‘â€šÃ‘Å’.", "Ãâ€™ÃÂºÃÂ»Ã‘Å½Ã‘â€¡ÃÂ¸ ÃÅ¸ÃÂ¾ÃÂºÃÂ°ÃÂ·ÃÂ°Ã‘â€šÃ‘Å’ ÃÂ²Ã‘ÂÃÂµ ÃÂ¸ÃÂ»ÃÂ¸ ÃÂ´ÃÂ¾ÃÂ±ÃÂ°ÃÂ²Ã‘Å’ ÃÂºÃÂ°Ã‘â‚¬Ã‘â€šÃÂ¾Ã‘â€¡ÃÂºÃÂ¸.", "ÃÅ¡ÃÂ¾ÃÂ¿ÃÂ¸Ã‘â‚¬ÃÂ¾ÃÂ²ÃÂ°Ã‘â€šÃ‘Å’", "OK", "ÃÂ ÃÂµÃÂ´ÃÂ°ÃÂºÃ‘â€šÃÂ¸Ã‘â‚¬ÃÂ¾ÃÂ²ÃÂ°Ã‘â€šÃ‘Å’ ÃÂºÃÂ°Ã‘â‚¬Ã‘â€šÃÂ¾Ã‘â€¡ÃÂºÃ‘Æ’", "ÃÂ ÃÂµÃÂ´ÃÂ°ÃÂºÃ‘â€šÃÂ¸Ã‘â‚¬Ã‘Æ’ÃÂ¹ ÃÂºÃÂ°Ã‘â‚¬Ã‘â€šÃÂ¾Ã‘â€¡ÃÂºÃ‘Æ’, ÃÂ½ÃÂµ ÃÂ²Ã‘â€¹Ã‘â€¦ÃÂ¾ÃÂ´Ã‘Â ÃÂ¸ÃÂ· Ã‘â€šÃ‘â‚¬ÃÂµÃÂ½ÃÂ¸Ã‘â‚¬ÃÂ¾ÃÂ²ÃÂºÃÂ¸.", "ÃÅ¾Ã‘Ë†ÃÂ¸ÃÂ±ÃÂºÃÂ° / ÃÂ¸Ã‘ÂÃ‘â€¦ÃÂ¾ÃÂ´ÃÂ½ÃÂ¾ÃÂµ ÃÂ·ÃÂ½ÃÂ°Ã‘â€¡ÃÂµÃÂ½ÃÂ¸ÃÂµ", "ÃÅ¸ÃÂ¾ÃÂ´Ã‘ÂÃÂºÃÂ°ÃÂ·ÃÂºÃÂ° ÃÂ¸ÃÂ»ÃÂ¸ ÃÂ¿Ã‘â‚¬ÃÂ°ÃÂ²ÃÂ¸ÃÂ»ÃÂ¾", "ÃÅ¡ÃÂ¾ÃÂ³ÃÂ´ÃÂ° Ã‘ÂÃÂ´ÃÂµÃÂ»ÃÂ°ÃÂ½ÃÂ¾", "Ãâ€œÃÂ´ÃÂµ", "ÃÂ¡ÃÂ¾Ã‘â€¦Ã‘â‚¬ÃÂ°ÃÂ½ÃÂ¸Ã‘â€šÃ‘Å’", "ÃÂ£ÃÂ´ÃÂ°ÃÂ»ÃÂ¸Ã‘â€šÃ‘Å’", "ÃÅ¾Ã‘â€šÃÂ¼ÃÂµÃÂ½ÃÂ°", "ÃÂ£ÃÂ´ÃÂ°ÃÂ»ÃÂ¸Ã‘â€šÃ‘Å’ ÃÂºÃÂ°Ã‘â‚¬Ã‘â€šÃÂ¾Ã‘â€¡ÃÂºÃ‘Æ’?", "ÃÂ­Ã‘â€šÃÂ° ÃÂºÃÂ°Ã‘â‚¬Ã‘â€šÃÂ¾Ã‘â€¡ÃÂºÃÂ° ÃÂ±Ã‘Æ’ÃÂ´ÃÂµÃ‘â€š Ã‘Æ’ÃÂ´ÃÂ°ÃÂ»ÃÂµÃÂ½ÃÂ° ÃÂ¸ÃÂ· Ã‘Æ’Ã‘â‚¬ÃÂ¾ÃÂºÃÂ°.", "ÃÂ¯ÃÂ·Ã‘â€¹ÃÂº ÃÂ¸ÃÂ½Ã‘â€šÃÂµÃ‘â‚¬Ã‘â€žÃÂµÃÂ¹Ã‘ÂÃÂ°")
        "pl" -> UiText("Wstecz", "Ukryj ukryte lekcje", "PokaÃ…Â¼ ukryte lekcje", "Ustawienia", "Skopiowano do pola", "Dobrze", "Popraw", "Alfabetycznie", "Losowo", "PokaÃ…Â¼ wszystko", "W porcji", "ZostaÃ…â€šo", "Zrobione", "Dalej", "PowtÃƒÂ³rz", "NastÃ„â„¢pna lekcja", "Ã…Å¡wietnie! Lekcja zakoÃ…â€žczona.", "Odpowiedziano na wszystkie widoczne karty.", "Gotowe. Wszystkie widoczne karty majÃ„â€¦ trzy gwiazdki.", "Nic do pokazania.", "WÃ…â€šÃ„â€¦cz PokaÃ…Â¼ wszystko albo dodaj karty.", "Kopiuj", "OK", "Edytuj kartÃ„â„¢", "Edytuj tÃ„â„¢ kartÃ„â„¢ bez wychodzenia z nauki.", "BÃ…â€šÃ„â€¦d / wartoÃ…â€ºÃ„â€¡ Ã…ÂºrÃƒÂ³dÃ…â€šowa", "PodpowiedÃ…Âº lub reguÃ…â€ša", "Data", "Gdzie", "Zapisz", "UsuÃ…â€ž", "Anuluj", "UsunÃ„â€¦Ã„â€¡ kartÃ„â„¢?", "Ta karta zostanie usuniÃ„â„¢ta z lekcji.", "JÃ„â„¢zyk interfejsu")
        else -> UiText("Back", "Hide hidden lessons", "Show hidden lessons", "Settings", "Copied to input", "Correct", "Make it right", "Alphabetical", "Random", "Show all", "In portion", "Left", "Done", "Next", "Repeat", "Next lesson", "Excellent work. This lesson is complete!", "You answered every visible card.", "All done. Every visible card already has three stars.", "Nothing to show yet.", "Turn on Show all or add cards to keep practicing.", "Copy", "OK", "Edit card", "Update this card without leaving practice.", "Mistake made / source value", "Hint or rule", "Made at", "Where", "Save", "Delete", "Cancel", "Delete card?", "This card will be removed from the lesson.", "Interface language")
    }
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
    var textToSpeechReady by remember { mutableStateOf(false) }
    val textToSpeech = remember {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            textToSpeechReady = status == TextToSpeech.SUCCESS
        }
        engine
    }
    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            null
        }
    }
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
    DisposableEffect(speechRecognizer) {
        val listener = object : RecognitionListener {
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
        speechRecognizer?.setRecognitionListener(listener)
        onDispose {
            speechRecognizer?.destroy()
        }
    }

    DisposableEffect(textToSpeech) {
        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
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
            speechRecognizer?.stopListening()
            return
        }
        if (speechRecognizer == null) {
            viewModel.showMessage("Voice input is not available on this device")
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
        voiceTarget = target
        voiceLanguageTag = language.first
        voiceExpectedLanguage = language.second
        viewModel.showMessage("Speak ${language.second}")
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
        speechRecognizer?.stopListening()
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
            speakText(
                state.translationOutput,
                languageForVoice(state.quickVocabularySourceLanguage, state.translationOutput, state.interfaceLanguage).first
            )
        }
    }
    LaunchedEffect(pendingVoiceStart) {
        if (!pendingVoiceStart || speechRecognizer == null) return@LaunchedEffect
        pendingVoiceStart = false
        voiceElapsedMs = 0L
        voiceStatus = "Listening..."
        voiceDialogVisible = true
        voiceRecording = true
        voiceHoldActive = false
        voiceHoldReleasedAt = 0L
        lastVoiceActivityAt = System.currentTimeMillis()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, voiceLanguageTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, voiceLanguageTag)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 60000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 60000L)
        }
        try {
            speechRecognizer.startListening(intent)
        } catch (_: Exception) {
            voiceRecording = false
            voiceDialogVisible = false
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
            languageLabel = if (voiceTarget == VoiceInputTarget.TRANSLATE_INPUT) "Translate Offline with Google Translate - $voiceExpectedLanguage" else voiceExpectedLanguage,
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
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Make Mistake", fontWeight = FontWeight.SemiBold, color = appTitleColor)
                            Text("v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        IconButton(onClick = {
                            titleActivated = true
                            val showAll = state.excludeMasteredCards
                            viewModel.setShowAllCards(showAll)
                            viewModel.showMessage(if (showAll) "Show all" else "Hide starred")
                        }) {
                            StudyHideStarIcon(active = !state.excludeMasteredCards)
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
                    if (state.screen == AppScreen.TRANSLATE) {
                        IconButton(onClick = {
                            titleActivated = true
                            showTranslationDownloadDialog = true
                        }) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Download translation languages")
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
                    answerLabel = state.currentCard?.answerInputLabel().orEmpty().ifBlank { ui.makeItRight },
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
                                normalizeAnswerText(state.answer) == normalizeAnswerText(card.correctText())
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
                                    viewModel.showMessage("Powered by Google Translate")
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
                    onQuickVoiceInput = {
                        titleActivated = true
                        if (state.workMode == WorkMode.TRANSLATE || state.workMode == WorkMode.SPLIT) {
                            viewModel.openTranslationMode(state.workMode)
                            startVoiceInput(VoiceInputTarget.TRANSLATE_INPUT)
                        } else {
                            startVoiceInput(VoiceInputTarget.QUICK_VOCABULARY)
                        }
                    },
                    onWorkModeChange = { mode -> titleActivated = true; viewModel.setWorkMode(mode) },
                    onImportLessons = {
                        titleActivated = true
                        importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                    }
                )
                AppScreen.TRANSLATE -> TranslateScreen(
                    state = state,
                    onSourceLanguageChange = viewModel::setQuickVocabularyTargetLanguage,
                    onTargetLanguageChange = viewModel::setQuickVocabularySourceLanguage,
                    onInputChange = viewModel::updateTranslationInput,
                    onClear = viewModel::clearTranslationInput,
                    onAddCard = viewModel::addTranslationCard,
                    onSwapLanguages = viewModel::swapQuickVocabularyLanguages,
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
              translationApiUrl = state.translationApiUrl,
              translationApiToken = state.translationApiToken,
                    downloadedTranslationLanguages = downloadedTranslationLanguages,
                    translationDownloadLanguage = translationDownloadLanguage,
                    translationDownloadingLabel = translationDownloadingLabel,
                    onTranslationDownloadLanguageChange = { translationDownloadLanguage = it },
                    onDownloadSelectedTranslationLanguage = { downloadGoogleLanguageModel(translationDownloadLanguage) },
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
              onQuickVocabularyTargetChange = viewModel::setQuickVocabularyTargetLanguage,
        onUseLocalTranslationChange = viewModel::setUseLocalTranslation,
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
                    color = BrandRedColor,
                    modifier = Modifier.graphicsLayer {
                        translationX = -56f * (1f - progress)
                        alpha = (1f - progress).coerceIn(0f, 1f)
                    }
                )
                Text(
                    text = "M",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Black,
                    color = BrandRedColor,
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
    displayTextSize: DisplayTextSize,
    controlSize: ControlSize,
    soundEffectsEnabled: Boolean,
    vibrationEnabled: Boolean,
    notificationIntervalDraft: String,
    notificationMaxDraft: String,
    quickVocabularySourceLanguage: String,
    quickVocabularyTargetLanguage: String,
    useLocalTranslation: Boolean,
    translationApiUrl: String,
    translationApiToken: String,
    downloadedTranslationLanguages: Set<String>,
    translationDownloadLanguage: String,
    translationDownloadingLabel: String?,
    onTranslationDownloadLanguageChange: (String) -> Unit,
    onDownloadSelectedTranslationLanguage: () -> Unit,
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
    onTranslationApiUrlChange: (String) -> Unit,
    onTranslationApiTokenChange: (String) -> Unit,
    onSaveNotificationInterval: () -> Unit,
    onSaveNotificationMax: () -> Unit,
    onDownloadTranslationLanguages: () -> Unit,
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
                    text = st("Default card side", "Standard-Kartenseite", "ÃÅ¸ÃÂ°Ã‘â€¡ÃÂ°Ã‘â€šÃÂºÃÂ¾ÃÂ²Ã‘â€¹ ÃÂ±ÃÂ¾ÃÂº ÃÂºÃÂ°Ã‘â‚¬Ã‘â€šÃÂºÃ‘â€“", "Lado inicial de la tarjeta", "ÃÅ¸ÃÂ¾Ã‘â€¡ÃÂ°Ã‘â€šÃÂºÃÂ¾ÃÂ²ÃÂ¸ÃÂ¹ ÃÂ±Ã‘â€“ÃÂº ÃÂºÃÂ°Ã‘â‚¬Ã‘â€šÃÂºÃÂ¸", "ÃÂ¡Ã‘â€šÃÂ¾Ã‘â‚¬ÃÂ¾ÃÂ½ÃÂ° ÃÂºÃÂ°Ã‘â‚¬Ã‘â€šÃÂ¾Ã‘â€¡ÃÂºÃÂ¸ ÃÂ¿ÃÂ¾ Ã‘Æ’ÃÂ¼ÃÂ¾ÃÂ»Ã‘â€¡ÃÂ°ÃÂ½ÃÂ¸Ã‘Å½", "DomyÃ…â€ºlna strona karty"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = st(
                        "Choose which side is shown first when a card opens.",
                        "WÃƒÂ¤hle, welche Seite beim Ãƒâ€“ffnen zuerst erscheint.",
                        "Ãâ€™Ã‘â€¹ÃÂ±ÃÂµÃ‘â‚¬Ã‘â€¹, Ã‘ÂÃÂºÃ‘â€“ ÃÂ±ÃÂ¾ÃÂº ÃÂ¿ÃÂ°ÃÂºÃÂ°ÃÂ·ÃÂ²ÃÂ°Ã‘â€ Ã‘Å’ ÃÂ¿ÃÂµÃ‘â‚¬Ã‘Ë†Ã‘â€¹ÃÂ¼ ÃÂ¿Ã‘â‚¬Ã‘â€¹ ÃÂ°ÃÂ´ÃÂºÃ‘â‚¬Ã‘â€¹Ã‘â€ Ã‘â€ Ã‘â€“ ÃÂºÃÂ°Ã‘â‚¬Ã‘â€šÃÂºÃ‘â€“.",
                        "Elige quÃƒÂ© lado se muestra primero al abrir una tarjeta.",
                        "Ãâ€™ÃÂ¸ÃÂ±ÃÂµÃ‘â‚¬ÃÂ¸, Ã‘ÂÃÂºÃÂ¸ÃÂ¹ ÃÂ±Ã‘â€“ÃÂº ÃÂ¿ÃÂ¾ÃÂºÃÂ°ÃÂ·Ã‘Æ’ÃÂ²ÃÂ°Ã‘â€šÃÂ¸ ÃÂ¿ÃÂµÃ‘â‚¬Ã‘Ë†ÃÂ¸ÃÂ¼ ÃÂ¿Ã‘â‚¬ÃÂ¸ ÃÂ²Ã‘â€“ÃÂ´ÃÂºÃ‘â‚¬ÃÂ¸Ã‘â€šÃ‘â€šÃ‘â€“ ÃÂºÃÂ°Ã‘â‚¬Ã‘â€šÃÂºÃÂ¸.",
                        "Ãâ€™Ã‘â€¹ÃÂ±ÃÂµÃ‘â‚¬ÃÂ¸, ÃÂºÃÂ°ÃÂºÃÂ°Ã‘Â Ã‘ÂÃ‘â€šÃÂ¾Ã‘â‚¬ÃÂ¾ÃÂ½ÃÂ° ÃÂ¿ÃÂ¾ÃÂºÃÂ°ÃÂ·Ã‘â€¹ÃÂ²ÃÂ°ÃÂµÃ‘â€šÃ‘ÂÃ‘Â ÃÂ¿ÃÂµÃ‘â‚¬ÃÂ²ÃÂ¾ÃÂ¹ ÃÂ¿Ã‘â‚¬ÃÂ¸ ÃÂ¾Ã‘â€šÃÂºÃ‘â‚¬Ã‘â€¹Ã‘â€šÃÂ¸ÃÂ¸ ÃÂºÃÂ°Ã‘â‚¬Ã‘â€šÃÂ¾Ã‘â€¡ÃÂºÃÂ¸.",
                        "Wybierz, ktÃƒÂ³ra strona pokazuje siÃ„â„¢ pierwsza po otwarciu karty."
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
                        text = st("Hide done cards", "Fertige Karten ausblenden", "ÃÂ¡Ã‘â€¦ÃÂ°ÃÂ²ÃÂ°Ã‘â€ Ã‘Å’ ÃÂ·Ã‘â‚¬ÃÂ¾ÃÂ±ÃÂ»ÃÂµÃÂ½Ã‘â€¹Ã‘Â ÃÂºÃÂ°Ã‘â‚¬Ã‘â€šÃÂºÃ‘â€“", "Ocultar tarjetas hechas", "ÃÂ¡Ã‘â€¦ÃÂ¾ÃÂ²ÃÂ°Ã‘â€šÃÂ¸ ÃÂ²ÃÂ¸ÃÂºÃÂ¾ÃÂ½ÃÂ°ÃÂ½Ã‘â€“ ÃÂºÃÂ°Ã‘â‚¬Ã‘â€šÃÂºÃÂ¸", "ÃÂ¡ÃÂºÃ‘â‚¬Ã‘â€¹ÃÂ²ÃÂ°Ã‘â€šÃ‘Å’ ÃÂ²Ã‘â€¹ÃÂ¿ÃÂ¾ÃÂ»ÃÂ½ÃÂµÃÂ½ÃÂ½Ã‘â€¹ÃÂµ ÃÂºÃÂ°Ã‘â‚¬Ã‘â€šÃÂ¾Ã‘â€¡ÃÂºÃÂ¸", "Ukryj zrobione karty"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = st(
                            "Do not show or count cards marked with 3 stars inside a lesson.",
                            "Karten mit 3 Sternen in der Lektion nicht anzeigen oder zÃƒÂ¤hlen.",
                            "ÃÂÃÂµ ÃÂ¿ÃÂ°ÃÂºÃÂ°ÃÂ·ÃÂ²ÃÂ°Ã‘â€ Ã‘Å’ Ã‘â€“ ÃÂ½ÃÂµ Ã‘Å¾ÃÂ»Ã‘â€“Ã‘â€¡ÃÂ²ÃÂ°Ã‘â€ Ã‘Å’ Ã‘Æ’ Ã‘Å¾Ã‘â‚¬ÃÂ¾ÃÂºÃ‘Æ’ ÃÂºÃÂ°Ã‘â‚¬Ã‘â€šÃÂºÃ‘â€“ ÃÂ· 3 ÃÂ·ÃÂ¾Ã‘â‚¬ÃÂºÃÂ°ÃÂ¼Ã‘â€“.",
                            "No mostrar ni contar dentro de la lecciÃƒÂ³n las tarjetas con 3 estrellas.",
                            "ÃÂÃÂµ ÃÂ¿ÃÂ¾ÃÂºÃÂ°ÃÂ·Ã‘Æ’ÃÂ²ÃÂ°Ã‘â€šÃÂ¸ ÃÂ¹ ÃÂ½ÃÂµ Ã‘â‚¬ÃÂ°Ã‘â€¦Ã‘Æ’ÃÂ²ÃÂ°Ã‘â€šÃÂ¸ ÃÂ² Ã‘Æ’Ã‘â‚¬ÃÂ¾Ã‘â€ Ã‘â€“ ÃÂºÃÂ°Ã‘â‚¬Ã‘â€šÃÂºÃÂ¸ ÃÂ· 3 ÃÂ·Ã‘â€“Ã‘â‚¬ÃÂºÃÂ°ÃÂ¼ÃÂ¸.",
                            "ÃÂÃÂµ ÃÂ¿ÃÂ¾ÃÂºÃÂ°ÃÂ·Ã‘â€¹ÃÂ²ÃÂ°Ã‘â€šÃ‘Å’ ÃÂ¸ ÃÂ½ÃÂµ Ã‘Æ’Ã‘â€¡ÃÂ¸Ã‘â€šÃ‘â€¹ÃÂ²ÃÂ°Ã‘â€šÃ‘Å’ ÃÂ² Ã‘Æ’Ã‘â‚¬ÃÂ¾ÃÂºÃÂµ ÃÂºÃÂ°Ã‘â‚¬Ã‘â€šÃÂ¾Ã‘â€¡ÃÂºÃÂ¸ Ã‘Â 3 ÃÂ·ÃÂ²ÃÂµÃÂ·ÃÂ´ÃÂ°ÃÂ¼ÃÂ¸.",
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
                    text = st("Card and feedback", "Karte und Feedback", "ÃÅ¡ÃÂ°Ã‘â‚¬Ã‘â€šÃÂºÃÂ° Ã‘â€“ ÃÂ²ÃÂ¾ÃÂ´ÃÂ³Ã‘Æ’ÃÂº", "Tarjeta y respuesta", "ÃÅ¡ÃÂ°Ã‘â‚¬Ã‘â€šÃÂºÃÂ° Ã‘â€“ ÃÂ²Ã‘â€“ÃÂ´ÃÂ³Ã‘Æ’ÃÂº", "ÃÅ¡ÃÂ°Ã‘â‚¬Ã‘â€šÃÂ¾Ã‘â€¡ÃÂºÃÂ° ÃÂ¸ ÃÂ¾Ã‘â€šÃÂºÃÂ»ÃÂ¸ÃÂº", "Karta i reakcje"),
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
                    title = st("Show card log", "Kartenlog anzeigen", "ÃÅ¸ÃÂ°ÃÂºÃÂ°ÃÂ·ÃÂ²ÃÂ°Ã‘â€ Ã‘Å’ ÃÂ»ÃÂ¾ÃÂ³ ÃÂºÃÂ°Ã‘â‚¬Ã‘â€šÃÂºÃ‘â€“", "Mostrar registro de tarjeta", "ÃÅ¸ÃÂ¾ÃÂºÃÂ°ÃÂ·Ã‘Æ’ÃÂ²ÃÂ°Ã‘â€šÃÂ¸ ÃÂ»ÃÂ¾ÃÂ³ ÃÂºÃÂ°Ã‘â‚¬Ã‘â€šÃÂºÃÂ¸", "ÃÅ¸ÃÂ¾ÃÂºÃÂ°ÃÂ·Ã‘â€¹ÃÂ²ÃÂ°Ã‘â€šÃ‘Å’ ÃÂ»ÃÂ¾ÃÂ³ ÃÂºÃÂ°Ã‘â‚¬Ã‘â€šÃÂ¾Ã‘â€¡ÃÂºÃÂ¸", "PokaÃ…Â¼ log karty"),
                    description = st(
                        "Show the M icon with mistakes and work log on study cards.",
                        "Zeigt das M-Symbol mit Fehlern und Arbeitslog auf Lernkarten.",
                        "ÃÅ¸ÃÂ°ÃÂºÃÂ°ÃÂ·ÃÂ²ÃÂ°ÃÂµ ÃÂ·ÃÂ½ÃÂ°Ã‘â€¡ÃÂ¾ÃÂº M ÃÂ· ÃÂ¿ÃÂ°ÃÂ¼Ã‘â€¹ÃÂ»ÃÂºÃÂ°ÃÂ¼Ã‘â€“ Ã‘â€“ ÃÂ»ÃÂ¾ÃÂ³ÃÂ°ÃÂ¼ ÃÂ¿Ã‘â‚¬ÃÂ°Ã‘â€ Ã‘â€¹ ÃÂ½ÃÂ° ÃÂºÃÂ°Ã‘â‚¬Ã‘â€šÃÂºÃÂ°Ã‘â€¦.",
                        "Muestra el icono M con errores y registro de trabajo en las tarjetas.",
                        "ÃÅ¸ÃÂ¾ÃÂºÃÂ°ÃÂ·Ã‘Æ’Ã‘â€ ÃÂ·ÃÂ½ÃÂ°Ã‘â€¡ÃÂ¾ÃÂº M ÃÂ· ÃÂ¿ÃÂ¾ÃÂ¼ÃÂ¸ÃÂ»ÃÂºÃÂ°ÃÂ¼ÃÂ¸ ÃÂ¹ ÃÂ»ÃÂ¾ÃÂ³ÃÂ¾ÃÂ¼ Ã‘â‚¬ÃÂ¾ÃÂ±ÃÂ¾Ã‘â€šÃÂ¸ ÃÂ½ÃÂ° ÃÂºÃÂ°Ã‘â‚¬Ã‘â€šÃÂºÃÂ°Ã‘â€¦.",
                        "ÃÅ¸ÃÂ¾ÃÂºÃÂ°ÃÂ·Ã‘â€¹ÃÂ²ÃÂ°ÃÂµÃ‘â€š ÃÂ·ÃÂ½ÃÂ°Ã‘â€¡ÃÂ¾ÃÂº M Ã‘Â ÃÂ¾Ã‘Ë†ÃÂ¸ÃÂ±ÃÂºÃÂ°ÃÂ¼ÃÂ¸ ÃÂ¸ ÃÂ»ÃÂ¾ÃÂ³ÃÂ¾ÃÂ¼ Ã‘â‚¬ÃÂ°ÃÂ±ÃÂ¾Ã‘â€šÃ‘â€¹ ÃÂ½ÃÂ° ÃÂºÃÂ°Ã‘â‚¬Ã‘â€šÃÂ¾Ã‘â€¡ÃÂºÃÂ°Ã‘â€¦.",
                        "Pokazuje ikonÃ„â„¢ M z bÃ…â€šÃ„â„¢dami i logiem pracy na kartach."
                    ),
                    checked = showCardLog,
                    onCheckedChange = onShowCardLogChange
                )
                SettingsSwitchRow(
                    title = st("Sound effects", "Soundeffekte", "Ãâ€œÃ‘Æ’ÃÂºÃÂ°ÃÂ²Ã‘â€¹Ã‘Â Ã‘ÂÃ‘â€žÃÂµÃÂºÃ‘â€šÃ‘â€¹", "Efectos de sonido", "Ãâ€”ÃÂ²Ã‘Æ’ÃÂºÃÂ¾ÃÂ²Ã‘â€“ ÃÂµÃ‘â€žÃÂµÃÂºÃ‘â€šÃÂ¸", "Ãâ€”ÃÂ²Ã‘Æ’ÃÂºÃÂ¾ÃÂ²Ã‘â€¹ÃÂµ Ã‘ÂÃ‘â€žÃ‘â€žÃÂµÃÂºÃ‘â€šÃ‘â€¹", "Efekty dÃ…ÂºwiÃ„â„¢kowe"),
                    description = st(
                        "Play short sounds for app actions and the splash screen.",
                        "Spielt kurze Sounds fÃƒÂ¼r Aktionen und den Startbildschirm.",
                        "ÃÅ¸Ã‘â‚¬ÃÂ°ÃÂ¹ÃÂ³Ã‘â‚¬ÃÂ°ÃÂµ ÃÂºÃÂ°Ã‘â‚¬ÃÂ¾Ã‘â€šÃÂºÃ‘â€“Ã‘Â ÃÂ³Ã‘Æ’ÃÂºÃ‘â€“ ÃÂ´ÃÂ»Ã‘Â ÃÂ´ÃÂ·ÃÂµÃ‘ÂÃÂ½ÃÂ½Ã‘ÂÃ‘Å¾ Ã‘â€“ ÃÂ·ÃÂ°Ã‘ÂÃ‘â€šÃÂ°Ã‘Å¾ÃÂºÃ‘â€“.",
                        "Reproduce sonidos cortos para acciones y la pantalla inicial.",
                        "Ãâ€™Ã‘â€“ÃÂ´Ã‘â€šÃÂ²ÃÂ¾Ã‘â‚¬Ã‘Å½Ã‘â€ ÃÂºÃÂ¾Ã‘â‚¬ÃÂ¾Ã‘â€šÃÂºÃ‘â€“ ÃÂ·ÃÂ²Ã‘Æ’ÃÂºÃÂ¸ ÃÂ´ÃÂ»Ã‘Â ÃÂ´Ã‘â€“ÃÂ¹ Ã‘â€“ ÃÂ·ÃÂ°Ã‘ÂÃ‘â€šÃÂ°ÃÂ²ÃÂºÃÂ¸.",
                        "Ãâ€™ÃÂ¾Ã‘ÂÃÂ¿Ã‘â‚¬ÃÂ¾ÃÂ¸ÃÂ·ÃÂ²ÃÂ¾ÃÂ´ÃÂ¸Ã‘â€š ÃÂºÃÂ¾Ã‘â‚¬ÃÂ¾Ã‘â€šÃÂºÃÂ¸ÃÂµ ÃÂ·ÃÂ²Ã‘Æ’ÃÂºÃÂ¸ ÃÂ´ÃÂ»Ã‘Â ÃÂ´ÃÂµÃÂ¹Ã‘ÂÃ‘â€šÃÂ²ÃÂ¸ÃÂ¹ ÃÂ¸ ÃÂ·ÃÂ°Ã‘ÂÃ‘â€šÃÂ°ÃÂ²ÃÂºÃÂ¸.",
                        "Odtwarza krÃƒÂ³tkie dÃ…ÂºwiÃ„â„¢ki dla akcji i ekranu startowego."
                    ),
                    checked = soundEffectsEnabled,
                    onCheckedChange = onSoundEffectsEnabledChange
                )
                SettingsSwitchRow(
                    title = st("Vibration", "Vibration", "Ãâ€™Ã‘â€“ÃÂ±Ã‘â‚¬ÃÂ°Ã‘â€ Ã‘â€¹Ã‘Â", "VibraciÃƒÂ³n", "Ãâ€™Ã‘â€“ÃÂ±Ã‘â‚¬ÃÂ°Ã‘â€ Ã‘â€“Ã‘Â", "Ãâ€™ÃÂ¸ÃÂ±Ã‘â‚¬ÃÂ°Ã‘â€ ÃÂ¸Ã‘Â", "Wibracja"),
                    description = st(
                        "Use short haptic feedback for app actions.",
                        "Nutzt kurze haptische RÃƒÂ¼ckmeldung fÃƒÂ¼r Aktionen.",
                        "Ãâ€™Ã‘â€¹ÃÂºÃÂ°Ã‘â‚¬Ã‘â€¹Ã‘ÂÃ‘â€šÃÂ¾Ã‘Å¾ÃÂ²ÃÂ°ÃÂµ ÃÂºÃÂ°Ã‘â‚¬ÃÂ¾Ã‘â€šÃÂºÃ‘â€“ ÃÂ²Ã‘â€“ÃÂ±Ã‘â‚¬ÃÂ°ÃÂ°ÃÂ´ÃÂ³Ã‘Æ’ÃÂº ÃÂ´ÃÂ»Ã‘Â ÃÂ´ÃÂ·ÃÂµÃ‘ÂÃÂ½ÃÂ½Ã‘ÂÃ‘Å¾.",
                        "Usa respuesta hÃƒÂ¡ptica corta para acciones.",
                        "Ãâ€™ÃÂ¸ÃÂºÃÂ¾Ã‘â‚¬ÃÂ¸Ã‘ÂÃ‘â€šÃÂ¾ÃÂ²Ã‘Æ’Ã‘â€ ÃÂºÃÂ¾Ã‘â‚¬ÃÂ¾Ã‘â€šÃÂºÃÂ¸ÃÂ¹ ÃÂ²Ã‘â€“ÃÂ±Ã‘â‚¬ÃÂ¾ÃÂ²Ã‘â€“ÃÂ´ÃÂ³Ã‘Æ’ÃÂº ÃÂ´ÃÂ»Ã‘Â ÃÂ´Ã‘â€“ÃÂ¹.",
                        "ÃËœÃ‘ÂÃÂ¿ÃÂ¾ÃÂ»Ã‘Å’ÃÂ·Ã‘Æ’ÃÂµÃ‘â€š ÃÂºÃÂ¾Ã‘â‚¬ÃÂ¾Ã‘â€šÃÂºÃÂ¸ÃÂ¹ ÃÂ²ÃÂ¸ÃÂ±Ã‘â‚¬ÃÂ¾ÃÂ¾Ã‘â€šÃÂºÃÂ»ÃÂ¸ÃÂº ÃÂ´ÃÂ»Ã‘Â ÃÂ´ÃÂµÃÂ¹Ã‘ÂÃ‘â€šÃÂ²ÃÂ¸ÃÂ¹.",
                        "UÃ…Â¼ywa krÃƒÂ³tkiej reakcji haptycznej dla akcji."
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
                    label = "Native language",
                    value = quickVocabularySourceLanguage,
                    onValueChange = onQuickVocabularySourceChange
                )
                DictionaryLanguageDropdown(
                    label = "Target language",
                    value = quickVocabularyTargetLanguage,
                    onValueChange = onQuickVocabularyTargetChange
                )
                SettingsSwitchRow(
                    title = "Local translation",
                    description = "Use on-device Google Translate after downloading the selected language pair. Translations are powered by Google Translate and may be inaccurate.",
                    checked = useLocalTranslation,
                    onCheckedChange = onUseLocalTranslationChange
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
                        modifier = Modifier.weight(1f)
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
                    text = st("JSON template", "JSON-Vorlage", "ÃÂ¨ÃÂ°ÃÂ±ÃÂ»ÃÂ¾ÃÂ½ JSON", "Plantilla JSON", "ÃÂ¨ÃÂ°ÃÂ±ÃÂ»ÃÂ¾ÃÂ½ JSON", "ÃÂ¨ÃÂ°ÃÂ±ÃÂ»ÃÂ¾ÃÂ½ JSON", "Szablon JSON"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = st(
                        "Download an instruction-ready JSON template for generating lessons from your mistakes.",
                        "Lade eine JSON-Vorlage mit Anweisungen herunter, um Lektionen aus deinen Fehlern zu erzeugen.",
                        "ÃÂ¡ÃÂ¿ÃÂ°ÃÂ¼ÃÂ¿Ã‘Æ’ÃÂ¹ JSON-Ã‘Ë†ÃÂ°ÃÂ±ÃÂ»ÃÂ¾ÃÂ½ ÃÂ· Ã‘â€“ÃÂ½Ã‘ÂÃ‘â€šÃ‘â‚¬Ã‘Æ’ÃÂºÃ‘â€ Ã‘â€¹Ã‘ÂÃÂ¹ ÃÂ´ÃÂ»Ã‘Â Ã‘ÂÃ‘â€šÃÂ²ÃÂ°Ã‘â‚¬Ã‘ÂÃÂ½ÃÂ½Ã‘Â Ã‘Å¾Ã‘â‚¬ÃÂ¾ÃÂºÃÂ°Ã‘Å¾ ÃÂ· Ã‘â€šÃÂ²ÃÂ°Ã‘â€“Ã‘â€¦ ÃÂ¿ÃÂ°ÃÂ¼Ã‘â€¹ÃÂ»ÃÂ°ÃÂº.",
                        "Descarga una plantilla JSON lista como instrucciÃƒÂ³n para generar lecciones desde tus errores.",
                        "Ãâ€”ÃÂ°ÃÂ²ÃÂ°ÃÂ½Ã‘â€šÃÂ°ÃÂ¶ JSON-Ã‘Ë†ÃÂ°ÃÂ±ÃÂ»ÃÂ¾ÃÂ½ ÃÂ· Ã‘â€“ÃÂ½Ã‘ÂÃ‘â€šÃ‘â‚¬Ã‘Æ’ÃÂºÃ‘â€ Ã‘â€“Ã‘â€Ã‘Å½ ÃÂ´ÃÂ»Ã‘Â Ã‘ÂÃ‘â€šÃÂ²ÃÂ¾Ã‘â‚¬ÃÂµÃÂ½ÃÂ½Ã‘Â Ã‘Æ’Ã‘â‚¬ÃÂ¾ÃÂºÃ‘â€“ÃÂ² Ã‘â€“ÃÂ· Ã‘â€šÃÂ²ÃÂ¾Ã‘â€”Ã‘â€¦ ÃÂ¿ÃÂ¾ÃÂ¼ÃÂ¸ÃÂ»ÃÂ¾ÃÂº.",
                        "ÃÂ¡ÃÂºÃÂ°Ã‘â€¡ÃÂ°ÃÂ¹ JSON-Ã‘Ë†ÃÂ°ÃÂ±ÃÂ»ÃÂ¾ÃÂ½ Ã‘Â ÃÂ¸ÃÂ½Ã‘ÂÃ‘â€šÃ‘â‚¬Ã‘Æ’ÃÂºÃ‘â€ ÃÂ¸ÃÂµÃÂ¹ ÃÂ´ÃÂ»Ã‘Â Ã‘ÂÃÂ¾ÃÂ·ÃÂ´ÃÂ°ÃÂ½ÃÂ¸Ã‘Â Ã‘Æ’Ã‘â‚¬ÃÂ¾ÃÂºÃÂ¾ÃÂ² ÃÂ¸ÃÂ· Ã‘â€šÃÂ²ÃÂ¾ÃÂ¸Ã‘â€¦ ÃÂ¾Ã‘Ë†ÃÂ¸ÃÂ±ÃÂ¾ÃÂº.",
                        "Pobierz szablon JSON z instrukcjÃ„â€¦ do tworzenia lekcji z Twoich bÃ…â€šÃ„â„¢dÃƒÂ³w."
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
                    text = st("Notifications", "Benachrichtigungen", "ÃÂÃÂ¿ÃÂ°ÃÂ²Ã‘ÂÃ‘Ë†Ã‘â€¡Ã‘ÂÃÂ½ÃÂ½Ã‘â€“", "Notificaciones", "ÃÂ¡ÃÂ¿ÃÂ¾ÃÂ²Ã‘â€“Ã‘â€°ÃÂµÃÂ½ÃÂ½Ã‘Â", "ÃÂ£ÃÂ²ÃÂµÃÂ´ÃÂ¾ÃÂ¼ÃÂ»ÃÂµÃÂ½ÃÂ¸Ã‘Â", "Powiadomienia"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = st(
                        "Shows weighted reminders for cards with 0-2 stars. Cards with 0 stars appear most often; 3-star cards are excluded.",
                        "Zeigt gewichtete Erinnerungen fÃƒÂ¼r Karten mit 0-2 Sternen. 0 Sterne erscheinen am hÃƒÂ¤ufigsten; 3 Sterne sind ausgeschlossen.",
                        "ÃÅ¸ÃÂ°ÃÂºÃÂ°ÃÂ·ÃÂ²ÃÂ°ÃÂµ Ã‘Å¾ÃÂ·ÃÂ²ÃÂ°ÃÂ¶ÃÂ°ÃÂ½Ã‘â€¹Ã‘Â ÃÂ½ÃÂ°ÃÂ¿ÃÂ°ÃÂ¼Ã‘â€“ÃÂ½Ã‘â€¹ ÃÂ´ÃÂ»Ã‘Â ÃÂºÃÂ°Ã‘â‚¬Ã‘â€šÃÂ°ÃÂº ÃÂ· 0-2 ÃÂ·ÃÂ¾Ã‘â‚¬ÃÂºÃÂ°ÃÂ¼Ã‘â€“. 0 ÃÂ·ÃÂ¾Ã‘â‚¬ÃÂ°ÃÂº Ã‘â€šÃ‘â‚¬ÃÂ°ÃÂ¿ÃÂ»Ã‘ÂÃ‘Å½Ã‘â€ Ã‘Å’ Ã‘â€¡ÃÂ°Ã‘ÂÃ‘â€ ÃÂµÃÂ¹; 3 ÃÂ·ÃÂ¾Ã‘â‚¬ÃÂºÃ‘â€“ ÃÂ²Ã‘â€¹ÃÂºÃÂ»Ã‘Å½Ã‘â€¡ÃÂ°Ã‘Å½Ã‘â€ Ã‘â€ ÃÂ°.",
                        "Muestra recordatorios ponderados para tarjetas con 0-2 estrellas. Las de 0 salen mÃƒÂ¡s; las de 3 no salen.",
                        "ÃÅ¸ÃÂ¾ÃÂºÃÂ°ÃÂ·Ã‘Æ’Ã‘â€ ÃÂ·ÃÂ²ÃÂ°ÃÂ¶ÃÂµÃÂ½Ã‘â€“ ÃÂ½ÃÂ°ÃÂ³ÃÂ°ÃÂ´Ã‘Æ’ÃÂ²ÃÂ°ÃÂ½ÃÂ½Ã‘Â ÃÂ´ÃÂ»Ã‘Â ÃÂºÃÂ°Ã‘â‚¬Ã‘â€šÃÂ¾ÃÂº Ã‘â€“ÃÂ· 0-2 ÃÂ·Ã‘â€“Ã‘â‚¬ÃÂºÃÂ°ÃÂ¼ÃÂ¸. 0 ÃÂ·Ã‘â€“Ã‘â‚¬ÃÂ¾ÃÂº ÃÂ·'Ã‘ÂÃÂ²ÃÂ»Ã‘ÂÃ‘Å½Ã‘â€šÃ‘Å’Ã‘ÂÃ‘Â ÃÂ½ÃÂ°ÃÂ¹Ã‘â€¡ÃÂ°Ã‘ÂÃ‘â€šÃ‘â€“Ã‘Ë†ÃÂµ; 3 ÃÂ·Ã‘â€“Ã‘â‚¬ÃÂºÃÂ¸ ÃÂ²ÃÂ¸ÃÂºÃÂ»Ã‘Å½Ã‘â€¡ÃÂµÃÂ½Ã‘â€“.",
                        "ÃÅ¸ÃÂ¾ÃÂºÃÂ°ÃÂ·Ã‘â€¹ÃÂ²ÃÂ°ÃÂµÃ‘â€š ÃÂ²ÃÂ·ÃÂ²ÃÂµÃ‘Ë†ÃÂµÃÂ½ÃÂ½Ã‘â€¹ÃÂµ ÃÂ½ÃÂ°ÃÂ¿ÃÂ¾ÃÂ¼ÃÂ¸ÃÂ½ÃÂ°ÃÂ½ÃÂ¸Ã‘Â ÃÂ´ÃÂ»Ã‘Â ÃÂºÃÂ°Ã‘â‚¬Ã‘â€šÃÂ¾Ã‘â€¡ÃÂµÃÂº Ã‘Â 0-2 ÃÂ·ÃÂ²ÃÂµÃÂ·ÃÂ´ÃÂ°ÃÂ¼ÃÂ¸. ÃÅ¡ÃÂ°Ã‘â‚¬Ã‘â€šÃÂ¾Ã‘â€¡ÃÂºÃÂ¸ Ã‘Â 0 ÃÂ·ÃÂ²ÃÂµÃÂ·ÃÂ´ÃÂ°ÃÂ¼ÃÂ¸ ÃÂ¿ÃÂ¾Ã‘ÂÃÂ²ÃÂ»Ã‘ÂÃ‘Å½Ã‘â€šÃ‘ÂÃ‘Â Ã‘â€¡ÃÂ°Ã‘â€°ÃÂµ ÃÂ²Ã‘ÂÃÂµÃÂ³ÃÂ¾; 3 ÃÂ·ÃÂ²ÃÂµÃÂ·ÃÂ´Ã‘â€¹ ÃÂ¸Ã‘ÂÃÂºÃÂ»Ã‘Å½Ã‘â€¡ÃÂµÃÂ½Ã‘â€¹.",
                        "Pokazuje waÃ…Â¼one przypomnienia dla kart z 0-2 gwiazdkami. 0 gwiazdek pojawia siÃ„â„¢ najczÃ„â„¢Ã…â€ºciej; 3 gwiazdki sÃ„â€¦ wykluczone."
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = notificationIntervalDraft,
                    onValueChange = onNotificationIntervalChange,
                    label = { Text(st("Interval minutes", "Intervall in Minuten", "Ãâ€ ÃÂ½Ã‘â€šÃ‘ÂÃ‘â‚¬ÃÂ²ÃÂ°ÃÂ» Ã‘Æ’ Ã‘â€¦ÃÂ²Ã‘â€“ÃÂ»Ã‘â€“ÃÂ½ÃÂ°Ã‘â€¦", "Intervalo en minutos", "Ãâ€ ÃÂ½Ã‘â€šÃÂµÃ‘â‚¬ÃÂ²ÃÂ°ÃÂ» Ã‘Æ’ Ã‘â€¦ÃÂ²ÃÂ¸ÃÂ»ÃÂ¸ÃÂ½ÃÂ°Ã‘â€¦", "ÃËœÃÂ½Ã‘â€šÃÂµÃ‘â‚¬ÃÂ²ÃÂ°ÃÂ» ÃÂ² ÃÂ¼ÃÂ¸ÃÂ½Ã‘Æ’Ã‘â€šÃÂ°Ã‘â€¦", "InterwaÃ…â€š w minutach")) },
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
                    label = { Text(st("Maximum active notifications", "Maximale aktive Benachrichtigungen", "ÃÅ“ÃÂ°ÃÂºÃ‘ÂÃ‘â€“ÃÂ¼Ã‘Æ’ÃÂ¼ ÃÂ°ÃÂºÃ‘â€šÃ‘â€¹Ã‘Å¾ÃÂ½Ã‘â€¹Ã‘â€¦ ÃÂ°ÃÂ¿ÃÂ°ÃÂ²Ã‘ÂÃ‘Ë†Ã‘â€¡Ã‘ÂÃÂ½ÃÂ½Ã‘ÂÃ‘Å¾", "MÃƒÂ¡ximo de notificaciones activas", "ÃÅ“ÃÂ°ÃÂºÃ‘ÂÃÂ¸ÃÂ¼Ã‘Æ’ÃÂ¼ ÃÂ°ÃÂºÃ‘â€šÃÂ¸ÃÂ²ÃÂ½ÃÂ¸Ã‘â€¦ Ã‘ÂÃÂ¿ÃÂ¾ÃÂ²Ã‘â€“Ã‘â€°ÃÂµÃÂ½Ã‘Å’", "ÃÅ“ÃÂ°ÃÂºÃ‘ÂÃÂ¸ÃÂ¼Ã‘Æ’ÃÂ¼ ÃÂ°ÃÂºÃ‘â€šÃÂ¸ÃÂ²ÃÂ½Ã‘â€¹Ã‘â€¦ Ã‘Æ’ÃÂ²ÃÂµÃÂ´ÃÂ¾ÃÂ¼ÃÂ»ÃÂµÃÂ½ÃÂ¸ÃÂ¹", "Maksimum aktywnych powiadomieÃ…â€ž")) },
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
                        "Android kann Hintergrundaufgaben verzÃƒÂ¶gert ausfÃƒÂ¼hren, besonders wenn das Telefon inaktiv ist.",
                        "Android ÃÂ¼ÃÂ¾ÃÂ¶ÃÂ° ÃÂ·ÃÂ°ÃÂ¿Ã‘Æ’Ã‘ÂÃÂºÃÂ°Ã‘â€ Ã‘Å’ Ã‘â€žÃÂ¾ÃÂ½ÃÂ°ÃÂ²Ã‘â€¹Ã‘Â ÃÂ·ÃÂ°ÃÂ´ÃÂ°Ã‘â€¡Ã‘â€¹ ÃÂ· ÃÂ·ÃÂ°Ã‘â€šÃ‘â‚¬Ã‘â€¹ÃÂ¼ÃÂºÃÂ°ÃÂ¹, ÃÂ°Ã‘ÂÃÂ°ÃÂ±ÃÂ»Ã‘â€“ÃÂ²ÃÂ° ÃÂºÃÂ°ÃÂ»Ã‘â€“ Ã‘â€šÃ‘ÂÃÂ»ÃÂµÃ‘â€žÃÂ¾ÃÂ½ ÃÂ½ÃÂµÃÂ°ÃÂºÃ‘â€šÃ‘â€¹Ã‘Å¾ÃÂ½Ã‘â€¹.",
                        "Android puede retrasar tareas en segundo plano, sobre todo cuando el telÃƒÂ©fono estÃƒÂ¡ inactivo.",
                        "Android ÃÂ¼ÃÂ¾ÃÂ¶ÃÂµ ÃÂ·ÃÂ°ÃÂ¿Ã‘Æ’Ã‘ÂÃÂºÃÂ°Ã‘â€šÃÂ¸ Ã‘â€žÃÂ¾ÃÂ½ÃÂ¾ÃÂ²Ã‘â€“ ÃÂ·ÃÂ°ÃÂ´ÃÂ°Ã‘â€¡Ã‘â€“ Ã‘â€“ÃÂ· ÃÂ·ÃÂ°Ã‘â€šÃ‘â‚¬ÃÂ¸ÃÂ¼ÃÂºÃÂ¾Ã‘Å½, ÃÂ¾Ã‘ÂÃÂ¾ÃÂ±ÃÂ»ÃÂ¸ÃÂ²ÃÂ¾ ÃÂºÃÂ¾ÃÂ»ÃÂ¸ Ã‘â€šÃÂµÃÂ»ÃÂµÃ‘â€žÃÂ¾ÃÂ½ ÃÂ½ÃÂµÃÂ°ÃÂºÃ‘â€šÃÂ¸ÃÂ²ÃÂ½ÃÂ¸ÃÂ¹.",
                        "Android ÃÂ¼ÃÂ¾ÃÂ¶ÃÂµÃ‘â€š ÃÂ·ÃÂ°ÃÂ¿Ã‘Æ’Ã‘ÂÃÂºÃÂ°Ã‘â€šÃ‘Å’ Ã‘â€žÃÂ¾ÃÂ½ÃÂ¾ÃÂ²Ã‘â€¹ÃÂµ ÃÂ·ÃÂ°ÃÂ´ÃÂ°Ã‘â€¡ÃÂ¸ Ã‘Â ÃÂ·ÃÂ°ÃÂ´ÃÂµÃ‘â‚¬ÃÂ¶ÃÂºÃÂ¾ÃÂ¹, ÃÂ¾Ã‘ÂÃÂ¾ÃÂ±ÃÂµÃÂ½ÃÂ½ÃÂ¾ ÃÂºÃÂ¾ÃÂ³ÃÂ´ÃÂ° Ã‘â€šÃÂµÃÂ»ÃÂµÃ‘â€žÃÂ¾ÃÂ½ ÃÂ½ÃÂµÃÂ°ÃÂºÃ‘â€šÃÂ¸ÃÂ²ÃÂµÃÂ½.",
                        "Android moÃ…Â¼e opÃƒÂ³Ã…ÂºniaÃ„â€¡ pracÃ„â„¢ w tle, szczegÃƒÂ³lnie gdy telefon jest bezczynny."
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
                    text = st("Version log", "Versionslog", "Ãâ€ºÃÂ¾ÃÂ³ ÃÂ²ÃÂµÃ‘â‚¬Ã‘ÂÃ‘â€“ÃÂ¹", "Registro de versiones", "Ãâ€ºÃÂ¾ÃÂ³ ÃÂ²ÃÂµÃ‘â‚¬Ã‘ÂÃ‘â€“ÃÂ¹", "Ãâ€ºÃÂ¾ÃÂ³ ÃÂ²ÃÂµÃ‘â‚¬Ã‘ÂÃÂ¸ÃÂ¹", "Historia wersji"),
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
private fun nativeLanguageLabel(language: String): String {
    return DictionaryLanguageOptions.firstOrNull { it.first.equals(language, ignoreCase = true) }?.second
        ?: language.ifBlank { "Language" }
}

@Composable
private fun DictionaryLanguageDropdown(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = nativeLanguageLabel(value)
    val selectedCode = selectedLabel.substringBefore(" - ").ifBlank { selectedLabel }
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
                    text = selectedCode,
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
                        text = { Text(optionLabel.substringBefore(" - ")) },
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
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (splitMode) {
            SplitTranslationPanel(
                title = nativeLanguageLabel(state.quickVocabularySourceLanguage),
                text = state.translationOutput,
                flipped = true,
                modifier = Modifier.weight(1f),
                attribution = state.translationOutput.isNotBlank()
            )
            SplitTranslationPanel(
                title = nativeLanguageLabel(state.quickVocabularyTargetLanguage),
                text = state.translationInput,
                flipped = false,
                editable = true,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f)
            )
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Box(Modifier.fillMaxSize().padding(18.dp)) {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(nativeLanguageLabel(state.quickVocabularySourceLanguage), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.50f))
                        if (state.translationOutput.isNotBlank()) {
                            Text(
                                text = state.translationOutput,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Powered by Google Translate",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(44.dp)
                            .pointerInput(translateAutoSpeakEnabled, state.translationOutput) {
                                detectTapGestures(
                                    onTap = {
                                        if (translateAutoSpeakEnabled) {
                                            if (state.translationOutput.isNotBlank()) onSpeakTranslation()
                                        } else {
                                            onTranslateAutoSpeakChange(true)
                                        }
                                    },
                                    onLongPress = { onTranslateAutoSpeakChange(false) }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (translateAutoSpeakEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = if (translateAutoSpeakEnabled) "Auto speak translation on" else "Auto speak translation off",
                            tint = if (translateAutoSpeakEnabled) Color(0xFF234231) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                        )
                    }
                }
            }
            OutlinedTextField(
                value = state.translationInput,
                onValueChange = onInputChange,
                modifier = Modifier.fillMaxWidth().weight(1f),
                label = { Text(nativeLanguageLabel(state.quickVocabularyTargetLanguage), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f)) },
                trailingIcon = {
                    IconButton(onClick = onSpeakInput, enabled = state.translationInput.isNotBlank()) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Speak input")
                    }
                },
                minLines = 8
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DictionaryLanguageDropdown(
                    label = "Source",
                    value = state.quickVocabularyTargetLanguage,
                    onValueChange = onSourceLanguageChange,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onSwapLanguages) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = "Swap languages")
                }
                DictionaryLanguageDropdown(
                    label = "Target",
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
private fun SplitTranslationPanel(
    title: String,
    text: String,
    flipped: Boolean,
    modifier: Modifier = Modifier,
    editable: Boolean = false,
    onValueChange: (String) -> Unit = {},
    attribution: Boolean = false
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.graphicsLayer { rotationZ = if (flipped) 180f else 0f },
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f)
                )
                if (editable) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = onValueChange,
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 5
                    )
                } else if (text.isNotBlank()) {
                    Text(text, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                    if (attribution) {
                        Text(
                            text = "Powered by Google Translate",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                        )
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
    selectedAnswer: String,
    answerFeedbackVisible: Boolean,
    onAnswer: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (card == null) return
    val choices = remember(card.id, card.correctText(), cards.map { it.id to it.correctText() }) {
        testChoices(card, cards)
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
                        selected && normalizeAnswerText(choice) == normalizeAnswerText(card.correctText()) -> BrandSaladColor
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

private fun testChoices(card: Flashcard, cards: List<Flashcard>): List<String> {
    val correct = card.correctText().ifBlank { card.nativeText() }.ifBlank { "Correct" }
    val distractors = cards
        .filterNot { it.id == card.id }
        .map { it.correctText().ifBlank { it.nativeText() } }
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
                .clickable(onClick = onQuickVoiceInput),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Mic, contentDescription = "Add vocabulary by voice", tint = BrandSaladColor)
                Spacer(Modifier.width(10.dp))
                Text("Add vocabulary", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
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
                        positionLabel = (animatedIndex + 1).toString(),
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

private fun UiText.languageKey(): String = when (settings) {
    "Einstellungen" -> "de"
    "ÃÂÃÂ°ÃÂ»ÃÂ°ÃÂ´Ã‘â€¹" -> "be"
    "Ajustes" -> "es"
    "ÃÂÃÂ°ÃÂ»ÃÂ°Ã‘Ë†Ã‘â€šÃ‘Æ’ÃÂ²ÃÂ°ÃÂ½ÃÂ½Ã‘Â" -> "uk"
    "ÃÂÃÂ°Ã‘ÂÃ‘â€šÃ‘â‚¬ÃÂ¾ÃÂ¹ÃÂºÃÂ¸" -> "ru"
    "Ustawienia" -> "pl"
    else -> "en"
}

private fun UiText.kindText(kindCode: String): String = when (languageKey()) {
    "de" -> if (kindCode == "LN") "Lektion" else "Fehler"
    "be" -> if (kindCode == "LN") "ÃÂ£Ã‘â‚¬ÃÂ¾ÃÂº" else "ÃÅ¸ÃÂ°ÃÂ¼Ã‘â€¹ÃÂ»ÃÂºÃ‘â€“"
    "es" -> if (kindCode == "LN") "LecciÃƒÂ³n" else "Errores"
    "uk" -> if (kindCode == "LN") "ÃÂ£Ã‘â‚¬ÃÂ¾ÃÂº" else "ÃÅ¸ÃÂ¾ÃÂ¼ÃÂ¸ÃÂ»ÃÂºÃÂ¸"
    "ru" -> if (kindCode == "LN") "ÃÂ£Ã‘â‚¬ÃÂ¾ÃÂº" else "ÃÅ¾Ã‘Ë†ÃÂ¸ÃÂ±ÃÂºÃÂ¸"
    "pl" -> if (kindCode == "LN") "Lekcja" else "BÃ…â€šÃ„â„¢dy"
    else -> if (kindCode == "LN") "Lesson" else "Mistakes"
}

private fun UiText.mixedKindText(): String = when (languageKey()) {
    "de" -> "Gemischt"
    "be" -> "Ãâ€”ÃÂ¼ÃÂµÃ‘Ë†ÃÂ°ÃÂ½ÃÂ°"
    "es" -> "Mixto"
    "uk" -> "Ãâ€”ÃÂ¼Ã‘â€“Ã‘Ë†ÃÂ°ÃÂ½ÃÂ¾"
    "ru" -> "ÃÂ¡ÃÂ¼ÃÂµÃ‘Ë†ÃÂ°ÃÂ½ÃÂ½Ã‘â€¹ÃÂ¹"
    "pl" -> "Mieszane"
    else -> "Mixed"
}

private fun UiText.questionsText(count: Int): String = when (languageKey()) {
    "de" -> "$count Fragen"
    "be" -> "$count ÃÂ¿Ã‘â€¹Ã‘â€šÃÂ°ÃÂ½ÃÂ½Ã‘ÂÃ‘Å¾"
    "es" -> "$count preguntas"
    "uk" -> "$count ÃÂ¿ÃÂ¸Ã‘â€šÃÂ°ÃÂ½Ã‘Å’"
    "ru" -> "$count ÃÂ²ÃÂ¾ÃÂ¿Ã‘â‚¬ÃÂ¾Ã‘ÂÃÂ¾ÃÂ²"
    "pl" -> "$count pytaÃ…â€ž"
    else -> "$count questions"
}

private fun UiText.doneText(done: Int, total: Int): String = when (languageKey()) {
    "de" -> "$done von $total erledigt"
    "be" -> "$done ÃÂ· $total ÃÂ·Ã‘â‚¬ÃÂ¾ÃÂ±ÃÂ»ÃÂµÃÂ½ÃÂ°"
    "es" -> "$done de $total hechas"
    "uk" -> "$done Ã‘â€“ÃÂ· $total ÃÂ²ÃÂ¸ÃÂºÃÂ¾ÃÂ½ÃÂ°ÃÂ½ÃÂ¾"
    "ru" -> "$done ÃÂ¸ÃÂ· $total ÃÂ²Ã‘â€¹ÃÂ¿ÃÂ¾ÃÂ»ÃÂ½ÃÂµÃÂ½Ã‘â€¹"
    "pl" -> "$done z $total zrobione"
    else -> "$done of $total done"
}

private fun UiText.completedText(count: Int): String = when (languageKey()) {
    "de" -> "$count-mal abgeschlossen"
    "be" -> "ÃÅ¸Ã‘â‚¬ÃÂ¾ÃÂ¹ÃÂ´ÃÂ·ÃÂµÃÂ½ÃÂ° $count Ã‘â‚¬ÃÂ°ÃÂ·ÃÂ¾Ã‘Å¾"
    "es" -> "Completado $count veces"
    "uk" -> "ÃÅ¸Ã‘â‚¬ÃÂ¾ÃÂ¹ÃÂ´ÃÂµÃÂ½ÃÂ¾ $count Ã‘â‚¬ÃÂ°ÃÂ·Ã‘â€“ÃÂ²"
    "ru" -> "ÃÅ¸Ã‘â‚¬ÃÂ¾ÃÂ¹ÃÂ´ÃÂµÃÂ½ÃÂ¾ $count Ã‘â‚¬ÃÂ°ÃÂ·"
    "pl" -> "UkoÃ…â€žczono $count razy"
    else -> "Completed $count times"
}

private fun Flashcard.displayedCardText(isBackVisible: Boolean): String {
    return if (isBackVisible) {
        correctText()
    } else if (kindCode() == "LN") {
        frontDisplayText()
    } else {
        mistakeText().ifBlank { nativeText().ifBlank { correctText() } }
    }
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

private fun Flashcard.answerInputLabel(): String {
    return when {
        nativeText().isEmptyPlaceholder() -> sourceLanguage.ifBlank { frontLabel() }
        correctText().isEmptyPlaceholder() -> targetLanguage.ifBlank { backLabel() }
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
        normalized in listOf("en", "eng", "english", "ÃÂ°ÃÂ½ÃÂ³ÃÂ»ÃÂ¸ÃÂ¹Ã‘ÂÃÂºÃÂ¸ÃÂ¹", "ÃÂ°ÃÂ½ÃÂ³ÃÂ»Ã‘â€“ÃÂ¹Ã‘ÂÃÂºÃÂ°Ã‘Â", "angielski") -> "en-US"
        normalized in listOf("de", "deu", "ger", "german", "deutsch", "ÃÂ½ÃÂµÃÂ¼ÃÂµÃ‘â€ ÃÂºÃÂ¸ÃÂ¹", "ÃÂ½Ã‘ÂÃÂ¼ÃÂµÃ‘â€ ÃÂºÃÂ°Ã‘Â", "niemiecki") -> "de-DE"
        normalized in listOf("be", "by", "belarusian", "belaruska", "ÃÂ±ÃÂµÃÂ»ÃÂ°Ã‘â‚¬Ã‘Æ’Ã‘ÂÃÂºÃÂ°Ã‘Â", "ÃÂ±ÃÂµÃÂ»ÃÂ¾Ã‘â‚¬Ã‘Æ’Ã‘ÂÃ‘ÂÃÂºÃÂ¸ÃÂ¹", "ÃÂ±ÃÂµÃÂ»ÃÂ°Ã‘â‚¬Ã‘Æ’Ã‘ÂÃ‘ÂÃÂºÃÂ¸ÃÂ¹") -> "be-BY"
        normalized in listOf("es", "spa", "spanish", "espanol", "espaÃƒÂ±ol", "ÃÂ¸Ã‘ÂÃÂ¿ÃÂ°ÃÂ½Ã‘ÂÃÂºÃÂ¸ÃÂ¹", "Ã‘â€“Ã‘ÂÃÂ¿ÃÂ°ÃÂ½Ã‘ÂÃÂºÃÂ°Ã‘Â") -> "es-ES"
        normalized in listOf("uk", "ua", "ukrainian", "Ã‘Æ’ÃÂºÃ‘â‚¬ÃÂ°ÃÂ¸ÃÂ½Ã‘ÂÃÂºÃÂ¸ÃÂ¹", "Ã‘Æ’ÃÂºÃ‘â‚¬ÃÂ°Ã‘â€”ÃÂ½Ã‘ÂÃ‘Å’ÃÂºÃÂ°", "Ã‘Æ’ÃÂºÃ‘â‚¬ÃÂ°Ã‘â€“ÃÂ½Ã‘ÂÃÂºÃÂ°Ã‘Â") -> "uk-UA"
        normalized in listOf("ru", "rus", "russian", "Ã‘â‚¬Ã‘Æ’Ã‘ÂÃ‘ÂÃÂºÃÂ¸ÃÂ¹", "Ã‘â‚¬Ã‘Æ’Ã‘ÂÃÂºÃÂ°Ã‘Â", "rosyjski") -> "ru-RU"
        normalized in listOf("pl", "pol", "polish", "polski", "ÃÂ¿ÃÂ¾ÃÂ»Ã‘Å’Ã‘ÂÃÂºÃÂ¸ÃÂ¹", "ÃÂ¿ÃÂ¾ÃÂ»Ã‘Å’Ã‘ÂÃÂºÃÂ°Ã‘Â") -> "pl-PL"
        else -> null
    }
}

private fun String.speechLanguageTagFromText(): String? {
    val text = lowercase(Locale.ROOT)
    return when {
        text.any { it in "Ã‘Å¾Ã‘â€“" } -> "be-BY"
        text.any { it in "Ã‘â€“Ã‘â€”Ã‘â€Ã’â€˜" } -> "uk-UA"
        text.any { it in "Ã„â€¦Ã„â€¡Ã„â„¢Ã…â€šÃ…â€žÃƒÂ³Ã…â€ºÃ…ÂºÃ…Â¼" } -> "pl-PL"
        text.any { it in "ÃƒÂ¤ÃƒÂ¶ÃƒÂ¼ÃƒÅ¸" } -> "de-DE"
        text.any { it in "ÃƒÂ¡ÃƒÂ©ÃƒÂ­ÃƒÂ±ÃƒÂ³ÃƒÂºÃƒÂ¼Ã‚Â¿Ã‚Â¡" } -> "es-ES"
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
        answer.isNotBlank() && normalizeAnswerText(answer) == normalizeAnswerText(card.correctText())
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
                    text = buildAnswerFeedback(answer, currentCard.correctText()),
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
private fun sampleLessonJson(): String {
    return """
        {
          "prompt": "Export all my mistakes or lesson translation cards into this JSON format. Use cardKind MK for mistake cards and LN for normal lesson/translation cards. Fill lessonInfo with instructions for how the learner should work with this specific lesson or card type. For LN fill sourceLanguage and targetLanguage so the app can show language names on the card.",
          "id": "replace_with_unique_lesson_id",
          "title": "Lesson title shown on the main screen",
          "lessonInfo": "Instructions shown from the lesson info icon. Use this for the purpose of the lesson, how to answer, what to pay attention to, and any workflow notes for this set of cards.",
          "sourceLanguage": "Front side input language selected from the app settings list, for example Russian.",
          "targetLanguage": "Back side input language selected from the app settings list, for example Polish.",
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
        Google Translate attribution - Translate mode can use on-device Google Translate via ML Kit. Google disclaims warranties related to translation accuracy and reliability. See https://cloud.google.com/translate and https://translate.google.com.
        v0.93 - Translator results now automatically create or update vocabulary cards while staying in Translate, language swap preserves text by swapping fields, and source input has its own speaker.
        v0.92 - Added downloaded Google Translate language tracking, single-language downloads, visible download progress, Translate auto-read toggle, safer native language labels, and quick microphone vocabulary capture without opening Translate.
        v0.91 - Added Google Translate language download popup access from Translate mode and Settings, improved the Translate microphone button contrast, added a G action for empty card sides, and auto-fills available Google translations for newly saved or captured cards.
        v0.90 - Restored offline Google Translate for Translate/Split mode with downloadable language models, Google attribution, quieter language labels, no empty translation placeholder, and a clearer Translate microphone.

        v0.89 - Uses native language labels in Translate panels, tuned the Translate microphone color and size, lets answered or empty-side Test cards flip freely, and adds a star after a correct Test answer.
        v0.88 - Compact language pickers to codes, emphasized the Translate microphone, made Tests reveal the answer only after a correct choice without auto-navigation, randomized choices with A-D markers, and made Test quick edit save the displayed side.
        v0.87 - Clears Translate input on entry, adds language swapping, includes the target vocabulary lesson in Add messages, and makes quick Edit work from Tests with refreshed answers.
        v0.86 - Simplified Translate controls so Add is left, Speak stays centered, Clear is right, and the buttons use icons only.
        v0.85 - Moved Translate original input below the translation result and added a plus action that saves the current translation pair as a vocabulary card.
        v0.84 - Moved Translate/Split language selectors to the bottom, made Tests use a compact card with scrollable answer choices, and added Very small display/control size settings.
        v0.83 - Replaced the Check action with Clear input, made OK handle answer checking, added Cards/Tests/Translate/Split work modes, introduced multiple-choice tests, and added placeholder Translate/Split voice screens.
        v0.82 - Updated notification selection to use 1/2/3-star weighted cards, downgraded unanswered 2-star notification cards, made the lesson editor fully scrollable with card controls underneath, and softened the red/mint brand colors.
        v0.81 - Made lesson opening atomic so the study screen receives a ready card portion immediately instead of briefly rendering an empty lesson state.
        v0.80 - Made lesson opening reload the latest saved lesson, discard broken empty restored sessions, and rebuild the study portion immediately so newly recorded voice cards appear on the first open.
        v0.79 - Made quick voice vocabulary save each card immediately at the top of the target lesson, keep the latest spoken card first, use synchronous lesson persistence, and show Added bubbles longer.
        v0.78 - Added lesson-level source/target input languages with editor dropdowns and JSON export support, used lesson languages as voice fallback for Mixed cards, and refreshed quick Edit taps so the second tap opens the full editor.
        v0.77 - Made the second quick Edit tap open the full card editor, reset lesson stars together with progress, and kept study-plus cards appended while quick voice vocabulary cards stay at the top.
        v0.76 - Restored study counter navigation, moved reset progress into the lesson info popup, placed Add next to lesson info, added study-plus cards at the end, renamed quick vocabulary lessons with language codes and creation date, and kept quick voice lessons opening at the first card.
        v0.75 - Kept quick vocabulary voice capture on the current screen for bulk entry, made Done jump to the last card, created new lessons and study-plus cards with an empty starter card, and changed Refresh into confirmed progress reset.
        v0.74 - Made only cards with an actually missing side auto-open on the filled side; completed normal cards now return to the configured start side during navigation and session restore.
        v0.73 - Added quick visible-side editing from the study card Edit button, kept long-press full editing, shortened transient messages, and made OK require a correct typed answer.
        v0.69 - Reverted segmented voice recognition to the previous single-pass flow for stability, keeping the pending-translation and lesson action refinements.
        v0.68 - Extended voice capture pauses to six seconds with segmented recognition, restored pending-translation cards so the missing side remains visible after flipping, and refined lesson delete/share actions with confirmation.
        v0.67 - Changed quick vocabulary languages to dropdowns, made quick voice capture listen in the target language, showed pending-translation cards on their filled side, and removed voice input from card meta/log fields.
        v0.66 - Added a 1x1 quick voice home-screen widget with a white microphone tile and red MM mark that opens directly into quick vocabulary capture.
        v0.65 - Fixed show-filter toggle semantics, made answer input grow for multiple lines, switched interface language to a dropdown, saved quick voice vocabulary as target-language text, and added bulk card copy/delete tools.
        v0.64 - Improved settings back navigation, added fast white service bubbles, voice input for card editor fields, and clearer filter toggle icons.
        v0.63 - Disabled the unreliable on-device translator, removed the heavy ML Kit translation dependency, and made voice vocabulary cards save with a clear pending-translation message when no server translator is configured.
        v0.62 - Added study text/control size settings, star and done filters, better answer feedback, and quieter study interactions.
        v0.61 - Added local on-device translation as the default quick vocabulary translation path, with server translation kept as a fallback when configured.
        v0.60 - Moved study order, restart, and hide-starred controls into the top bar, replaced the hide-done icon with a crossed star state, enlarged the study card, centered the answer action row, and added server-backed translation settings for quick voice vocabulary.
        v0.59 - Moved quick vocabulary voice capture below the lesson list, fixed quick voice lesson creation and source-language recognition, tightened the OK button, changed Correct feedback into a flying star, refined hide-completed controls, and improved Left-counter navigation.
        v0.58 - Fixed answer-bar spacing, replaced Show all text with an icon, made study counters navigable, added quick voice capture into a New vocabulary lesson with configurable source/target languages, and extended voice silence retry behavior.
        v0.57 - Centered and enlarged voice controls, moved speech playback to the answer bar, added hold-to-keep-recording voice behavior, hid the lesson title behind the info popup, refined missing-letter hints, and kept technical bubbles in English.
        v0.56 - Simplified the Copy action to an icon-only button, kept Check active for empty answers with a white Enter answer bubble, and changed order switching feedback to show the active mode name.
        v0.55 - Made Original the default study order, added a next-order hint when changing order, changed voice input to tap-to-record/tap-to-stop with silence timeout, and added a Check action with visual answer highlighting.
        v0.54 - Added text-to-speech playback on study cards with a speaker icon that reads the currently visible side using the card language when available.
        v0.53 - Replaced the study order label with a compact cycling icon, added a Refresh action to restart the current lesson session, and saved unfinished lesson sessions so returning to a lesson restores the last card, order, answer, and completed progress.
        v0.52 - Made voice recognition choose the answer language from the card, with fallbacks from card text and interface language, and added a five-second silence timeout while keeping press-and-hold microphone behavior.
        v0.51 - Added press-and-hold voice input with recording status, timer, and speech recognition into the answer field; restored a lighter study top area with a single cycling order button; and moved lesson info to the top bar beside Settings.
        v0.50 - Added the Original study order, changed the study order controls to a segmented switch, and made mode changes reorder the current lesson immediately.
        v0.49 - Fixed the header version to read from BuildConfig, added lessonInfo to bundled lessons so info icons are visible immediately, and kept the lesson instructions feature visible in default content.
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
