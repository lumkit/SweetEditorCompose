package io.github.lumkit.sweeteditor

data class InlineSuggestion(
    val line: Int,
    val column: Int,
    val text: String,
)

interface InlineSuggestionListener {
    fun onSuggestionAccepted(suggestion: InlineSuggestion)
    fun onSuggestionDismissed(suggestion: InlineSuggestion)
}
