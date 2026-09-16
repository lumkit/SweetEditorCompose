package io.github.lumkit.sweeteditor.highlight.runtime

internal expect object NativeBridge {
    val isAvailable: Boolean

    fun createEngine(tabSize: Int): Long
    fun freeEngine(engine: Long)
    fun registerStyleName(engine: Long, name: String, styleId: Int)
    fun compileJson(engine: Long, json: String)
    fun compileFile(engine: Long, path: String)
    fun createDocument(uri: String, text: String): Long
    fun freeDocument(document: Long)
    fun loadDocument(engine: Long, document: Long): Long
    fun removeDocument(engine: Long, uri: String)
    fun freeDocumentAnalyzer(analyzer: Long)

    fun analyzeLineRange(analyzer: Long, startLine: Int, lineCount: Int): IntArray?
    fun analyzeIncrementalInLineRange(
        analyzer: Long,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
        newText: String,
        visibleStartLine: Int,
        visibleLineCount: Int,
    ): IntArray?
    fun getHighlightSlice(analyzer: Long, startLine: Int, lineCount: Int): IntArray?

    fun analyzeIndentGuidesInLineRange(analyzer: Long, startLine: Int, lineCount: Int): IntArray?
    fun analyzeBracketPairsInLineRange(analyzer: Long, startLine: Int, lineCount: Int): IntArray?
}

internal expect suspend fun awaitHighlightNativeReady()
