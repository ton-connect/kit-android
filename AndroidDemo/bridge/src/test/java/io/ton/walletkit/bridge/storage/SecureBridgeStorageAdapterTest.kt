package io.ton.walletkit.bridge.storage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.ton.walletkit.storage.bridge.BridgeStorageAdapter
import io.ton.walletkit.storage.bridge.SecureBridgeStorageAdapter
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Unit tests for SecureBridgeStorageAdapter.
 * 
 * Tests verify:
 * - Basic CRUD operations (Create, Read, Update, Delete)
 * - Data persistence and encryption
 * - Error handling
 * - Clear functionality
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28]) // Android 9.0 for StrongBox support
class SecureBridgeStorageAdapterTest {

    private lateinit var context: Context
    private lateinit var storage: BridgeStorageAdapter

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        storage = SecureBridgeStorageAdapter(context)
    }

    @After
    fun tearDown() = runTest {
        // Clean up all test data
        storage.clear()
    }

    @Test
    fun `get returns null for non-existent key`() = runTest {
        val result = storage.get("non_existent_key")
        assertNull(result, "Non-existent key should return null")
    }

    @Test
    fun `set and get round-trip works`() = runTest {
        val key = "test_key"
        val value = "test_value"

        storage.set(key, value)
        val retrieved = storage.get(key)

        assertEquals(value, retrieved, "Retrieved value should match stored value")
    }

    @Test
    fun `set and get with JSON data works`() = runTest {
        val key = "wallet_metadata"
        val jsonValue = """{"address":"EQD...","publicKey":"abc123","version":"v4R2"}"""

        storage.set(key, jsonValue)
        val retrieved = storage.get(key)

        assertEquals(jsonValue, retrieved, "Retrieved JSON should match stored JSON")
    }

    @Test
    fun `set overwrites existing value`() = runTest {
        val key = "overwrite_test"
        val value1 = "first_value"
        val value2 = "second_value"

        storage.set(key, value1)
        storage.set(key, value2)
        val retrieved = storage.get(key)

        assertEquals(value2, retrieved, "Retrieved value should be the second value")
    }

    @Test
    fun `remove deletes key`() = runTest {
        val key = "remove_test"
        val value = "value_to_remove"

        storage.set(key, value)
        assertNotNull(storage.get(key), "Value should exist before removal")

        storage.remove(key)
        assertNull(storage.get(key), "Value should be null after removal")
    }

    @Test
    fun `remove non-existent key does not throw`() = runTest {
        // Should not throw an exception
        storage.remove("non_existent_key")
    }

    @Test
    fun `clear removes all bridge keys`() = runTest {
        // Add multiple keys
        storage.set("key1", "value1")
        storage.set("key2", "value2")
        storage.set("key3", "value3")

        // Verify they exist
        assertNotNull(storage.get("key1"))
        assertNotNull(storage.get("key2"))
        assertNotNull(storage.get("key3"))

        // Clear all
        storage.clear()

        // Verify they're gone
        assertNull(storage.get("key1"))
        assertNull(storage.get("key2"))
        assertNull(storage.get("key3"))
    }

    @Test
    fun `handles empty string value`() = runTest {
        val key = "empty_string_test"
        val value = ""

        storage.set(key, value)
        val retrieved = storage.get(key)

        assertEquals(value, retrieved, "Empty string should be stored and retrieved")
    }

    @Test
    fun `handles large value`() = runTest {
        val key = "large_value_test"
        val value = "x".repeat(10000) // 10KB of data

        storage.set(key, value)
        val retrieved = storage.get(key)

        assertEquals(value, retrieved, "Large value should be stored and retrieved")
    }

    @Test
    fun `handles special characters in key`() = runTest {
        val key = "bridge:wallets:special-chars_123"
        val value = "test_value"

        storage.set(key, value)
        val retrieved = storage.get(key)

        assertEquals(value, retrieved, "Key with special characters should work")
    }

    @Test
    fun `handles special characters in value`() = runTest {
        val key = "special_chars_test"
        val value = "Special: !@#$%^&*(){}[]|\\:\";<>?,./~`"

        storage.set(key, value)
        val retrieved = storage.get(key)

        assertEquals(value, retrieved, "Value with special characters should work")
    }

    @Test
    fun `handles unicode in value`() = runTest {
        val key = "unicode_test"
        val value = "Hello 世界 🌍 مرحبا"

        storage.set(key, value)
        val retrieved = storage.get(key)

        assertEquals(value, retrieved, "Unicode value should be stored and retrieved")
    }

    @Test
    fun `multiple operations maintain data integrity`() = runTest {
        // Simulate realistic usage pattern
        storage.set("wallets", """[{"address":"EQD1"}]""")
        storage.set("sessions", """[{"sessionId":"abc"}]""")
        storage.set("config", """{"network":"testnet"}""")

        // Verify all exist
        assertNotNull(storage.get("wallets"))
        assertNotNull(storage.get("sessions"))
        assertNotNull(storage.get("config"))

        // Update one
        storage.set("wallets", """[{"address":"EQD1"},{"address":"EQD2"}]""")

        // Verify others unchanged
        assertEquals("""[{"sessionId":"abc"}]""", storage.get("sessions"))
        assertEquals("""{"network":"testnet"}""", storage.get("config"))

        // Verify updated one
        assertEquals(
            """[{"address":"EQD1"},{"address":"EQD2"}]""",
            storage.get("wallets")
        )
    }

    @Test
    fun `storage persists across adapter instances`() = runTest {
        val key = "persistence_test"
        val value = "persistent_value"

        // Store with first instance
        storage.set(key, value)

        // Create new instance
        val newStorage = SecureBridgeStorageAdapter(context)
        val retrieved = newStorage.get(key)

        assertEquals(value, retrieved, "Value should persist across adapter instances")

        // Cleanup
        newStorage.clear()
    }

    @Test
    fun `handles concurrent operations`() = runTest {
        val key = "concurrent_test"

        // Simulate concurrent writes (not truly concurrent due to runTest)
        storage.set(key, "value1")
        storage.set(key, "value2")
        storage.set(key, "value3")

        // Should have the last value
        assertEquals("value3", storage.get(key))
    }
}
