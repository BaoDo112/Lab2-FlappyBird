package com.example.lab2_23520094

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.util.*

class GameView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private enum class GameState {
        START, RUNNING, GAME_OVER
    }

    private var gameState = GameState.START

    private var birdBitmap: Bitmap
    private var backgroundBitmap: Bitmap
    private var titleBitmap: Bitmap
    private var playButtonBitmap: Bitmap
    private var pipeBitmap: Bitmap
    private var pipeInvertedBitmap: Bitmap

    private var birdY = 0f
    private var birdVelocity = 0f
    private val gravity = 2.4f
    private val jumpStrength = -28f
    private val birdSize = 130

    private var score = 0
    private val pipes = mutableListOf<Pipe>()
    private val pipeWidth = 200f
    private val pipeGap = 450f
    private val pipeSpeed = 12f
    private val random = Random()

    private val scorePaint = Paint().apply {
        color = Color.WHITE
        textSize = 150f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        setShadowLayer(10f, 0f, 0f, Color.BLACK)
    }

    init {
        // 1. Load ảnh Chim (Avatar của bạn)
        val avatarImg = BitmapFactory.decodeResource(resources, R.drawable.my_avatar)
        birdBitmap = Bitmap.createScaledBitmap(avatarImg, birdSize, birdSize, true)

        // 2. Load các assets giao diện
        backgroundBitmap = BitmapFactory.decodeResource(resources, R.drawable.background)
        titleBitmap = BitmapFactory.decodeResource(resources, R.drawable.title)
        playButtonBitmap = BitmapFactory.decodeResource(resources, R.drawable.play)

        // 3. Xử lý Ống nước (Xoay ngược cho ống trên)
        val originalPipe = BitmapFactory.decodeResource(resources, R.drawable.greenpipe)
        // Scale ống dài ra để không bị hở khi màn hình cao
        pipeBitmap = Bitmap.createScaledBitmap(originalPipe, pipeWidth.toInt(), 1500, true)

        val matrix = Matrix()
        matrix.postRotate(180f)
        pipeInvertedBitmap = Bitmap.createBitmap(pipeBitmap, 0, 0, pipeBitmap.width, pipeBitmap.height, matrix, true)
    }

    private fun resetGame() {
        birdY = height / 2f
        birdVelocity = 0f
        score = 0
        pipes.clear()
        spawnPipe()
    }

    private fun spawnPipe() {
        val minPipeHeight = 150f
        val maxPipeHeight = height - pipeGap - 150f
        val topPipeHeight = if (height > 0) random.nextFloat() * (maxPipeHeight - minPipeHeight) + minPipeHeight else 400f
        pipes.add(Pipe(width.toFloat(), topPipeHeight))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Vẽ Background
        val destRect = Rect(0, 0, width, height)
        canvas.drawBitmap(backgroundBitmap, null, destRect, null)

        when (gameState) {
            GameState.START -> drawStartScreen(canvas)
            GameState.RUNNING -> {
                updateLogic()
                drawGame(canvas)
            }
            GameState.GAME_OVER -> {
                drawGame(canvas)
                drawGameOverScreen(canvas)
            }
        }
        invalidate()
    }

    private fun drawStartScreen(canvas: Canvas) {
        val titleWidth = width * 0.8f
        val titleHeight = titleWidth * (titleBitmap.height.toFloat() / titleBitmap.width)
        val titleDest = RectF((width - titleWidth) / 2, height * 0.25f, (width + titleWidth) / 2, height * 0.25f + titleHeight)
        canvas.drawBitmap(titleBitmap, null, titleDest, null)

        val playWidth = width * 0.35f
        val playHeight = playWidth * (playButtonBitmap.height.toFloat() / playButtonBitmap.width)
        val playDest = RectF((width - playWidth) / 2, height * 0.55f, (width + playWidth) / 2, height * 0.55f + playHeight)
        canvas.drawBitmap(playButtonBitmap, null, playDest, null)
    }

    private fun drawGame(canvas: Canvas) {
        // Vẽ các ống nước
        val iterator = pipes.iterator()
        while (iterator.hasNext()) {
            val pipe = iterator.next()

            // Vẽ Ống TRÊN (Đã lật ngược)
            // Lấy phần đuôi của bitmap lật ngược (đầu ống) để vẽ từ cạnh trên màn hình xuống
            val topPipeSrc = Rect(0, pipeInvertedBitmap.height - pipe.topHeight.toInt(), pipeInvertedBitmap.width, pipeInvertedBitmap.height)
            val topPipeDest = RectF(pipe.x, 0f, pipe.x + pipeWidth, pipe.topHeight)
            canvas.drawBitmap(pipeInvertedBitmap, topPipeSrc, topPipeDest, null)

            // Vẽ Ống DƯỚI
            val bottomPipeHeight = height - (pipe.topHeight + pipeGap)
            val bottomPipeSrc = Rect(0, 0, pipeBitmap.width, bottomPipeHeight.toInt())
            val bottomPipeDest = RectF(pipe.x, pipe.topHeight + pipeGap, pipe.x + pipeWidth, height.toFloat())
            canvas.drawBitmap(pipeBitmap, bottomPipeSrc, bottomPipeDest, null)

            if (gameState == GameState.RUNNING) {
                pipe.x -= pipeSpeed

                if (!pipe.passed && pipe.x < 150f) {
                    pipe.passed = true
                    score++
                }

                // Va chạm
                val birdRect = RectF(150f, birdY, 150f + birdSize, birdY + birdSize)
                if (RectF.intersects(birdRect, topPipeDest) || RectF.intersects(birdRect, bottomPipeDest) || birdY < 0 || birdY > height) {
                    gameState = GameState.GAME_OVER
                }
            }
            if (pipe.x + pipeWidth < 0) iterator.remove()
        }

        if (gameState == GameState.RUNNING && pipes.isNotEmpty() && pipes.last().x < width - 650) {
            spawnPipe()
        }

        // Vẽ Chim và Điểm
        canvas.drawBitmap(birdBitmap, 150f, birdY, null)
        canvas.drawText(score.toString(), width / 2f, 250f, scorePaint)
    }

    private fun drawGameOverScreen(canvas: Canvas) {
        val overlayPaint = Paint().apply { color = Color.argb(120, 0, 0, 0) }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)

        val goPaint = Paint().apply {
            color = Color.WHITE
            textSize = 160f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("GAME OVER", width / 2f, height / 2f - 50f, goPaint)
        canvas.drawText("TAP TO RESTART", width / 2f, height / 2f + 100f, scorePaint.apply { textSize = 70f; color = Color.YELLOW })
    }

    private fun updateLogic() {
        birdVelocity += gravity
        birdY += birdVelocity
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            when (gameState) {
                GameState.START -> { resetGame(); gameState = GameState.RUNNING }
                GameState.RUNNING -> birdVelocity = jumpStrength
                GameState.GAME_OVER -> gameState = GameState.START
            }
        }
        return true
    }

    data class Pipe(var x: Float, val topHeight: Float, var passed: Boolean = false)
}