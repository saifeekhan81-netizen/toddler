package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.ActivityType
import com.example.domain.ChildAge
import com.example.domain.SessionDuration
import com.example.domain.SoundLevel
import com.example.security.ParentAuthManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tiny_hands_prefs")

class PreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val CHILD_AGE = intPreferencesKey("child_age")
        val SESSION_DURATION_MINUTES = intPreferencesKey("session_duration_minutes")
        val SOUND_LEVEL = stringPreferencesKey("sound_level")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val AUTO_SWITCH_ENABLED = booleanPreferencesKey("auto_switch_enabled")
        val AUTO_SWITCH_INTERVAL = intPreferencesKey("auto_switch_interval")
        val ENABLED_ACTIVITIES = stringSetPreferencesKey("enabled_activities")
        val SCREEN_PINNING_PROMPT = booleanPreferencesKey("screen_pinning_prompt")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val allActivityIds = ActivityType.entries.map { it.id }.toSet()
        UserPreferences(
            isOnboardingCompleted = prefs[PreferencesKeys.ONBOARDING_COMPLETED] ?: false,
            pinHash = prefs[PreferencesKeys.PIN_HASH] ?: "",
            pinSalt = prefs[PreferencesKeys.PIN_SALT] ?: "",
            childAge = ChildAge.fromValue(prefs[PreferencesKeys.CHILD_AGE] ?: 2),
            sessionDuration = SessionDuration.fromMinutes(prefs[PreferencesKeys.SESSION_DURATION_MINUTES] ?: 10),
            soundLevel = SoundLevel.fromName(prefs[PreferencesKeys.SOUND_LEVEL] ?: SoundLevel.LOW.name),
            hapticsEnabled = prefs[PreferencesKeys.HAPTICS_ENABLED] ?: true,
            autoSwitchEnabled = prefs[PreferencesKeys.AUTO_SWITCH_ENABLED] ?: false,
            autoSwitchIntervalMinutes = prefs[PreferencesKeys.AUTO_SWITCH_INTERVAL] ?: 3,
            enabledActivities = prefs[PreferencesKeys.ENABLED_ACTIVITIES] ?: allActivityIds,
            screenPinningPromptEnabled = prefs[PreferencesKeys.SCREEN_PINNING_PROMPT] ?: true
        )
    }

    suspend fun savePin(pin: String) {
        val salt = ParentAuthManager.generateSalt()
        val hash = ParentAuthManager.hashPin(pin, salt)
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.PIN_HASH] = hash
            prefs[PreferencesKeys.PIN_SALT] = salt
        }
    }

    suspend fun completeOnboarding() {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.ONBOARDING_COMPLETED] = true
        }
    }

    suspend fun updateChildAge(age: ChildAge) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.CHILD_AGE] = age.ageValue
        }
    }

    suspend fun updateSessionDuration(duration: SessionDuration) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SESSION_DURATION_MINUTES] = duration.minutes
        }
    }

    suspend fun updateSoundLevel(level: SoundLevel) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SOUND_LEVEL] = level.name
        }
    }

    suspend fun updateHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.HAPTICS_ENABLED] = enabled
        }
    }

    suspend fun updateAutoSwitchEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.AUTO_SWITCH_ENABLED] = enabled
        }
    }

    suspend fun updateAutoSwitchInterval(minutes: Int) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.AUTO_SWITCH_INTERVAL] = minutes
        }
    }

    suspend fun toggleActivityEnabled(activityId: String, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[PreferencesKeys.ENABLED_ACTIVITIES] ?: ActivityType.entries.map { it.id }.toSet()
            val updated = current.toMutableSet()
            if (enabled) {
                updated.add(activityId)
            } else {
                // Keep at least one activity enabled
                if (updated.size > 1) {
                    updated.remove(activityId)
                }
            }
            prefs[PreferencesKeys.ENABLED_ACTIVITIES] = updated
        }
    }

    suspend fun updateScreenPinningPrompt(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SCREEN_PINNING_PROMPT] = enabled
        }
    }

    suspend fun resetPreferences() {
        context.dataStore.edit { prefs ->
            val pinHash = prefs[PreferencesKeys.PIN_HASH]
            val pinSalt = prefs[PreferencesKeys.PIN_SALT]
            prefs.clear()
            // Retain PIN and onboarding status so user is not locked out
            if (pinHash != null && pinSalt != null) {
                prefs[PreferencesKeys.PIN_HASH] = pinHash
                prefs[PreferencesKeys.PIN_SALT] = pinSalt
                prefs[PreferencesKeys.ONBOARDING_COMPLETED] = true
            }
        }
    }
}
