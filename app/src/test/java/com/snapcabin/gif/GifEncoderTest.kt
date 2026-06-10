package com.snapcabin.gif

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Structural validation of the (previously never-run) GIF encoder. Pure JVM —
 * no Android/Bitmap — so it runs with `./gradlew testDebugUnitTest`.
 *
 * It doesn't decode the GIF back (no codec on the JVM); instead it walks the
 * byte stream as a GIF89a parser would: header, global color table, looping
 * extension, one correctly-framed image block per frame, trailer. Walking the
 * sub-block framing is the real value — that's where hand-rolled LZW encoders
 * most often emit corrupt output, and it was impossible to verify when the
 * encoder had zero call sites.
 */
class GifEncoderTest {

    private fun frame(w: Int, h: Int, color: Int) =
        GifEncoder.PixelFrame(IntArray(w * h) { color }, w, h)

    @Test
    fun emitsWellFormedHeaderAndTrailer() {
        val bytes = GifEncoder().encodePixelFrames(
            listOf(frame(4, 4, 0xFFFF0000.toInt())),
            delayMs = 200
        )
        assertEquals("GIF89a", String(bytes, 0, 6, Charsets.US_ASCII))
        assertEquals(0x3B.toByte(), bytes.last())
        val parsed = parse(bytes)
        assertEquals(4, parsed.width)
        assertEquals(4, parsed.height)
        assertEquals(1, parsed.imageBlocks)
        assertTrue(parsed.hasLoopExtension)
        assertTrue(parsed.reachedTrailer)
    }

    @Test
    fun oneImageBlockPerFrame() {
        val frames = listOf(
            frame(8, 6, 0xFFFF0000.toInt()),
            frame(8, 6, 0xFF00FF00.toInt()),
            frame(8, 6, 0xFF0000FF.toInt())
        )
        val parsed = parse(GifEncoder().encodePixelFrames(frames, delayMs = 250))
        assertEquals(3, parsed.imageBlocks)
        assertTrue(parsed.reachedTrailer)
    }

    @Test
    fun mismatchedFrameSizesDoNotCrashAndUseFirstFrameDimensions() {
        val parsed = parse(
            GifEncoder().encodePixelFrames(
                listOf(frame(8, 8, 0xFFFFFFFF.toInt()), frame(4, 4, 0xFF000000.toInt())),
                delayMs = 100
            )
        )
        assertEquals(8, parsed.width)
        assertEquals(8, parsed.height)
        assertEquals(2, parsed.imageBlocks)
        assertTrue(parsed.reachedTrailer)
    }

    @Test
    fun largerImageCompressesAndStaysWellFormed() {
        // Exercises LZW code-size growth + sub-block splitting (>255 bytes).
        val w = 64
        val h = 48
        val pixels = IntArray(w * h) { ((it * 2654435761u.toInt()) and 0xFFFFFF) or 0xFF000000.toInt() }
        val parsed = parse(GifEncoder().encodePixelFrames(listOf(GifEncoder.PixelFrame(pixels, w, h))))
        assertEquals(w, parsed.width)
        assertEquals(1, parsed.imageBlocks)
        assertTrue(parsed.reachedTrailer)
    }

    @Test
    fun emptyInputProducesNoBytes() {
        assertEquals(0, GifEncoder().encodePixelFrames(emptyList()).size)
    }

    // ---- Minimal GIF89a structural parser ----

    private data class Parsed(
        val width: Int,
        val height: Int,
        val imageBlocks: Int,
        val hasLoopExtension: Boolean,
        val reachedTrailer: Boolean
    )

    private fun parse(b: ByteArray): Parsed {
        fun u8(i: Int) = b[i].toInt() and 0xFF
        fun u16(i: Int) = u8(i) or (u8(i + 1) shl 8)

        var p = 6 // after "GIF89a"
        val width = u16(p); val height = u16(p + 2)
        val packed = u8(p + 4)
        p += 7
        // Global Color Table present? size = 2^(N+1) entries × 3 bytes.
        if (packed and 0x80 != 0) {
            val n = packed and 0x07
            p += 3 * (1 shl (n + 1))
        }

        var images = 0
        var loop = false
        var trailer = false

        fun skipSubBlocks() {
            while (true) {
                val size = u8(p); p += 1
                if (size == 0) break
                p += size
            }
        }

        loop@ while (p < b.size) {
            when (u8(p)) {
                0x3B -> { trailer = true; break@loop }     // trailer
                0x21 -> {                                  // extension
                    p += 1
                    val label = u8(p); p += 1
                    if (label == 0xFF) loop = true         // application ext (NETSCAPE loop)
                    skipSubBlocks()
                }
                0x2C -> {                                  // image descriptor
                    images += 1
                    p += 1
                    p += 8                                 // left/top/w/h
                    val imgPacked = u8(p); p += 1
                    if (imgPacked and 0x80 != 0) {         // local color table
                        val n = imgPacked and 0x07
                        p += 3 * (1 shl (n + 1))
                    }
                    p += 1                                 // LZW min code size
                    skipSubBlocks()                        // image data sub-blocks
                }
                else -> throw AssertionError("Unexpected block id 0x${u8(p).toString(16)} at offset $p")
            }
        }
        return Parsed(width, height, images, loop, trailer)
    }
}
