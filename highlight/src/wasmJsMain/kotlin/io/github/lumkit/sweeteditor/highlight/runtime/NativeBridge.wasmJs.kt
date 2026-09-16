package io.github.lumkit.sweeteditor.highlight.runtime

import io.github.lumkit.sweeteditor.highlight.HighlightException
import io.github.lumkit.sweeteditor.highlight.internal.jni.ensureWebAbiStarted
import kotlinx.coroutines.delay
import kotlin.js.JsAny

internal actual object NativeBridge {
    actual val isAvailable: Boolean
        get() {
            ensureWebAbiStarted()
            return abiReady()
        }

    actual fun createEngine(tabSize: Int): Long {
        if (!isAvailable) return 0L
        return createEngineJs(tabSize).toLong()
    }

    actual fun freeEngine(engine: Long) {
        if (!isAvailable) return
        freeEngineJs(engine.toInt())
    }

    actual fun registerStyleName(engine: Long, name: String, styleId: Int) {
        if (!isAvailable) return
        registerStyleNameJs(engine.toInt(), name, styleId)
    }

    actual fun compileJson(engine: Long, json: String) {
        if (!isAvailable) return
        throwIfCompileFailed(compileJsonJs(engine.toInt(), json))
    }

    actual fun compileFile(engine: Long, path: String) {
        if (!isAvailable) return
        throwIfCompileFailed(compileFileJs(engine.toInt(), path))
    }

    actual fun createDocument(uri: String, text: String): Long {
        if (!isAvailable) return 0L
        return createDocumentJs(uri, text).toLong()
    }

    actual fun freeDocument(document: Long) {
        if (!isAvailable) return
        freeDocumentJs(document.toInt())
    }

    actual fun loadDocument(engine: Long, document: Long): Long {
        if (!isAvailable) return 0L
        return loadDocumentJs(engine.toInt(), document.toInt()).toLong()
    }

    actual fun removeDocument(engine: Long, uri: String) {
        if (!isAvailable) return
        removeDocumentJs(engine.toInt(), uri)
    }

    actual fun freeDocumentAnalyzer(analyzer: Long) {
        if (!isAvailable) return
        freeDocumentAnalyzerJs(analyzer.toInt())
    }

    actual fun analyzeLineRange(analyzer: Long, startLine: Int, lineCount: Int): IntArray? {
        if (!isAvailable) return null
        return adoptJsInts(analyzeLineRangeJs(analyzer.toInt(), startLine, lineCount))
    }

    actual fun analyzeIncrementalInLineRange(
        analyzer: Long,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
        newText: String,
        visibleStartLine: Int,
        visibleLineCount: Int,
    ): IntArray? {
        if (!isAvailable) return null
        return adoptJsInts(
            analyzeIncrementalInLineRangeJs(
                analyzer.toInt(),
                startLine,
                startColumn,
                endLine,
                endColumn,
                newText,
                visibleStartLine,
                visibleLineCount,
            ),
        )
    }

    actual fun getHighlightSlice(analyzer: Long, startLine: Int, lineCount: Int): IntArray? {
        if (!isAvailable) return null
        return adoptJsInts(getHighlightSliceJs(analyzer.toInt(), startLine, lineCount))
    }

    actual fun analyzeIndentGuidesInLineRange(analyzer: Long, startLine: Int, lineCount: Int): IntArray? {
        if (!isAvailable) return null
        return adoptJsInts(analyzeIndentGuidesInLineRangeJs(analyzer.toInt(), startLine, lineCount))
    }

    actual fun analyzeBracketPairsInLineRange(analyzer: Long, startLine: Int, lineCount: Int): IntArray? {
        if (!isAvailable) return null
        return adoptJsInts(analyzeBracketPairsInLineRangeJs(analyzer.toInt(), startLine, lineCount))
    }
}

private fun throwIfCompileFailed(code: Int) {
    if (code == 0) return
    throw HighlightException(code, lastCompileMessageJs() ?: "")
}

