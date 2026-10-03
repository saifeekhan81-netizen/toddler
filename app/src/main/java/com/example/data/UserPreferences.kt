package com.example.data

import com.example.domain.ActivityType
import com.example.domain.ChildAge
import com.example.domain.SessionDuration
import com.example.domain.SoundLevel

data class UserPreferences(
    val isOnboardingCompleted: Boolean = false,
    val pinHash: String = "",
    val pinSalt: String = "",
    val childAge: ChildAge = ChildAge.TWO,
    val sessionDuration: SessionDuration = SessionDuration.TEN,
    val soundLevel: SoundLevel = SoundLevel.LOW,
    val hapticsEnabled: Boolean = true,
    val autoSwitchEnabled: Boolean = false,
    val autoSwitchIntervalMinutes: Int = 3,
    val enabledActivities: Set<String> = ActivityType.entries.map { it.id }.toSet(),
    val screenPinningPromptEnabled: Boolean = true
) {
    val isPinConfigured: Boolean
        get() = pinHash.isNotBlank() && pinSalt.isNotBlank()
}
