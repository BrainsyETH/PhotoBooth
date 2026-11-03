package com.snapcabin.dslr

/**
 * Pure parsers for the PTP datasets the standard / Nikon capture path reads.
 * No Android dependencies, so they're unit-tested on the JVM.
 */
object PtpParsing {

    data class Event(val code: Int, val param: Long)

    /**
     * Nikon CheckEvent (0x90C7) data: u16 count, then count × (u16 code, u32
     * param). Truncated or garbled data yields the events that parsed cleanly.
     */
    fun parseNikonEvents(data: ByteArray?): List<Event> {
        if (data == null || data.size < 2) return emptyList()
        val count = PtpBytes.readU16(data, 0)
        val events = ArrayList<Event>(count.coerceAtMost(64))
        var off = 2
        repeat(count) {
            if (off + 6 > data.size) return events
            events += Event(PtpBytes.readU16(data, off), PtpBytes.readU32(data, off + 2))
            off += 6
        }
        return events
    }

    /**
     * A USB interrupt-endpoint event container: u32 length · u16 type (4) ·
     * u16 code · u32 transactionId · up to three u32 params. Returns null when
     * it isn't a well-formed event.
     */
    fun parseInterruptEvent(buf: ByteArray, length: Int): Event? {
        if (length < Ptp.HEADER_LEN) return null
        if (PtpBytes.readU16(buf, 4) != Ptp.TYPE_EVENT) return null
        val code = PtpBytes.readU16(buf, 6)
        val param = if (length >= Ptp.HEADER_LEN + 4) PtpBytes.readU32(buf, Ptp.HEADER_LEN) else 0L
        return Event(code, param)
    }

    /** ObjectInfo dataset: u32 StorageID, then u16 ObjectFormat. -1 if too short. */
    fun objectFormat(objectInfo: ByteArray?): Int =
        if (objectInfo == null || objectInfo.size < 6) -1 else PtpBytes.readU16(objectInfo, 4)

    /** Object handles a capture announced (standard or Nikon-SDRAM ObjectAdded), in order. */
    fun addedHandles(events: List<Event>): List<Long> = events
        .filter { it.code == Ptp.EC_OBJECT_ADDED || it.code == Ptp.EC_NIKON_OBJECT_ADDED_IN_SDRAM }
        .map { it.param }

    fun captureCompleted(events: List<Event>): Boolean = events.any {
        it.code == Ptp.EC_CAPTURE_COMPLETE || it.code == Ptp.EC_NIKON_CAPTURE_COMPLETE_REC_IN_SDRAM
    }
}
