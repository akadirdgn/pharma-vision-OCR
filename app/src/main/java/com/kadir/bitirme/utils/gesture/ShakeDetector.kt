package com.kadir.bitirme.utils.gesture

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * İvme ölçer kullanarak sallama hareketi algılar
 * Threshold: 12 m/s² — kullanıcı belirgin şekilde sallamalı
 */
class ShakeDetector(
    context: Context,
    private val onShakeDetected: () -> Unit
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private var lastShakeTime = 0L
    private val shakeCooldownMs = 1500L   // Ardarda tetiklemeyi önle
    private val shakeThreshold = 12f      // m/s²

    companion object {
        private const val SHAKE_THRESHOLD_GRAVITY = 2.7f
        private const val SHAKE_SLOP_TIME_MS = 500
        private const val SHAKE_COUNT_RESET_TIME_MS = 3000
    }

    private var shakeCount = 0
    private var lastShakeTimestamp = 0L

    fun start() {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val gForce = sqrt((x * x + y * y + z * z).toDouble()).toFloat() / SensorManager.GRAVITY_EARTH

        if (gForce > SHAKE_THRESHOLD_GRAVITY) {
            val now = System.currentTimeMillis()

            // Çok kısa aralıklı tekrar sayıma dahil etme
            if (lastShakeTimestamp + SHAKE_SLOP_TIME_MS > now) return

            // Uzun süre geçtiyse sayacı sıfırla
            if (lastShakeTimestamp + SHAKE_COUNT_RESET_TIME_MS < now) {
                shakeCount = 0
            }

            lastShakeTimestamp = now
            shakeCount++

            if (shakeCount >= 2) {
                shakeCount = 0

                val cooldownPassed = now - lastShakeTime > shakeCooldownMs
                if (cooldownPassed) {
                    lastShakeTime = now
                    onShakeDetected()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
