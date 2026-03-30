package com.example.lab2_23520094

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.scale
import java.util.*
import kotlin.math.tanh

class GameView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private enum class GameState {
        START, RUNNING, GAME_OVER, AI_PLAYING
    }

    private var gameState = GameState.START

    // Assets
    private val birdBitmap: Bitmap
    private val backgroundBitmap: Bitmap
    private val titleBitmap: Bitmap
    private val playButtonBitmap: Bitmap
    private val pipeBitmap: Bitmap
    private val pipeInvertedBitmap: Bitmap

    // AI Variables
    private val populationSize = 50
    private var birds = mutableListOf<NeuralBird>()
    private var generation = 1
    private var currentBestScore = 0
    private var allTimeBestScore = 0

    // Game Logic
    private val pipes = mutableListOf<Pipe>()
    private val pipeWidth = 200f
    private val pipeGap = 450f
    private val pipeSpeed = 12f
    private val birdSize = 130
    private val random = Random()

    // UI Paints
    private val scorePaint = Paint().apply {
        color = Color.WHITE
        textSize = 120f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        setShadowLayer(10f, 0f, 0f, Color.BLACK)
    }
    private val infoPaint = Paint().apply {
        color = Color.YELLOW
        textSize = 50f
        textAlign = Paint.Align.LEFT
        typeface = Typeface.MONOSPACE
        setShadowLayer(5f, 0f, 0f, Color.BLACK)
    }

    // Preallocated Rects for performance
    private val birdRect = RectF()
    private val topPipeSrc = Rect()
    private val topPipeDest = RectF()
    private val bottomPipeSrc = Rect()
    private val bottomPipeDest = RectF()
    private val bgDestRect = Rect()

    init {
        val avatarImg = BitmapFactory.decodeResource(resources, R.drawable.my_avatar)
        birdBitmap = avatarImg.scale(birdSize, birdSize, true)
        backgroundBitmap = BitmapFactory.decodeResource(resources, R.drawable.background)
        titleBitmap = BitmapFactory.decodeResource(resources, R.drawable.title)
        playButtonBitmap = BitmapFactory.decodeResource(resources, R.drawable.play)
        
        val originalPipe = BitmapFactory.decodeResource(resources, R.drawable.greenpipe)
        pipeBitmap = originalPipe.scale(pipeWidth.toInt(), 1500, true)
        
        val matrix = Matrix().apply { postRotate(180f) }
        pipeInvertedBitmap = Bitmap.createBitmap(pipeBitmap, 0, 0, pipeBitmap.width, pipeBitmap.height, matrix, true)
    }

    private fun startHumanGame() {
        birds.clear()
        birds.add(NeuralBird(null))
        resetGameEnvironment()
        gameState = GameState.RUNNING
    }

    private fun startAIGame() {
        if (birds.isEmpty() || birds.all { it.isDead }) {
            if (birds.isEmpty()) {
                repeat(populationSize) { birds.add(NeuralBird(Brain())) }
            } else {
                nextGeneration()
            }
        }
        resetGameEnvironment()
        gameState = GameState.AI_PLAYING
    }

    private fun resetGameEnvironment() {
        currentBestScore = 0
        pipes.clear()
        spawnPipe()
        birds.forEach { it.reset(height / 2f) }
    }

    private fun nextGeneration() {
        generation++
        val sortedBirds = birds.sortedByDescending { it.fitness }
        val bestBrain = sortedBirds[0].brain ?: Brain()
        
        val newBirds = mutableListOf<NeuralBird>()
        // Elitism: Giữ lại con tốt nhất
        newBirds.add(NeuralBird(bestBrain.copy()))
        
        // Tạo ra thế hệ mới từ các con top đầu
        while (newBirds.size < populationSize) {
            val parent = sortedBirds[random.nextInt(Math.min(10, sortedBirds.size))]
            val childBrain = parent.brain?.copy() ?: Brain()
            childBrain.mutate(0.1f)
            newBirds.add(NeuralBird(childBrain))
        }
        birds = newBirds
    }

    private fun spawnPipe() {
        val minPipeHeight = 150f
        val maxPipeHeight = height - pipeGap - 150f
        val topHeight = if (height > 0) random.nextFloat() * (maxPipeHeight - minPipeHeight) + minPipeHeight else 400f
        pipes.add(Pipe(width.toFloat(), topHeight))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        bgDestRect.set(0, 0, width, height)
        canvas.drawBitmap(backgroundBitmap, null, bgDestRect, null)

        when (gameState) {
            GameState.START -> drawStartScreen(canvas)
            GameState.RUNNING, GameState.AI_PLAYING -> {
                updateGameLogic()
                drawGameScene(canvas)
            }
            GameState.GAME_OVER -> {
                drawGameScene(canvas)
                drawGameOverScreen(canvas)
            }
        }
        invalidate()
    }

    private fun drawStartScreen(canvas: Canvas) {
        val titleWidth = width * 0.8f
        val titleHeight = titleWidth * (titleBitmap.height.toFloat() / titleBitmap.width)
        canvas.drawBitmap(titleBitmap, null, RectF(width*0.1f, height*0.15f, width*0.9f, height*0.15f + titleHeight), null)

        val playWidth = width * 0.4f
        val playHeight = playWidth * (playButtonBitmap.height.toFloat() / playButtonBitmap.width)
        canvas.drawBitmap(playButtonBitmap, null, RectF(width*0.3f, height*0.45f, width*0.7f, height*0.45f + playHeight), null)
        
        canvas.drawText("TAP TOP: PLAY", width/2f, height*0.75f, infoPaint.apply { textAlign = Paint.Align.CENTER; color = Color.WHITE })
        canvas.drawText("TAP BOTTOM: AI TRAIN", width/2f, height*0.82f, infoPaint.apply { color = Color.YELLOW })
    }

    private fun updateGameLogic() {
        val it = pipes.iterator()
        while (it.hasNext()) {
            val pipe = it.next()
            pipe.x -= pipeSpeed
            if (pipe.x + pipeWidth < 0) it.remove()
        }
        if (pipes.isNotEmpty() && pipes.last().x < width - 700) spawnPipe()

        val targetPipe = pipes.firstOrNull { it.x + pipeWidth > 150f } ?: pipes[0]

        birds.forEach { bird ->
            if (!bird.isDead) {
                if (gameState == GameState.AI_PLAYING) {
                    val inputs = floatArrayOf(
                        bird.y / height,
                        bird.velocity / 30f,
                        (targetPipe.x - 150f) / width,
                        targetPipe.topHeight / height,
                        (targetPipe.topHeight + pipeGap) / height
                    )
                    if (bird.brain?.think(inputs) ?: 0f > 0.5f) bird.jump()
                }

                bird.update()

                birdRect.set(150f, bird.y, 150f + birdSize, bird.y + birdSize)
                topPipeDest.set(targetPipe.x, 0f, targetPipe.x + pipeWidth, targetPipe.topHeight)
                bottomPipeDest.set(targetPipe.x, targetPipe.topHeight + pipeGap, targetPipe.x + pipeWidth, height.toFloat())

                if (RectF.intersects(birdRect, topPipeDest) || RectF.intersects(birdRect, bottomPipeDest) || bird.y < 0 || bird.y > height) {
                    bird.isDead = true
                }

                if (!targetPipe.passed && targetPipe.x < 150f) {
                    targetPipe.passed = true
                    currentBestScore++
                    // Cộng điểm fitness cực lớn khi vượt qua ống
                    birds.forEach { if (!it.isDead) it.fitness += 1000f }
                    if (currentBestScore > allTimeBestScore) allTimeBestScore = currentBestScore
                }
            }
        }

        if (birds.all { it.isDead }) {
            if (gameState == GameState.AI_PLAYING) {
                nextGeneration()
                resetGameEnvironment()
            } else {
                gameState = GameState.GAME_OVER
            }
        }
    }

    private fun drawGameScene(canvas: Canvas) {
        pipes.forEach { pipe ->
            topPipeSrc.set(0, pipeInvertedBitmap.height - pipe.topHeight.toInt(), pipeInvertedBitmap.width, pipeInvertedBitmap.height)
            topPipeDest.set(pipe.x, 0f, pipe.x + pipeWidth, pipe.topHeight)
            canvas.drawBitmap(pipeInvertedBitmap, topPipeSrc, topPipeDest, null)

            val bHeight = height - (pipe.topHeight + pipeGap)
            bottomPipeSrc.set(0, 0, pipeBitmap.width, bHeight.toInt())
            bottomPipeDest.set(pipe.x, pipe.topHeight + pipeGap, pipe.x + pipeWidth, height.toFloat())
            canvas.drawBitmap(pipeBitmap, bottomPipeSrc, bottomPipeDest, null)
        }

        birds.forEach { if (!it.isDead) canvas.drawBitmap(birdBitmap, 150f, it.y, null) }

        canvas.drawText(currentBestScore.toString(), width/2f, 250f, scorePaint)
        
        if (gameState == GameState.AI_PLAYING) {
            canvas.drawText("GEN: $generation", 50f, 100f, infoPaint.apply { textAlign = Paint.Align.LEFT; color = Color.YELLOW })
            canvas.drawText("ALIVE: ${birds.count { !it.isDead }}/$populationSize", 50f, 170f, infoPaint)
            canvas.drawText("BEST: $allTimeBestScore", 50f, 240f, infoPaint)
        }
    }

    private fun drawGameOverScreen(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), Paint().apply { color = Color.argb(150, 0, 0, 0) })
        canvas.drawText("GAME OVER", width/2f, height/2f - 50f, scorePaint)
        canvas.drawText("SCORE: $currentBestScore", width/2f, height/2f + 50f, infoPaint.apply { textAlign = Paint.Align.CENTER; color = Color.WHITE })
        canvas.drawText("TAP TO RESTART", width/2f, height/2f + 200f, infoPaint.apply { color = Color.YELLOW })
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            performClick()
            when (gameState) {
                GameState.START -> {
                    if (event.y < height / 2) startHumanGame() else startAIGame()
                }
                GameState.RUNNING -> birds.firstOrNull()?.jump()
                GameState.GAME_OVER -> gameState = GameState.START
                GameState.AI_PLAYING -> {}
            }
        }
        return true
    }

    inner class NeuralBird(val brain: Brain?) {
        var y = 0f
        var velocity = 0f
        var isDead = false
        var fitness = 0f

        fun reset(startY: Float) {
            y = startY
            velocity = 0f
            isDead = false
            fitness = 0f
        }

        fun update() {
            if (isDead) return
            velocity += 2.4f
            y += velocity
            fitness += 1f // Sống sót lâu hơn thì fitness cao hơn
        }

        fun jump() { velocity = -28f }
    }

    class Brain(private var weights: FloatArray = FloatArray(WEIGHT_COUNT) { (Math.random() * 2 - 1).toFloat() }) {
        companion object {
            const val INPUTS = 5
            const val HIDDEN = 8
            const val WEIGHT_COUNT = (INPUTS + 1) * HIDDEN + (HIDDEN + 1) * 1
        }

        fun think(inputs: FloatArray): Float {
            val hiddenNodes = FloatArray(HIDDEN)
            var idx = 0
            
            // Input to Hidden layer
            for (i in 0 until HIDDEN) {
                var sum = weights[idx++] // bias
                for (j in 0 until INPUTS) {
                    sum += inputs[j] * weights[idx++]
                }
                hiddenNodes[i] = tanh(sum.toDouble()).toFloat()
            }
            
            // Hidden to Output layer
            var outputSum = weights[idx++] // bias
            for (i in 0 until HIDDEN) {
                outputSum += hiddenNodes[i] * weights[idx++]
            }
            
            return 1f / (1f + Math.exp(-outputSum.toDouble()).toFloat()) // Sigmoid
        }

        fun mutate(rate: Float) {
            for (i in weights.indices) {
                if (Math.random() < rate) {
                    // Sử dụng nhiễu Gaussian để tiến hóa mịn màng hơn
                    weights[i] += (Random().nextGaussian().toFloat() * 0.2f)
                }
            }
        }

        fun copy() = Brain(weights.copyOf())
    }

    data class Pipe(var x: Float, val topHeight: Float, var passed: Boolean = false)
}
