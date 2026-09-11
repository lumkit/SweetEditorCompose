package io.github.lumkit.sweeteditor.core

import io.github.lumkit.sweeteditor.EditorSearchOptions
import io.github.lumkit.sweeteditor.EditorSearchStatus
import io.github.lumkit.sweeteditor.encodeSearchReplacement
import io.github.lumkit.sweeteditor.encodeSearchRequest
import io.github.lumkit.sweeteditor.toPublic
import io.github.lumkit.sweeteditor.core.protocol.CoreProtocol
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EditorSearchJniTest {
    @Test
    fun searchReplaceAndClear() {
        val host = HostTextMeasurer(charWidth = 8f, ascent = 12f, descent = 4f)
        val document = Document.fromUtf8("hello hello")
        val editor = EditorCore.create(host)
        try {
            assertTrue(editor.setDocument(document)?.handled == true)
            assertTrue(editor.setViewport(400, 300)?.handled == true)
            assertTrue(
                editor.search(encodeSearchRequest("hello", EditorSearchOptions()))?.handled == true,
            )
            val found = CoreProtocol.decodeSearchState(requireNotNull(editor.getSearchState())).toPublic()
            assertEquals(EditorSearchStatus.READY, found.status)
            assertEquals(2, found.matchCount)
            assertTrue(found.hasCurrentMatch)

            assertTrue(editor.findNextSearchMatch()?.handled == true)
            assertTrue(
                editor.replaceCurrentSearchMatch(encodeSearchReplacement("hi"))?.handled == true,
            )
            val afterOne = document.utf8Text()
            assertTrue(afterOne.contains("hi"), afterOne)
            assertTrue(afterOne.contains("hello"), afterOne)

            assertTrue(editor.search(encodeSearchRequest("hello", EditorSearchOptions()))?.handled == true)
            assertTrue(
                editor.replaceAllSearchMatches(encodeSearchReplacement("world"))?.handled == true,
            )
            val afterAll = document.utf8Text()
            assertTrue(afterAll.contains("hi"), afterAll)
            assertTrue(afterAll.contains("world"), afterAll)
            assertTrue(!afterAll.contains("hello"), afterAll)

            assertTrue(editor.clearSearch()?.handled == true)
            val cleared = CoreProtocol.decodeSearchState(requireNotNull(editor.getSearchState())).toPublic()
            assertEquals(EditorSearchStatus.INACTIVE, cleared.status)
            assertEquals(0, cleared.matchCount)
        } finally {
            editor.close()
            document.close()
        }
    }
}
