package com.example.util

object MotivationalQuotes {
    private val quotes = listOf(
        "Small daily improvements over time lead to stunning results." to "Robin Sharma",
        "The secret to getting ahead is getting started." to "Mark Twain",
        "It always seems impossible until it's done." to "Nelson Mandela",
        "Focus on progress, not perfection." to "Bill Phillips",
        "Success is the sum of small efforts repeated day in and day out." to "Robert Collier",
        "The expert in anything was once a beginner." to "Helen Hayes",
        "You don't have to be great to start, but you have to start to be great." to "Zig Ziglar",
        "Discipline is choosing between what you want now and what you want most." to "Abraham Lincoln",
        "Great work! Keep your streak alive and finish today's study goals." to "StudyTrack",
        "Knowledge is power. Information is liberating. Education is the premise of progress." to "Kofi Annan"
    )

    fun getDailyQuote(seed: Long = System.currentTimeMillis() / (1000 * 60 * 60 * 24)): Pair<String, String> {
        val index = (seed % quotes.size).toInt().let { if (it < 0) it + quotes.size else it }
        return quotes[index]
    }
}
