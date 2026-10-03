package com.example.domain

enum class ChildAge(val ageValue: Int, val label: String, val description: String) {
    ONE(1, "1 year", "Gentle, extra large & slow targets"),
    TWO(2, "2 years", "Playful motion & responsive dragging"),
    THREE(3, "3 years", "Richer cause & effect interactions");

    companion object {
        fun fromValue(value: Int): ChildAge = entries.firstOrNull { it.ageValue == value } ?: TWO
    }
}

enum class SoundLevel(val volumeFraction: Float, val label: String) {
    MUTED(0f, "Muted"),
    LOW(0.35f, "Low"),
    MEDIUM(0.75f, "Medium");

    companion object {
        fun fromName(name: String): SoundLevel = entries.firstOrNull { it.name == name } ?: LOW
    }
}

enum class SessionDuration(val minutes: Int, val label: String) {
    FIVE(5, "5 min"),
    TEN(10, "10 min"),
    FIFTEEN(15, "15 min"),
    TWENTY(20, "20 min"),
    UNLIMITED(-1, "Unlimited");

    val totalSeconds: Int
        get() = if (minutes < 0) -1 else minutes * 60

    companion object {
        fun fromMinutes(minutes: Int): SessionDuration = entries.firstOrNull { it.minutes == minutes } ?: TEN
    }
}

enum class ActivityType(val id: String, val title: String, val subtitle: String, val iconDescription: String) {
    BUBBLE_POP("bubbles", "Bubble Pop", "Floating gentle colorful bubbles", "Soap bubbles floating"),
    GLOWING_STARS("stars", "Glowing Stars", "Twinkling night sky & stardust", "Night sky with twinkling stars"),
    MUSICAL_SHAPES("shapes", "Musical Shapes", "Bouncing shapes & soothing tones", "Geometric shapes that make music"),
    LITTLE_WORLD("little_world", "Little World", "Interactive sun, cloud, car & tree", "A cozy interactive cartoon world"),
    PLAY_CAMERA("camera", "Play Camera", "Pretend toy camera with sticker cards", "Pretend shutter with fun pictures");

    companion object {
        fun fromId(id: String): ActivityType = entries.firstOrNull { it.id == id } ?: BUBBLE_POP
    }
}

data class SessionState(
    val isActive: Boolean = false,
    val isWindingDown: Boolean = false,
    val isFinished: Boolean = false,
    val selectedActivity: ActivityType = ActivityType.BUBBLE_POP,
    val remainingSeconds: Int = -1,
    val childAge: ChildAge = ChildAge.TWO,
    val soundLevel: SoundLevel = SoundLevel.LOW,
    val hapticsEnabled: Boolean = true,
    val autoSwitchEnabled: Boolean = false,
    val autoSwitchIntervalMinutes: Int = 3
)
