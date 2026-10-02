package com.pixelbot.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelbot.service.PixelForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _permissions = MutableStateFlow<List<PermissionItem>>(PermissionItem.all(context))
    val permissions = _permissions.asStateFlow()

    private val _onboardingComplete = MutableStateFlow(false)
    val onboardingComplete = _onboardingComplete.asStateFlow()

    fun refreshPermissions() {
        _permissions.value = PermissionItem.all(context)
        checkAllGranted()
    }

    private fun checkAllGranted() {
        val allGranted = _permissions.value.all { it.checkGranted(context) }
        if (allGranted && !_onboardingComplete.value) {
            _onboardingComplete.value = true
            startForegroundService()
        }
    }

    private fun startForegroundService() {
        val intent = android.content.Intent(context, PixelForegroundService::class.java)
        context.startForegroundService(intent)
    }

    fun onPermissionClick(item: PermissionItem) {
        item.openSettings(context)
    }

    fun skipOnboarding() {
        _onboardingComplete.value = true
        startForegroundService()
    }
}