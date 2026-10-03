# Testing Guide — TinyHands

This guide outlines verification test cases to ensure safety, reliability, and smooth performance.

---

## Critical Test Cases

### 1. First-Launch Onboarding
- **Steps:**
  1. Launch app on fresh install.
  2. Verify Welcome screen displays app purpose.
  3. Create 4-digit PIN (e.g. `1234`), confirm it. Check that mismatched PIN displays an error and resets cleanly.
  4. Review the 3-finger 3-second hidden exit gesture explanation.
  5. Tap "Start Safe Play" to complete onboarding.
- **Expected:** Preference data is stored via DataStore and app proceeds to Parent Home.

### 2. Toddler Multi-Touch & Rapid Interaction
- **Scenario:** Child taps rapidly with palms, 5+ fingers, and drags across the screen.
- **Verification:**
  - Bubbles pop with particle bursts without UI freeze or crash.
  - Glowing Stars trail follows multiple finger drags smoothly.
  - Audio sounds are debounced without distortion or volume spikes.
  - Shapes in Musical Shapes play distinct pentatonic notes simultaneously.

### 3. Back Button Navigation Lock
- **Scenario:** System back gesture or hardware back button is pressed during Child Mode.
- **Verification:**
  - The app stays in Child Mode.
  - The child is never sent back to Parent Mode or the home screen via the in-app back button.

### 4. Hidden Parent Exit Gesture
- **Scenario:**
  - Place 1 or 2 fingers on screen: Nothing happens.
  - Place 3 fingers and release before 1 second: Nothing happens (accidental taps ignored).
  - Place 3 fingers and hold for 1 second: Subtle circular progress ring appears.
  - Move fingers violently: Progress resets (accidental toddler drag rejected).
  - Hold 3 fingers steadily for 3 seconds: Parent Check dialog opens.
- **Verification:**
  - Entering correct PIN exits Child Mode and returns to Parent Home.
  - Entering incorrect PIN shows error message and clears dots.
  - "Forgot PIN?" presents the adult math calculation challenge.

### 5. Session Timer & Calm Wind-down
- **Scenario:** Start a 5-minute session (or test with short timer).
- **Verification:**
  - No countdown timer is visible to the child.
  - In the final 60 seconds, activity motion slows down gently (calm wind-down).
  - At 0 seconds, screen transitions to "Play time is finished" with a sleeping moon.
  - Child interaction becomes non-distracting.
  - Parent can enter PIN and optionally tap "+ 5 Min" to extend play.

### 6. Pretend Camera Safety
- **Verification:**
  - Tap shutter: Soft flash, click sound, instant picture card appears.
  - Verify that NO camera permission is requested.
  - Verify that NO images are written to Android MediaStore or Gallery.

### 7. Sound Levels & Haptics
- **Settings:**
  - Test "Muted": Zero audio is produced.
  - Test "Low" / "Medium": Gentle sound plays with bounded volume.
  - Toggle "Soft Haptic Feedback" On/Off: Vibration operates or stays disabled as configured.

### 8. Running Automated Unit Tests
To run local JVM unit tests:
```bash
gradle :app:testDebugUnitTest
```
Tests in `TinyHandsCoreTest` verify:
- PIN hashing, salting, and verification.
- Adult math recovery challenge generator.
- Session duration conversion to seconds.
- Child age value mapping.
- Default user preferences and activity sets.
