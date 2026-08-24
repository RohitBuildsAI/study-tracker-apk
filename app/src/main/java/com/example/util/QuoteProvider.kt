package com.example.util

data class MotivationalQuote(
    val quote: String,
    val author: String,
    val category: String = "Focus"
)

object QuoteProvider {
    private val quotes = listOf(
        MotivationalQuote(
            quote = "Concentrate all your thoughts upon the work in hand. The sun's rays do not burn until brought to a focus.",
            author = "Alexander Graham Bell",
            category = "Focus"
        ),
        MotivationalQuote(
            quote = "It is during our darkest moments that we must focus to see the light.",
            author = "Aristotle",
            category = "Perseverance"
        ),
        MotivationalQuote(
            quote = "The secret of getting ahead is getting started.",
            author = "Mark Twain",
            category = "Action"
        ),
        MotivationalQuote(
            quote = "It's not that I'm so smart, it's just that I stay with problems longer.",
            author = "Albert Einstein",
            category = "Persistence"
        ),
        MotivationalQuote(
            quote = "We are what we repeatedly do. Excellence, then, is not an act, but a habit.",
            author = "Will Durant",
            category = "Habit"
        ),
        MotivationalQuote(
            quote = "You do not rise to the level of your goals. You fall to the level of your systems.",
            author = "James Clear",
            category = "Systems"
        ),
        MotivationalQuote(
            quote = "Deep work is the ability to focus without distraction on a cognitively demanding task.",
            author = "Cal Newport",
            category = "Deep Work"
        ),
        MotivationalQuote(
            quote = "Small disciplines repeated with consistency every day lead to great achievements gained slowly over time.",
            author = "John C. Maxwell",
            category = "Consistency"
        ),
        MotivationalQuote(
            quote = "Nothing in this world can take the place of persistence.",
            author = "Calvin Coolidge",
            category = "Persistence"
        ),
        MotivationalQuote(
            quote = "Focus on being productive instead of busy.",
            author = "Tim Ferriss",
            category = "Productivity"
        ),
        MotivationalQuote(
            quote = "One reason so few of us achieve what we truly want is that we never direct our focus.",
            author = "Tony Robbins",
            category = "Focus"
        ),
        MotivationalQuote(
            quote = "Energy flows where attention goes.",
            author = "Michael Beckwith",
            category = "Mindset"
        ),
        MotivationalQuote(
            quote = "Success is the sum of small efforts, repeated day in and day out.",
            author = "Robert Collier",
            category = "Consistency"
        ),
        MotivationalQuote(
            quote = "He who has a why to live can bear almost any how.",
            author = "Friedrich Nietzsche",
            category = "Purpose"
        ),
        MotivationalQuote(
            quote = "The mind is everything. What you think you become.",
            author = "Buddha",
            category = "Mindset"
        )
    )

    fun getRandomQuote(): MotivationalQuote {
        return quotes.random()
    }

    fun getAllQuotes(): List<MotivationalQuote> = quotes
}
