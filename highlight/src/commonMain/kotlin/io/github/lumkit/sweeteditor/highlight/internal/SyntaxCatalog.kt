package io.github.lumkit.sweeteditor.highlight.internal

import io.github.lumkit.sweeteditor.highlight.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

internal class SyntaxCatalog(
    private val loadBuiltins: Boolean = true,
    private val builtinFilter: Set<String>? = null,
) {
    private val extras = LinkedHashMap<String, String>()
    private val compiled = mutableSetOf<String>()

    fun registerJson(json: String) {
        val name = syntaxNameOfJson(json) ?: return
        extras[name] = json
    }

    fun markCompiled(name: String) {
        compiled += name
    }

    fun isCompiled(name: String): Boolean = name in compiled

    fun registeredJson(name: String): String? = extras[name]

    suspend fun loadJson(name: String): String? {
        extras[name]?.let { return it }
        if (!loadBuiltins) return null
        if (builtinFilter != null && name !in builtinFilter) return null
        val fileName = FILE_BY_SYNTAX_NAME[name] ?: return null
        return readSyntaxFile(fileName)
    }

    companion object {
        val FILE_BY_SYNTAX_NAME: Map<String, String> = mapOf(
            "kotlin" to "kotlin.json",
            "java" to "java.json",
            "javascript" to "javascript.json",
            "typescript" to "typescript.json",
            "python" to "python.json",
            "json" to "json-sweetline.json",
            "xml" to "xml.json",
            "html" to "html.json",
            "markdown" to "markdown.json",
            "c" to "c.json",
            "cpp" to "cpp.json",
            "rust" to "rust.json",
            "go" to "go.json",
            "toml" to "toml.json",
            "yaml" to "yaml.json",
            "shell" to "shell.json",
            "properties" to "properties.json",
            "gradle" to "gradle.json",
        )
    }
}

internal fun syntaxNameOfJson(json: String): String? =
    Regex("\"name\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1)

@OptIn(ExperimentalResourceApi::class)
private suspend fun readSyntaxFile(fileName: String): String {
    val bytes = Res.readBytes("files/syntaxes/$fileName")
    return bytes.decodeToString()
}
