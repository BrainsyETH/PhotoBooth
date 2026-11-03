package com.snapcabin.dslr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PtpParsingTest {

    private fun nikonEvents(vararg events: Pair<Int, Long>): ByteArray {
        val buf = ByteArray(2 + events.size * 6)
        PtpBytes.writeU16(buf, 0, events.size)
        events.forEachIndexed { i, (code, param) ->
            PtpBytes.writeU16(buf, 2 + i * 6, code)
            PtpBytes.writeU32(buf, 4 + i * 6, param.toInt())
        }
        return buf
    }

    @Test
    fun parsesNikonCheckEvent() {
        val events = PtpParsing.parseNikonEvents(
            nikonEvents(Ptp.EC_OBJECT_ADDED to 0x1234L, Ptp.EC_CAPTURE_COMPLETE to 0L)
        )
        assertEquals(2, events.size)
        assertEquals(listOf(0x1234L), PtpParsing.addedHandles(events))
        assertTrue(PtpParsing.captureCompleted(events))
    }

    @Test
    fun rawPlusJpegAnnouncesBothObjectsInOrder() {
        val events = PtpParsing.parseNikonEvents(
            nikonEvents(Ptp.EC_OBJECT_ADDED to 0x10L, Ptp.EC_OBJECT_ADDED to 0x11L)
        )
        assertEquals(listOf(0x10L, 0x11L), PtpParsing.addedHandles(events))
        assertFalse(PtpParsing.captureCompleted(events))
    }

    @Test
    fun sdramHandleKeepsItsHighBit() {
        val events = PtpParsing.parseNikonEvents(
            nikonEvents(Ptp.EC_NIKON_OBJECT_ADDED_IN_SDRAM to Ptp.NIKON_SDRAM_HANDLE)
        )
        assertEquals(listOf(Ptp.NIKON_SDRAM_HANDLE), PtpParsing.addedHandles(events))
    }

    @Test
    fun truncatedNikonEventsYieldWhatParsed() {
        val full = nikonEvents(Ptp.EC_OBJECT_ADDED to 7L, Ptp.EC_OBJECT_ADDED to 8L)
        val events = PtpParsing.parseNikonEvents(full.copyOf(full.size - 3))
        assertEquals(listOf(7L), PtpParsing.addedHandles(events))
        assertTrue(PtpParsing.parseNikonEvents(null).isEmpty())
        assertTrue(PtpParsing.parseNikonEvents(ByteArray(1)).isEmpty())
    }

    @Test
    fun parsesInterruptEventContainer() {
        val buf = ByteArray(16)
        PtpBytes.writeU32(buf, 0, 16)
        PtpBytes.writeU16(buf, 4, Ptp.TYPE_EVENT)
        PtpBytes.writeU16(buf, 6, Ptp.EC_OBJECT_ADDED)
        PtpBytes.writeU32(buf, 8, 3)
        PtpBytes.writeU32(buf, 12, 0x2A)
        assertEquals(PtpParsing.Event(Ptp.EC_OBJECT_ADDED, 0x2AL), PtpParsing.parseInterruptEvent(buf, 16))

        PtpBytes.writeU16(buf, 4, Ptp.TYPE_RESPONSE)
        assertNull("non-event containers are ignored", PtpParsing.parseInterruptEvent(buf, 16))
        assertNull(PtpParsing.parseInterruptEvent(buf, 4))
    }

    @Test
    fun readsObjectFormat() {
        val info = ByteArray(52)
        PtpBytes.writeU16(info, 4, Ptp.FORMAT_EXIF_JPEG)
        assertEquals(Ptp.FORMAT_EXIF_JPEG, PtpParsing.objectFormat(info))
        assertEquals(-1, PtpParsing.objectFormat(ByteArray(3)))
        assertEquals(-1, PtpParsing.objectFormat(null))
    }

    // ── Which capture path a body gets ──

    private fun device(vendor: Int, manufacturer: String, vararg ops: Int) = PtpDeviceInfo(
        manufacturer = manufacturer, model = "X", serialNumber = "",
        vendorExtensionId = vendor.toLong(), operationsSupported = ops
    )

    @Test
    fun nikonInPtpModeUsesStandardCapture() {
        val d3100 = device(Ptp.VENDOR_NIKON, "Nikon Corporation", Ptp.OP_INITIATE_CAPTURE, Ptp.OP_NIKON_CHECK_EVENT)
        assertTrue(d3100.isNikon)
        assertTrue(d3100.usesStandardCapture)
        assertTrue(d3100.supportsRemoteCapture)
        assertFalse(d3100.supportsEosRemote)
    }

    @Test
    fun nikonInMtpModeIsStillRecognised() {
        val mtp = device(0x06, "Nikon Corporation", Ptp.OP_INITIATE_CAPTURE)
        assertTrue(mtp.isNikon)
        assertTrue(mtp.usesStandardCapture)
    }

    @Test
    fun nikonWithOnlySdramCaptureIsSupported() {
        val body = device(Ptp.VENDOR_NIKON, "Nikon", Ptp.OP_NIKON_INITIATE_CAPTURE_REC_IN_SDRAM)
        assertTrue(body.usesStandardCapture)
    }

    @Test
    fun canonEosKeepsItsOwnPath() {
        val eos = device(
            Ptp.VENDOR_CANON, "Canon Inc.",
            Ptp.OP_EOS_SET_REMOTE_MODE, Ptp.OP_EOS_REMOTE_RELEASE_ON, Ptp.OP_INITIATE_CAPTURE
        )
        assertTrue(eos.supportsEosRemote)
        assertFalse("EOS bodies must not be routed to InitiateCapture", eos.usesStandardCapture)
        assertTrue(eos.supportsRemoteCapture)
    }

    @Test
    fun bodyWithoutCaptureOpsIsNotOffered() {
        val viewer = device(Ptp.VENDOR_NIKON, "Nikon", Ptp.OP_GET_OBJECT)
        assertFalse(viewer.usesStandardCapture)
        assertFalse(viewer.supportsRemoteCapture)
    }
}
