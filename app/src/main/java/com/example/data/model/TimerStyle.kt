package com.example.data.model

enum class TimerStyle(
    val title: String,
    val subtitle: String,
    val iconEmoji: String
) {
    FlipClock(
        title = "Flip Clock",
        subtitle = "Mechanical split-flap retro tiles",
        iconEmoji = "🎛️"
    ),
    RetroSplit(
        title = "Retro Split",
        subtitle = "80s segmented neon digital display",
        iconEmoji = "📟"
    ),
    VintageTick(
        title = "Vintage Tick",
        subtitle = "Classic ticking pocket watch dial",
        iconEmoji = "🕰️"
    ),
    ChronosAnalog(
        title = "Chronos Analog",
        subtitle = "Precision chronograph watch dial",
        iconEmoji = "⏱️"
    ),
    GhostOutline(
        title = "Ghost Outline",
        subtitle = "Futuristic neon wireframe glow",
        iconEmoji = "🌌"
    ),
    CleanDigital(
        title = "Clean Digital",
        subtitle = "Modern minimalist Material 3",
        iconEmoji = "⚡"
    );

    companion object {
        fun fromString(value: String): TimerStyle {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: CleanDigital
        }
    }
}
