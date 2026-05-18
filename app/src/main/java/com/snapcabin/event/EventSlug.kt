package com.snapcabin.event

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object EventSlug {

    private val SLUG_BAD = Regex("[^a-z0-9-]+")
    private val DASH_RUN = Regex("-+")

    /**
     * Produce a date-prefixed URL-safe slug:  "The Hewlett Wedding" → "2026-05-18-the-hewlett-wedding".
     * Date prefix ensures uniqueness when the same event name is reused across years.
     */
    fun from(name: String, at: Long = System.currentTimeMillis()): String {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(at))
        val cleaned = name.trim().lowercase(Locale.US)
            .replace(SLUG_BAD, "-")
            .replace(DASH_RUN, "-")
            .trim('-')
        return if (cleaned.isEmpty()) date else "$date-$cleaned"
    }
}
