package io.github.lumkit.sweeteditor.highlight.internal

import io.github.lumkit.sweeteditor.DecorationContext
import io.github.lumkit.sweeteditor.DecorationProvider
import io.github.lumkit.sweeteditor.DecorationReceiver
import io.github.lumkit.sweeteditor.DecorationType

internal class HighlightDecorationProvider(
    private val session: HighlightSession,
) : DecorationProvider {
    override fun capabilities(): Set<DecorationType> = session.decorationCapabilities()

    override fun provideDecorations(context: DecorationContext, receiver: DecorationReceiver) {
        if (session.disabled || receiver.isCancelled) return
        if (capabilities().isEmpty()) {
            runOnHighlightHostThread {
                if (receiver.isCancelled) return@runOnHighlightHostThread
                receiver.accept(session.clearingResult())
                session.applyMatchedBrackets()
            }
            return
        }
        val gen = session.generation
        session.submitAnalyze(gen) {
            if (receiver.isCancelled || gen != session.generation) return@submitAnalyze
            val result = session.buildDecorationResult(context)
            runOnHighlightHostThread {
                if (receiver.isCancelled || gen != session.generation) return@runOnHighlightHostThread
                receiver.accept(result)
                session.applyMatchedBrackets()
            }
        }
    }
}
