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

    fun getPhotoUrl(localIp: String): String {
        return "http://$localIp:8080/photo.jpg"
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
                <head><meta name="viewport" content="width=device-width, initial-scale=1">
                <style>body{margin:0;background:#121212;display:flex;justify-content:center;align-items:center;min-height:100vh}
                img{max-width:100%;max-height:100vh;object-fit:contain}</style></head>
                <body><img src="/photo.jpg" alt="Photo"></body>
                </html>
            """.trimIndent()
            newFixedLengthResponse(Response.Status.OK, "text/html", html)
        } else {
            newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found")
        }
    }
}
