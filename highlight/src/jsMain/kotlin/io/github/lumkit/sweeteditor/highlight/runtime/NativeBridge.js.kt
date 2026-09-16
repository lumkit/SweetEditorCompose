package io.github.lumkit.sweeteditor.highlight.runtime

import io.github.lumkit.sweeteditor.highlight.HighlightException
import io.github.lumkit.sweeteditor.highlight.internal.jni.ensureWebAbiStarted

internal actual object NativeBridge {
    actual val isAvailable: Boolean
        get() {
            ensureWebAbiStarted()
            return abiReady()
        }

    actual fun createEngine(tabSize: Int): Long {
        if (!isAvailable) return 0L
        return (abi().createEngine(tabSize) as Number).toLong()
    }

    actual fun freeEngine(engine: Long) {
        if (!isAvailable) return
        abi().freeEngine(engine.toInt())
    }

    actual fun registerStyleName(engine: Long, name: String, styleId: Int) {
        if (!isAvailable) return
        abi().registerStyleName(engine.toInt(), name, styleId)
    }

    actual fun compileJson(engine: Long, json: String) {
        if (!isAvailable) return
        throwIfCompileFailed(abi().compileJson(engine.toInt(), json))
    }

    actual fun compileFile(engine: Long, path: String) {
        if (!isAvailable) return
        throwIfCompileFailed(abi().compileFile(engine.toInt(), path))
    }

    actual fun createDocument(uri: String, text: String): Long {
        if (!isAvailable) return 0L
        return (abi().createDocument(uri, text) as Number).toLong()
    }

    actual fun freeDocument(document: Long) {
        if (!isAvailable) return
        abi().freeDocument(document.toInt())
    }

    actual fun loadDocument(engine: Long, document: Long): Long {
        if (!isAvailable) return 0L
        return (abi().loadDocument(engine.toInt(), document.toInt()) as Number).toLong()
    }

    actual fun removeDocument(engine: Long, uri: String) {
        if (!isAvailable) return
        abi().removeDocument(engine.toInt(), uri)
    }

    actual fun freeDocumentAnalyzer(analyzer: Long) {
        if (!isAvailable) return
        abi().freeDocumentAnalyzer(analyzer.toInt())
    }

    actual fun analyzeLineRange(analyzer: Long, startLine: Int, lineCount: Int): IntArray? {
        if (!isAvailable) return null
        return toInts(abi().analyzeLineRange(analyzer.toInt(), startLine, lineCount))
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
        return toInts(
            abi().analyzeIncrementalInLineRange(
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
        return toInts(abi().getHighlightSlice(analyzer.toInt(), startLine, lineCount))
    }

    actual fun analyzeIndentGuidesInLineRange(analyzer: Long, startLine: Int, lineCount: Int): IntArray? {
        if (!isAvailable) return null
        return toInts(abi().analyzeIndentGuidesInLineRange(analyzer.toInt(), startLine, lineCount))
    }

    actual fun analyzeBracketPairsInLineRange(analyzer: Long, startLine: Int, lineCount: Int): IntArray? {
        if (!isAvailable) return null
        return toInts(abi().analyzeBracketPairsInLineRange(analyzer.toInt(), startLine, lineCount))
    }
}

private fun abi(): dynamic = js("globalThis.SweetLineWebAbi")

private fun abiReady(): Boolean =
    js("!!(globalThis.SweetLineWebAbi && globalThis.SweetLineWebAbi.ready)")

private fun throwIfCompileFailed(codeValue: dynamic) {
    val code = (codeValue as Number).toInt()
    if (code == 0) return
    val message = abi().lastCompileMessage
    throw HighlightException(code, if (message == null) "" else message.toString())
}

private fun toInts(value: dynamic): IntArray? {
    if (value == null) return null
    val length = value.length as Int
    return IntArray(length) { index -> (value[index] as Number).toInt() }
}
