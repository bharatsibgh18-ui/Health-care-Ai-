package com.example.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

class StepSensorTracker(
    context: Context,
    private val onStepCountChanged: (stepIncrement: Int) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val stepCounterSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val stepDetectorSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

    val isSensorAvailable: Boolean = stepCounterSensor != null || stepDetectorSensor != null
    val sensorTypeDescription: String = when {
        stepCounterSensor != null -> "Hardware Step Counter (${stepCounterSensor.name})"
        stepDetectorSensor != null -> "Hardware Step Detector (${stepDetectorSensor.name})"
        else -> "Sensor Simulation Mode (No Physical Sensor Detected)"
    }

    private var isTracking = false
    private var lastRawCounterValue: Float? = null

    fun startTracking(): Boolean {
        if (isTracking || sensorManager == null) return isTracking

        var registered = false
        // Prefer TYPE_STEP_DETECTOR for immediate per-step delta events if available
        if (stepDetectorSensor != null) {
            registered = sensorManager.registerListener(
                this,
                stepDetectorSensor,
                SensorManager.SENSOR_DELAY_UI
            )
        }

        // Fallback to TYPE_STEP_COUNTER if detector is not present or failed
        if (!registered && stepCounterSensor != null) {
            registered = sensorManager.registerListener(
                this,
                stepCounterSensor,
                SensorManager.SENSOR_DELAY_UI
            )
        }

        isTracking = registered
        Log.d("StepSensorTracker", "Started step tracking: $isTracking ($sensorTypeDescription)")
        return isTracking
    }

    fun stopTracking() {
        if (!isTracking) return
        try {
            sensorManager?.unregisterListener(this)
        } catch (e: Exception) {
            Log.e("StepSensorTracker", "Error unregistering sensor listener", e)
        }
        isTracking = false
        lastRawCounterValue = null
        Log.d("StepSensorTracker", "Stopped step tracking")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !isTracking) return

        when (event.sensor.type) {
            Sensor.TYPE_STEP_DETECTOR -> {
                // Step detector fires 1.0f on each step detected
                val steps = event.values.firstOrNull()?.toInt() ?: 1
                if (steps > 0) {
                    onStepCountChanged(steps)
                }
            }
            Sensor.TYPE_STEP_COUNTER -> {
                val currentRaw = event.values.firstOrNull() ?: return
                val prevRaw = lastRawCounterValue
                if (prevRaw != null) {
                    val delta = (currentRaw - prevRaw).toInt()
                    // Filter out device reboots or negative delta anomalies
                    if (delta in 1..2000) {
                        onStepCountChanged(delta)
                    }
                }
                lastRawCounterValue = currentRaw
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op for step sensors
    }
}
