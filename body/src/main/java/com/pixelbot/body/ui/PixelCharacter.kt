package com.pixelbot.body.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.graphics.drawscope.FilterQuality
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Rect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.pixelbot.body.model.BodyState
import com.pixelbot.body.model.Skin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PixelCharacter(
    skin: Skin,
    bitmap: Bitmap,
    state: BodyState = BodyState.DORMIDO,
    modifier: Modifier = Modifier,
    sizeDp: Int = 128
) {
    val imageBitmap = remember { bitmap.asImageBitmap() }
    val animationState by remember { mutableStateOf(AnimationProgress()) }
    
    val currentAnim = remember(state, skin) { skin.getState(state.name.lowercase()) }
    val totalFrames = currentAnim?.frames ?: 2
    val rowIndex = currentAnim?.rowIndex ?: 0
    val frameDuration = currentAnim?.frameDurationMs ?: 250
    val shouldLoop = currentAnim?.loop ?: true
    
    val frameWidth = imageBitmap.width / 8
    val frameHeight = imageBitmap.height / 8
    
    val srcRect = remember {
        Rect(
            left = 0f,
            top = 0f,
            right = frameWidth.toFloat(),
            bottom = frameHeight.toFloat()
        )
    }
    
    // Update animation frame
    LaunchedEffect(state, currentAnim) {
        animationState.reset()
        if (totalFrames > 1) {
            while (true) {
                delay(frameDuration)
                animationState.nextFrame(totalFrames, shouldLoop)
            }
        }
    }
    
    val currentFrame = animationState.currentFrame
    
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .drawBehind {
                val frameX = (currentFrame % 8) * frameWidth
                val frameY = rowIndex * frameHeight
                
                srcRect.left = frameX.toFloat()
                srcRect.top = frameY.toFloat()
                srcRect.right = (frameX + frameWidth).toFloat()
                srcRect.bottom = (frameY + frameHeight).toFloat()
                
                drawImageRect(
                    imageBitmap = imageBitmap,
                    src = srcRect,
                    dst = Rect(
                        left = 0f,
                        top = 0f,
                        right = sizeDp.toFloat(),
                        bottom = sizeDp.toFloat()
                    ),
                    filterQuality = FilterQuality.None // Nearest neighbor - no smoothing!
                )
            },
        contentAlignment = Alignment.Center
    )
}

class AnimationProgress {
    var currentFrame = 0
        private set
    
    fun nextFrame(totalFrames: Int, loop: Boolean) {
        if (currentFrame < totalFrames - 1) {
            currentFrame++
        } else if (loop) {
            currentFrame = 0
        }
    }
    
    fun reset() {
        currentFrame = 0
    }
}