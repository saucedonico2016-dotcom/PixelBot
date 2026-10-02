package com.pixelbot.onboarding

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.pixelbot.MainActivity
import com.pixelbot.ui.theme.PixelBotTheme
import kotlinx.coroutines.launch

class OnboardingActivity : ComponentActivity() {

    private val viewModel: OnboardingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PixelBotTheme {
                OnboardingScreen(onFinish = {
                    startActivity(android.content.Intent(this, MainActivity::class.java))
                    finish()
                })
            }
        }
        
        lifecycleScope.launch {
            viewModel.onboardingComplete
                .distinctUntilChanged()
                .filter { it }
                .collect {
                    startActivity(android.content.Intent(this@OnboardingActivity, MainActivity::class.java))
                    finish()
                }
        }
    }
    
    private fun enableEdgeToEdge() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            window.setDecorFitsSystemWindows(false)
        }
    }
}