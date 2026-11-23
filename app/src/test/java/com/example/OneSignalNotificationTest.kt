package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.utils.ONESIGNAL_API_URL
import com.example.utils.OneSignalConfig
import com.example.utils.OneSignalNotificationService
import com.example.utils.OneSignalScheduleResult
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OneSignalNotificationTest {

    private lateinit var context: Context
    private lateinit var originalClient: OkHttpClient
    private lateinit var originalUrl: String

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        originalClient = OneSignalNotificationService.httpClient
        originalUrl = OneSignalNotificationService.endpointUrl
        OneSignalNotificationService.dismissInAppAlert()
        OneSignalNotificationService.connectivityOverrideForTesting = null
    }

    @After
    fun tearDown() {
        OneSignalNotificationService.httpClient = originalClient
        OneSignalNotificationService.endpointUrl = originalUrl
        OneSignalNotificationService.connectivityOverrideForTesting = null
        OneSignalNotificationService.dismissInAppAlert()
    }

    @Test
    fun `formatIso8601Utc produces valid ISO-8601 UTC timestamp for OneSignal send_after`() {
        // 2026-11-20T08:30:00Z
        val iso = OneSignalNotificationService.formatIso8601Utc(1795163400000L)
        assertTrue(iso.matches(Regex("""\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z""")))
    }

    @Test
    fun `scheduleOrSendPush posts exact ISO send_after payload to OneSignal REST API`() = runBlocking {
        OneSignalNotificationService.saveConfig(
            context = context,
            appId = "11111111-2222-3333-4444-555555555555",
            restApiKey = "test_rest_key_xyz",
            targetExternalUserId = "",
            enabled = true
        )
        OneSignalNotificationService.connectivityOverrideForTesting = true

        var capturedRequest: Request? = null
        var capturedBodyText = ""
        OneSignalNotificationService.httpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request()
                capturedRequest = req
                val buf = Buffer()
                req.body?.writeTo(buf)
                capturedBodyText = buf.readUtf8()
                Response.Builder()
                    .request(req)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("""{"id":"os-notif-12345"}""".toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val futureTime = System.currentTimeMillis() + 10 * 60_000L
        val expectedIso = OneSignalNotificationService.formatIso8601Utc(futureTime)

        val result = OneSignalNotificationService.scheduleOrSendPush(
            context = context,
            title = "Medicine Reminder",
            message = "Take Vitamin D3 1 Tablet",
            scheduledAtMillis = futureTime,
            extraData = mapOf("reminder_type" to "medicine")
        )

        assertTrue(result is OneSignalScheduleResult.Scheduled)
        val scheduled = result as OneSignalScheduleResult.Scheduled
        assertEquals("os-notif-12345", scheduled.notificationId)
        assertEquals(expectedIso, scheduled.scheduledIsoTime)

        assertNotNull(capturedRequest)
        assertEquals("POST", capturedRequest!!.method)
        assertEquals(ONESIGNAL_API_URL, capturedRequest!!.url.toString())
        assertEquals("Key test_rest_key_xyz", capturedRequest!!.header("Authorization"))

        val json = JSONObject(capturedBodyText)
        assertEquals("11111111-2222-3333-4444-555555555555", json.getString("app_id"))
        assertEquals(expectedIso, json.getString("send_after"))
        assertEquals("Medicine Reminder", json.getJSONObject("headings").getString("en"))
        assertEquals("Take Vitamin D3 1 Tablet", json.getJSONObject("contents").getString("en"))
    }

    @Test
    fun `offline mode triggers in-app fallback alert instead of failing silently`() = runBlocking {
        OneSignalNotificationService.saveConfig(
            context = context,
            appId = "11111111-2222-3333-4444-555555555555",
            restApiKey = "test_rest_key_xyz",
            enabled = true
        )
        OneSignalNotificationService.connectivityOverrideForTesting = false

        val futureTime = System.currentTimeMillis() + 5 * 60_000L
        val result = OneSignalNotificationService.scheduleOrSendPush(
            context = context,
            title = "Water Reminder",
            message = "Drink 250ml water now",
            scheduledAtMillis = futureTime
        )

        assertTrue(result is OneSignalScheduleResult.OfflineFallback)
        val pendingAlert = OneSignalNotificationService.pendingInAppAlert.value
        assertNotNull(pendingAlert)
        assertTrue(pendingAlert!!.isOffline)
        assertEquals("Water Reminder", pendingAlert.title)
        assertEquals("Drink 250ml water now", pendingAlert.message)
        assertNotNull(pendingAlert.scheduledIsoTime)
    }

    @Test
    fun `payload uses include_aliases when targetExternalUserId is configured`() {
        val cfg = OneSignalConfig(
            appId = "app-id-99",
            restApiKey = "rest-key-99",
            targetExternalUserId = "patient_raj",
            enabled = true
        )
        val payload = OneSignalNotificationService.buildOneSignalPayload(
            config = cfg,
            title = "Appointment",
            message = "Doctor visit at 5 PM",
            scheduledAtMillis = null
        )
        assertFalse(payload.has("send_after"))
        assertTrue(payload.has("include_aliases"))
        assertEquals(
            "patient_raj",
            payload.getJSONObject("include_aliases").getJSONArray("external_id").getString(0)
        )
    }
}
