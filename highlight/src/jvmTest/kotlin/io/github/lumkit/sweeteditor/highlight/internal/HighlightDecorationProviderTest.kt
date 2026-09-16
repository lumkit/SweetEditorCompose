package io.github.lumkit.sweeteditor.highlight.internal

import io.github.lumkit.sweeteditor.DecorationApplyMode
import io.github.lumkit.sweeteditor.DecorationContext
import io.github.lumkit.sweeteditor.DecorationReceiver
import io.github.lumkit.sweeteditor.DecorationResult
import io.github.lumkit.sweeteditor.DecorationType
import io.github.lumkit.sweeteditor.StyleSpan
import io.github.lumkit.sweeteditor.VisibleLineRange
import io.github.lumkit.sweeteditor.highlight.HighlightFeatureFlags
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HighlightDecorationProviderTest {
    @Test
    fun capabilitiesMatchFeatureFlags() {
        val native = RecordingHighlightNative()
        val session = HighlightSession(
            native,
            bindingId = "caps",
            features = HighlightFeatureFlags(
                syntaxHighlight = true,
                indentGuides = false,
                bracketGuides = true,
                matchedBrackets = false,
                rainbowBrackets = false,
            ),
        )
        try {
            val provider = HighlightDecorationProvider(session)
            assertEquals(
                setOf(DecorationType.SYNTAX_HIGHLIGHT, DecorationType.BRACKET_GUIDE),
                provider.capabilities(),
            )
        } finally {
            session.close()
        }
    }

    @Test
    fun acceptUsesReplaceRangeForFixedSyntaxSpans() {
        val native = RecordingHighlightNative()
        val session = HighlightSession(native, bindingId = "spans")
        val provider = HighlightDecorationProvider(session)
        session.decorationOverride = DecorationResult(
            syntaxSpans = mapOf(0 to listOf(StyleSpan(column = 0, length = 3, styleId = 1))),
            syntaxSpansMode = DecorationApplyMode.REPLACE_RANGE,
        )
        val latch = CountDownLatch(1)
        var accepted: DecorationResult? = null
        try {
            provider.provideDecorations(
                DecorationContext(
                    visibleLineRange = VisibleLineRange(0, 0),
                    totalLineCount = 1,
                    textChanges = emptyList(),
                ),
                object : DecorationReceiver {
                    override val isCancelled: Boolean = false
                    override fun accept(result: DecorationResult): Boolean {
                        accepted = result
                        latch.countDown()
                        return true
                    }
                },
            )
            assertTrue(latch.await(5, TimeUnit.SECONDS))
            assertEquals(DecorationApplyMode.REPLACE_RANGE, accepted?.syntaxSpansMode)
            assertEquals(1, accepted?.syntaxSpans?.get(0)?.size)
        } finally {
            session.close()
        }
    }
}
