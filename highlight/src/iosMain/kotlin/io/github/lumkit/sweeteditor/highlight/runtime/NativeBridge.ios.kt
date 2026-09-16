package io.github.lumkit.sweeteditor.highlight.runtime

internal actual object NativeBridge {
    actual val isAvailable: Boolean = false

    actual fun createEngine(tabSize: Int): Long = 0
    actual fun freeEngine(engine: Long) = Unit
    actual fun registerStyleName(engine: Long, name: String, styleId: Int) = Unit
    actual fun compileJson(engine: Long, json: String) = Unit
    actual fun compileFile(engine: Long, path: String) = Unit
    actual fun createDocument(uri: String, text: String): Long = 0
    actual fun freeDocument(document: Long) = Unit
    actual fun loadDocument(engine: Long, document: Long): Long = 0
    actual fun removeDocument(engine: Long, uri: String) = Unit
    actual fun freeDocumentAnalyzer(analyzer: Long) = Unit
    actual fun analyzeLineRange(analyzer: Long, startLine: Int, lineCount: Int): IntArray? = null
    actual fun analyzeIncrementalInLineRange(
        analyzer: Long,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
        newText: String,
        visibleStartLine: Int,
        visibleLineCount: Int,
    ): IntArray? = null
    actual fun getHighlightSlice(analyzer: Long, startLine: Int, lineCount: Int): IntArray? = null
    actual fun analyzeIndentGuidesInLineRange(analyzer: Long, startLine: Int, lineCount: Int): IntArray? = null
    actual fun analyzeBracketPairsInLineRange(analyzer: Long, startLine: Int, lineCount: Int): IntArray? = null
}
