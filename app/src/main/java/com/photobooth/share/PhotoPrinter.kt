package com.photobooth.share

import android.content.Context
import android.graphics.Bitmap
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintDocumentInfo
import android.graphics.pdf.PdfDocument
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoPrinter @Inject constructor() {

    companion object {
        private const val TAG = "PhotoPrinter"
    }

    fun print(context: Context, bitmap: Bitmap, jobName: String = "PhotoBooth Print") {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            Log.e(TAG, "PrintManager not available")
            return
        }

        val adapter = PhotoPrintAdapter(bitmap, jobName)
        printManager.print(jobName, adapter, PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A6)
            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()
        )
    }

    /**
     * Print with specific paper size for common photo booth sizes
     */
    fun printPhotoSize(context: Context, bitmap: Bitmap, size: PhotoPrintSize) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            Log.e(TAG, "PrintManager not available")
            return
        }

        val adapter = PhotoPrintAdapter(bitmap, "PhotoBooth - ${size.label}")
        printManager.print("PhotoBooth Print", adapter, PrintAttributes.Builder()
            .setMediaSize(size.mediaSize)
            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()
        )
    }
}

enum class PhotoPrintSize(val label: String, val mediaSize: PrintAttributes.MediaSize) {
    SIZE_4X6("4x6\"", PrintAttributes.MediaSize.NA_INDEX_4X6),
    SIZE_5X7("5x7\"", PrintAttributes.MediaSize.NA_INDEX_5X8), // Closest standard
    SIZE_A6("A6", PrintAttributes.MediaSize.ISO_A6),
    SIZE_A5("A5", PrintAttributes.MediaSize.ISO_A5),
    SIZE_LETTER("Letter", PrintAttributes.MediaSize.NA_LETTER)
}

private class PhotoPrintAdapter(
    private val bitmap: Bitmap,
    private val jobName: String
) : PrintDocumentAdapter() {

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal,
        callback: LayoutResultCallback,
        extras: Bundle?
    ) {
        if (cancellationSignal.isCanceled) {
            callback.onLayoutCancelled()
            return
        }

        val info = PrintDocumentInfo.Builder(jobName)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_PHOTO)
            .setPageCount(1)
            .build()

        callback.onLayoutFinished(info, newAttributes != oldAttributes)
    }

    override fun onWrite(
        pages: Array<out PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal,
        callback: WriteResultCallback
    ) {
        if (cancellationSignal.isCanceled) {
            callback.onWriteCancelled()
            return
        }

        try {
            val pdfDocument = PdfDocument()

            // Calculate page dimensions to fit photo
            val pageWidth = 432 // 6 inches at 72 DPI
            val pageHeight = 288 // 4 inches at 72 DPI (landscape)

            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 0).create()
            val page = pdfDocument.startPage(pageInfo)

            val canvas = page.canvas

            // Scale bitmap to fit page, maintaining aspect ratio
            val bitmapAspect = bitmap.width.toFloat() / bitmap.height
            val pageAspect = pageWidth.toFloat() / pageHeight

            val drawWidth: Float
            val drawHeight: Float
            if (bitmapAspect > pageAspect) {
                drawWidth = pageWidth.toFloat()
                drawHeight = pageWidth / bitmapAspect
            } else {
                drawHeight = pageHeight.toFloat()
                drawWidth = pageHeight * bitmapAspect
            }

            val left = (pageWidth - drawWidth) / 2
            val top = (pageHeight - drawHeight) / 2

            val scaledBitmap = Bitmap.createScaledBitmap(
                bitmap,
                drawWidth.toInt(),
                drawHeight.toInt(),
                true
            )
            canvas.drawBitmap(scaledBitmap, left, top, null)

            pdfDocument.finishPage(page)

            pdfDocument.writeTo(java.io.FileOutputStream(destination.fileDescriptor))
            pdfDocument.close()

            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
            Log.e("PhotoPrintAdapter", "Failed to write print document", e)
            callback.onWriteFailed(e.message)
        }
    }
}
