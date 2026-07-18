package com.lexaprograms.polishcards

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import app.rive.runtime.kotlin.RiveAnimationView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val SORT_TRAY_CAPACITY = 7
private const val SORT_LEVEL_SECONDS = 75

private data class SortHero(
    val name: String,
    val color: Color,
    val accent: Color,
    val silhouette: Int
)

private data class SortTile(val uid: Int, val heroId: Int)

private val sortHeroes = listOf(
    SortHero("Луна", Color(0xFF9B7BEA), Color(0xFFC9B9FF), 0),
    SortHero("Мята", Color(0xFF43C7AA), Color(0xFF8BE4D0), 1),
    SortHero("Персик", Color(0xFFFFAE72), Color(0xFFFFD1AE), 2),
    SortHero("Искра", Color(0xFFF3C84B), Color(0xFFFFE99D), 3),
    SortHero("Слива", Color(0xFF76507E), Color(0xFFB69ABD), 4),
    SortHero("Коралл", Color(0xFFEE6F78), Color(0xFFFFAAB0), 5)
)

private val SortPlum = Color(0xFF512D58)
private val SortCream = Color(0xFFFFF6E9)
private val SortCoral = Color(0xFFEE6F78)

@Composable
fun RiveCharacterSortScreen(onDismiss: () -> Unit) {
    var level by remember { mutableIntStateOf(1) }
    var score by remember { mutableIntStateOf(0) }
    var combo by remember { mutableIntStateOf(0) }
    var timeLeft by remember { mutableIntStateOf(SORT_LEVEL_SECONDS) }
    var board by remember { mutableStateOf(newSortBoard(level)) }
    var tray by remember { mutableStateOf<List<Int>>(emptyList()) }
    var message by remember { mutableStateOf("Найди три одинаковых героя") }
    var resolving by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<Boolean?>(null) }
    var resultTitle by remember { mutableStateOf("") }
    var reactionNonce by remember { mutableIntStateOf(0) }
    var roundNonce by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    fun startRound(nextLevel: Int = level) {
        level = nextLevel
        combo = 0
        timeLeft = maxOf(55, SORT_LEVEL_SECONDS - (level - 1) * 3)
        board = newSortBoard(level)
        tray = emptyList()
        message = "Найди три одинаковых героя"
        resolving = false
        result = null
        resultTitle = ""
        roundNonce += 1
        reactionNonce += 1
    }

    BackHandler(onBack = onDismiss)

    LaunchedEffect(roundNonce, result) {
        while (result == null && timeLeft > 0) {
            delay(1_000)
            if (result == null) timeLeft -= 1
        }
        if (timeLeft <= 0 && result == null) {
            message = "Время вышло"
            resultTitle = "ВРЕМЯ ВЫШЛО"
            result = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.character_sort_dressing_room),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x1F512D58))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                SortPill(color = SortPlum, modifier = Modifier.size(46.dp)) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = Color.White)
                    }
                }
                SortPill("УРОВЕНЬ $level", Color(0xFF76507E), Modifier.weight(1f))
                SortPill("$score ★", SortCoral, Modifier.weight(0.82f))
                SortPill(formatSortTime(timeLeft), Color(0xFF239B8A), Modifier.weight(0.82f))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                RiveSortMascot(reactionNonce, Modifier.size(width = 66.dp, height = 48.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "СОБЕРИ КОМПАНИЮ",
                        color = SortCream,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    Text("RIVE EDITION", color = Color(0xFFFFE0AD), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Text(
                message,
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF603B58),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                color = Color(0xDEFFF6E9),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(2.dp, Color.White.copy(alpha = 0.68f)),
                shadowElevation = 5.dp
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    userScrollEnabled = false
                ) {
                    items(board, key = { it.uid }) { tile ->
                        SortHeroToken(
                            hero = sortHeroes[tile.heroId],
                            enabled = !resolving && result == null,
                            onClick = {
                                if (resolving || result != null) return@SortHeroToken
                                resolving = true
                                board = board.filterNot { it.uid == tile.uid }
                                val insertAt = tray.indexOfLast { it == tile.heroId }
                                    .let { if (it < 0) tray.size else it + 1 }
                                val updatedTray = tray.toMutableList().apply { add(insertAt, tile.heroId) }
                                tray = updatedTray
                                scope.launch {
                                    delay(230)
                                    if (updatedTray.count { it == tile.heroId } >= 3) {
                                        val remaining = updatedTray.toMutableList()
                                        repeat(3) { remaining.remove(tile.heroId) }
                                        tray = remaining
                                        combo += 1
                                        score += 30 * maxOf(1, combo)
                                        message = "${sortHeroes[tile.heroId].name}: сценка собрана!"
                                        reactionNonce += 1
                                        delay(220)
                                        if (board.isEmpty() && remaining.isEmpty()) {
                                            message = "Эпизод открыт!"
                                            resultTitle = "ЭПИЗОД ОТКРЫТ!"
                                            result = true
                                        } else {
                                            message = "Отлично! Ищи следующую тройку"
                                        }
                                    } else if (updatedTray.size >= SORT_TRAY_CAPACITY) {
                                        message = "Лоток заполнен"
                                        resultTitle = "ЛОТОК ЗАПОЛНЕН"
                                        result = false
                                    } else {
                                        message = "В лотке: ${updatedTray.size} из $SORT_TRAY_CAPACITY"
                                    }
                                    resolving = false
                                }
                            }
                        )
                    }
                }
            }

            AnimatedVisibility(visible = combo > 0) {
                Text(
                    "КОМБО ×${maxOf(1, combo)}",
                    modifier = Modifier.fillMaxWidth(),
                    color = SortCream,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
            }

            SortTray(tray)
        }

        result?.let { won ->
            SortResultOverlay(
                won = won,
                title = resultTitle,
                level = level,
                score = score,
                reactionNonce = reactionNonce,
                onContinue = {
                    if (won) startRound(level + 1) else startRound(level)
                },
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
private fun SortHeroToken(hero: SortHero, enabled: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (enabled) 1f else 0.96f, tween(120), label = "sortTokenScale")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.96f)
            .scale(scale)
            .alpha(if (enabled) 1f else 0.86f)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.55f))
            .border(1.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(20.dp))
            .semantics { contentDescription = hero.name }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(3.dp),
        contentAlignment = Alignment.Center
    ) {
        SortHeroFace(hero, Modifier.fillMaxSize())
    }
}

