package com.snapcabin.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraPlanTest {

    private val front = KnownCamera("1", isExternal = false)
    private val back = KnownCamera("0", isExternal = false)
    private val webcam = KnownCamera("102", isExternal = true)
    private val builtIns = listOf(back, front)

    private fun plan(
        requestedId: String,
        system: List<KnownCamera> = builtIns,
        provider: List<KnownCamera> = builtIns,
        usbVideoPresent: Boolean = false,
        micGranted: Boolean = true,
        externalPaused: Boolean = false,
        refreshTried: Set<String> = emptySet()
    ) = planCamera(requestedId, system, provider, usbVideoPresent, micGranted, externalPaused, refreshTried)

    @Test
    fun `automatic uses the built-in default`() {
        val p = plan("", system = builtIns + webcam, provider = builtIns + webcam, usbVideoPresent = true)
        assertEquals(CameraPlan(), p)
    }

    @Test
    fun `specific built-in camera is bound by id`() {
        assertEquals(CameraPlan(cameraId = "0"), plan("0"))
    }

    @Test
    fun `webcam present at startup binds`() {
        val p = plan(
            EXTERNAL_CAMERA_ID,
            system = builtIns + webcam, provider = builtIns + webcam, usbVideoPresent = true
        )
        assertEquals("102", p.cameraId)
        assertTrue(p.acceptAnyExternal)
        assertNull(p.fallback)
    }

    @Test
    fun `webcam plugged in after startup asks for a camera library restart`() {
        val p = plan(EXTERNAL_CAMERA_ID, system = builtIns + webcam, usbVideoPresent = true)
        assertNull(p.cameraId)
        assertEquals("102", p.refreshFor)
    }

    @Test
    fun `a restart is attempted only once per camera`() {
        val p = plan(
            EXTERNAL_CAMERA_ID,
            system = builtIns + webcam, usbVideoPresent = true, refreshTried = setOf("102")
        )
        assertNull(p.refreshFor)
        assertNull(p.cameraId)
        assertEquals(FallbackReason.NOT_USABLE, p.fallback)
    }

    @Test
    fun `selected webcam by id plugged in after startup also restarts`() {
        val p = plan("102", system = builtIns + webcam, usbVideoPresent = true)
        assertEquals("102", p.refreshFor)
        assertFalse(p.acceptAnyExternal)
    }

    @Test
    fun `unplugged webcam is not bound even though the camera library still lists it`() {
        val p = plan(EXTERNAL_CAMERA_ID, provider = builtIns + webcam)
        assertNull(p.cameraId)
        assertNull(p.refreshFor)
        assertEquals(FallbackReason.NOT_CONNECTED, p.fallback)
    }

    @Test
    fun `webcam the system list misses is still used while it is plugged in`() {
        val p = plan(EXTERNAL_CAMERA_ID, provider = builtIns + webcam, usbVideoPresent = true)
        assertEquals("102", p.cameraId)
    }

    @Test
    fun `webcam that came back under a new id satisfies the old selection`() {
        val replugged = KnownCamera("103", isExternal = true)
        val p = plan(
            "102",
            system = builtIns + replugged, provider = builtIns + webcam + replugged, usbVideoPresent = true
        )
        assertEquals("103", p.cameraId)
        assertTrue(p.acceptAnyExternal)
    }

    @Test
    fun `the exact selected webcam wins over another external camera`() {
        val other = KnownCamera("101", isExternal = true)
        val cams = builtIns + other + webcam
        val p = plan("102", system = cams, provider = cams, usbVideoPresent = true)
        assertEquals("102", p.cameraId)
    }

    @Test
    fun `usb video device the tablet does not offer as a camera is reported`() {
        val p = plan(EXTERNAL_CAMERA_ID, usbVideoPresent = true)
        assertNull(p.cameraId)
        assertEquals(FallbackReason.NOT_OFFERED_BY_TABLET, p.fallback)
    }

    @Test
    fun `no microphone permission means no usb camera`() {
        val p = plan(
            EXTERNAL_CAMERA_ID,
            system = builtIns + webcam, provider = builtIns + webcam, usbVideoPresent = true,
            micGranted = false
        )
        assertNull(p.cameraId)
        assertNull(p.refreshFor)
        assertEquals(FallbackReason.MIC_NOT_ALLOWED, p.fallback)
    }

    @Test
    fun `paused webcam is skipped`() {
        val p = plan(
            EXTERNAL_CAMERA_ID,
            system = builtIns + webcam, provider = builtIns + webcam, usbVideoPresent = true,
            externalPaused = true
        )
        assertNull(p.cameraId)
        assertEquals(FallbackReason.PAUSED_AFTER_FAILURES, p.fallback)
    }

    @Test
    fun `microphone and pause do not affect a built-in camera`() {
        val p = plan("1", micGranted = false, externalPaused = true)
        assertEquals(CameraPlan(cameraId = "1"), p)
    }
}
