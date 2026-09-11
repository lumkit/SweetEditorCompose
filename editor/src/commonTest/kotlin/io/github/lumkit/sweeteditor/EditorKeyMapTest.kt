package io.github.lumkit.sweeteditor

import io.github.lumkit.sweeteditor.core.protocol.EditorBuiltinCommand
import io.github.lumkit.sweeteditor.core.protocol.KeyCode
import io.github.lumkit.sweeteditor.core.protocol.KeyModifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EditorKeyMapTest {
    @Test
    fun registerCommandAssignsCustomId() {
        val map = EditorKeyMap()
        val id = map.registerCommand(
            EditorKeyBinding(EditorKeyChord(KeyModifier.CTRL, KeyCode.D), command = 0),
        ) { _, _ -> }
        assertTrue(id > EditorBuiltinCommand.TRIGGER_COMPLETION.value)
        assertEquals(id, map.snapshotBindings().single().command)
        assertTrue(map.handlerFor(id) != null)
    }
}
