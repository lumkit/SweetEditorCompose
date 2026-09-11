package io.github.lumkit.sweeteditor

import androidx.compose.ui.graphics.ImageBitmap

interface EditorMetadata

fun interface EditorIconProvider {
    fun getIcon(iconId: Int): ImageBitmap?
}

data class BracketPair(
    val open: String,
    val close: String,
)

data class LanguageConfiguration(
    val languageId: String,
    val brackets: List<BracketPair>? = null,
    val autoClosingPairs: List<BracketPair>? = null,
    val tabSize: Int? = null,
    val insertSpaces: Boolean? = null,
) {
    class Builder(private val languageId: String) {
        private var brackets: MutableList<BracketPair>? = null
        private var autoClosingPairs: MutableList<BracketPair>? = null
        private var tabSize: Int? = null
        private var insertSpaces: Boolean? = null

        fun addBracket(open: String, close: String): Builder {
            val items = brackets ?: mutableListOf<BracketPair>().also { brackets = it }
            items += BracketPair(open, close)
            return this
        }

        fun addAutoClosingPair(open: String, close: String): Builder {
            val items = autoClosingPairs ?: mutableListOf<BracketPair>().also { autoClosingPairs = it }
            items += BracketPair(open, close)
            return this
        }

        fun setTabSize(tabSize: Int?): Builder {
            this.tabSize = tabSize
            return this
        }

        fun setInsertSpaces(insertSpaces: Boolean?): Builder {
            this.insertSpaces = insertSpaces
            return this
        }

        fun build(): LanguageConfiguration = LanguageConfiguration(
            languageId = languageId,
            brackets = brackets?.toList(),
            autoClosingPairs = autoClosingPairs?.toList(),
            tabSize = tabSize,
            insertSpaces = insertSpaces,
        )
    }

    companion object {
        const val DEFAULT_TAB_SIZE: Int = 4

        fun builder(languageId: String): Builder = Builder(languageId)
    }
}

internal fun List<BracketPair>.toCodePointArrays(): Pair<IntArray, IntArray> {
    val opens = IntArray(size) { index -> this[index].open.firstCodePoint() }
    val closes = IntArray(size) { index -> this[index].close.firstCodePoint() }
    return opens to closes
}

private fun String.firstCodePoint(): Int {
    if (isEmpty()) return 0
    val first = this[0]
    if (first.isHighSurrogate() && length > 1 && this[1].isLowSurrogate()) {
        return ((first.code - 0xD800) shl 10) + (this[1].code - 0xDC00) + 0x10000
    }
    return first.code
}
