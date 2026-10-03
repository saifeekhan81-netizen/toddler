package com.example.security

import android.app.Activity
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class NativeProtectionService(private val context: Context) {

    private val activityManager: ActivityManager? by lazy {
        context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    }

    private val devicePolicyManager: DevicePolicyManager? by lazy {
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
    }

    fun enableImmersiveMode(activity: Activity) {
        try {
            val window = activity.window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())

            // Prevent screen from turning off while toddler is playing
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling immersive mode", e)
        }
    }

    fun restoreSystemUI(activity: Activity) {
        try {
            val window = activity.window
            WindowCompat.setDecorFitsSystemWindows(window, true)
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
            insetsController.show(WindowInsetsCompat.Type.systemBars())
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring system UI", e)
        }
    }

    fun isLockTaskActive(): Boolean {
        return try {
            val state = activityManager?.lockTaskModeState ?: ActivityManager.LOCK_TASK_MODE_NONE
            state != ActivityManager.LOCK_TASK_MODE_NONE
        } catch (e: Exception) {
            false
        }
    }

    fun isLockTaskPermitted(activity: Activity): Boolean {
        return try {
            devicePolicyManager?.isLockTaskPermitted(activity.packageName) ?: false
        } catch (e: Exception) {
            false
        }
    }

    fun isDeviceOwner(): Boolean {
        return try {
            devicePolicyManager?.isDeviceOwnerApp(context.packageName) ?: false
        } catch (e: Exception) {
            false
        }
    }

    fun requestScreenPinning(activity: Activity): Boolean {
        return try {
            if (!isLockTaskActive()) {
                activity.startLockTask()
                true
            } else {
                true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not start lock task: ${e.message}")
            false
        }
    }

    fun stopLockTaskMode(activity: Activity) {
        try {
            if (isLockTaskActive()) {
                activity.stopLockTask()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping lock task: ${e.message}")
        }
    }

    fun enterChildMode(activity: Activity, requestPinning: Boolean = false) {
        enableImmersiveMode(activity)
        if (requestPinning) {
            requestScreenPinning(activity)
        }
    }

    fun exitChildMode(activity: Activity) {
        stopLockTaskMode(activity)
        restoreSystemUI(activity)
    }

    companion object {
        private const val TAG = "NativeProtectionService"
    }
}
