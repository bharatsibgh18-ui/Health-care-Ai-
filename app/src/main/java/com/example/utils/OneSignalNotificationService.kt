package com.example.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

// =============================================================================
// OneSignal Push Notification Configuration Variables (BuildConfig + Settings)
// =============================================================================
val ONESIGNAL_APP_ID: String = BuildConfig.ONESIGNAL_APP_ID
val ONESIGNAL_REST_API_KEY: String = BuildConfig.ONESIGNAL_REST_API_KEY
const val ONESIGNAL_API_URL: String = "https://api.onesignal.com/notifications"

data class OneSignalConfig(
    val appId: String,
    val restApiKey: String,
    val targetExternalUserId: String = "",
    val enabled: Boolean = true
) {
    val isConfigured: Boolean
        get() = appId.isNotBlank() &&
            !appId.equals("MY_ONESIGNAL_APP_ID", ignoreCase = true) &&
            restApiKey.isNotBlank() &&
            !restApiKey.equals("MY_ONESIGNAL_REST_API_KEY", ignoreCase = true)
}

data class InAppFallbackAlert(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val message: String,
    val scheduledIsoTime: String? = null,
    val reason: String,
    val isOffline: Boolean = false
)

sealed interface OneSignalScheduleResult {
    data class Scheduled(
        val notificationId: String,
        val scheduledIsoTime: String?,
        val httpCode: Int
    ) : OneSignalScheduleResult

    data class OfflineFallback(
        val alert: InAppFallbackAlert
    ) : OneSignalScheduleResult

    data class NotConfiguredFallback(
        val alert: InAppFallbackAlert? = null
    ) : OneSignalScheduleResult

    data class ErrorFallback(
        val errorMessage: String,
        val alert: InAppFallbackAlert
    ) : OneSignalScheduleResult
}

object OneSignalNotificationService {

    private const val PREF_NAME = "onesignal_push_settings"
    private const val KEY_APP_ID = "onesignal_app_id"
    private const val KEY_REST_API_KEY = "onesignal_rest_api_key"
    private const val KEY_TARGET_EXTERNAL_ID = "onesignal_target_external_id"
    private const val KEY_ENABLED = "onesignal_enabled"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _pendingInAppAlert = MutableStateFlow<InAppFallbackAlert?>(null)
    val pendingInAppAlert: StateFlow<InAppFallbackAlert?> = _pendingInAppAlert.asStateFlow()

    private val _lastSyncStatus = MutableStateFlow<String?>(null)
    val lastSyncStatus: StateFlow<String?> = _lastSyncStatus.asStateFlow()

    internal var httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    internal var endpointUrl: String = ONESIGNAL_API_URL
    internal var connectivityOverrideForTesting: Boolean? = null

    fun dismissInAppAlert() {
        _pendingInAppAlert.value = null
    }

    fun showInAppFallbackAlert(alert: InAppFallbackAlert) {
        _pendingInAppAlert.value = alert
    }

    fun loadConfig(context: Context): OneSignalConfig {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val savedAppId = prefs.getString(KEY_APP_ID, null)?.trim().orEmpty()
        val savedRestKey = prefs.getString(KEY_REST_API_KEY, null)?.trim().orEmpty()
        val savedExtId = prefs.getString(KEY_TARGET_EXTERNAL_ID, null)?.trim().orEmpty()
        val enabled = prefs.getBoolean(KEY_ENABLED, true)

        val effectiveAppId = savedAppId.ifBlank {
            ONESIGNAL_APP_ID.takeUnless { it.equals("MY_ONESIGNAL_APP_ID", ignoreCase = true) }.orEmpty()
        }
        val effectiveRestKey = savedRestKey.ifBlank {
            ONESIGNAL_REST_API_KEY.takeUnless { it.equals("MY_ONESIGNAL_REST_API_KEY", ignoreCase = true) }.orEmpty()
        }

        return OneSignalConfig(
            appId = effectiveAppId,
            restApiKey = effectiveRestKey,
            targetExternalUserId = savedExtId,
            enabled = enabled
        )
    }

