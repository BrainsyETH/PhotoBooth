package com.snapcabin.event

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class EventEmailLimiterTest {
    @get:Rule val temporary = TemporaryFolder()
    private val jobs = mutableListOf<Job>()

    private fun store(file: File) = PreferenceDataStoreFactory.create(
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob().also { jobs += it }),
        produceFile = { file }
    )

    @After fun closeStores() = runBlocking { jobs.forEach { it.cancelAndJoin() } }

    @Test fun `limits survive new guest screens and a process restart`() = runBlocking {
        val file = File(temporary.root, "limits.preferences_pb")
        val firstStore = store(file)
        assertNotNull(EventEmailLimiter(firstStore).reserve("event:1", "guest@example.com", 2))
        assertNotNull(EventEmailLimiter(firstStore).reserve("event:1", "guest@example.com", 2))
        jobs.last().cancelAndJoin()
        val reopened = EventEmailLimiter(store(file))
        assertNull(reopened.reserve("event:1", "guest@example.com", 2))
    }

    @Test fun `concurrent sends reserve no more than the cap`() = runBlocking {
        val limiter = EventEmailLimiter(store(File(temporary.root, "limits.preferences_pb")))
        val reservations = (1..20).map {
            async(Dispatchers.Default) { limiter.reserve("event:1", "guest@example.com", 3) }
        }.awaitAll()
        assertEquals(3, reservations.count { it != null })
    }

    @Test fun `address case and whitespace cannot bypass limits and raw addresses are not stored`() = runBlocking {
        val data = store(File(temporary.root, "limits.preferences_pb"))
        val limiter = EventEmailLimiter(data)
        assertNotNull(limiter.reserve("event:1", " Guest@Example.com ", 1))
        assertNull(limiter.reserve("event:1", "guest@example.com", 1))
        assertFalse(data.data.first().asMap().toString().contains("example.com", ignoreCase = true))
        assertNotNull(limiter.reserve("event:1", "other@example.com", 1))
    }

    @Test fun `failed delivery releases its reservation`() = runBlocking {
        val limiter = EventEmailLimiter(store(File(temporary.root, "limits.preferences_pb")))
        val reservation = requireNotNull(limiter.reserve("event:1", "guest@example.com", 1))
        assertNull(limiter.reserve("event:1", "guest@example.com", 1))
        limiter.release(reservation)
        assertNotNull(limiter.reserve("event:1", "guest@example.com", 1))
    }

    @Test fun `new event resets limits and old failures cannot reduce its count`() = runBlocking {
        val limiter = EventEmailLimiter(store(File(temporary.root, "limits.preferences_pb")))
        val old = requireNotNull(limiter.reserve("wedding:1", "guest@example.com", 1))
        // Same event name, newly started event.
        assertNotNull(limiter.reserve("wedding:2", "guest@example.com", 1))
        limiter.release(old)
        assertNull(limiter.reserve("wedding:2", "guest@example.com", 1))
    }
}
