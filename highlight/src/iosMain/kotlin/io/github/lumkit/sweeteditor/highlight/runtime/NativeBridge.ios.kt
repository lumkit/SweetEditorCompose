package io.github.lumkit.sweeteditor.highlight.runtime

import cnames.structs.sl_analyzer_handle
import cnames.structs.sl_document_handle
import cnames.structs.sl_engine_handle
import io.github.lumkit.sweeteditor.highlight.HighlightException
import io.github.lumkit.sweeteditor.highlight.internal.NativeLibraryLoader
import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.interpretCPointer
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.nativeNullPtr
import kotlinx.cinterop.set
import kotlinx.cinterop.toKString
import kotlinx.cinterop.useContents
import sweetline.cinterop.SL_OK
import sweetline.cinterop.sl_create_document
import sweetline.cinterop.sl_create_engine
import sweetline.cinterop.sl_document_analyze_bracket_pairs_in_line_range
import sweetline.cinterop.sl_document_analyze_incremental_in_line_range
import sweetline.cinterop.sl_document_analyze_indent_guides_in_line_range
import sweetline.cinterop.sl_document_analyze_line_range
import sweetline.cinterop.sl_document_get_highlight_slice
import sweetline.cinterop.sl_engine_compile_file
import sweetline.cinterop.sl_engine_compile_json
import sweetline.cinterop.sl_engine_load_document
import sweetline.cinterop.sl_engine_register_style_name
import sweetline.cinterop.sl_engine_remove_document
import sweetline.cinterop.sl_free_buffer
import sweetline.cinterop.sl_free_document
import sweetline.cinterop.sl_free_document_analyzer
import sweetline.cinterop.sl_free_engine
import sweetline.cinterop.sl_syntax_error

@OptIn(ExperimentalForeignApi::class)
internal actual object NativeBridge {
    actual val isAvailable: Boolean = NativeLibraryLoader.loadIfAvailable()

    actual fun createEngine(tabSize: Int): Long =
        sl_create_engine(false, false, tabSize).toHandle()

    actual fun freeEngine(engine: Long) {
        sl_free_engine(engine.toPtr())
    }

    actual fun registerStyleName(engine: Long, name: String, styleId: Int) {
        sl_engine_register_style_name(engine.toPtr<sl_engine_handle>(), name, styleId)
    }

    actual fun compileJson(engine: Long, json: String) {
        throwIfFailed(sl_engine_compile_json(engine.toPtr<sl_engine_handle>(), json))
    }

    actual fun compileFile(engine: Long, path: String) {
        throwIfFailed(sl_engine_compile_file(engine.toPtr<sl_engine_handle>(), path))
    }

    actual fun createDocument(uri: String, text: String): Long =
        sl_create_document(uri, text).toHandle()

    actual fun freeDocument(document: Long) {
        sl_free_document(document.toPtr())
    }

    actual fun loadDocument(engine: Long, document: Long): Long =
        sl_engine_load_document(
            engine.toPtr<sl_engine_handle>(),
            document.toPtr<sl_document_handle>(),
        ).toHandle()

    actual fun removeDocument(engine: Long, uri: String) {
        sl_engine_remove_document(engine.toPtr<sl_engine_handle>(), uri)
    }

    actual fun freeDocumentAnalyzer(analyzer: Long) {
        sl_free_document_analyzer(analyzer.toPtr<sl_analyzer_handle>())
    }

    actual fun analyzeLineRange(analyzer: Long, startLine: Int, lineCount: Int): IntArray? = memScoped {
        val range = allocArray<IntVar>(2)
        range[0] = startLine
        range[1] = lineCount
        copyHighlightSlice(sl_document_analyze_line_range(analyzer.toPtr<sl_analyzer_handle>(), range))
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
    ): IntArray? = memScoped {
        val changes = allocArray<IntVar>(4)
        changes[0] = startLine
        changes[1] = startColumn
        changes[2] = endLine
        changes[3] = endColumn
        val visible = allocArray<IntVar>(2)
        visible[0] = visibleStartLine
        visible[1] = visibleLineCount
        copyHighlightSlice(
            sl_document_analyze_incremental_in_line_range(
                analyzer.toPtr<sl_analyzer_handle>(),
                changes,
                newText,
                visible,
            ),
        )
    }

    actual fun getHighlightSlice(analyzer: Long, startLine: Int, lineCount: Int): IntArray? = memScoped {
        val range = allocArray<IntVar>(2)
        range[0] = startLine
        range[1] = lineCount
        copyHighlightSlice(sl_document_get_highlight_slice(analyzer.toPtr<sl_analyzer_handle>(), range))
    }

    actual fun analyzeIndentGuidesInLineRange(analyzer: Long, startLine: Int, lineCount: Int): IntArray? = memScoped {
        val range = allocArray<IntVar>(2)
        range[0] = startLine
        range[1] = lineCount
        copyIndentBuffer(
            sl_document_analyze_indent_guides_in_line_range(analyzer.toPtr<sl_analyzer_handle>(), range),
        )
    }

    actual fun analyzeBracketPairsInLineRange(analyzer: Long, startLine: Int, lineCount: Int): IntArray? = memScoped {
        val range = allocArray<IntVar>(2)
        range[0] = startLine
        range[1] = lineCount
        copyHighlightSlice(
            sl_document_analyze_bracket_pairs_in_line_range(analyzer.toPtr<sl_analyzer_handle>(), range),
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun CPointer<*>?.toHandle(): Long = this?.rawValue?.toLong() ?: 0L

@OptIn(ExperimentalForeignApi::class)
private fun <T : CPointed> Long.toPtr(): CPointer<T>? {
    if (this == 0L) return null
    return interpretCPointer(nativeNullPtr + this)
}

@OptIn(ExperimentalForeignApi::class)
private fun throwIfFailed(error: CValue<sl_syntax_error>) {
    error.useContents {
        if (err_code != SL_OK) {
            throw HighlightException(err_code, err_msg?.toKString().orEmpty())
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun copyHighlightSlice(buffer: CPointer<IntVar>?): IntArray? {
    if (buffer == null) return null
    return copyAndFree(buffer, highlightSliceLength(buffer))
}

@OptIn(ExperimentalForeignApi::class)
private fun copyIndentBuffer(buffer: CPointer<IntVar>?): IntArray? {
    if (buffer == null) return null
    return copyAndFree(buffer, indentBufferLength(buffer))
}

@OptIn(ExperimentalForeignApi::class)
private fun copyAndFree(buffer: CPointer<IntVar>, length: Int): IntArray {
    val result = if (length <= 0) intArrayOf() else IntArray(length) { buffer[it] }
    sl_free_buffer(buffer)
    return result
}

@OptIn(ExperimentalForeignApi::class)
private fun highlightSliceLength(buffer: CPointer<IntVar>): Int {
    val stride = buffer[1]
    val lineCount = buffer[4]
    if (stride <= 0 || lineCount < 0) return 0
    var index = 5
    repeat(lineCount) {
        val spanCount = buffer[index++]
        if (spanCount < 0) return 0
        index += spanCount * stride
    }
    return index
}

@OptIn(ExperimentalForeignApi::class)
private fun indentBufferLength(buffer: CPointer<IntVar>): Int {
    val lineStateCount = buffer[1]
    val guideCount = buffer[2]
    if (lineStateCount < 0 || guideCount < 0) return 0
    var index = 3
    repeat(guideCount) {
        val branchCount = buffer[index + 4]
        if (branchCount < 0) return 0
        index += 5 + branchCount * 2
    }
    return index + lineStateCount * 4
}
