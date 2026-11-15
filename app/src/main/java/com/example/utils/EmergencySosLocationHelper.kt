package com.example.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

object EmergencySosLocationHelper {

    /**
     * Checks if fine location and SMS permissions are granted.
     */
    fun hasRequiredPermissions(context: Context): Boolean {
        val hasFineLoc = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasSms = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        return hasFineLoc && hasSms
    }

    /**
     * Obtains the live GPS location coordinates using FusedLocationProviderClient.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Pair<Double, Double>? {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return null

        val fusedClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(context)

        return try {
            val cts = CancellationTokenSource()
            val location = fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token).await()
                ?: fusedClient.lastLocation.await()

            if (location != null) {
                Pair(location.latitude, location.longitude)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generates a Google Maps live location link.
     */
    fun buildGoogleMapsLink(latitude: Double, longitude: Double): String {
        return "https://maps.google.com/?q=$latitude,$longitude"
    }

    /**
     * Dispatches an emergency SOS SMS to the emergency contact with location link.
     * Falls back to opening the SMS composer if direct background SMS permission is not available.
     */
    fun sendSosSms(
        context: Context,
        phoneNumber: String,
        patientName: String,
        mapsLink: String?
    ): Boolean {
        val cleanNumber = phoneNumber.replace(" ", "").trim()
        if (cleanNumber.isBlank()) return false

        val locationText = if (mapsLink != null) {
            "\n📍 Live Location: $mapsLink"
        } else {
            "\n(GPS location could not be acquired)"
        }

        val message = "🚨 EMERGENCY SOS ALERT! $patientName is in critical distress and triggered an Emergency Red Flag alert.$locationText\nPlease assist immediately or send medical help!"

        val hasSmsPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        return if (hasSmsPermission) {
            try {
                val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }

                val parts = smsManager.divideMessage(message)
                smsManager.sendMultipartTextMessage(cleanNumber, null, parts, null, null)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to launching SMS app
                openSmsApp(context, cleanNumber, message)
                false
            }
        } else {
            // Intent fallback
            openSmsApp(context, cleanNumber, message)
            false
        }
    }

    private fun openSmsApp(context: Context, phoneNumber: String, message: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$phoneNumber")
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}
