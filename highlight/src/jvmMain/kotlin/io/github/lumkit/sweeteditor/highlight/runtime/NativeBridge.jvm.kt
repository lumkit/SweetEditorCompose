package io.github.lumkit.sweeteditor.highlight.runtime

import io.github.lumkit.sweeteditor.highlight.internal.NativeLibraryLoader
import io.github.lumkit.sweeteditor.highlight.jni.SweetLineJni

internal actual object NativeBridge {
    actual val isAvailable: Boolean = NativeLibraryLoader.loadIfAvailable()

    actual fun createEngine(tabSize: Int): Long =
        if (isAvailable) SweetLineJni.createEngine(tabSize) else 0L

    actual fun freeEngine(engine: Long) {
        if (isAvailable) SweetLineJni.freeEngine(engine)
    }

    actual fun registerStyleName(engine: Long, name: String, styleId: Int) {
        if (isAvailable) SweetLineJni.registerStyleName(engine, name, styleId)
    }

    actual fun compileJson(engine: Long, json: String) {
        if (isAvailable) SweetLineJni.compileJson(engine, json)
    }

    actual fun compileFile(engine: Long, path: String) {
        if (isAvailable) SweetLineJni.compileFile(engine, path)
    }

    actual fun createDocument(uri: String, text: String): Long =
        if (isAvailable) SweetLineJni.createDocument(uri, text) else 0L

    actual fun freeDocument(document: Long) {
        if (isAvailable) SweetLineJni.freeDocument(document)
    }

    actual fun loadDocument(engine: Long, document: Long): Long =
        if (isAvailable) SweetLineJni.loadDocument(engine, document) else 0L

    actual fun removeDocument(engine: Long, uri: String) {
        if (isAvailable) SweetLineJni.removeDocument(engine, uri)
    }

    actual fun freeDocumentAnalyzer(analyzer: Long) {
        if (isAvailable) SweetLineJni.freeDocumentAnalyzer(analyzer)
    }

    actual fun analyzeLineRange(analyzer: Long, startLine: Int, lineCount: Int): IntArray? =
        if (isAvailable) SweetLineJni.analyzeLineRange(analyzer, startLine, lineCount) else null

    actual fun analyzeIncrementalInLineRange(
        analyzer: Long,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
        newText: String,
        visibleStartLine: Int,
        visibleLineCount: Int,
    ): IntArray? =
        if (isAvailable) {
            SweetLineJni.analyzeIncrementalInLineRange(
                analyzer,
                startLine,
                startColumn,
                endLine,
                endColumn,
                newText,
                visibleStartLine,
                visibleLineCount,
            )
        } else {
            null
        }

    actual fun getHighlightSlice(analyzer: Long, startLine: Int, lineCount: Int): IntArray? =
        if (isAvailable) SweetLineJni.getHighlightSlice(analyzer, startLine, lineCount) else null

    actual fun analyzeIndentGuidesInLineRange(analyzer: Long, startLine: Int, lineCount: Int): IntArray? =
        if (isAvailable) SweetLineJni.analyzeIndentGuidesInLineRange(analyzer, startLine, lineCount) else null

    actual fun analyzeBracketPairsInLineRange(analyzer: Long, startLine: Int, lineCount: Int): IntArray? =
        if (isAvailable) SweetLineJni.analyzeBracketPairsInLineRange(analyzer, startLine, lineCount) else null
}

internal actual suspend fun awaitHighlightNativeReady() {}
