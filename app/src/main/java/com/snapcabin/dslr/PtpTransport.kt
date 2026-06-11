package com.snapcabin.dslr

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * PTP-over-USB transport: frames operations into USB bulk containers and runs
 * the command → (optional data) → response transaction. One instance owns a
 * claimed [UsbInterface] for the life of a DSLR connection.
 *
 * Container layout (little-endian): 4B length (incl. header) · 2B type · 2B
 * code · 4B transactionId · payload.
 */
class PtpTransport private constructor(
    private val connection: UsbDeviceConnection,
    private val iface: UsbInterface,
    private val bulkIn: UsbEndpoint,
    private val bulkOut: UsbEndpoint,
    private val intrIn: UsbEndpoint?
) {
    companion object {
        private const val TAG = "PtpTransport"
        private const val TIMEOUT_MS = 5_000
        private const val READ_BUF = 16 * 1024

        /**
         * Find the PTP interface + its bulk endpoints on [device] and claim it.
         * Returns null if the device exposes no usable still-image interface or
         * the interface can't be claimed.
         */
        fun open(device: UsbDevice, connection: UsbDeviceConnection): PtpTransport? {
            for (i in 0 until device.interfaceCount) {
                val iface = device.getInterface(i)
                if (iface.interfaceClass != Ptp.USB_CLASS_STILL_IMAGE) continue

                var bulkIn: UsbEndpoint? = null
                var bulkOut: UsbEndpoint? = null
                var intrIn: UsbEndpoint? = null
                for (e in 0 until iface.endpointCount) {
                    val ep = iface.getEndpoint(e)
                    when {
                        ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK &&
                            ep.direction == UsbConstants.USB_DIR_IN -> bulkIn = ep
                        ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK &&
                            ep.direction == UsbConstants.USB_DIR_OUT -> bulkOut = ep
                        ep.type == UsbConstants.USB_ENDPOINT_XFER_INT &&
                            ep.direction == UsbConstants.USB_DIR_IN -> intrIn = ep
                    }
                }
                if (bulkIn == null || bulkOut == null) continue

                if (!connection.claimInterface(iface, true)) {
                    Log.w(TAG, "claimInterface failed")
                    continue
                }
                Log.i(TAG, "Claimed PTP interface ${iface.id} (in=${bulkIn.address} out=${bulkOut.address})")
                return PtpTransport(connection, iface, bulkIn, bulkOut, intrIn)
            }
            return null
        }
    }

    private var transactionId = 0

    /** A response container: a result code plus up to five uint32 params. */
    data class PtpResponse(val code: Int, val params: LongArray) {
        val ok: Boolean get() = code == Ptp.RESP_OK || code == Ptp.RESP_SESSION_ALREADY_OPEN
    }

    private data class Container(
        val length: Long,
        val type: Int,
        val code: Int,
        val transactionId: Long,
        val payload: ByteArray
    )

    /**
     * Run one PTP transaction. [params] are the operation parameters; if
     * [dataToSend] is non-null a data phase is sent to the camera. Returns the
     * response and any data the camera sent back (null when there was no data
     * phase).
     */
    @Synchronized
    fun transact(
        opCode: Int,
        params: IntArray = IntArray(0),
        dataToSend: ByteArray? = null
    ): Pair<PtpResponse, ByteArray?> {
        val tid = transactionId++
        sendContainer(Ptp.TYPE_COMMAND, opCode, tid, paramsToBytes(params))
        if (dataToSend != null) {
            sendContainer(Ptp.TYPE_DATA, opCode, tid, dataToSend)
        }

        var data: ByteArray? = null
        var response: PtpResponse? = null
        var guard = 0
        while (response == null && guard++ < 8) {
            val c = readContainer() ?: throw IOException("No PTP container (timeout)")
            when (c.type) {
                Ptp.TYPE_DATA -> data = c.payload
                Ptp.TYPE_RESPONSE -> response = PtpResponse(c.code, paramsFromBytes(c.payload))
                Ptp.TYPE_EVENT -> Log.d(TAG, "Async event 0x${c.code.toString(16)} ignored")
                else -> Log.w(TAG, "Unexpected container type ${c.type}")
            }
        }
        return (response ?: throw IOException("No PTP response container")) to data
    }

    private fun sendContainer(type: Int, code: Int, tid: Int, payload: ByteArray) {
        val total = Ptp.HEADER_LEN + payload.size
        val buf = ByteArray(total)
        PtpBytes.writeU32(buf, 0, total)
        PtpBytes.writeU16(buf, 4, type)
        PtpBytes.writeU16(buf, 6, code)
        PtpBytes.writeU32(buf, 8, tid)
        System.arraycopy(payload, 0, buf, Ptp.HEADER_LEN, payload.size)

        var sent = 0
        while (sent < total) {
            val n = connection.bulkTransfer(bulkOut, buf.copyOfRange(sent, total), total - sent, TIMEOUT_MS)
            if (n <= 0) throw IOException("bulk OUT failed at $sent/$total (rc=$n)")
            sent += n
        }
    }

    private fun readContainer(): Container? {
        val buf = ByteArray(READ_BUF)
        // Skip stray zero-length packets (terminators after multiple-of-512 data).
        var first = 0
        var attempts = 0
        while (first <= 0 && attempts++ < 3) {
            first = connection.bulkTransfer(bulkIn, buf, buf.size, TIMEOUT_MS)
        }
        if (first < Ptp.HEADER_LEN) {
            Log.w(TAG, "Short/empty IN read: $first")
            return null
        }
        val length = PtpBytes.readU32(buf, 0)
        val type = PtpBytes.readU16(buf, 4)
        val code = PtpBytes.readU16(buf, 6)
        val tid = PtpBytes.readU32(buf, 8)

        val out = ByteArrayOutputStream()
        out.write(buf, Ptp.HEADER_LEN, first - Ptp.HEADER_LEN)
        var received = first.toLong()
        while (received < length) {
            val n = connection.bulkTransfer(bulkIn, buf, buf.size, TIMEOUT_MS)
            if (n <= 0) break
            out.write(buf, 0, n)
            received += n
        }
        return Container(length, type, code, tid, out.toByteArray())
    }

    private fun paramsToBytes(params: IntArray): ByteArray {
        val buf = ByteArray(params.size * 4)
        params.forEachIndexed { i, p -> PtpBytes.writeU32(buf, i * 4, p) }
        return buf
    }

    private fun paramsFromBytes(payload: ByteArray): LongArray {
        val n = payload.size / 4
        return LongArray(n) { PtpBytes.readU32(payload, it * 4) }
    }

    fun close() {
        try { connection.releaseInterface(iface) } catch (_: Exception) {}
    }
}
