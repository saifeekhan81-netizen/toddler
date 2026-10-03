package com.example.ui.child

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.audio.TinyAudioPlayer
import com.example.domain.ActivityType
import com.example.domain.ChildAge
import com.example.domain.SessionDuration
import com.example.domain.SoundLevel
import com.example.security.NativeProtectionService
import com.example.ui.activities.BubblePopActivity
import com.example.ui.activities.GlowingStarsActivity
import com.example.ui.activities.LittleWorldActivity
import com.example.ui.activities.MusicalShapesActivity
import com.example.ui.activities.PlayCameraActivity
import com.example.utils.HapticFeedbackUtil
import kotlinx.coroutines.delay

@Composable
fun ChildPlayScreen(
    initialActivity: ActivityType,
    childAge: ChildAge,
    sessionDuration: SessionDuration,
    soundLevel: SoundLevel,
    hapticsEnabled: Boolean,
    autoSwitchEnabled: Boolean,
    autoSwitchIntervalMinutes: Int,
    enabledActivities: Set<String>,
    storedPinHash: String,
    storedPinSalt: String,
    audioPlayer: TinyAudioPlayer,
    hapticUtil: HapticFeedbackUtil,
    protectionService: NativeProtectionService,
    onExitToParentMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // Prevent back button from exiting Child Mode
    BackHandler(enabled = true) {
        // Intentionally intercept: Child cannot press back to leave!
    }

    // Fullscreen Immersive Mode Lifecycle
    DisposableEffect(Unit) {
        activity?.let {
            protectionService.enterChildMode(it, requestPinning = false)
        }
        onDispose {
            activity?.let {
                protectionService.restoreSystemUI(it)
            }
        }
    }

    // Set audio player level
    LaunchedEffect(soundLevel) {
        audioPlayer.setSoundLevel(soundLevel)
    }

    var currentActivity by remember { mutableStateOf(initialActivity) }
    var remainingSeconds by remember { mutableIntStateOf(sessionDuration.totalSeconds) }
    var isWindingDown by remember { mutableStateOf(false) }
    var isSessionFinished by remember { mutableStateOf(false) }
    var showParentUnlockDialog by remember { mutableStateOf(false) }

    // Session Timer Countdown
    LaunchedEffect(remainingSeconds, isSessionFinished) {
        if (remainingSeconds > 0 && !isSessionFinished) {
            while (remainingSeconds > 0 && !isSessionFinished) {
                delay(1000L)
                remainingSeconds -= 1
                if (remainingSeconds in 1..60) {
                    isWindingDown = true
                }
                if (remainingSeconds == 0) {
                    isSessionFinished = true
                    isWindingDown = false
                }
            }
        }
    }

    // Auto-Switch Activities Loop
    LaunchedEffect(autoSwitchEnabled, autoSwitchIntervalMinutes) {
        if (autoSwitchEnabled && enabledActivities.size > 1) {
            val intervalMs = autoSwitchIntervalMinutes * 60 * 1000L
            while (!isSessionFinished) {
                delay(intervalMs)
                val available = ActivityType.entries.filter { enabledActivities.contains(it.id) }
                if (available.isNotEmpty()) {
                    val next = available[(available.indexOf(currentActivity) + 1) % available.size]
                    currentActivity = next
                }
            }
        }
    }

    ParentExitGestureDetector(
        onTriggerUnlock = {
            showParentUnlockDialog = true
        },
        modifier = modifier.testTag("child_play_screen")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isSessionFinished) {
                SessionEndScreen(
                    onRequestUnlock = { showParentUnlockDialog = true }
                )
            } else {
                Crossfade(
                    targetState = currentActivity,
                    animationSpec = tween(500),
                    label = "ActivityTransition"
                ) { activityType ->
                    when (activityType) {
                        ActivityType.BUBBLE_POP -> {
                            BubblePopActivity(
                                audioPlayer = audioPlayer,
                                hapticUtil = hapticUtil,
                                childAge = childAge,
                                isWindingDown = isWindingDown
                            )
                        }
                        ActivityType.GLOWING_STARS -> {
                            GlowingStarsActivity(
                                audioPlayer = audioPlayer,
                                hapticUtil = hapticUtil,
                                childAge = childAge,
                                isWindingDown = isWindingDown
                            )
                        }
                        ActivityType.MUSICAL_SHAPES -> {
                            MusicalShapesActivity(
                                audioPlayer = audioPlayer,
                                hapticUtil = hapticUtil,
                                childAge = childAge,
                                isWindingDown = isWindingDown
                            )
                        }
                        ActivityType.LITTLE_WORLD -> {
                            LittleWorldActivity(
                                audioPlayer = audioPlayer,
                                hapticUtil = hapticUtil,
                                childAge = childAge,
                                isWindingDown = isWindingDown
                            )
                        }
                        ActivityType.PLAY_CAMERA -> {
                            PlayCameraActivity(
                                audioPlayer = audioPlayer,
                                hapticUtil = hapticUtil,
                                childAge = childAge,
                                isWindingDown = isWindingDown
                            )
                        }
                    }
                }
            }

            // Parent Check Dialog
            if (showParentUnlockDialog) {
                ParentCheckDialog(
                    storedPinHash = storedPinHash,
                    storedPinSalt = storedPinSalt,
                    onSuccess = {
                        showParentUnlockDialog = false
                        activity?.let { protectionService.exitChildMode(it) }
                        onExitToParentMode()
                    },
                    onDismiss = {
                        showParentUnlockDialog = false
                        // Re-enforce immersive mode
                        activity?.let { protectionService.enableImmersiveMode(it) }
                    },
                    onAddFiveMinutes = if (isSessionFinished) {
                        {
                            remainingSeconds += 300
                            isSessionFinished = false
                            isWindingDown = false
                            showParentUnlockDialog = false
                            activity?.let { protectionService.enableImmersiveMode(it) }
                        }
                    } else null
                )
            }
        }
    }
}
