package com.example

import com.example.domain.ActivityType
import com.example.domain.ChildAge
import com.example.domain.SessionDuration
import com.example.domain.SoundLevel
import com.example.data.UserPreferences
import com.example.security.ParentAuthManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TinyHandsCoreTest {

    @Test
    fun testPinHashingAndVerification() {
        val pin = "4826"
        val salt = ParentAuthManager.generateSalt()
        val hash = ParentAuthManager.hashPin(pin, salt)

        // Valid PIN must pass
        assertTrue(ParentAuthManager.verifyPin(pin, hash, salt))

        // Invalid PIN must fail
        assertFalse(ParentAuthManager.verifyPin("0000", hash, salt))
        assertFalse(ParentAuthManager.verifyPin("4825", hash, salt))
        assertFalse(ParentAuthManager.verifyPin("", hash, salt))
        assertFalse(ParentAuthManager.verifyPin("123", hash, salt))
    }

    @Test
    fun testSaltUniqueness() {
        val salt1 = ParentAuthManager.generateSalt()
        val salt2 = ParentAuthManager.generateSalt()
        assertNotEquals(salt1, salt2)
    }

    @Test
    fun testRecoveryChallengeAnswerCorrectness() {
        for (i in 0 until 10) {
            val challenge = ParentAuthManager.createRecoveryChallenge()
            assertTrue(challenge.options.contains(challenge.correctAnswer))
            assertEquals(4, challenge.options.size)
        }
    }

    @Test
    fun testSessionDurations() {
        assertEquals(300, SessionDuration.FIVE.totalSeconds)
        assertEquals(600, SessionDuration.TEN.totalSeconds)
        assertEquals(900, SessionDuration.FIFTEEN.totalSeconds)
        assertEquals(1200, SessionDuration.TWENTY.totalSeconds)
        assertEquals(-1, SessionDuration.UNLIMITED.totalSeconds)

        assertEquals(SessionDuration.TEN, SessionDuration.fromMinutes(10))
        assertEquals(SessionDuration.UNLIMITED, SessionDuration.fromMinutes(-1))
    }

    @Test
    fun testChildAgeMapping() {
        assertEquals(ChildAge.ONE, ChildAge.fromValue(1))
        assertEquals(ChildAge.TWO, ChildAge.fromValue(2))
        assertEquals(ChildAge.THREE, ChildAge.fromValue(3))
        assertEquals(ChildAge.TWO, ChildAge.fromValue(99)) // default fallback
    }

    @Test
    fun testUserPreferencesDefaults() {
        val defaultPrefs = UserPreferences()
        assertFalse(defaultPrefs.isOnboardingCompleted)
        assertFalse(defaultPrefs.isPinConfigured)
        assertEquals(ChildAge.TWO, defaultPrefs.childAge)
        assertEquals(SessionDuration.TEN, defaultPrefs.sessionDuration)
        assertEquals(SoundLevel.LOW, defaultPrefs.soundLevel)
        assertTrue(defaultPrefs.hapticsEnabled)
        assertEquals(5, defaultPrefs.enabledActivities.size)
        assertTrue(defaultPrefs.enabledActivities.contains(ActivityType.BUBBLE_POP.id))
        assertTrue(defaultPrefs.enabledActivities.contains(ActivityType.PLAY_CAMERA.id))

        val configuredPrefs = defaultPrefs.copy(pinHash = "dummyHash", pinSalt = "dummySalt")
        assertTrue(configuredPrefs.isPinConfigured)
    }
}
