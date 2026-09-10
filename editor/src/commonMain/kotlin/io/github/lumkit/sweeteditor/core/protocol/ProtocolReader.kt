package io.github.lumkit.sweeteditor.core.protocol

internal class ProtocolReader(private val data: ByteArray) {
    var offset: Int = 0
        private set

    val remaining: Int get() = data.size - offset

    fun readU8(): Int {
        checkRemaining(1)
        return data[offset++].toInt() and 0xFF
    }

    fun readU16(): Int {
        checkRemaining(2)
        val value = (data[offset].toInt() and 0xFF) or ((data[offset + 1].toInt() and 0xFF) shl 8)
        offset += 2
        return value
    }

    fun readI32(): Int {
        checkRemaining(4)
        val value = (data[offset].toInt() and 0xFF) or
            ((data[offset + 1].toInt() and 0xFF) shl 8) or
            ((data[offset + 2].toInt() and 0xFF) shl 16) or
            ((data[offset + 3].toInt() and 0xFF) shl 24)
        offset += 4
        return value
    }

    fun readU32(): Int = readI32()

    fun readI64(): Long {
        checkRemaining(8)
        var value = 0L
        for (i in 0 until 8) {
            value = value or ((data[offset + i].toLong() and 0xFFL) shl (8 * i))
        }
        offset += 8
        return value
    }

    fun readU64(): Long = readI64()

    fun readF32(): Float = Float.fromBits(readI32())

    fun readF64(): Double = Double.fromBits(readI64())

    fun readBoolI32(): Boolean = readI32() != 0

    fun readBoolU8(): Boolean = readU8() != 0

    fun readUtf8String(): String {
        val length = readI32()
        if (length < 0 || length > remaining) {
            throw IllegalArgumentException("Invalid protocol string length: $length at $offset remaining=$remaining")
        }
        if (length == 0) return ""
        val bytes = data.copyOfRange(offset, offset + length)
        offset += length
        return bytes.decodeToString()
    }

    fun readListCount(): Int {
        val count = readI32()
        if (count < 0) {
            throw IllegalArgumentException("Negative protocol list count: $count at $offset")
        }
        if (count == 0) return 0
        if (count > remaining) {
            throw IllegalArgumentException(
                "Protocol list count $count exceeds remaining $remaining bytes at offset $offset (buffer ${data.size})",
            )
        }
        return count
    }

    private fun checkRemaining(count: Int) {
        if (remaining < count) {
            throw IllegalArgumentException("Protocol buffer underflow at $offset")
        }
    }
}

internal class ProtocolWriter(capacity: Int = 256) {
    private var data = ByteArray(capacity.coerceAtLeast(16))
    private var offset = 0

    fun toByteArray(): ByteArray = data.copyOf(offset)

    fun writeU8(value: Int) {
        ensure(1)
        data[offset++] = value.toByte()
    }

    fun writeU16(value: Int) {
        ensure(2)
        data[offset++] = value.toByte()
        data[offset++] = (value ushr 8).toByte()
    }

    fun writeI32(value: Int) {
        ensure(4)
        data[offset++] = value.toByte()
        data[offset++] = (value ushr 8).toByte()
        data[offset++] = (value ushr 16).toByte()
        data[offset++] = (value ushr 24).toByte()
    }

    fun writeU32(value: Int) = writeI32(value)

    fun writeI64(value: Long) {
        ensure(8)
        for (i in 0 until 8) {
            data[offset++] = (value ushr (8 * i)).toByte()
        }
    }

    fun writeU64(value: Long) = writeI64(value)

    fun writeF32(value: Float) = writeI32(value.toRawBits())

    fun writeF64(value: Double) = writeI64(value.toRawBits())

    fun writeBoolI32(value: Boolean) = writeI32(if (value) 1 else 0)

    fun writeBoolU8(value: Boolean) = writeU8(if (value) 1 else 0)

    fun writeUtf8String(value: String) {
        val bytes = value.encodeToByteArray()
        writeI32(bytes.size)
        ensure(bytes.size)
        bytes.copyInto(data, offset)
        offset += bytes.size
    }

    private fun ensure(count: Int) {
        val needed = offset + count
        if (needed <= data.size) return
        var cap = data.size
        while (cap < needed) cap *= 2
        data = data.copyOf(cap)
    }
}
