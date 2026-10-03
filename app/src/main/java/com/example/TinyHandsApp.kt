package com.example

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.audio.TinyAudioPlayer
import com.example.data.PreferencesRepository
import com.example.domain.ActivityType
import com.example.security.NativeProtectionService
import com.example.ui.child.ChildPlayScreen
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.parent.ParentHomeScreen
import com.example.ui.settings.ParentSettingsScreen
import com.example.utils.HapticFeedbackUtil
import kotlinx.coroutines.launch

private enum class AppScreen {
    ONBOARDING,
    PARENT_HOME,
    PARENT_SETTINGS,
    CHILD_PLAY
}

@Composable
fun TinyHandsApp(
    preferencesRepository: PreferencesRepository,
    protectionService: NativeProtectionService,
    audioPlayer: TinyAudioPlayer,
    hapticUtil: HapticFeedbackUtil,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val preferencesState by preferencesRepository.preferencesFlow.collectAsState(initial = null)

    val prefs = preferencesState
    if (prefs == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    var currentScreen by remember(prefs.isOnboardingCompleted, prefs.isPinConfigured) {
        mutableStateOf(
            if (!prefs.isOnboardingCompleted || !prefs.isPinConfigured) {
                AppScreen.ONBOARDING
            } else {
                AppScreen.PARENT_HOME
            }
        )
    }

    var playActivity by remember { mutableStateOf(ActivityType.BUBBLE_POP) }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "AppScreenTransition",
        modifier = modifier.fillMaxSize()
    ) { screen ->
        when (screen) {
            AppScreen.ONBOARDING -> {
                OnboardingScreen(
                    onComplete = { pin ->
                        scope.launch {
                            preferencesRepository.savePin(pin)
                            preferencesRepository.completeOnboarding()
                            currentScreen = AppScreen.PARENT_HOME
                        }
                    }
                )
            }

            AppScreen.PARENT_HOME -> {
                ParentHomeScreen(
                    userPreferences = prefs,
                    preferencesRepository = preferencesRepository,
                    onStartChildMode = { activity ->
                        playActivity = activity
                        currentScreen = AppScreen.CHILD_PLAY
                    },
                    onOpenSettings = {
                        currentScreen = AppScreen.PARENT_SETTINGS
                    }
                )
            }

            AppScreen.PARENT_SETTINGS -> {
                BackHandler {
                    currentScreen = AppScreen.PARENT_HOME
                }
                ParentSettingsScreen(
                    userPreferences = prefs,
                    preferencesRepository = preferencesRepository,
                    protectionService = protectionService,
                    onBack = { currentScreen = AppScreen.PARENT_HOME }
                )
            }

            AppScreen.CHILD_PLAY -> {
                ChildPlayScreen(
                    initialActivity = playActivity,
                    childAge = prefs.childAge,
                    sessionDuration = prefs.sessionDuration,
                    soundLevel = prefs.soundLevel,
                    hapticsEnabled = prefs.hapticsEnabled,
                    autoSwitchEnabled = prefs.autoSwitchEnabled,
                    autoSwitchIntervalMinutes = prefs.autoSwitchIntervalMinutes,
                    enabledActivities = prefs.enabledActivities,
                    storedPinHash = prefs.pinHash,
                    storedPinSalt = prefs.pinSalt,
                    audioPlayer = audioPlayer,
                    hapticUtil = hapticUtil,
                    protectionService = protectionService,
                    onExitToParentMode = {
                        currentScreen = AppScreen.PARENT_HOME
                    }
                )
            }
        }
    }
}