    fun saveConfig(
        context: Context,
        appId: String,
        restApiKey: String,
        targetExternalUserId: String = "",
        enabled: Boolean = true
    ): OneSignalConfig {
        val cleanAppId = appId.trim()
        val cleanKey = restApiKey.trim()
        val cleanExt = targetExternalUserId.trim()
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_APP_ID, cleanAppId)
            .putString(KEY_REST_API_KEY, cleanKey)
            .putString(KEY_TARGET_EXTERNAL_ID, cleanExt)
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
        val cfg = loadConfig(context)
        DevLogger.i("OneSignal", "Saved OneSignal settings (configured=${cfg.isConfigured}, enabled=${cfg.enabled})")
        return cfg
    }

    /**
     * Formats epoch milliseconds into ISO-8601 UTC format required by OneSignal `send_after`
     * (e.g., "2026-11-20T08:30:00Z").
     */
    fun formatIso8601Utc(epochMillis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(epochMillis))
    }

    fun isDeviceOnline(context: Context): Boolean {
        connectivityOverrideForTesting?.let { return it }
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN))
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Builds the exact JSON payload for POST https://api.onesignal.com/notifications.
     */
    fun buildOneSignalPayload(
        config: OneSignalConfig,
        title: String,
        message: String,
        scheduledAtMillis: Long? = null,
        extraData: Map<String, String> = emptyMap(),
        nowMillis: Long = System.currentTimeMillis()
    ): JSONObject {
        return JSONObject().apply {
            put("app_id", config.appId)
            put("target_channel", "push")
            if (config.targetExternalUserId.isNotBlank()) {
                put(
                    "include_aliases",
                    JSONObject().apply {
                        put("external_id", JSONArray().put(config.targetExternalUserId))
                    }
                )
            } else {
                put("included_segments", JSONArray().put("Subscribed Users"))
            }
            put("headings", JSONObject().apply { put("en", title) })
            put("contents", JSONObject().apply { put("en", message) })

            if (scheduledAtMillis != null && scheduledAtMillis > nowMillis + 5_000L) {
                put("send_after", formatIso8601Utc(scheduledAtMillis))
            }

            if (extraData.isNotEmpty()) {
                val dataObj = JSONObject()
                extraData.forEach { (k, v) -> dataObj.put(k, v) }
                if (scheduledAtMillis != null) {
                    dataObj.put("scheduled_iso", formatIso8601Utc(scheduledAtMillis))
                }
                put("data", dataObj)
            }
        }
    }

    /**
     * Sends a POST request to https://api.onesignal.com/notifications with `send_after` in ISO-8601 format.
     * Falls back to displaying an in-app alert if the user is offline or push delivery fails.
     */
    suspend fun scheduleOrSendPush(
        context: Context,
        title: String,
        message: String,
        scheduledAtMillis: Long? = null,
        extraData: Map<String, String> = emptyMap(),
        showInAppFallbackWhenUnconfigured: Boolean = false
    ): OneSignalScheduleResult = withContext(Dispatchers.IO) {
        val config = loadConfig(context)
        val now = System.currentTimeMillis()
        val scheduledIso = scheduledAtMillis?.takeIf { it > now + 5_000L }?.let { formatIso8601Utc(it) }

        if (!config.enabled) {
            return@withContext OneSignalScheduleResult.NotConfiguredFallback(null)
        }

        // Offline fallback: immediately surface in-app alert while local AlarmManager/WorkManager handles background firing
        if (!isDeviceOnline(context)) {
            val alert = InAppFallbackAlert(
                title = title,
                message = message,
                scheduledIsoTime = scheduledIso,
                reason = "Offline mode: In-app alert & local device background alarm active (OneSignal cloud push unavailable offline).",
                isOffline = true
            )
            _pendingInAppAlert.value = alert
            _lastSyncStatus.value = "Offline fallback alert shown"
            DevLogger.w("OneSignal", "Device offline; triggered in-app fallback alert for '$title'")
            return@withContext OneSignalScheduleResult.OfflineFallback(alert)
        }

        if (!config.isConfigured) {
            val alert = if (showInAppFallbackWhenUnconfigured) {
                InAppFallbackAlert(
                    title = title,
                    message = message,
                    scheduledIsoTime = scheduledIso,
                    reason = "OneSignal keys not yet set — showing in-app alert & local device alarm. Add ONESIGNAL_APP_ID & ONESIGNAL_REST_API_KEY in Profile > OneSignal Settings.",
                    isOffline = false
                ).also { _pendingInAppAlert.value = it }
            } else {
                null
            }
            _lastSyncStatus.value = "Local background alarm active (OneSignal keys not configured)"
            return@withContext OneSignalScheduleResult.NotConfiguredFallback(alert)
        }

        val payload = buildOneSignalPayload(
            config = config,
            title = title,
            message = message,
            scheduledAtMillis = scheduledAtMillis,
            extraData = extraData,
            nowMillis = now
        )

        val request = Request.Builder()
            .url(endpointUrl)
            .addHeader("Authorization", "Key ${config.restApiKey}")
            .addHeader("Content-Type", "application/json; charset=utf-8")
            .addHeader("Accept", "application/json")
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        return@withContext try {
            httpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val notifId = try {
                        JSONObject(bodyStr).optString("id", "scheduled")
                    } catch (_: Exception) {
                        "scheduled"
                    }
                    val statusMsg = if (scheduledIso != null) {
                        "OneSignal push scheduled ($scheduledIso)"
                    } else {
                        "OneSignal push sent (ID: $notifId)"
                    }
                    _lastSyncStatus.value = statusMsg
                    DevLogger.i("OneSignal", "$statusMsg [HTTP ${response.code}]")
                    OneSignalScheduleResult.Scheduled(
                        notificationId = notifId,
                        scheduledIsoTime = scheduledIso,
                        httpCode = response.code
                    )
                } else {
                    val errSummary = "HTTP ${response.code}: ${bodyStr.take(160)}"
                    val alert = InAppFallbackAlert(
                        title = title,
                        message = message,
                        scheduledIsoTime = scheduledIso,
                        reason = "OneSignal delivery error ($errSummary) — local background alarm & in-app alert active.",
                        isOffline = false
                    )
                    _pendingInAppAlert.value = alert
                    _lastSyncStatus.value = "OneSignal error: HTTP ${response.code}"
                    DevLogger.w("OneSignal", "OneSignal REST POST failed: $errSummary")
                    OneSignalScheduleResult.ErrorFallback(errSummary, alert)
                }
            }
        } catch (e: IOException) {
            val alert = InAppFallbackAlert(
                title = title,
                message = message,
                scheduledIsoTime = scheduledIso,
                reason = "Network offline/interrupted (${e.message ?: "IO error"}) — showing in-app fallback alert & local alarm.",
                isOffline = true
            )
            _pendingInAppAlert.value = alert
            _lastSyncStatus.value = "Offline fallback alert shown"
            DevLogger.w("OneSignal", "OneSignal IOException; triggered offline in-app fallback: ${e.message}")
            OneSignalScheduleResult.OfflineFallback(alert)
        } catch (e: Exception) {
            val errMsg = e.message ?: "Unexpected OneSignal error"
            val alert = InAppFallbackAlert(
                title = title,
                message = message,
                scheduledIsoTime = scheduledIso,
                reason = "Push fallback ($errMsg) — local background alarm & in-app alert active.",
                isOffline = false
            )
            _pendingInAppAlert.value = alert
            _lastSyncStatus.value = "Push fallback: $errMsg"
            OneSignalScheduleResult.ErrorFallback(errMsg, alert)
        }
    }

    /**
     * Fire-and-forget wrapper for background reminder schedulers (Medicine, Snooze, Water).
     */
    fun schedulePushAsync(
        context: Context,
        title: String,
        message: String,
        scheduledAtMillis: Long? = null,
        extraData: Map<String, String> = emptyMap(),
        showInAppFallbackWhenUnconfigured: Boolean = false
    ) {
        val appCtx = context.applicationContext
        backgroundScope.launch {
            scheduleOrSendPush(
                context = appCtx,
                title = title,
                message = message,
                scheduledAtMillis = scheduledAtMillis,
                extraData = extraData,
                showInAppFallbackWhenUnconfigured = showInAppFallbackWhenUnconfigured
            )
        }
    }
}
