package com.qijing.chess

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.graphics.RectF
import android.view.MotionEvent
import android.widget.Toast
import android.media.AudioManager
import android.media.ToneGenerator
import kotlin.math.abs

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(35, 28, 23)
        window.navigationBarColor = Color.rgb(35, 28, 23)
        setContentView(ChessView())
    }
}

data class Piece(val name: String, val side: Int)

class ChessView : View {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 70)

    private val board = Array(10) { arrayOfNulls<Piece>(9) }
    private var selected: Pair<Int, Int>? = null
    private var redTurn = true
    private var gameOver = false

    private val redPieces = listOf("车","马","象","士","将","士","象","马","车")

    init {
        setupBoard()
    }

    private fun setupBoard() {
        for (i in 0..8) {
            board[0][i] = Piece(redPieces[i], -1)
            board[9][i] = Piece(redPieces[i], 1)
        }

        board[2][1] = Piece("炮", -1)
        board[2][7] = Piece("炮", -1)
        board[7][1] = Piece("炮", 1)
        board[7][7] = Piece("炮", 1)

        for (i in 0..8 step 2) {
            board[3][i] = Piece("兵", -1)
            board[6][i] = Piece("兵", 1)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cell = width / 9f
        val top = (height - cell * 10) / 2f
        val left = cell / 2

        paint.color = Color.rgb(226, 190, 130)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        paint.color = Color.rgb(95, 54, 30)
        paint.strokeWidth = 3f

        for (r in 0..9) {
            canvas.drawLine(left, top + r * cell, width - left, top + r * cell, paint)
        }

        for (c in 0..8) {
            if (c == 0 || c == 8) {
                canvas.drawLine(left + c * cell, top, left + c * cell, top + 9 * cell, paint)
            } else {
                canvas.drawLine(left + c * cell, top, left + c * cell, top + 4 * cell, paint)
                canvas.drawLine(left + c * cell, top + 5 * cell, left + c * cell, top + 9 * cell, paint)
            }
        }

        drawPalace(canvas, left, top, cell, 0)
        drawPalace(canvas, left, top, cell, 7)

        paint.textSize = cell * 0.34f
        paint.textAlign = Paint.Align.CENTER
        paint.color = Color.rgb(115, 67, 37)
        canvas.drawText("楚河", width * 0.29f, top + 4.7f * cell, paint)
        canvas.drawText("汉界", width * 0.70f, top + 4.7f * cell, paint)

        selected?.let { (r, c) ->
            paint.color = Color.argb(100, 50, 180, 70)
            canvas.drawCircle(left + c * cell, top + r * cell, cell * 0.45f, paint)
            for (rr in 0..9) for (cc in 0..8) {
                if (isLegal(r, c, rr, cc)) {
                    paint.color = Color.argb(150, 30, 120, 50)
                    canvas.drawCircle(left + cc * cell, top + rr * cell, cell * 0.13f, paint)
                }
            }
        }

        for (r in 0..9) for (c in 0..8) {
            board[r][c]?.let { drawPiece(canvas, it, left + c * cell, top + r * cell, cell) }
        }

        paint.color = Color.rgb(70, 35, 20)
        paint.textSize = cell * 0.38f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(if (redTurn) "红方走棋" else "黑方走棋", width / 2f, top - cell * 0.35f, paint)
    }

    private fun drawPalace(canvas: Canvas, left: Float, top: Float, cell: Float, row: Int) {
        paint.color = Color.rgb(95, 54, 30)
        paint.strokeWidth = 3f
        canvas.drawLine(left + 3 * cell, top + row * cell, left + 5 * cell, top + (row + 2) * cell, paint)
        canvas.drawLine(left + 5 * cell, top + row * cell, left + 3 * cell, top + (row + 2) * cell, paint)
    }

    private fun drawPiece(canvas: Canvas, p: Piece, x: Float, y: Float, cell: Float) {
        paint.color = Color.rgb(95, 54, 30)
        canvas.drawCircle(x, y, cell * 0.43f, paint)

        paint.color = Color.rgb(240, 215, 170)
        canvas.drawCircle(x, y, cell * 0.38f, paint)

        paint.color = if (p.side == 1) Color.rgb(190, 35, 30) else Color.rgb(35, 35, 35)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = cell * 0.025f
        canvas.drawCircle(x, y, cell * 0.33f, paint)
        paint.style = Paint.Style.FILL

        paint.textSize = cell * 0.48f
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        canvas.drawText(p.name, x, y + cell * 0.16f, paint)
    }

    private fun isLegal(r: Int, c: Int, rr: Int, cc: Int): Boolean {
        if (rr !in 0..9 || cc !in 0..8) return false
        val p = board[r][c] ?: return false
        if (r == rr && c == cc) return false
        if (board[rr][cc]?.side == p.side) return false

        val dr = rr - r
        val dc = cc - c

        return when (p.name) {
            "车" -> (dr == 0 || dc == 0) && clearPath(r, c, rr, cc)
            "马" -> abs(dr) == 2 && abs(dc) == 1 || abs(dr) == 1 && abs(dc) == 2
            "象" -> abs(dr) == 2 && abs(dc) == 2 && board[r + dr / 2][c + dc / 2] == null
            "士" -> abs(dr) == 1 && abs(dc) == 1
            "将" -> abs(dr) + abs(dc) == 1
            "炮" -> (dr == 0 || dc == 0) && cannonRule(r, c, rr, cc)
            "兵" -> if (p.side == 1) {
                dr == -1 && dc == 0 || r <= 4 && dr == 0 && abs(dc) == 1
            } else {
                dr == 1 && dc == 0 || r >= 5 && dr == 0 && abs(dc) == 1
            }
            else -> false
        }
    }

    private fun clearPath(r: Int, c: Int, rr: Int, cc: Int): Boolean {
        val dr = (rr - r).compareTo(0)
        val dc = (cc - c).compareTo(0)
        var x = r + dr
        var y = c + dc
        while (x != rr || y != cc) {
            if (board[x][y] != null) return false
            x += dr
            y += dc
        }
        return true
    }

    private fun cannonRule(r: Int, c: Int, rr: Int, cc: Int): Boolean {
        val dr = (rr - r).compareTo(0)
        val dc = (cc - c).compareTo(0)
        var screens = 0
        var x = r + dr
        var y = c + dc
        while (x != rr || y != cc) {
            if (board[x][y] != null) screens++
            x += dr
            y += dc
        }
        return if (board[rr][cc] == null) screens == 0 else screens == 1
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_UP || gameOver) return true

        val cell = width / 9f
        val top = (height - cell * 10) / 2f
        val left = cell / 2
        val c = ((event.x - left + cell / 2) / cell).toInt()
        val r = ((event.y - top + cell / 2) / cell).toInt()

        if (r !in 0..9 || c !in 0..8) return true

        val p = board[r][c]

        if (selected == null) {
            if (p != null && p.side == if (redTurn) 1 else -1) {
                selected = r to c
                playTone(1100)
            }
        } else {
            val (sr, sc) = selected!!
            if (isLegal(sr, sc, r, c)) {
                board[r][c] = board[sr][sc]
                board[sr][sc] = null
                selected = null
                redTurn = !redTurn
                playTone(750)
            } else if (p != null && p.side == if (redTurn) 1 else -1) {
                selected = r to c
            } else {
                selected = null
            }
        }

        invalidate()
        return true
    }

    private fun playTone(freq: Int) {
        tone.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
    }
}
