package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PlantLightZone(val label: String, val minLux: Float, val maxLux: Float, val description: String) {
    LOW_LIGHT("Low Light", 0f, 150f, "Suitable for Peace Lily, Snake Plant, Cast Iron Plant"),
    MEDIUM_INDIRECT("Medium Indirect Light", 150f, 1000f, "Suitable for Pothos, Monstera, Calathea"),
    BRIGHT_DAYLIGHT("Bright Daylight", 1000f, 100000f, "Suitable for Lavender, Succulents, Bonsai Juniper")
}

class LightSensorManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val lightSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    private val _currentLux = MutableStateFlow(350f) // Default indoor ambient reading
    val currentLux: StateFlow<Float> = _currentLux.asStateFlow()

    private val _lightZone = MutableStateFlow(PlantLightZone.MEDIUM_INDIRECT)
    val lightZone: StateFlow<PlantLightZone> = _lightZone.asStateFlow()

    val isSensorAvailable: Boolean = lightSensor != null

    fun startListening() {
        lightSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopListening() {
        if (isSensorAvailable) {
            sensorManager.unregisterListener(this)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_LIGHT) {
                val lux = it.values[0]
                _currentLux.value = lux
                _lightZone.value = when {
                    lux < 150f -> PlantLightZone.LOW_LIGHT
                    lux in 150f..1000f -> PlantLightZone.MEDIUM_INDIRECT
                    else -> PlantLightZone.BRIGHT_DAYLIGHT
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
