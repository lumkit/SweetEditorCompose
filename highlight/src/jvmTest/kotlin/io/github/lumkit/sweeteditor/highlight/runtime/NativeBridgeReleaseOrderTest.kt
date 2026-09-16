package io.github.lumkit.sweeteditor.highlight.runtime

import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class NativeBridgeReleaseOrderTest {
    @Test
    fun createAndFreeEngineDocumentAnalyzerTwice() {
        assertTrue(NativeBridge.isAvailable)
        repeat(2) { index ->
            val uri = "release-order-$index.sltest"
            val engine = NativeBridge.createEngine(4)
            assertNotEquals(0L, engine)
            var document = 0L
            var analyzer = 0L
            try {
                NativeBridge.compileJson(engine, RELEASE_ORDER_SYNTAX)
                document = NativeBridge.createDocument(uri, "int x;\n")
                assertNotEquals(0L, document)
                analyzer = NativeBridge.loadDocument(engine, document)
                assertNotEquals(0L, analyzer)
            } finally {
                if (analyzer != 0L) NativeBridge.freeDocumentAnalyzer(analyzer)
                NativeBridge.removeDocument(engine, uri)
                if (document != 0L) NativeBridge.freeDocument(document)
                NativeBridge.freeEngine(engine)
            }
        }
    }
}

private const val RELEASE_ORDER_SYNTAX = """
{
  "name": "releaseOrder",
  "fileSuffix": ".sltest",
  "states": {
    "default": [
      { "pattern": "\\w+", "style": "keyword" }
    ]
  }
}
"""
