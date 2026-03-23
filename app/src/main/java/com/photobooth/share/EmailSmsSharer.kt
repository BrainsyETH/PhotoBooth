package com.photobooth.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmailSmsSharer @Inject constructor() {

    companion object {
        private const val TAG = "EmailSmsSharer"
    }

    fun shareViaEmail(
        context: Context,
        bitmap: Bitmap,
        recipientEmail: String = "",
        subject: String = "Your PhotoBooth Photo",
        body: String = "Here's your photo from the photo booth!"
    ) {
        try {
            val file = saveTempFile(context, bitmap)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Send via Email"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share via email", e)
        }
    }

    fun shareViaSms(
        context: Context,
        bitmap: Bitmap,
        phoneNumber: String = "",
        message: String = "Check out my photo from the photo booth!"
    ) {
        try {
            val file = saveTempFile(context, bitmap)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra("sms_body", message)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (phoneNumber.isNotBlank()) {
                    data = Uri.parse("smsto:$phoneNumber")
                }
                setPackage(null) // Let user choose messaging app
            }
            context.startActivity(Intent.createChooser(intent, "Send via Message"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share via SMS", e)
        }
    }

    /**
     * Open native sharing (AirDrop equivalent for Android - Nearby Share)
     */
    fun shareViaNearbyShare(context: Context, bitmap: Bitmap) {
        try {
            val file = saveTempFile(context, bitmap)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Photo"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share via nearby", e)
        }
    }

    private fun saveTempFile(context: Context, bitmap: Bitmap): File {
        val file = File(context.cacheDir, "email_share_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
        }
        return file
    }
}
