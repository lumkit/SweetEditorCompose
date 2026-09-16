package io.github.lumkit.sweeteditor.highlight

class HighlightException(
    val code: Int,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
