package com.photobooth.gif

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Simple GIF encoder using LZW compression.
 * Creates animated GIFs from a list of Bitmaps.
 */
class GifEncoder {

    fun encode(frames: List<Bitmap>, delayMs: Int = 500, outputFile: File): Boolean {
        if (frames.isEmpty()) return false

        return try {
            FileOutputStream(outputFile).use { fos ->
                writeGif(frames, delayMs, fos)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun encodeToBytes(frames: List<Bitmap>, delayMs: Int = 500): ByteArray {
        val bos = ByteArrayOutputStream()
        writeGif(frames, delayMs, bos)
        return bos.toByteArray()
    }

    private fun writeGif(frames: List<Bitmap>, delayMs: Int, out: OutputStream) {
        val width = frames[0].width
        val height = frames[0].height

        // GIF Header
        out.write("GIF89a".toByteArray())

        // Logical Screen Descriptor
        writeShort(out, width)
        writeShort(out, height)
        out.write(0xF7) // GCT flag, 8 bit color resolution, 256 colors
        out.write(0)    // Background color index
        out.write(0)    // Pixel aspect ratio

        // Global Color Table (256 colors, uniform quantization)
        writeColorTable(out)

        // Netscape extension for looping
        out.write(0x21) // Extension
        out.write(0xFF) // App extension
        out.write(11)   // Block size
        out.write("NETSCAPE2.0".toByteArray())
        out.write(3)    // Sub-block size
        out.write(1)    // Loop sub-block ID
        writeShort(out, 0) // Loop count (0 = infinite)
        out.write(0)    // Block terminator

        for (frame in frames) {
            val scaled = if (frame.width != width || frame.height != height) {
                Bitmap.createScaledBitmap(frame, width, height, true)
            } else {
                frame
            }

            // Graphic Control Extension
            out.write(0x21) // Extension
            out.write(0xF9) // GCE
            out.write(4)    // Block size
            out.write(0)    // Packed (no transparency)
            writeShort(out, delayMs / 10) // Delay in 1/100 seconds
            out.write(0)    // Transparent color index
            out.write(0)    // Block terminator

            // Image Descriptor
            out.write(0x2C) // Image separator
            writeShort(out, 0) // Left
            writeShort(out, 0) // Top
            writeShort(out, width)
            writeShort(out, height)
            out.write(0) // No local color table

            // Image data (LZW compressed)
            writeLzwData(out, scaled, width, height)
        }

        // GIF Trailer
        out.write(0x3B)
        out.flush()
    }

    private fun writeColorTable(out: OutputStream) {
        // Uniform 8-8-4 color quantization (256 entries)
        for (i in 0 until 256) {
            val r = (i shr 5 and 0x07) * 255 / 7
            val g = (i shr 2 and 0x07) * 255 / 7
            val b = (i and 0x03) * 255 / 3
            out.write(r)
            out.write(g)
            out.write(b)
        }
    }

    private fun quantizePixel(argb: Int): Int {
        val r = (argb shr 16 and 0xFF) * 7 / 255
        val g = (argb shr 8 and 0xFF) * 7 / 255
        val b = (argb and 0xFF) * 3 / 255
        return (r shl 5) or (g shl 2) or b
    }

    private fun writeLzwData(out: OutputStream, bitmap: Bitmap, width: Int, height: Int) {
        val minCodeSize = 8
        out.write(minCodeSize)

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val indexed = ByteArray(pixels.size) { quantizePixel(pixels[it]).toByte() }

        // Simple LZW encoding
        val clearCode = 1 shl minCodeSize
        val eoiCode = clearCode + 1

        val subBlock = ByteArrayOutputStream()
        var bitBuffer = 0
        var bitsInBuffer = 0
        var codeSize = minCodeSize + 1

        fun writeBits(code: Int, size: Int) {
            bitBuffer = bitBuffer or (code shl bitsInBuffer)
            bitsInBuffer += size
            while (bitsInBuffer >= 8) {
                subBlock.write(bitBuffer and 0xFF)
                bitBuffer = bitBuffer shr 8
                bitsInBuffer -= 8
            }
        }

        fun flushSubBlocks() {
            if (bitsInBuffer > 0) {
                subBlock.write(bitBuffer and 0xFF)
                bitBuffer = 0
                bitsInBuffer = 0
            }
            val data = subBlock.toByteArray()
            subBlock.reset()
            var offset = 0
            while (offset < data.size) {
                val blockSize = minOf(255, data.size - offset)
                out.write(blockSize)
                out.write(data, offset, blockSize)
                offset += blockSize
            }
            out.write(0) // Block terminator
        }

        // Use a simple table with max 4096 entries
        val table = HashMap<Long, Int>(4096)
        var nextCode = eoiCode + 1

        fun resetTable() {
            table.clear()
            for (i in 0 until clearCode) {
                table[i.toLong()] = i
            }
            nextCode = eoiCode + 1
            codeSize = minCodeSize + 1
        }

        writeBits(clearCode, codeSize)
        resetTable()

        if (indexed.isEmpty()) {
            writeBits(eoiCode, codeSize)
            flushSubBlocks()
            return
        }

        var current = (indexed[0].toInt() and 0xFF).toLong()

        for (i in 1 until indexed.size) {
            val pixel = (indexed[i].toInt() and 0xFF)
            val key = (current shl 8) or pixel.toLong() or (1L shl 32)

            if (table.containsKey(key)) {
                current = table[key]!!.toLong()
            } else {
                writeBits(table[current]!!, codeSize)

                if (nextCode < 4096) {
                    table[key] = nextCode++
                    if (nextCode > (1 shl codeSize) && codeSize < 12) {
                        codeSize++
                    }
                } else {
                    writeBits(clearCode, codeSize)
                    resetTable()
                }
                current = pixel.toLong()
            }
        }

        writeBits(table[current]!!, codeSize)
        writeBits(eoiCode, codeSize)
        flushSubBlocks()
    }

    private fun writeShort(out: OutputStream, value: Int) {
        out.write(value and 0xFF)
        out.write(value shr 8 and 0xFF)
    }
}