@Composable
private fun SortHeroFace(hero: SortHero, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val radius = size.minDimension * 0.31f
        val center = Offset(size.width / 2f, size.height * 0.49f)
        drawCircle(Color(0x33512D58), radius + 8f, center + Offset(0f, 5f))
        drawCircle(Color(0xFFFFF8EF), radius + 7f, center)
        when (hero.silhouette) {
            0 -> {
                drawCircle(hero.accent, radius * 0.39f, center + Offset(-radius * 0.62f, -radius * 0.70f))
                drawCircle(hero.accent, radius * 0.39f, center + Offset(radius * 0.62f, -radius * 0.70f))
            }
            1 -> {
                val left = Path().apply {
                    moveTo(center.x - radius * 0.82f, center.y - radius * 0.45f)
                    lineTo(center.x - radius * 0.42f, center.y - radius * 1.20f)
                    lineTo(center.x - radius * 0.08f, center.y - radius * 0.55f)
                    close()
                }
                val right = Path().apply {
                    moveTo(center.x + radius * 0.10f, center.y - radius * 0.55f)
                    lineTo(center.x + radius * 0.50f, center.y - radius * 1.20f)
                    lineTo(center.x + radius * 0.84f, center.y - radius * 0.42f)
                    close()
                }
                drawPath(left, hero.accent)
                drawPath(right, hero.accent)
            }
            3 -> repeat(5) { index ->
                val angle = Math.toRadians((index * 72.0) - 90.0)
                drawCircle(hero.accent, radius * 0.22f, center + Offset((cos(angle) * radius).toFloat(), (sin(angle) * radius).toFloat()))
            }
            5 -> drawCircle(Color(0xFF72D7CD), radius * 0.30f, center + Offset(radius * 0.72f, -radius * 0.68f))
        }
        drawCircle(hero.color, radius, center)
        if (hero.silhouette == 2) {
            drawArc(Color(0xFFD96854), 190f, 160f, false, Offset(center.x - radius, center.y - radius), Size(radius * 2, radius * 2), style = Stroke(radius * 0.18f))
        } else if (hero.silhouette == 4) {
            drawArc(Color(0xFF4A3156), 195f, 150f, false, Offset(center.x - radius, center.y - radius * 1.10f), Size(radius * 2, radius * 2), style = Stroke(radius * 0.20f))
        }
        val eyeY = center.y - radius * 0.08f
        drawCircle(Color(0xFF462A49), radius * 0.09f, Offset(center.x - radius * 0.34f, eyeY))
        drawCircle(Color(0xFF462A49), radius * 0.09f, Offset(center.x + radius * 0.34f, eyeY))
        drawArc(Color(0xFF462A49), 12f, 156f, false, Offset(center.x - radius * 0.25f, center.y), Size(radius * 0.50f, radius * 0.35f), style = Stroke(maxOf(2f, radius * 0.07f)))
        drawCircle(Color(0x55FF7A91), radius * 0.11f, Offset(center.x - radius * 0.54f, center.y + radius * 0.15f))
        drawCircle(Color(0x55FF7A91), radius * 0.11f, Offset(center.x + radius * 0.54f, center.y + radius * 0.15f))
    }
}

