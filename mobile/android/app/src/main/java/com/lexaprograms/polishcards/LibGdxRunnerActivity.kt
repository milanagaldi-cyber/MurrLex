package com.lexaprograms.polishcards

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import org.json.JSONArray
import kotlin.math.abs

class LibGdxRunnerActivity : AndroidApplication() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = AndroidApplicationConfiguration().apply {
            useAccelerometer = false
            useCompass = false
            useImmersiveMode = true
        }
        try {
            val game = LibGdxSpinRunnerGame(parseCards(intent.getStringExtra(EXTRA_CARDS_JSON)))
            val gameView = initializeForView(game, config)
            val root = FrameLayout(this).apply {
                addView(
                    gameView,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                addView(
                    Button(context).apply {
                        text = "Задание"
                        textSize = 12f
                        setOnClickListener {
                            isEnabled = false
                            game.requestTask()
                        }
                    },
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        Gravity.TOP or Gravity.END
                    ).apply {
                        setMargins(0, 18, 18, 0)
                    }
                )
            }
            setContentView(root)
        } catch (error: Throwable) {
            Log.e("MurrLexLibGDX", "LibGDX runner failed to start", error)
            Toast.makeText(this, "LibGDX game failed to start: ${error.javaClass.simpleName}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    companion object {
        private const val EXTRA_CARDS_JSON = "com.lexaprograms.polishcards.LIBGDX_CARDS_JSON"

        fun createIntent(context: Context, cardsJson: String): Intent =
            Intent(context, LibGdxRunnerActivity::class.java).putExtra(EXTRA_CARDS_JSON, cardsJson)

        private fun parseCards(cardsJson: String?): List<LibGdxGameCard> {
            if (cardsJson.isNullOrBlank()) return emptyList()
            return runCatching {
                val array = JSONArray(cardsJson)
                List(array.length()) { index ->
                    val item = array.getJSONObject(index)
                    LibGdxGameCard(
                        front = item.optString("front").trim(),
                        back = item.optString("back").trim()
                    )
                }.filter { it.front.isNotBlank() && it.back.isNotBlank() }
            }.getOrDefault(emptyList())
        }
    }
}

private data class LibGdxGameCard(
    val front: String,
    val back: String
)

