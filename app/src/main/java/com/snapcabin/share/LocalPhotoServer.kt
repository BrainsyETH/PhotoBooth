package com.snapcabin.share

import android.graphics.Bitmap
import fi.iki.elonen.NanoHTTPD
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalPhotoServer @Inject constructor() : NanoHTTPD(8080) {

    private var currentPhoto: ByteArray? = null

    fun servePhoto(bitmap: Bitmap) {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
        currentPhoto = stream.toByteArray()

        if (!isAlive) {
            start(SOCKET_READ_TIMEOUT, false)
        }
    }

    fun stopServing() {
        currentPhoto = null
        if (isAlive) {
            stop()
        }
    }

    /**
     * URL we put in the QR code. Points at the HTML viewer page so a guest's
     * browser shows the photo inline with a tap-to-save link instead of
     * triggering an immediate file download for the bare .jpg endpoint.
     */
    fun getPhotoUrl(localIp: String): String {
        return "http://$localIp:8080/"
    }

    override fun serve(session: IHTTPSession): Response {
        val photo = currentPhoto
        return if (photo != null && session.uri == "/photo.jpg") {
            newFixedLengthResponse(
                Response.Status.OK,
                "image/jpeg",
                ByteArrayInputStream(photo),
                photo.size.toLong()
            )
        } else if (photo != null && session.uri == "/") {
            val html = """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>Your SnapCabin photo</title>
                  <style>
                    body { margin: 0; background: #121212; color: #FAF5EA; font-family: -apple-system, system-ui, sans-serif; }
                    .wrap { min-height: 100vh; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 16px; box-sizing: border-box; }
                    img { max-width: 100%; max-height: 78vh; object-fit: contain; border-radius: 8px; box-shadow: 0 4px 24px rgba(0,0,0,0.4); }
                    .save { margin-top: 18px; display: inline-block; padding: 12px 22px; background: #6B8F73; color: #FDFAF1; border-radius: 999px; text-decoration: none; font-weight: 600; font-size: 16px; }
                    .hint { margin-top: 10px; font-size: 12px; color: #B5A892; }
                  </style>
                </head>
                <body>
                  <div class="wrap">
                    <img src="/photo.jpg" alt="Your photo">
                    <a class="save" href="/photo.jpg" download="snapcabin-photo.jpg">Save photo</a>
                    <p class="hint">Or long-press the photo and choose Save image.</p>
                  </div>
                </body>
                </html>
            """.trimIndent()
            newFixedLengthResponse(Response.Status.OK, "text/html", html)
        } else {
            newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found")
        }
    }
}
