package com.snapcabin.event

import org.json.JSONArray
import org.json.JSONObject

/**
 * Lightweight append-only audit log of every guest-facing send (SMS/email).
 * Stored as JSON in DataStore so we don't need Room for ~500 entries.
 * Recipients are masked at write time so the log itself isn't a PII reservoir.
 */
data class SendLogEntry(
    val timestampMs: Long,
    val eventSlug: String,
    val channel: String,         // "sms" | "email" | "intent" | "print"
    val recipientMasked: String, // e.g. "+1***-***-5678" or "e***@example.com"
    val status: String,          // "ok" | "err"
    val note: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("t", timestampMs)
        put("e", eventSlug)
        put("c", channel)
        put("r", recipientMasked)
        put("s", status)
        if (note.isNotEmpty()) put("n", note)
    }
}

object SendLog {

    const val MAX_ENTRIES = 500

    fun parse(json: String): List<SendLogEntry> = try {
        val arr = JSONArray(json)
        buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    SendLogEntry(
                        timestampMs = o.optLong("t"),
                        eventSlug = o.optString("e"),
                        channel = o.optString("c"),
                        recipientMasked = o.optString("r"),
                        status = o.optString("s"),
                        note = o.optString("n", "")
                    )
                )
            }
        }
    } catch (_: Exception) {
        emptyList()
    }

    fun serialize(entries: List<SendLogEntry>): String {
        val capped = if (entries.size > MAX_ENTRIES) entries.takeLast(MAX_ENTRIES) else entries
        val arr = JSONArray()
        capped.forEach { arr.put(it.toJson()) }
        return arr.toString()
    }

    fun append(currentJson: String, entry: SendLogEntry): String =
        serialize(parse(currentJson) + entry)

    /** Mask a phone number to its last 4 digits: "+15551234567" -> "+1***-***-4567". */
    fun maskPhone(e164: String): String {
        val digits = e164.filter { it.isDigit() }
        if (digits.length < 4) return "***"
        val last4 = digits.takeLast(4)
        val prefix = if (e164.startsWith("+1") && digits.length == 11) "+1" else "+"
        return "$prefix***-***-$last4"
    }

    /** Mask an email's local part: "evan@example.com" -> "e***@example.com". */
    fun maskEmail(address: String): String {
        val at = address.indexOf('@')
        if (at <= 1) return "***"
        return "${address[0]}***${address.substring(at)}"
    }
}
