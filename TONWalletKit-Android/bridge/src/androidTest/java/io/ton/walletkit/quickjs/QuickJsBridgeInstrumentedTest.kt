package io.ton.walletkit.quickjs

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuickJsBridgeInstrumentedTest {
    private lateinit var quickJs: QuickJs

    @Before
    fun setUp() {
        InstrumentationRegistry.getInstrumentation() // Ensure instrumentation is ready
        quickJs = QuickJs.create()
    }

    @After
    fun tearDown() {
        quickJs.close()
    }

    @Test
    fun evaluateExpression() {
        val result = quickJs.evaluate("2 + 2", "test.js") as Double
        assertEquals(4.0, result, 0.0)
    }

    @Test
    fun evaluateStringResult() {
        val result = quickJs.evaluate("['he', 'llo'].join('')", "string-result.js") as String
        assertEquals("hello", result)
    }

    @Test
    fun nullRoundTrip() {
        val raw = quickJs.evaluate("(function() { return null; })();", "pure-null.js")
        val normalized = when (raw) {
            null -> null
            is Number -> if (raw.toDouble() == 0.0) null else raw
            is String -> raw.ifEmpty { null }
            else -> raw
        }
        assertNull(normalized)
    }
}
