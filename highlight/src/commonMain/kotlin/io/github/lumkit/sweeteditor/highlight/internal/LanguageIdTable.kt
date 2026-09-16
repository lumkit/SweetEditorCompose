package io.github.lumkit.sweeteditor.highlight.internal

internal object LanguageIdTable {
    val suffixToSyntaxFiles: Map<String, List<String>> = mapOf(
        ".kt" to listOf("kotlin.json"),
        ".kts" to listOf("kotlin.json"),
        ".java" to listOf("java.json"),
        ".js" to listOf("javascript.json"),
        ".ts" to listOf("typescript.json"),
        ".py" to listOf("python.json"),
        ".json" to listOf("json-sweetline.json"),
        ".xml" to listOf("xml.json"),
        ".html" to listOf("html.json"),
        ".htm" to listOf("html.json"),
        ".md" to listOf("markdown.json"),
        ".c" to listOf("c.json"),
        ".h" to listOf("cpp.json"),
        ".cpp" to listOf("cpp.json"),
        ".hpp" to listOf("cpp.json"),
        ".cc" to listOf("cpp.json"),
        ".rs" to listOf("rust.json"),
        ".go" to listOf("go.json"),
        ".toml" to listOf("toml.json"),
        ".yml" to listOf("yaml.json"),
        ".yaml" to listOf("yaml.json"),
        ".sh" to listOf("shell.json"),
        ".bash" to listOf("shell.json"),
        ".zsh" to listOf("shell.json"),
        ".properties" to listOf("properties.json"),
        ".gradle" to listOf("gradle.json"),
    )

    private val languageToSyntaxName: Map<String, String> = mapOf(
        "kotlin" to "kotlin",
        "kt" to "kotlin",
        "java" to "java",
        "js" to "javascript",
        "javascript" to "javascript",
        "ts" to "typescript",
        "typescript" to "typescript",
        "py" to "python",
        "python" to "python",
        "json" to "json",
        "xml" to "xml",
        "html" to "html",
        "md" to "markdown",
        "markdown" to "markdown",
        "c" to "c",
        "cpp" to "cpp",
        "c++" to "cpp",
        "h" to "cpp",
        "hpp" to "cpp",
        "rs" to "rust",
        "rust" to "rust",
        "go" to "go",
        "toml" to "toml",
        "yml" to "yaml",
        "yaml" to "yaml",
        "sh" to "shell",
        "bash" to "shell",
        "zsh" to "shell",
        "shell" to "shell",
        "properties" to "properties",
        "gradle" to "gradle",
    )

    fun resolve(languageId: String, overrides: Map<String, String> = emptyMap()): String? {
        val key = languageId.lowercase()
        val normalizedOverrides = overrides.mapKeys { it.key.lowercase() }
        return normalizedOverrides[key] ?: languageToSyntaxName[key]
    }

    fun syntaxFilesForFileName(fileName: String): List<String> {
        val lower = fileName.lowercase()
        val suffix = "." + lower.substringAfterLast('.', missingDelimiterValue = "")
        if (suffix == "." || !lower.contains('.')) return emptyList()
        return suffixToSyntaxFiles[suffix].orEmpty()
    }
}
