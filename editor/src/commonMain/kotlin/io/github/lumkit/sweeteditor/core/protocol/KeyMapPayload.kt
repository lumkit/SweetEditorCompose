package io.github.lumkit.sweeteditor.core.protocol

internal fun encodeSetKeyMapPayload(bindings: List<KeyBinding>): ByteArray {
    val writer = ProtocolWriter()
    writer.writeI32(bindings.size)
    for (binding in bindings) {
        writer.writeRaw(CoreProtocol.encodeKeyBinding(binding))
    }
    return writer.toByteArray()
}
