package io.github.lumkit.sweeteditor.highlight.internal

import io.github.lumkit.sweeteditor.highlight.runtime.NativeBridge

internal interface HighlightNativeOps {
    val isAvailable: Boolean

    fun createEngine(tabSize: Int): Long
    fun freeEngine(engine: Long)
    fun createDocument(uri: String, text: String): Long
    fun freeDocument(document: Long)
    fun loadDocument(engine: Long, document: Long): Long
    fun removeDocument(engine: Long, uri: String)
    fun freeDocumentAnalyzer(analyzer: Long)
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
}

internal object DefaultHighlightNative : HighlightNativeOps {
    override val isAvailable: Boolean
        get() = NativeBridge.isAvailable

    override fun createEngine(tabSize: Int): Long = NativeBridge.createEngine(tabSize)

    override fun freeEngine(engine: Long) = NativeBridge.freeEngine(engine)

    override fun createDocument(uri: String, text: String): Long =
        NativeBridge.createDocument(uri, text)

    override fun freeDocument(document: Long) = NativeBridge.freeDocument(document)

    override fun loadDocument(engine: Long, document: Long): Long =
        NativeBridge.loadDocument(engine, document)

    override fun removeDocument(engine: Long, uri: String) =
        NativeBridge.removeDocument(engine, uri)

    override fun freeDocumentAnalyzer(analyzer: Long) =
        NativeBridge.freeDocumentAnalyzer(analyzer)

    override fun analyzeIncrementalInLineRange(
        analyzer: Long,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
        newText: String,
        visibleStartLine: Int,
        visibleLineCount: Int,
    ): IntArray? = NativeBridge.analyzeIncrementalInLineRange(
        analyzer,
        startLine,
        startColumn,
        endLine,
        endColumn,
        newText,
        visibleStartLine,
        visibleLineCount,
    )
}
