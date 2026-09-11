package io.github.lumkit.sweeteditor

data class EditorCursorRect(
    val x: Float,
    val y: Float,
    val height: Float,
)

data class VisibleLineRange(
    val startLine: Int,
    val endLine: Int,
) {
    val isEmpty: Boolean get() = endLine < 0 || endLine < startLine
}

data class EditorScrollMetrics(
    val scale: Float,
    val scrollX: Float,
    val scrollY: Float,
    val maxScrollX: Float,
    val maxScrollY: Float,
    val contentWidth: Float,
    val contentHeight: Float,
    val viewportWidth: Float,
    val viewportHeight: Float,
    val textAreaX: Float,
    val textAreaWidth: Float,
    val canScrollX: Boolean,
    val canScrollY: Boolean,
)