private class LibGdxSpinRunnerGame(
    cards: List<LibGdxGameCard>
) : ApplicationAdapter() {
    private val deck = cards.ifEmpty {
        listOf(
            LibGdxGameCard("speed", "szybkosc"),
            LibGdxGameCard("jump", "skok"),
            LibGdxGameCard("life", "zycie"),
            LibGdxGameCard("word", "slowo")
        )
    }.take(30)
    private lateinit var shape: ShapeRenderer
    private lateinit var batch: SpriteBatch
    private lateinit var font: BitmapFont
    private var worldOffset = 0f
    private var runnerY = 0f
    private var velocityY = 0f
    private var spinAngle = 0f
    private var boost = false
    private var spinning = false
    private var lives = 3
    private var rings = 0
    private var score = 0
    private var invulnerable = 0f
    private var message = "Press TASK to open the only card."
    private var questionIndex: Int? = null
    private var taskUsed = false
    private var choiceRects = emptyList<Pair<Rectangle, String>>()

    override fun create() {
        shape = ShapeRenderer()
        batch = SpriteBatch()
        font = BitmapFont()
        font.data.setScale(1.25f)
        runnerY = terrainY(86f) - RunnerRadius
    }

    override fun render() {
        val dt = Gdx.graphics.deltaTime.coerceAtMost(1f / 30f)
        handleInput()
        update(dt)
        draw()
    }

    private fun handleInput() {
        if (!Gdx.input.justTouched()) return
        val x = Gdx.input.x.toFloat()
        val y = Gdx.graphics.height - Gdx.input.y.toFloat()
        val activeQuestion = questionIndex
        if (activeQuestion != null) {
            choiceRects.firstOrNull { (rect, _) -> rect.contains(x, y) }?.let { (_, choice) ->
                answerQuestion(activeQuestion, choice)
            }
            return
        }
        val width = Gdx.graphics.width.toFloat()
        when {
            x < width * 0.32f -> {
                boost = !boost
                message = if (boost) "Boost on." else "Cruise speed."
            }
            x < width * 0.66f -> jump()
            else -> {
                spinning = true
                message = "Spin attack."
            }
        }
    }

    private fun jump() {
        val ground = terrainY(worldOffset + RunnerX)
        if (abs((runnerY + RunnerRadius) - ground) < 4f) {
            velocityY = -340f
            spinning = true
            message = "Jump."
        }
    }

    private fun update(dt: Float) {
        if (questionIndex != null) return
        val speed = if (boost) 250f else 165f
        worldOffset += speed * dt
        val worldX = worldOffset + RunnerX
        val floor = terrainY(worldX)
        velocityY = (velocityY + 650f * dt).coerceAtMost(520f)
        runnerY += velocityY * dt
        if (runnerY + RunnerRadius >= floor) {
            runnerY = floor - RunnerRadius
            velocityY = 0f
            spinning = false
        }
        if (spinning || velocityY != 0f) spinAngle = (spinAngle + speed * dt * 3.2f + 8f) % 360f
        if (invulnerable > 0f) invulnerable -= dt
        checkCollisions()
        if (lives <= 0) {
            lives = 3
            rings = 0
            score = 0
            worldOffset = 0f
            runnerY = terrainY(RunnerX) - RunnerRadius
            velocityY = 0f
            message = "Restarted. Three lives again."
        }
    }

    private fun startTask() {
        if (taskUsed || questionIndex != null) {
            message = if (questionIndex != null) "Task already open." else "Task already used."
            return
        }
        val nextIndex = (score + rings + lives) % deck.size
        questionIndex = nextIndex
        taskUsed = true
        message = deck[nextIndex].front
    }

    fun requestTask() {
        Gdx.app?.postRunnable { startTask() } ?: startTask()
    }

    private fun checkCollisions() {
        val runnerWorldX = worldOffset + RunnerX
        repeat(12) { index ->
            val enemyX = 340f + index * 245f + ((worldOffset / 2940f).toInt() * 2940f)
            val enemyGround = terrainY(enemyX)
            val close = abs(runnerWorldX - enemyX) < 34f && abs((runnerY + RunnerRadius) - enemyGround) < 38f
            if (close && invulnerable <= 0f) {
                if (spinning && velocityY > -150f) {
                    score += 1
                    rings += 3
                    invulnerable = 0.35f
                    message = "Spin hit +3 rings."
                } else {
                    lives -= 1
                    invulnerable = 1.2f
                    message = if (taskUsed) "Ouch. Task already used." else "Ouch. Press TASK for the only card."
                }
            }
        }
    }

    private fun answerQuestion(index: Int, choice: String) {
        val card = deck[index % deck.size]
        if (choice.normalized() == card.back.normalized()) {
            lives += 1
            score += 2
            message = "Task complete: +1 life."
        } else {
            message = "Answer: ${card.back.take(26)}"
        }
        questionIndex = null
    }

    private fun draw() {
        val w = Gdx.graphics.width.toFloat()
        val h = Gdx.graphics.height.toFloat()
        Gdx.gl.glClearColor(0.08f, 0.19f, 0.29f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        drawWorld(w, h)
        drawHud(w, h)
        questionIndex?.let { drawQuestion(deck[it % deck.size], w, h) }
    }

    private fun drawWorld(w: Float, h: Float) {
        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.color = Color(0.10f, 0.48f, 0.72f, 1f)
        shape.rect(0f, 0f, w, h)
        repeat(7) { layer ->
            val alpha = 0.22f + layer * 0.055f
            shape.color = Color(0.14f, 0.42f, 0.32f, alpha)
            var x = -((worldOffset * (0.10f + layer * 0.04f)) % 190f)
            val baseY = h * (0.42f - layer * 0.035f)
            while (x < w + 230f) {
                shape.triangle(x, baseY - 58f, x + 110f, baseY + 48f, x + 220f, baseY - 58f)
                x += 205f
            }
        }
        drawTerrain(w)
        repeat(12) { index ->
            val enemyX = 340f + index * 245f + ((worldOffset / 2940f).toInt() * 2940f)
            val x = screenX(enemyX)
            if (x in -70f..(w + 70f)) drawEnemy(x, terrainY(enemyX) - 24f, index)
        }
        repeat(10) { index ->
            val capsuleX = 540f + index * 330f + ((worldOffset / 3300f).toInt() * 3300f)
            val x = screenX(capsuleX)
            if (x in -60f..(w + 60f)) {
                val y = terrainY(capsuleX) - 86f - (index % 2) * 30f
                shape.color = Color(1f, 0.84f, 0.28f, 1f)
                shape.circle(x, y, 14f, 18)
                shape.color = Color(1f, 1f, 0.86f, 1f)
                shape.circle(x - 4f, y + 4f, 4f, 10)
            }
        }
        drawRunner()
        shape.end()
    }

    private fun drawTerrain(w: Float) {
        shape.color = Color(0.16f, 0.45f, 0.28f, 1f)
        var x = 0f
        while (x < w) {
            val worldX1 = worldOffset + x
            val worldX2 = worldOffset + x + 18f
            shape.triangle(
                x, 0f,
                x, terrainY(worldX1),
                x + 18f, terrainY(worldX2)
            )
            shape.triangle(
                x, 0f,
                x + 18f, terrainY(worldX2),
                x + 18f, 0f
            )
            x += 18f
        }
        var tileX = -worldOffset % 38f
        while (tileX < w) {
            val top = terrainY(worldOffset + tileX)
            shape.color = if (((tileX + worldOffset) / 38f).toInt() % 2 == 0) {
                Color(0.96f, 0.66f, 0.35f, 1f)
            } else {
                Color(0.58f, 0.31f, 0.16f, 1f)
            }
            shape.rect(tileX, top - 26f, 40f, 16f)
            tileX += 38f
        }
    }

    private fun drawRunner() {
        val flicker = invulnerable > 0f && ((invulnerable * 16f).toInt() % 2 == 0)
        if (flicker) return
        shape.color = Color(1f, 0.52f, 0.19f, 1f)
        shape.circle(RunnerX, runnerY, RunnerRadius, 28)
        shape.color = Color(1f, 0.79f, 0.43f, 1f)
        shape.circle(RunnerX, runnerY + 4f, RunnerRadius * 0.62f, 22)
        shape.color = Color(0.12f, 0.08f, 0.05f, 1f)
        shape.circle(RunnerX - 7f, runnerY + 9f, 2.6f, 8)
        shape.circle(RunnerX + 7f, runnerY + 9f, 2.6f, 8)
        shape.rectLine(
            RunnerX - MathUtils.cosDeg(spinAngle) * 23f,
            runnerY - MathUtils.sinDeg(spinAngle) * 23f,
            RunnerX + MathUtils.cosDeg(spinAngle) * 23f,
            runnerY + MathUtils.sinDeg(spinAngle) * 23f,
            5f
        )
    }

    private fun drawEnemy(x: Float, y: Float, index: Int) {
        shape.color = if (index % 2 == 0) Color(0.55f, 0.16f, 0.22f, 1f) else Color(0.21f, 0.18f, 0.46f, 1f)
        shape.circle(x, y, 18f, 20)
        shape.color = Color(0.05f, 0.05f, 0.08f, 1f)
        shape.triangle(x - 16f, y + 10f, x - 8f, y + 26f, x, y + 10f)
        shape.triangle(x + 2f, y + 10f, x + 12f, y + 27f, x + 18f, y + 10f)
    }

    private fun drawHud(w: Float, h: Float) {
        batch.begin()
        font.color = Color.WHITE
        font.draw(batch, "LibGDX Ginger Runner", 18f, h - 18f)
        font.draw(batch, "Lives $lives   Rings $rings   Score $score   Task ${if (taskUsed) "0" else "1"}", 18f, h - 46f)
        font.color = Color(0.95f, 0.96f, 0.76f, 1f)
        font.draw(batch, message.take(52), 18f, 34f)
        font.color = Color(0.80f, 0.95f, 0.90f, 1f)
        font.draw(batch, "Left: boost   Middle: jump   Right: spin", w - 360f, 34f)
        batch.end()
    }

    private fun drawQuestion(card: LibGdxGameCard, w: Float, h: Float) {
        val choices = rememberChoices(card)
        choiceRects = choices.mapIndexed { index, choice ->
            val col = index % 2
            val row = index / 2
            Rectangle(w * 0.16f + col * w * 0.34f, h * 0.16f + row * 58f, w * 0.28f, 44f) to choice
        }
        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.color = Color(0.04f, 0.04f, 0.05f, 0.86f)
        shape.rect(w * 0.10f, h * 0.12f, w * 0.80f, h * 0.68f)
        choiceRects.forEach { (rect, _) ->
            shape.color = Color(0.94f, 0.90f, 0.84f, 1f)
            shape.rect(rect.x, rect.y, rect.width, rect.height)
        }
        shape.end()
        batch.begin()
        font.color = Color.WHITE
        font.draw(batch, "Task: ${card.front.take(42)}", w * 0.16f, h * 0.72f)
        font.color = Color(0.08f, 0.10f, 0.09f, 1f)
        choiceRects.forEach { (rect, choice) ->
            font.draw(batch, choice.take(28), rect.x + 12f, rect.y + 29f)
        }
        batch.end()
    }

    private fun rememberChoices(card: LibGdxGameCard): List<String> {
        val seed = card.front.hashCode() * 31 + card.back.hashCode()
        val wrong = deck
            .map { it.back }
            .filter { it.normalized() != card.back.normalized() }
            .distinctBy { it.normalized() }
            .shuffled(kotlin.random.Random(seed))
            .take(3)
        return (listOf(card.back) + wrong)
            .let { options -> if (options.size >= 4) options.take(4) else options + List(4 - options.size) { card.back } }
            .shuffled(kotlin.random.Random(seed + 19))
    }

    private fun terrainY(worldX: Float): Float =
        92f + MathUtils.sin(worldX / 90f) * 17f + MathUtils.sin(worldX / 37f) * 5f

    private fun screenX(worldX: Float): Float = worldX - worldOffset

    override fun dispose() {
        if (::shape.isInitialized) shape.dispose()
        if (::batch.isInitialized) batch.dispose()
        if (::font.isInitialized) font.dispose()
    }

    companion object {
        private const val RunnerX = 86f
        private const val RunnerRadius = 22f
    }
}

private fun String.normalized(): String =
    lowercase()
        .filter { it.isLetterOrDigit() }
