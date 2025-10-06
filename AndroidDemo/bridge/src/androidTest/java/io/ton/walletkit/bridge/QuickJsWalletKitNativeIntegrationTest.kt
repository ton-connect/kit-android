package io.ton.walletkit.bridge

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.ton.walletkit.quickjs.QuickJs
import org.json.JSONException
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private class RecordingNativeBridge {
    val messages = mutableListOf<String>()

    fun postMessage(json: String) {
        messages += json
    }
}

@RunWith(AndroidJUnit4::class)
class QuickJsWalletKitNativeIntegrationTest {
    private lateinit var quickJs: QuickJs
    private lateinit var nativeBridge: RecordingNativeBridge

    @Before
    fun setUp() {
        InstrumentationRegistry.getInstrumentation()
        quickJs = QuickJs.create()
        nativeBridge = RecordingNativeBridge()
        quickJs.set("WalletKitNative", RecordingNativeBridge::class.java, nativeBridge)
    }

    @After
    fun tearDown() {
        quickJs.close()
    }

    @Test
    fun walletKitNativePostMessageDeliversPayload() {
        val result = quickJs.evaluate(
            """
            (function() {
              const payload = { kind: 'ready', value: 42 };
              WalletKitNative.postMessage(JSON.stringify(payload));
              return payload.kind;
            })();
            """.trimIndent(),
            "walletkit-native-postmessage.js",
        ) as String

        assertEquals("ready", result)
        assertEquals(1, nativeBridge.messages.size)

        val payload = JSONObject(nativeBridge.messages.single())
        assertEquals("ready", payload.getString("kind"))
        assertEquals(42, payload.getInt("value"))
    }

    @Test
    fun microtaskIsDrainedByExecutePendingJob() {
        quickJs.evaluate(
            """
            (function() {
              Promise.resolve().then(function() {
                WalletKitNative.postMessage(JSON.stringify({ kind: 'ready', source: 'microtask' }));
              });
            })();
            """.trimIndent(),
            "walletkit-native-microtask.js",
        )

        assertTrue("message should be queued until jobs are executed", nativeBridge.messages.isEmpty())

        var totalJobs = 0
        while (true) {
            val processed = quickJs.executePendingJob()
            if (processed <= 0) {
                break
            }
            totalJobs += processed
        }

        assertTrue("expected QuickJS to process at least one pending job", totalJobs > 0)
        assertEquals(1, nativeBridge.messages.size)

        val payload = JSONObject(nativeBridge.messages.single())
        assertEquals("ready", payload.getString("kind"))
        assertEquals("microtask", payload.getString("source"))
    }

    @Test
    fun walletKitNativePostMessageWithJsonNumberIsNotParsableAsObject() {
        quickJs.evaluate(
            """
            (function() {
              WalletKitNative.postMessage(JSON.stringify(32));
            })();
            """.trimIndent(),
            "walletkit-native-number.js",
        )

        assertEquals(1, nativeBridge.messages.size)
        val message = nativeBridge.messages.single()
        assertEquals("32", message)

        var thrown: JSONException? = null
        try {
            JSONObject(message)
        } catch (err: JSONException) {
            thrown = err
        }

        assertNotNull("JSONObject should reject numeric payload", thrown)
        assertTrue(
            "Unexpected error message: ${'$'}{thrown?.message}",
            thrown!!.message?.contains("Value 32") == true,
        )
    }
}