private fun adoptJsInts(value: JsAny?): IntArray? {
    if (value == null) return null
    val length = typedLength(value)
    return IntArray(length) { index -> intGet(value, index) }
}

internal actual suspend fun awaitHighlightNativeReady() {
    ensureWebAbiStarted()
    repeat(200) {
        if (abiReady()) return
        if (abiFailed()) return
        delay(50)
    }
}

private fun abiReady(): Boolean =
    js("!!(globalThis.SweetLineWebAbi && globalThis.SweetLineWebAbi.ready)")

private fun abiFailed(): Boolean =
    js("!!(globalThis.SweetLineWebAbi && globalThis.SweetLineWebAbi.error)")

private fun lastCompileMessageJs(): String? =
    js("globalThis.SweetLineWebAbi && globalThis.SweetLineWebAbi.lastCompileMessage || null")

private fun typedLength(arr: JsAny): Int = js("arr.length")

private fun intGet(arr: JsAny, index: Int): Int = js("arr[index]")

private fun createEngineJs(tabSize: Int): Int =
    js("globalThis.SweetLineWebAbi.createEngine(tabSize)")

private fun freeEngineJs(engine: Int): Int =
    js("globalThis.SweetLineWebAbi.freeEngine(engine)")

private fun registerStyleNameJs(engine: Int, name: String, styleId: Int): Int =
    js("globalThis.SweetLineWebAbi.registerStyleName(engine, name, styleId)")

private fun compileJsonJs(engine: Int, json: String): Int =
    js("globalThis.SweetLineWebAbi.compileJson(engine, json)")

private fun compileFileJs(engine: Int, path: String): Int =
    js("globalThis.SweetLineWebAbi.compileFile(engine, path)")

private fun createDocumentJs(uri: String, text: String): Int =
    js("globalThis.SweetLineWebAbi.createDocument(uri, text)")

private fun freeDocumentJs(document: Int): Int =
    js("globalThis.SweetLineWebAbi.freeDocument(document)")

private fun loadDocumentJs(engine: Int, document: Int): Int =
    js("globalThis.SweetLineWebAbi.loadDocument(engine, document)")

private fun removeDocumentJs(engine: Int, uri: String): Int =
    js("globalThis.SweetLineWebAbi.removeDocument(engine, uri)")

private fun freeDocumentAnalyzerJs(analyzer: Int): Int =
    js("globalThis.SweetLineWebAbi.freeDocumentAnalyzer(analyzer)")

private fun analyzeLineRangeJs(analyzer: Int, startLine: Int, lineCount: Int): JsAny? =
    js("globalThis.SweetLineWebAbi.analyzeLineRange(analyzer, startLine, lineCount)")

private fun analyzeIncrementalInLineRangeJs(
    analyzer: Int,
    startLine: Int,
    startColumn: Int,
    endLine: Int,
    endColumn: Int,
    newText: String,
    visibleStartLine: Int,
    visibleLineCount: Int,
): JsAny? =
    js(
        "globalThis.SweetLineWebAbi.analyzeIncrementalInLineRange(analyzer, startLine, startColumn, endLine, endColumn, newText, visibleStartLine, visibleLineCount)",
    )

private fun getHighlightSliceJs(analyzer: Int, startLine: Int, lineCount: Int): JsAny? =
    js("globalThis.SweetLineWebAbi.getHighlightSlice(analyzer, startLine, lineCount)")

private fun analyzeIndentGuidesInLineRangeJs(analyzer: Int, startLine: Int, lineCount: Int): JsAny? =
    js("globalThis.SweetLineWebAbi.analyzeIndentGuidesInLineRange(analyzer, startLine, lineCount)")

private fun analyzeBracketPairsInLineRangeJs(analyzer: Int, startLine: Int, lineCount: Int): JsAny? =
    js("globalThis.SweetLineWebAbi.analyzeBracketPairsInLineRange(analyzer, startLine, lineCount)")
