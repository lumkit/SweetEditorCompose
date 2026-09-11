package io.github.lumkit.sweeteditor.decoration

import io.github.lumkit.sweeteditor.DecorationApplyMode
import io.github.lumkit.sweeteditor.DecorationResult
import io.github.lumkit.sweeteditor.StyleSpan
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DecorationProviderManagerTest {
    @Test
    fun mergeModePrefersReplaceAll() {
        assertEquals(
            DecorationApplyMode.REPLACE_ALL,
            mergeMode(DecorationApplyMode.MERGE, DecorationApplyMode.REPLACE_ALL),
        )
        assertEquals(
            DecorationApplyMode.REPLACE_RANGE,
            mergeMode(DecorationApplyMode.MERGE, DecorationApplyMode.REPLACE_RANGE),
        )
        assertEquals(
            DecorationApplyMode.REPLACE_ALL,
            mergeMode(DecorationApplyMode.REPLACE_RANGE, DecorationApplyMode.REPLACE_ALL),
        )
    }

    @Test
    fun mergePatchKeepsPreviousWhenMergeAndNull() {
        val previous = DecorationResult(
            syntaxSpans = mapOf(0 to listOf(StyleSpan(0, 3, 1))),
            syntaxSpansMode = DecorationApplyMode.REPLACE_RANGE,
        )
        val merged = mergePatch(previous, DecorationResult())
        assertEquals(previous.syntaxSpans, merged.syntaxSpans)
        assertEquals(DecorationApplyMode.REPLACE_RANGE, merged.syntaxSpansMode)
    }

    @Test
    fun mergePatchClearsWhenModeIsReplaceWithoutData() {
        val previous = DecorationResult(
            diagnostics = mapOf(1 to emptyList()),
            diagnosticsMode = DecorationApplyMode.MERGE,
        )
        val merged = mergePatch(
            previous,
            DecorationResult(diagnosticsMode = DecorationApplyMode.REPLACE_ALL),
        )
        assertNull(merged.diagnostics)
        assertEquals(DecorationApplyMode.REPLACE_ALL, merged.diagnosticsMode)
    }

    @Test
    fun mergePatchKeepsFoldRegionsOnMerge() {
        val previous = DecorationResult(
            foldRegions = listOf(io.github.lumkit.sweeteditor.FoldRegion(0, 4)),
            foldRegionsMode = DecorationApplyMode.REPLACE_ALL,
        )
        val merged = mergePatch(previous, DecorationResult())
        assertEquals(previous.foldRegions, merged.foldRegions)
        assertEquals(DecorationApplyMode.REPLACE_ALL, merged.foldRegionsMode)
    }
}
