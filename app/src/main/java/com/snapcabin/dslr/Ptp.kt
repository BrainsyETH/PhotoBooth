package com.snapcabin.dslr

/**
 * Picture Transfer Protocol (PTP / ISO 15740) over USB — the protocol Canon and
 * Nikon DSLRs speak when tethered. This is the foundation for native DSLR
 * support (live view + remote capture) that doesn't need an HDMI capture stick.
 *
 * Milestone 1 (this slice) uses only the standard operations needed to OPEN a
 * session and READ the camera model, to prove the transport works on a given
 * tablet + camera. Capture and live view (the Canon EOS vendor operations,
 * stubbed below for reference) come in later milestones.
 *
 * Everything is little-endian, the PTP wire convention.
 */
object Ptp {
    // ── USB container types (header `type` field) ──
    const val TYPE_COMMAND = 1
    const val TYPE_DATA = 2
    const val TYPE_RESPONSE = 3
    const val TYPE_EVENT = 4

    const val HEADER_LEN = 12

    // ── Standard PTP operations ──
    const val OP_GET_DEVICE_INFO = 0x1001
    const val OP_OPEN_SESSION = 0x1002
    const val OP_CLOSE_SESSION = 0x1003
    const val OP_GET_STORAGE_IDS = 0x1004
    const val OP_GET_OBJECT_HANDLES = 0x1007
    const val OP_GET_OBJECT_INFO = 0x1008
    const val OP_GET_OBJECT = 0x1009
    const val OP_INITIATE_CAPTURE = 0x100E

    // ── Canon EOS vendor operations (later milestones; listed for the map) ──
    const val OP_EOS_SET_REMOTE_MODE = 0x9114
    const val OP_EOS_SET_EVENT_MODE = 0x9115
    const val OP_EOS_GET_EVENT = 0x9116
    const val OP_EOS_REMOTE_RELEASE_ON = 0x9128
    const val OP_EOS_REMOTE_RELEASE_OFF = 0x9129
    const val OP_EOS_GET_VIEWFINDER_DATA = 0x9153

    // ── Response codes ──
    const val RESP_OK = 0x2001
    const val RESP_SESSION_ALREADY_OPEN = 0x201E

    // ── Vendor extension IDs (from DeviceInfo) ──
    const val VENDOR_CANON = 0x0000000B
    const val VENDOR_NIKON = 0x0000000A
    const val VENDOR_SONY = 0x00000011

    const val SESSION_ID = 1

    /** USB interface class for still-image (PTP) devices. */
    const val USB_CLASS_STILL_IMAGE = 6

    fun vendorName(id: Long): String = when (id.toInt()) {
        VENDOR_CANON -> "Canon"
        VENDOR_NIKON -> "Nikon"
        VENDOR_SONY -> "Sony"
        else -> "vendor 0x${id.toString(16)}"
    }
}

/** Little-endian cursor over a PTP dataset (DeviceInfo, etc.). */
internal class PtpReader(private val b: ByteArray, private var pos: Int = 0) {
    fun u8(): Int = b[pos++].toInt() and 0xFF
    fun u16(): Int = u8() or (u8() shl 8)
    fun u32(): Long {
        val v = (u8().toLong()) or (u8().toLong() shl 8) or
            (u8().toLong() shl 16) or (u8().toLong() shl 24)
        return v and 0xFFFFFFFFL
    }

    /** PTP string: 1 byte char-count (incl. trailing null), then UTF-16LE chars. */
    fun string(): String {
        val count = u8()
        if (count == 0) return ""
        val sb = StringBuilder()
        for (i in 0 until count) {
            val c = u16()
            if (c != 0) sb.append(c.toChar())
        }
        return sb.toString()
    }

    /** Array of uint16 (AUINT16): uint32 count, then count × uint16. */
    fun u16Array(): IntArray {
        val n = u32().toInt().coerceIn(0, 100_000)
        return IntArray(n) { u16() }
    }

    fun skipU32Array() {
        val n = u32().toInt().coerceIn(0, 100_000)
        pos += n * 4
    }
}

object PtpBytes {
    fun writeU16(buf: ByteArray, off: Int, value: Int) {
        buf[off] = (value and 0xFF).toByte()
        buf[off + 1] = ((value shr 8) and 0xFF).toByte()
    }

    fun writeU32(buf: ByteArray, off: Int, value: Int) {
        buf[off] = (value and 0xFF).toByte()
        buf[off + 1] = ((value shr 8) and 0xFF).toByte()
        buf[off + 2] = ((value shr 16) and 0xFF).toByte()
        buf[off + 3] = ((value shr 24) and 0xFF).toByte()
    }

    fun readU16(buf: ByteArray, off: Int): Int =
        (buf[off].toInt() and 0xFF) or ((buf[off + 1].toInt() and 0xFF) shl 8)

    fun readU32(buf: ByteArray, off: Int): Long {
        var v = 0L
        for (i in 0 until 4) v = v or ((buf[off + i].toLong() and 0xFF) shl (8 * i))
        return v
    }
}
