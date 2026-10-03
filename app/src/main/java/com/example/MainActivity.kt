package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.audio.TinyAudioPlayer
import com.example.data.PreferencesRepository
import com.example.security.NativeProtectionService
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.HapticFeedbackUtil

class MainActivity : ComponentActivity() {

    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var protectionService: NativeProtectionService
    private lateinit var audioPlayer: TinyAudioPlayer
    private lateinit var hapticUtil: HapticFeedbackUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        preferencesRepository = PreferencesRepository(applicationContext)
        protectionService = NativeProtectionService(applicationContext)
        audioPlayer = TinyAudioPlayer()
        hapticUtil = HapticFeedbackUtil(applicationContext)

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TinyHandsApp(
                        preferencesRepository = preferencesRepository,
                        protectionService = protectionService,
                        audioPlayer = audioPlayer,
                        hapticUtil = hapticUtil
                    )
                }
            }
        }
    }
}
