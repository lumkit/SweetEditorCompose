package io.github.lumkit.sweeteditor.highlight

import io.github.lumkit.sweeteditor.SweetEditorController
import io.github.lumkit.sweeteditor.highlight.internal.DefaultHighlightNative
import io.github.lumkit.sweeteditor.highlight.internal.HighlightDecorationProvider
import io.github.lumkit.sweeteditor.highlight.internal.HighlightSession
import io.github.lumkit.sweeteditor.highlight.internal.LanguageIdTable
import io.github.lumkit.sweeteditor.highlight.internal.SyntaxCatalog
import io.github.lumkit.sweeteditor.highlight.internal.runOnHighlightHostThread
import io.github.lumkit.sweeteditor.highlight.internal.syntaxNameOfJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class SweetLineHighlight(
    private val config: SweetLineHighlightConfig = SweetLineHighlightConfig(),
) {
    private val catalog = SyntaxCatalog(
        loadBuiltins = config.loadBuiltinSyntaxes,
        builtinFilter = config.builtinSyntaxFilter,
    )
    private var binding: HighlightBindingImpl? = null
    private var closed = false

    fun bind(controller: SweetEditorController): HighlightBinding {
        check(!closed) { "SweetLineHighlight is closed" }
        if (binding != null) {
            throw IllegalStateException("SweetLineHighlight can only bind once")
        }
        val created = HighlightBindingImpl(controller, config, catalog)
        binding = created
        created.attach()
        return created
    }

    fun registerSyntaxJson(json: String) {
        catalog.registerJson(json)
        binding?.compileJson(json)
    }

    fun registerSyntaxFile(path: String) {
        binding?.compileFile(path)
    }

    fun updateDocument(descriptor: HighlightDocumentDescriptor) {
        binding?.updateDocument(descriptor)
    }

    fun updateTheme(theme: HighlightTheme) {
        binding?.updateTheme(theme)
    }

    fun updateFeatures(features: HighlightFeatureFlags) {
        binding?.updateFeatures(features)
    }

    fun requestRefresh() {
        binding?.requestRefresh()
    }

    fun close() {
        if (closed) return
        closed = true
        binding?.close()
    }
}

internal class HighlightBindingImpl(
    private val controller: SweetEditorController,
    private val config: SweetLineHighlightConfig,
    private val catalog: SyntaxCatalog,
) : HighlightBinding {
    private val bindingId: String = nextBindingId()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val session: HighlightSession
    private val provider: HighlightDecorationProvider
    private var closed = false
    private var providerRegistered = false
    private var started = false

    init {
        val tabSize = config.tabSize
            ?: controller.getLanguageConfiguration()?.tabSize
            ?: controller.getSettings().tabSize
        session = HighlightSession(
            native = DefaultHighlightNative,
            requestRefresh = { controller.requestDecorationRefresh() },
            bindingId = bindingId,
            tabSize = tabSize,
            descriptor = config.document,
            features = config.features,
            theme = config.theme,
        )
        provider = HighlightDecorationProvider(session)
    }

    fun attach() {
        session.bind(controller)
        controller.whenReady {
            if (closed || started) return@whenReady
            started = true
            scope.launch {
                startReady()
            }
        }
    }

    private suspend fun startReady() {
        if (closed) return
        val payloads = loadSyntaxPayloads()
        awaitHost {
            if (closed) return@awaitHost
            controller.registerBatchTextStyles(session.batchTextStyles())
            payloads.forEach { (name, json) -> compileLoaded(json, name) }
            session.start()
            if (!session.disabled) {
                controller.addDecorationProvider(provider)
                providerRegistered = true
            }
            controller.requestDecorationRefresh()
        }
    }

    fun compileJson(json: String) {
        val name = syntaxNameOfJson(json)
        compileLoaded(json, name)
    }

    fun compileFile(path: String) {
        if (closed) return
        try {
            session.compileSyntaxFile(path)
        } catch (error: HighlightException) {
            println("SweetLine compileFile failed: ${error.message}")
        }
    }

    fun updateDocument(descriptor: HighlightDocumentDescriptor) {
        session.updateDocument(descriptor)
        scope.launch {
            val payloads = loadSyntaxPayloads(descriptor)
            awaitHost {
                payloads.forEach { (name, json) -> compileLoaded(json, name) }
                if (!closed) controller.requestDecorationRefresh()
            }
        }
    }

    fun updateTheme(theme: HighlightTheme) {
        session.updateTheme(theme)
        controller.registerBatchTextStyles(session.batchTextStyles())
        controller.requestDecorationRefresh()
    }

    fun updateFeatures(features: HighlightFeatureFlags) {
        session.updateFeatures(features)
    }

    override fun requestRefresh() {
        controller.requestDecorationRefresh()
    }

    override fun close() {
        if (closed) return
        closed = true
        scope.cancel()
        if (providerRegistered) {
            controller.removeDecorationProvider(provider)
            providerRegistered = false
        }
        session.close()
    }

    private suspend fun loadSyntaxPayloads(
        descriptor: HighlightDocumentDescriptor = config.document,
    ): List<Pair<String, String>> {
        val payloads = ArrayList<Pair<String, String>>()
        syntaxNamesToCompile(descriptor).forEach { name ->
            if (catalog.isCompiled(name)) return@forEach
            val json = catalog.registeredJson(name) ?: catalog.loadJson(name) ?: return@forEach
            payloads += name to json
        }
        return payloads
    }

    private suspend fun awaitHost(block: () -> Unit) {
        suspendCancellableCoroutine { continuation ->
            runOnHighlightHostThread {
                try {
                    block()
                    if (continuation.isActive) continuation.resume(Unit)
                } catch (error: Throwable) {
                    if (continuation.isActive) continuation.resumeWithException(error)
                }
            }
        }
    }

    private fun compileLoaded(json: String, name: String?) {
        try {
            session.compileSyntaxJson(json)
            if (name != null) catalog.markCompiled(name)
        } catch (error: HighlightException) {
            println("SweetLine compileJson failed: ${error.message}")
        }
    }

    private fun syntaxNamesToCompile(descriptor: HighlightDocumentDescriptor): Set<String> {
        val names = LinkedHashSet<String>()
        names += config.preloadSyntaxNames
        val syntaxName = descriptor.syntaxName
        if (!syntaxName.isNullOrEmpty()) {
            names += syntaxName
            return names
        }
        val fileName = descriptor.fileName
        if (!fileName.isNullOrEmpty()) {
            LanguageIdTable.syntaxFilesForFileName(fileName).forEach { file ->
                SyntaxCatalog.FILE_BY_SYNTAX_NAME.entries.firstOrNull { it.value == file }?.key?.let { names += it }
            }
            return names
        }
        val languageId = descriptor.languageId
        if (!languageId.isNullOrEmpty()) {
            LanguageIdTable.resolve(languageId, config.languageIdOverrides)?.let { names += it }
        }
        return names
    }

    companion object {
        private var nextId = 0

        private fun nextBindingId(): String {
            nextId += 1
            return nextId.toString()
        }
    }
}
