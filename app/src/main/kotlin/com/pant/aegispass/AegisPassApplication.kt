package com.pant.aegispass

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log

class AegisPassApplication : Application() {

    private var activityReferences = 0
    private var isActivityChangingConfigurations = false
    private val backgroundHandler = Handler(Looper.getMainLooper())

    private val PASSWORD_SCREEN_DELAY_MS = 5000L // 5 seconds

    private val passwordScreenRunnable = Runnable {
        if (activityReferences == 0) {
            PasswordSecurityManager.shouldShowPasswordScreen = true
            Log.d("AegisPassApp", "App in background for $PASSWORD_SCREEN_DELAY_MS ms. Flagging for password screen.")
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("AegisPassApp", "AegisPassApplication onCreate")
    }

    fun activityResumed() {
        activityReferences++
        backgroundHandler.removeCallbacks(passwordScreenRunnable)
        PasswordSecurityManager.shouldShowPasswordScreen = false
        Log.d("AegisPassApp", "Activity resumed. References: $activityReferences. Password screen runnable cancelled.")
    }

    fun activityPaused() {
        activityReferences--
        if (activityReferences == 0 && !isActivityChangingConfigurations) {
            Log.d("AegisPassApp", "App going to background. Scheduling password screen in ${PASSWORD_SCREEN_DELAY_MS}ms.")
            backgroundHandler.postDelayed(passwordScreenRunnable, PASSWORD_SCREEN_DELAY_MS)
        } else {
            Log.d("AegisPassApp", "Activity paused, but app is still in foreground (references: $activityReferences) or configuration changing.")
        }
    }

    fun activityConfigurationChanged(isChanging: Boolean) {
        isActivityChangingConfigurations = isChanging
        Log.d("AegisPassApp", "Activity configuration changed: $isChanging")
    }
}