package com.pixelbot.body.generator

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.pixelbot.body.model.Skin
import kotlinx.serialization.json.Json

object SkinGenerator {
    
    private const val FRAME_SIZE = 32
    private const val COLS = 8
    private const val ROWS = 8
    private const val SHEET_WIDTH = FRAME_SIZE * COLS
    private const val SHEET_HEIGHT = FRAME_SIZE * ROWS
    
    fun generateClassicSkin(): GeneratedSkin {
        val bitmap = Bitmap.createBitmap(SHEET_WIDTH, SHEET_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { isAntiAlias = false }
        
        // Paleta: negro, blanco, morado, cyan
        val black = Color.BLACK
        val white = Color.WHITE
        val purple = Color.parseColor("#6C5CE7")
        val cyan = Color.parseColor("#00CEC9")
        val darkPurple = Color.parseColor("#4A3BC7")
        val darkCyan = Color.parseColor("#00A8A5")
        
        // Row 0: DORMIDO (2 frames) - ojos cerrados, Zzz
        drawSleepingFrame(canvas, paint, 0, 0, black, white, purple, cyan, darkPurple, frame = 0)
        drawSleepingFrame(canvas, paint, 1, 0, black, white, purple, cyan, darkPurple, frame = 1)
        
        // Row 1: ESCUCHANDO (6 frames) - orejas animadas, ojos abiertos
        for (i in 0..5) {
            drawListeningFrame(canvas, paint, i, 1, black, white, purple, cyan, darkPurple, frame = i)
        }
        
        // Row 2: PENSANDO (6 frames) - burbuja de pensamiento, ojos mirando arriba
        for (i in 0..5) {
            drawThinkingFrame(canvas, paint, i, 2, black, white, purple, cyan, darkPurple, frame = i)
        }
        
        // Row 3: HABLANDO (4 frames) - boca moviéndose
        for (i in 0..3) {
            drawTalkingFrame(canvas, paint, i, 3, black, white, purple, cyan, darkPurple, frame = i)
        }
        
        // Row 4: FELIZ (4 frames) - sonrisa, brillo en ojos
        for (i in 0..3) {
            drawHappyFrame(canvas, paint, i, 4, black, white, purple, cyan, darkPurple, frame = i)
        }
        
        // Row 5: ERROR (4 frames) - X en ojos, sacudida
        for (i in 0..3) {
            drawErrorFrame(canvas, paint, i, 5, black, white, purple, cyan, darkPurple, frame = i)
        }
        
        // Row 6: CONFUNDIDO (4 frames) - ojos desiguales, ? sobre cabeza
        for (i in 0..3) {
            drawConfusedFrame(canvas, paint, i, 6, black, white, purple, cyan, darkPurple, frame = i)
        }
        
        // Row 7: Extra/Reserved
        
        val skin = Skin(
            id = "pixel_classic",
            name = "Pixel Clásico",
            author = "PixelBot",
            frameSize = FRAME_SIZE,
            palette = listOf("#000000", "#FFFFFF", "#6C5CE7", "#00CEC9"),
            rows = mapOf(
                "dormido" to Skin.RowData(frames = 2, rowIndex = 0),
                "escuchando" to Skin.RowData(frames = 6, rowIndex = 1),
                "pensando" to Skin.RowData(frames = 6, rowIndex = 2),
                "hablando" to Skin.RowData(frames = 4, rowIndex = 3),
                "feliz" to Skin.RowData(frames = 4, rowIndex = 4),
                "error" to Skin.RowData(frames = 4, rowIndex = 5, loop = false),
                "confundido" to Skin.RowData(frames = 4, rowIndex = 6)
            )
        )
        
        return GeneratedSkin(skin, bitmap)
    }
    
    fun generateNeonSkin(): GeneratedSkin {
        val bitmap = Bitmap.createBitmap(SHEET_WIDTH, SHEET_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { isAntiAlias = false }
        
        // Paleta neón: negro, rosa neón, cyan neón, amarillo neón
        val black = Color.BLACK
        val neonPink = Color.parseColor("#FF10F0")
        val neonCyan = Color.parseColor("#00F0FF")
        val neonYellow = Color.parseColor("#FFE600")
        val darkPink = Color.parseColor("#CC00CC")
        val darkCyan = Color.parseColor("#00CCCC")
        
        // Row 0: DORMIDO (2 frames)
        drawSleepingFrame(canvas, paint, 0, 0, black, neonPink, neonCyan, neonYellow, darkPink, frame = 0)
        drawSleepingFrame(canvas, paint, 1, 0, black, neonPink, neonCyan, neonYellow, darkPink, frame = 1)
        
        // Row 1: ESCUCHANDO (6 frames)
        for (i in 0..5) {
            drawListeningFrame(canvas, paint, i, 1, black, neonPink, neonCyan, neonYellow, darkPink, frame = i)
        }
        
        // Row 2: PENSANDO (6 frames)
        for (i in 0..5) {
            drawThinkingFrame(canvas, paint, i, 2, black, neonPink, neonCyan, neonYellow, darkPink, frame = i)
        }
        
        // Row 3: HABLANDO (4 frames)
        for (i in 0..3) {
            drawTalkingFrame(canvas, paint, i, 3, black, neonPink, neonCyan, neonYellow, darkPink, frame = i)
        }
        
        // Row 4: FELIZ (4 frames)
        for (i in 0..3) {
            drawHappyFrame(canvas, paint, i, 4, black, neonPink, neonCyan, neonYellow, darkPink, frame = i)
        }
        
        // Row 5: ERROR (4 frames)
        for (i in 0..3) {
            drawErrorFrame(canvas, paint, i, 5, black, neonPink, neonCyan, neonYellow, darkPink, frame = i)
        }
        
        // Row 6: CONFUNDIDO (4 frames)
        for (i in 0..3) {
            drawConfusedFrame(canvas, paint, i, 6, black, neonPink, neonCyan, neonYellow, darkPink, frame = i)
        }
        
        val skin = Skin(
            id = "pixel_neon",
            name = "Pixel Neón",
            author = "PixelBot",
            frameSize = FRAME_SIZE,
            palette = listOf("#000000", "#FF10F0", "#00F0FF", "#FFE600"),
            rows = mapOf(
                "dormido" to Skin.RowData(frames = 2, rowIndex = 0),
                "escuchando" to Skin.RowData(frames = 6, rowIndex = 1),
                "pensando" to Skin.RowData(frames = 6, rowIndex = 2),
                "hablando" to Skin.RowData(frames = 4, rowIndex = 3),
                "feliz" to Skin.RowData(frames = 4, rowIndex = 4),
                "error" to Skin.RowData(frames = 4, rowIndex = 5, loop = false),
                "confundido" to Skin.RowData(frames = 4, rowIndex = 6)
            )
        )
        
        return GeneratedSkin(skin, bitmap)
    }
    
    private fun drawSleepingFrame(canvas: Canvas, paint: Paint, col: Int, row: Int, 
        black: Int, white: Int, primary: Int, secondary: Int, darkPrimary: Int, frame: Int) {
        val x = col * FRAME_SIZE
        val y = row * FRAME_SIZE
        val cx = x + FRAME_SIZE / 2
        val cy = y + FRAME_SIZE / 2
        
        // Cuerpo redondeado
        paint.color = primary
        canvas.drawRect(Rect(x + 6, y + 10, x + 26, y + 28), paint)
        
        // Cabeza
        paint.color = white
        canvas.drawRect(Rect(x + 8, y + 4, x + 24, y + 18), paint)
        
        // Contorno cabeza
        paint.color = black
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRect(Rect(x + 8, y + 4, x + 24, y + 18), paint)
        paint.style = Paint.Style.FILL
        
        // Ojos cerrados (línea horizontal)
        paint.color = black
        val eyeY = y + 9
        canvas.drawLine((x + 11).toFloat(), eyeY.toFloat(), (x + 15).toFloat(), eyeY.toFloat(), paint)
        canvas.drawLine((x + 17).toFloat(), eyeY.toFloat(), (x + 21).toFloat(), eyeY.toFloat(), paint)
        
        // Zzz burbujas (animación)
        val zzzOffset = if (frame == 0) 0 else 4
        paint.color = secondary
        paint.textSize = 10f
        canvas.drawText("z", (x + 22).toFloat(), (y + 6 - zzzOffset).toFloat(), paint)
        canvas.drawText("z", (x + 24).toFloat(), (y + 2 - zzzOffset).toFloat(), paint)
        
        // Orejas
        paint.color = primary
        canvas.drawRect(Rect(x + 4, y + 6, x + 8, y + 12), paint)
        canvas.drawRect(Rect(x + 24, y + 6, x + 28, y + 12), paint)
    }
    
    private fun drawListeningFrame(canvas: Canvas, paint: Paint, col: Int, row: Int,
        black: Int, white: Int, primary: Int, secondary: Int, darkPrimary: Int, frame: Int) {
        val x = col * FRAME_SIZE
        val y = row * FRAME_SIZE
        
        // Cuerpo
        paint.color = primary
        canvas.drawRect(Rect(x + 6, y + 10, x + 26, y + 28), paint)
        
        // Cabeza
        paint.color = white
        canvas.drawRect(Rect(x + 8, y + 4, x + 24, y + 18), paint)
        
        // Contorno
        paint.color = black
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRect(Rect(x + 8, y + 4, x + 24, y + 18), paint)
        paint.style = Paint.Style.FILL
        
        // Ojos abiertos
        paint.color = black
        canvas.drawRect(Rect(x + 11, y + 8, x + 13, y + 10), paint)
        canvas.drawRect(Rect(x + 19, y + 8, x + 21, y + 10), paint)
        
        // Orejas animadas (se mueven arriba/abajo)
        val earOffset = when (frame % 3) {
            0 -> 0
            1 -> -2
            else -> 2
        }
        paint.color = primary
        canvas.drawRect(Rect(x + 4, y + 6 + earOffset, x + 8, y + 12 + earOffset), paint)
        canvas.drawRect(Rect(x + 24, y + 6 - earOffset, x + 28, y + 12 - earOffset), paint)
        
        // Líneas de "onda" sonido
        paint.color = secondary
        paint.strokeWidth = 1f
        paint.style = Paint.Style.STROKE
        val wavePhase = frame % 3
        for (w in 0..2) {
            val wx = x + 28 + w * 3
            canvas.drawLine(wx.toFloat(), (y + 14).toFloat(), (wx + 2).toFloat(), (y + 10).toFloat(), paint)
            canvas.drawLine(wx.toFloat(), (y + 14).toFloat(), (wx + 2).toFloat(), (y + 18).toFloat(), paint)
        }
        paint.style = Paint.Style.FILL
    }
    
    private fun drawThinkingFrame(canvas: Canvas, paint: Paint, col: Int, row: Int,
        black: Int, white: Int, primary: Int, secondary: Int, darkPrimary: Int, frame: Int) {
        val x = col * FRAME_SIZE
        val y = row * FRAME_SIZE
        
        // Cuerpo
        paint.color = primary
        canvas.drawRect(Rect(x + 6, y + 10, x + 26, y + 28), paint)
        
        // Cabeza
        paint.color = white
        canvas.drawRect(Rect(x + 8, y + 4, x + 24, y + 18), paint)
        
        // Contorno
        paint.color = black
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRect(Rect(x + 8, y + 4, x + 24, y + 18), paint)
        paint.style = Paint.Style.FILL
        
        // Ojos mirando arriba (pensando)
        paint.color = black
        val eyeY = y + 6 + (frame % 2)
        canvas.drawRect(Rect(x + 11, eyeY, x + 13, eyeY + 2), paint)
        canvas.drawRect(Rect(x + 19, eyeY, x + 21, eyeY + 2), paint)
        
        // Burbuja de pensamiento
        paint.color = secondary
        val bubbleX = x + 24
        val bubbleY = y + 2
        val bubbleSize = 6 + (frame % 3) * 2
        canvas.drawCircle((bubbleX).toFloat(), (bubbleY).toFloat(), bubbleSize.toFloat(), paint)
        // Burbujas pequeñas
        canvas.drawCircle((bubbleX + 6).toFloat(), (bubbleY - 4).toFloat(), 3f, paint)
        canvas.drawCircle((bubbleX + 10).toFloat(), (bubbleY - 8).toFloat(), 2f, paint)
        
        // Orejas
        paint.color = primary
        canvas.drawRect(Rect(x + 4, y + 6, x + 8, y + 12), paint)
        canvas.drawRect(Rect(x + 24, y + 6, x + 28, y + 12), paint)
    }
    
    private fun drawTalkingFrame(canvas: Canvas, paint: Paint, col: Int, row: Int,
        black: Int, white: Int, primary: Int, secondary: Int, darkPrimary: Int, frame: Int) {
        val x = col * FRAME_SIZE
        val y = row * FRAME_SIZE
        
        // Cuerpo
        paint.color = primary
        canvas.drawRect(Rect(x + 6, y + 10, x + 26, y + 28), paint)
        
        // Cabeza
        paint.color = white
        canvas.drawRect(Rect(x + 8, y + 4, x + 24, y + 18), paint)
        
        // Contorno
        paint.color = black
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRect(Rect(x + 8, y + 4, x + 24, y + 18), paint)
        paint.style = Paint.Style.FILL
        
        // Ojos
        paint.color = black
        canvas.drawRect(Rect(x + 11, y + 8, x + 13, y + 10), paint)
        canvas.drawRect(Rect(x + 19, y + 8, x + 21, y + 10), paint)
        
        // Boca animada (abre/cierra)
        paint.color = black
        val mouthHeight = when (frame) {
            0 -> 1
            1 -> 4
            2 -> 2
            else -> 5
        }
        canvas.drawRect(Rect(x + 14, y + 14, x + 18, y + 14 + mouthHeight), paint)
        
        // Orejas
        paint.color = primary
        canvas.drawRect(Rect(x + 4, y + 6, x + 8, y + 12), paint)
        canvas.drawRect(Rect(x + 24, y + 6, x + 28, y + 12), paint)
    }
    
    private fun drawHappyFrame(canvas: Canvas, paint: Paint, col: Int, row: Int,
        black: Int, white: Int, primary: Int, secondary: Int, darkPrimary: Int, frame: Int) {
        val x = col * FRAME_SIZE
        val y = row * FRAME_SIZE
        
        // Cuerpo con brillo
        paint.color = primary
        canvas.drawRect(Rect(x + 6, y + 10, x + 26, y + 28), paint)
        
        // Brillo en cuerpo
        paint.color = secondary
        canvas.drawRect(Rect(x + 8, y + 12, x + 12, y + 16), paint)
        
        // Cabeza
        paint.color = white
        canvas.drawRect(Rect(x + 8, y + 4, x + 24, y + 18), paint)
        
        // Contorno
        paint.color = black
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRect(Rect(x + 8, y + 4, x + 24, y + 18), paint)
        paint.style = Paint.Style.FILL
        
        // Ojos con brillo (corazones o estrellas)
        paint.color = black
        if (frame % 2 == 0) {
            // Ojos normales con brillo
            canvas.drawRect(Rect(x + 11, y + 8, x + 13, y + 10), paint)
            canvas.drawRect(Rect(x + 19, y + 8, x + 21, y + 10), paint)
            // Brillo
            paint.color = white
            canvas.drawPoint((x + 11.5f), (y + 8.5f), paint)
            canvas.drawPoint((x + 19.5f), (y + 8.5f), paint)
        } else {
            // Ojos cerrados feliz
            canvas.drawLine((x + 11).toFloat(), (y + 9).toFloat(), (x + 15).toFloat(), (y + 9).toFloat(), paint)
            canvas.drawLine((x + 17).toFloat(), (y + 9).toFloat(), (x + 21).toFloat(), (y + 9).toFloat(), paint)
        }
        
        // Sonrisa
        paint.color = black
        paint.strokeWidth = 1f
        paint.style = Paint.Style.STROKE
        canvas.drawArc(Rect(x + 12, y + 12, x + 20, y + 18), 0f, 180f, false, paint)
        paint.style = Paint.Style.FILL
        
        // Orejas alegres (hacia arriba)
        paint.color = primary
        canvas.drawRect(Rect(x + 4, y + 4, x + 8, y + 10), paint)
        canvas.drawRect(Rect(x + 24, y + 4, x + 28, y + 10), paint)
    }
    
    private fun drawErrorFrame(canvas: Canvas, paint: Paint, col: Int, row: Int,
        black: Int, white: Int, primary: Int, secondary: Int, darkPrimary: Int, frame: Int) {
        val x = col * FRAME_SIZE
        val y = row * FRAME_SIZE
        
        // Cuerpo rojo error
        paint.color = Color.RED
        canvas.drawRect(Rect(x + 6, y + 10, x + 26, y + 28), paint)
        
        // Cabeza
        paint.color = white
        canvas.drawRect(Rect(x + 8, y + 4, x + 24, y + 18), paint)
        
        // Contorno
        paint.color = black
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRect(Rect(x + 8, y + 4, x + 24, y + 18), paint)
        paint.style = Paint.Style.FILL
        
        // Ojos en X
        paint.color = black
        paint.strokeWidth = 2f
        paint.style = Paint.Style.STROKE
        canvas.drawLine((x + 11).toFloat(), (y + 8).toFloat(), (x + 15).toFloat(), (y + 12).toFloat(), paint)
        canvas.drawLine((x + 15).toFloat(), (y + 8).toFloat(), (x + 11).toFloat(), (y + 12).toFloat(), paint)
        canvas.drawLine((x + 19).toFloat(), (y + 8).toFloat(), (x + 23).toFloat(), (y + 12).toFloat(), paint)
        canvas.drawLine((x + 23).toFloat(), (y + 8).toFloat(), (x + 19).toFloat(), (y + 12).toFloat(), paint)
        paint.style = Paint.Style.FILL
        paint.strokeWidth = 1f
        
        // Sacudida (offset horizontal)
        // Se maneja en el compositor con offset
        
        // Orejas caídas
        paint.color = Color.RED
        canvas.drawRect(Rect(x + 4, y + 10, x + 8, y + 16), paint)
        canvas.drawRect(Rect(x + 24, y + 10, x + 28, y + 16), paint)
    }
    
    private fun drawConfusedFrame(canvas: Canvas, paint: Paint, col: Int, row: Int,
        black: Int, white: Int, primary: Int, secondary: Int, darkPrimary: Int, frame: Int) {
        val x = col * FRAME_SIZE
        val y = row * FRAME_SIZE
        
        // Cuerpo
        paint.color = primary
        canvas.drawRect(Rect(x + 6, y + 10, x + 26, y + 28), paint)
        
        // Cabeza
        paint.color = white
        canvas.drawRect(Rect(x + 8, y + 4, x + 24, y + 18), paint)
        
        // Contorno
        paint.color = black
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRect(Rect(x + 8, y + 4, x + 24, y + 18), paint)
        paint.style = Paint.Style.FILL
        
        // Ojos desiguales (uno grande, uno pequeño)
        paint.color = black
        canvas.drawRect(Rect(x + 11, y + 7, x + 14, y + 11), paint) // Ojo grande
        canvas.drawRect(Rect(x + 20, y + 9, x + 21, y + 10), paint) // Ojo pequeño
        
        // Signo de interrogación sobre cabeza
        paint.color = secondary
        paint.textSize = 14f
        canvas.drawText("?", (x + 14).toFloat(), (y + 2).toFloat(), paint)
        
        // Orejas torcidas
        paint.color = primary
        canvas.drawRect(Rect(x + 4, y + 6, x + 8, y + 12), paint)
        canvas.drawRect(Rect(x + 25, y + 5, x + 29, y + 11), paint)
    }
    
    data class GeneratedSkin(val skin: Skin, val bitmap: Bitmap)
}