package com.snapcabin.event

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.util.Locale
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** Durable per-event reservations, independent of guest screens and the capped audit log.
 * Recipient keys are keyed hashes, never raw addresses. A reservation is written BEFORE
 * sending, so killing the app during a request cannot reset the limit. Explicit failures
 * release it; an interrupted/unknown delivery conservatively keeps its slot.
 */
class EventEmailLimiter(private val store: DataStore<Preferences>) {
    data class Reservation internal constructor(val event: String, val recipientKey: String)

    suspend fun reserve(event: String, address: String, limit: Int): Reservation? {
        var reservation: Reservation? = null
        store.edit { prefs ->
            if (prefs[EVENT] != event) {
                prefs.clear()
                prefs[EVENT] = event
                prefs[SALT] = UUID.randomUUID().toString()
            }
            val salt = prefs[SALT] ?: UUID.randomUUID().toString().also { prefs[SALT] = it }
            val key = recipientKey(address, salt)
            val countKey = intPreferencesKey(key)
            val count = prefs[countKey] ?: 0
            if (count < limit.coerceAtLeast(1)) {
                prefs[countKey] = count + 1
                reservation = Reservation(event, key)
            }
        }
        return reservation
    }

    suspend fun release(reservation: Reservation) {
        store.edit { prefs ->
            // A late failure from the previous event must never alter the new event.
            if (prefs[EVENT] == reservation.event) {
                val key = intPreferencesKey(reservation.recipientKey)
                val count = prefs[key] ?: 0
                if (count <= 1) prefs.remove(key) else prefs[key] = count - 1
            }
        }
    }

    private fun recipientKey(address: String, salt: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(salt.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return "recipient_" + mac.doFinal(
            address.trim().lowercase(Locale.ROOT).toByteArray(Charsets.UTF_8)
        ).joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    companion object {
        private val EVENT = stringPreferencesKey("event")
        private val SALT = stringPreferencesKey("salt")
    }
}
