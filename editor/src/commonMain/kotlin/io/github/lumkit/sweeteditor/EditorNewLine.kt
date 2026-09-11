package io.github.lumkit.sweeteditor

data class NewLineAction(
    val text: String,
)

data class NewLineContext(
    val lineNumber: Int,
    val column: Int,
    val lineText: String,
    val languageConfiguration: LanguageConfiguration? = null,
    val editorMetadata: EditorMetadata? = null,
)

fun interface NewLineActionProvider {
    fun provideNewLineAction(context: NewLineContext): NewLineAction?
}
