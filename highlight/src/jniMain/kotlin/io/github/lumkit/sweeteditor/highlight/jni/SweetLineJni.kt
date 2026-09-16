package io.github.lumkit.sweeteditor.highlight.jni

import io.github.lumkit.sweeteditor.highlight.internal.NativeLibraryLoader

internal object SweetLineJni {
    init {
        NativeLibraryLoader.loadComposeJni()
    }

    @JvmStatic external fun createEngine(tabSize: Int): Long
    @JvmStatic external fun freeEngine(engine: Long)
    @JvmStatic external fun registerStyleName(engine: Long, name: String, styleId: Int)
    @JvmStatic external fun compileJson(engine: Long, json: String)
    @JvmStatic external fun compileFile(engine: Long, path: String)
    @JvmStatic external fun createDocument(uri: String, text: String): Long
    @JvmStatic external fun freeDocument(document: Long)
    @JvmStatic external fun loadDocument(engine: Long, document: Long): Long
    @JvmStatic external fun removeDocument(engine: Long, uri: String)
    @JvmStatic external fun freeDocumentAnalyzer(analyzer: Long)
    @JvmStatic external fun analyzeLineRange(analyzer: Long, startLine: Int, lineCount: Int): IntArray?
    @JvmStatic external fun analyzeIncrementalInLineRange(
        analyzer: Long,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
        newText: String,
        visibleStartLine: Int,
        visibleLineCount: Int,
    ): IntArray?
    @JvmStatic external fun getHighlightSlice(analyzer: Long, startLine: Int, lineCount: Int): IntArray?
    @JvmStatic external fun analyzeIndentGuidesInLineRange(
        analyzer: Long,
        startLine: Int,
        lineCount: Int,
    ): IntArray?
    @JvmStatic external fun analyzeBracketPairsInLineRange(
        analyzer: Long,
        startLine: Int,
        lineCount: Int,
    ): IntArray?
}
