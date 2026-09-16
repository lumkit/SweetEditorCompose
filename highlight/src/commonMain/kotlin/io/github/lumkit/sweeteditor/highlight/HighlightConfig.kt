package io.github.lumkit.sweeteditor.highlight

data class HighlightDocumentDescriptor(
    val fileName: String? = null,
    val languageId: String? = null,
    val syntaxName: String? = null,
)

data class HighlightFeatureFlags(
    val syntaxHighlight: Boolean = true,
    val indentGuides: Boolean = true,
    val bracketGuides: Boolean = true,
    val matchedBrackets: Boolean = true,
    val rainbowBrackets: Boolean = true,
)

data class SweetLineHighlightConfig(
    val document: HighlightDocumentDescriptor = HighlightDocumentDescriptor(),
    val features: HighlightFeatureFlags = HighlightFeatureFlags(),
    val loadBuiltinSyntaxes: Boolean = true,
    val builtinSyntaxFilter: Set<String>? = null,
    val preloadSyntaxNames: Set<String> = emptySet(),
    val theme: HighlightTheme = HighlightTheme.dark(),
    val tabSize: Int? = null,
    val languageIdOverrides: Map<String, String> = emptyMap(),
)
