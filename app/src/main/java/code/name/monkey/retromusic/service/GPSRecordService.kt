package code.name.monkey.retromusic.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import code.name.monkey.retromusic.util.logD
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class GPSRecordService : Service() {
    private val binder = LocalBinder()
    private var gpsRecordingListener: GPSRecordingListener? = null
    private var acceleroValueListener: AcceleroValueListener? = null

    private var timestamp: Long = 0
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var altitude: Double = 0.0
    private var bearing: Float = 0.0f
    private var speed: Float = 0.0f
    private var provider: String? = ""

    private var acceleroX: Float = 0.0f
    private var acceleroY: Float = 0.0f
    private var acceleroZ: Float = 0.0f

    private lateinit var locationManager: LocationManager
    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor
    private lateinit var textViewLocationListener: LocationListener
    private lateinit var recordingLocationListener: LocationListener
    private lateinit var accelerometerListener: SensorEventListener
    private lateinit var recordingFile: File

    public var textviewMinTimeMs = 100L
    public var textviewMinDistanceM = 0.001f
    private var recordingMinTimeMs = 1000L
    private var recordingMinDistanceM = 10f

    private val storageSizeLimit = 20000000000 //[byte] = 20GB
    var doesFileSizeExceed = false

    inner class LocalBinder : Binder() {
        fun getService(): GPSRecordService = this@GPSRecordService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        textViewLocationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                logD("Location changed 1: $location")

                timestamp = location.time
                latitude = location.latitude
                longitude = location.longitude
                altitude = location.altitude
                bearing = location.bearing
                speed = location.speed
                provider = location.provider
                updateGPSTextView()
            }
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) { }
            override fun onProviderEnabled(provider: String) { }
            override fun onProviderDisabled(provider: String) { }
        }
        recordingLocationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                logD("Location changed 2: $location")
                val recordedValue = ByteArray(52)
                val buffer = ByteBuffer.wrap(recordedValue).order(ByteOrder.LITTLE_ENDIAN)
                buffer.putLong(timestamp)
                buffer.putDouble(latitude)
                buffer.putDouble(longitude)
                buffer.putDouble(altitude)
                buffer.putFloat(bearing)
                buffer.putFloat(speed)
                buffer.putFloat(acceleroX)
                buffer.putFloat(acceleroY)
                buffer.putFloat(acceleroZ)

                try {
                    writeToFile(recordingFile, recordedValue)
                } catch (e: IOException) {
                    Log.e("GPSRecordService", "Error writing to file", e)
                }
                if (recordingFile.length() > storageSizeLimit) {
                    if (!doesFileSizeExceed) {
                        gpsRecordingListener?.onFileSizeExceeded()
                        stopRecording()
                        doesFileSizeExceed = true
                    }
                }
            }
            override fun onProviderEnabled(provider: String) { }
            override fun onProviderDisabled(provider: String) { }
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) { }
        }

        try {
            locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
                textviewMinTimeMs,
                textviewMinDistanceM,
                textViewLocationListener)
        } catch (e: SecurityException) {
            Log.e("GPSRecordService", "Location permission not granted", e)
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        accelerometerListener = object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

            override fun onSensorChanged(event: SensorEvent) {
                acceleroX = event.values[0]
                acceleroY = event.values[1]
                acceleroZ = event.values[2]
                acceleroValueListener?.updateAcceleroTextView(acceleroX, acceleroY)
            }
        }
        sensorManager.registerListener(accelerometerListener, accelerometer, SensorManager.SENSOR_DELAY_UI)


        return START_NOT_STICKY
    }

    private fun initializeRecordingFile() {
        val fileName = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
            .format(Calendar.getInstance().time)
        recordingFile = File(getExternalFilesDir(null), fileName)
        if (recordingFile.exists()) {
            recordingFile.delete()
        }
        recordingFile.createNewFile()
    }

    private fun writeToFile(localFile: File, recordedValue: ByteArray) {
        FileOutputStream(localFile, true).use { fos ->
            fos.write(recordedValue)
        }
    }

    public fun startRecording() {
        initializeRecordingFile()
        doesFileSizeExceed = false
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                recordingMinTimeMs,
                recordingMinDistanceM,
                recordingLocationListener)
        } catch (e: SecurityException) {
            Log.e("GPSRecordService", "Location permission not granted", e)
        }
        gpsRecordingListener?.onRecordingStarted()
    }
    public fun stopRecording() {
        try {
            locationManager.removeUpdates(recordingLocationListener)
        } catch (e: UninitializedPropertyAccessException) {
            Log.e("GPSRecordService", "Failed to remove location updates", e)
        }
        gpsRecordingListener?.onRecordingStopped()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            locationManager.removeUpdates(textViewLocationListener)
        } catch (e: UninitializedPropertyAccessException) {
            Log.e("GPSRecordService", "Failed to remove location updates", e)
        }
        sensorManager.unregisterListener(accelerometerListener)
    }

    fun changeTextviewAccuracy(minTimeMS: Long, minDistanceM: Float) {
        textviewMinTimeMs = minTimeMS
        textviewMinDistanceM = minDistanceM
        locationManager.removeUpdates(textViewLocationListener)
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                textviewMinTimeMs,
                textviewMinDistanceM,
                textViewLocationListener)
        } catch (e: SecurityException) {
            Log.e("GPSRecordService", "Location permission not granted", e)
        }
    }

    fun registerGPSListener(gpsRecordingListener: GPSRecordingListener) {
        this.gpsRecordingListener = gpsRecordingListener
    }

    fun unregisterGPSListener() {
        this.gpsRecordingListener = null
    }

    fun registerAcceleroListener(acceleroValueListener: AcceleroValueListener) {
        this.acceleroValueListener = acceleroValueListener
    }

    fun unregisterAcceleroListener() {
        this.acceleroValueListener = null
    }

    fun updateGPSTextView() {
        gpsRecordingListener?.updateGPSTextView(latitude, longitude, altitude, bearing, speed, provider)
    }

    fun updateAcceleroTextView() {
        acceleroValueListener?.updateAcceleroTextView(acceleroX, acceleroY)
    }
}

interface GPSRecordingListener {
    fun onRecordingStarted()
    fun onRecordingStopped()
    fun onFileSizeExceeded()
    fun updateGPSTextView(latitude: Double, longitude: Double, altitude: Double, bearing: Float, speed: Float, provider: String?)
}

interface AcceleroValueListener{
    fun updateAcceleroTextView(x: Float, y: Float)
}