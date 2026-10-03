# TinyHands — Safe Play for Toddlers

> **“Parent controls the phone. Child only controls the fun.”**

TinyHands creates a protected, entertaining sensory playground for children aged 1–3 years. When parents hand their phone to a toddler, the child can freely touch, swipe, pop bubbles, touch glowing stars, make music, and explore without accidentally making phone calls, opening WhatsApp, altering settings, or cluttering the gallery.

---

## Key Features

### 1. Two Distinct Modes
- **Parent Mode:** Warm, clean, modern interface where parents select child age (1, 2, or 3 years), session duration (5 to 20 minutes or unlimited), sound levels, and enabled activities.
- **Child Mode:** Edge-to-edge immersive environment with zero text menus, zero ads, no back buttons, no links, and no failure states.

### 2. Five Toddler-Centric Activities
1. **Bubble Pop:** Floating colorful bubbles with realistic particle bursts and soft pop audio. Multi-touch supported with special sparkle bubbles.
2. **Glowing Stars:** Soft night sky where stars twinkle and grow when touched, leaving stardust trails that follow the child's fingers.
3. **Musical Shapes:** Large bouncing geometric shapes producing gentle pentatonic tones (C, D, E, G, A, C5) like a toddler harp.
4. **Little World:** Interactive countryside scene featuring a tap-to-drive car, tap-to-rain cloud, rotating sun, apple-dropping tree, chirping bird, and a physics-enabled bouncing ball.
5. **Play Camera (Pretend Camera):** A simulated toy camera with a soft flash, mechanical click, and slide-out instant sticker photo cards. **100% pretend: no camera hardware accessed, no photos saved, zero permissions.**

### 3. Hidden Parent Exit Mechanism
- **Three-Finger 3-Second Hold:** No visible exit button exists for the child. To exit, the parent places 3 fingers on the screen and holds them for 3 seconds. A subtle progress ring appears after 1 second to avoid accidental toddler triggers.
- **4-Digit Parent PIN:** Secure salted SHA-256 parent authentication.
- **Secondary Adult Challenge:** Math gate for parent recovery if the PIN is forgotten.

### 4. Session Timer with Gentle Wind-down
- No countdown visible to the toddler.
- At 60 seconds remaining, activities smoothly slow down for a calming transition.
- When time finishes, the screen dims into a serene starry night with a sleeping moon and displays "Play time is finished". The parent can unlock or add 5 minutes.

### 5. Sound & Haptics
- **Offline Procedural Synthesizer:** Real-time PCM audio synthesis using Android `AudioTrack` with zero external media files.
- Safe volume limits: Muted, Low, or Medium (never reaches harsh max volume).
- Gentle optional haptic feedback on touch.

---

## Architecture

- **UI:** 100% Jetpack Compose with Material 3 theming and procedural `Canvas` rendering.
- **State Management:** MVVM with Kotlin Coroutines and StateFlow.
- **Data Persistence:** Android Jetpack DataStore Preferences for settings; salted SHA-256 hashing for Parent PIN.
- **Protection Abstraction:** `NativeProtectionService` handles immersive UI, screen pinning, and lock task mode lifecycle.
- **Audio:** `TinyAudioPlayer` procedural synthesizer with automatic debouncing.

```
com.example/
├── MainActivity.kt
├── TinyHandsApp.kt
├── domain/
│   └── Models.kt (ChildAge, SoundLevel, SessionDuration, ActivityType)
├── data/
│   ├── PreferencesRepository.kt
│   └── UserPreferences.kt
├── security/
│   ├── NativeProtectionService.kt
│   └── ParentAuthManager.kt
├── audio/
│   └── TinyAudioPlayer.kt
├── utils/
│   └── HapticFeedbackUtil.kt
└── ui/
    ├── theme/ (Color.kt, Theme.kt, Type.kt)
    ├── onboarding/ (OnboardingScreen.kt)
    ├── parent/ (ParentHomeScreen.kt)
    ├── settings/ (ParentSettingsScreen.kt)
    ├── child/ (ChildPlayScreen.kt, ParentExitOverlay.kt, SessionEndScreen.kt)
    └── activities/
        ├── BubblePopActivity.kt
        ├── GlowingStarsActivity.kt
        ├── MusicalShapesActivity.kt
        ├── LittleWorldActivity.kt
        └── PlayCameraActivity.kt
```

---

## Android Protection Modes

| Mode | Protection Level | How to Enable |
|---|---|---|
| **Standard Safe Mode** | High (Consumer) | Active by default. Uses `WindowInsetsControllerCompat` to hide status & navigation bars (`BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`) and intercepts in-app back navigation. |
| **Android Screen Pinning** | Very High (Consumer) | Turn on in Android Settings (`Security > App Pinning`). When enabled, Android pins TinyHands to the front of the screen. Unpinning requires holding Back and Home/Overview. |
| **Dedicated Device (Kiosk)** | Maximum (Enterprise) | For dedicated child tablets provisioned with Device Owner (`dpm set-device-owner com.example/.DeviceAdminReceiver`). Allows full `startLockTask()` allow-listing. |

### How to Enable Screen Pinning on Android
1. Open device **Settings**.
2. Go to **Security** (or **Security & Privacy** > **More security settings**).
3. Tap **App Pinning** (or **Screen Pinning**) and turn it **ON**.
4. Check "Ask for PIN before unpinning".

---

## Privacy & Safety

- **No Internet Required:** Works completely offline.
- **No Permissions Needed:** Does not require Camera, Microphone, Contacts, Location, or Storage permissions.
- **Zero Data Collection:** No personal data, telemetry, or analytics are ever collected.
- **Emergency Safe:** Power button and emergency dialer remain operational at all times at the Android OS level.
