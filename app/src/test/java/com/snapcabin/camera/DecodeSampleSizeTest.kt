package com.snapcabin.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Decoded frames must stay within budget: this is what keeps a GIF session from exhausting the heap. */
class DecodeSampleSizeTest {

    @Test
    fun smallerThanCapIsNotSampled() {
        assertEquals(1, decodeSampleSize(1920, 1440, 4096))
        assertEquals(1, decodeSampleSize(4096, 3072, 4096))
    }

    @Test
    fun twelveMegapixelGifFrameIsSampledUnderGifCap() {
        val sample = decodeSampleSize(4000, 3000, 1280)
        assertEquals(4, sample)
        assertTrue(4000 / sample <= 1280)
    }

    @Test
    fun fiftyMegapixelSingleStaysUnderHardCeiling() {
        val sample = decodeSampleSize(8160, 6120, CameraManager.MAX_DECODE_DIMENSION)
        assertEquals(2, sample)
        assertTrue(8160 / sample <= CameraManager.MAX_DECODE_DIMENSION)
    }

    @Test
    fun portraitUsesLongEdge() {
        assertEquals(decodeSampleSize(3000, 4000, 2048), decodeSampleSize(4000, 3000, 2048))
    }

    @Test
    fun resultIsAlwaysWithinCapAcrossSizes() {
        for (w in listOf(640, 1280, 1281, 2560, 3264, 4000, 6000, 8160, 12000)) {
            for (cap in listOf(1280, 2048, 4096)) {
                val s = decodeSampleSize(w, w * 3 / 4, cap)
                assertTrue("w=$w cap=$cap sample=$s", w / s <= cap)
                assertTrue("power of two", s and (s - 1) == 0)
            }
        }
    }
}
