package com.pixelbot.body.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.IBinder
import android.view.Display
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelbot.body.model.BodyState
import com.pixelbot.body.ui.PixelCharacter
import com.pixelbot.body.generator.SkinGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class FloatingBubbleService : Service() {

    companion object {
        const val ACTION_TOGGLE = "com.pixelbot.body.ACTION_TOGGLE_BUBBLE"
    }

    private var windowManager: WindowManager? = null
    private var bubbleView: FrameLayout? = null
    private var composeView: ComposeView? = null
    private var initialX = 0f
    private var initialY = 0f
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isMoving = false
    private var layoutParams: WindowManager.LayoutParams? = null
    private var isVisible = true
    
    var currentState: BodyState = BodyState.DORMIDO
        private set

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createBubbleView()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_TOGGLE) {
            toggleVisibility()
        }
        return START_STICKY
    }

    private fun createBubbleView() {
        val params = WindowManager.LayoutParams(
            width = WindowManager.LayoutParams.WRAP_CONTENT,
            height = WindowManager.LayoutParams.WRAP_CONTENT,
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            format = PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.START or Gravity.TOP
            x = 100
            y = 100
        }
        layoutParams = params

        bubbleView = FrameLayout(this).apply {
            layoutParams = params
            setOnTouchListener { v, event -> onBubbleTouch(event) }
        }

        composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                BubbleContent(
                    state = currentState,
                    onStateChange = { newState ->
                        currentState = newState
                    }
                )
            }
        }

        bubbleView?.addView(composeView!!)
        windowManager?.addView(bubbleView!!, params)
    }

    private fun onBubbleTouch(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = layoutParams?.x.toFloat() ?: 0f
                initialY = layoutParams?.y.toFloat() ?: 0f
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isMoving = false
                true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - initialTouchX
                val dy = event.rawY - initialTouchY
                
                if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                    isMoving = true
                    updatePosition(initialX + dx, initialY + dy)
                }
                true
            }
            MotionEvent.ACTION_UP -> {
                if (!isMoving) {
                    // Tap simple - cambiar estado
                    cycleState()
                }
                true
            }
            else -> false
        }
    }

    private fun updatePosition(x: Float, y: Float) {
        val display = windowManager?.defaultDisplay ?: return
        val size = Point()
        display.getRealSize(size)
        
        val maxX = size.x - (bubbleView?.width ?: 150)
        val maxY = size.y - (bubbleView?.height ?: 150) - 100
        
        layoutParams?.x = x.coerceIn(0f, maxX.toFloat()).toInt()
        layoutParams?.y = y.coerceIn(0f, maxY.toFloat()).toInt()
        
        layoutParams?.let { windowManager?.updateViewLayout(bubbleView!!, it) }
    }

    private fun toggleVisibility() {
        isVisible = !isVisible
        bubbleView?.visibility = if (isVisible) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun cycleState() {
        val states = BodyState.values()
        val currentIndex = states.indexOf(currentState)
        val nextIndex = (currentIndex + 1) % states.size
        currentState = states[nextIndex]
        composeView?.invalidate()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (bubbleView != null) {
            windowManager?.removeView(bubbleView)
            bubbleView = null
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

@Composable
fun BubbleContent(
    state: BodyState,
    onStateChange: (BodyState) -> Unit
) {
    var currentState by remember { mutableStateOf(state) }
    
    val generatedSkin = remember { SkinGenerator.generateClassicSkin() }
    
    Box(
        modifier = Modifier
            .size(128.dp)
            .padding(8.dp)
    ) {
        PixelCharacter(
            skin = generatedSkin.skin,
            bitmap = generatedSkin.bitmap,
            state = currentState,
            sizeDp = 112
        )
        
        // Indicador de estado pequeño
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(24.dp)
                .background(getStateColor(currentState), CircleShape)
        ) {
            Text(
                text = getStateInitial(currentState),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center)
            )
        }
    }
}

private fun getStateColor(state: BodyState): Color {
    return when (state) {
        BodyState.DORMIDO -> Color(0xFF6C5CE7)
        BodyState.ESCUCHANDO -> Color(0xFF00CEC9)
        BodyState.PENSANDO -> Color(0xFFFFD93D)
        BodyState.EJECUTANDO -> Color(0xFF00B5B0)
        BodyState.HABLANDO -> Color(0xFFFF6B6B)
        BodyState.FELIZ -> Color(0xFF00E676)
        BodyState.ERROR -> Color(0xFFFF1744)
        BodyState.CONFUNDIDO -> Color(0xFFAA00FF)
    }
}

private fun getStateInitial(state: BodyState): String {
    return when (state) {
        BodyState.DORMIDO -> "Z"
        BodyState.ESCUCHANDO -> "👂"
        BodyState.PENSANDO -> "💭"
        BodyState.EJECUTANDO -> "⚡"
        BodyState.HABLANDO -> "💬"
        BodyState.FELIZ -> "✨"
        BodyState.ERROR -> "✖"
        BodyState.CONFUNDIDO -> "?"
    }
}