@Composable
private fun SortTray(tray: List<Int>) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp),
        color = SortPlum,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(3.dp, Color(0xFFFFCF91)),
        shadowElevation = 7.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(SORT_TRAY_CAPACITY) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(0.88f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    tray.getOrNull(index)?.let { SortHeroFace(sortHeroes[it], Modifier.fillMaxSize()) }
                }
            }
        }
    }
}

@Composable
private fun SortResultOverlay(
    won: Boolean,
    title: String,
    level: Int,
    score: Int,
    reactionNonce: Int,
    onContinue: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE6512D58)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            color = SortCream,
            shape = RoundedCornerShape(32.dp),
            border = BorderStroke(4.dp, Color(0xFFFFCB89)),
            shadowElevation = 14.dp
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                RiveSortMascot(reactionNonce + 1, Modifier.size(124.dp))
                Text(
                    title.ifBlank { if (won) "ЭПИЗОД ОТКРЫТ!" else "РАУНД ЗАВЕРШЁН" },
                    color = SortPlum,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                Text(
                    if (won) episodeText(level) else "Собирай одинаковых героев ближе друг к другу и не заполняй все семь мест.",
                    color = Color(0xFF603B58),
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center
                )
                Text("Счёт: $score ★", color = SortCoral, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SortCoral)
                ) {
                    Text(if (won) "СЛЕДУЮЩИЙ ЭПИЗОД" else "ПОПРОБОВАТЬ СНОВА", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = SortPlum)
                ) { Text("Закрыть игру") }
            }
        }
    }
}

@Composable
private fun RiveSortMascot(nonce: Int, modifier: Modifier = Modifier) {
    key(nonce) {
        AndroidView(
            factory = { context ->
                RiveAnimationView(context).also { view ->
                    view.isClickable = false
                    view.isFocusable = false
                    view.setRiveResource(R.raw.rive_character_mascot)
                }
            },
            modifier = modifier.clip(CircleShape)
        )
    }
}

@Composable
private fun SortPill(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.height(46.dp), color = color, shape = RoundedCornerShape(17.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.30f))) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun SortPill(color: Color, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(modifier = modifier, color = color, shape = RoundedCornerShape(17.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.30f)), content = content)
}

private fun newSortBoard(level: Int): List<SortTile> {
    val ids = buildList {
        repeat(8) { group -> repeat(3) { add(group % sortHeroes.size) } }
    }.shuffled(Random(level * 1_009 + 47))
    return ids.mapIndexed { index, heroId -> SortTile(level * 100 + index, heroId) }
}

private fun formatSortTime(seconds: Int): String = "%02d:%02d".format(seconds / 60, seconds % 60)

private fun episodeText(level: Int): String = listOf(
    "Луна позвала всех на совместный ролик, но Искра уже включила камеру. Улыбаемся!",
    "Мята нашла идеальный реквизит. Персик уверен, что это шляпа. Кажется, это абажур.",
    "Коралл обещала спокойную репетицию. Через три секунды конфетти было уже везде."
)[(level - 1).mod(3)]
