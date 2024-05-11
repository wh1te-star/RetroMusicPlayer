package code.name.monkey.retromusic.service

import android.app.Service
import android.content.Intent
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
import java.lang.Math.toRadians
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class GPSRecordService : Service() {
    private val binder = LocalBinder()
    private var listener: TextViewUpdateListener? = null

    private var previousTimestamp: Long = 0
    private var previousLatitude: Double = 0.0
    private var previousLongitude: Double = 0.0
    private var timestamp: Long = 0
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

    private lateinit var locationManager: LocationManager
    private lateinit var textViewLocationListener: LocationListener
    private lateinit var recordingLocationListener: LocationListener
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
                previousTimestamp = timestamp
                previousLatitude = latitude
                previousLongitude = longitude

                timestamp = location.time
                latitude = location.latitude
                longitude = location.longitude

                updateTextView(location.speed*3.6f)
            }
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) { }
            override fun onProviderEnabled(provider: String) { }
            override fun onProviderDisabled(provider: String) { }
        }
        recordingLocationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                logD("Location changed 2: $location")
                val recordedValue = ByteArray(24)
                val buffer = ByteBuffer.wrap(recordedValue).order(ByteOrder.LITTLE_ENDIAN)
                buffer.putLong(timestamp)
                buffer.putDouble(latitude)
                buffer.putDouble(longitude)

                try {
                    writeToFile(recordingFile, recordedValue)
                } catch (e: IOException) {
                    Log.e("GPSRecordService", "Error writing to file", e)
                }
                if (recordingFile.length() > storageSizeLimit) {
                    if (!doesFileSizeExceed) {
                        listener?.onFileSizeExceeded()
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
        listener?.onRecordingStarted()
    }
    public fun stopRecording() {
        try {
            locationManager.removeUpdates(recordingLocationListener)
        } catch (e: UninitializedPropertyAccessException) {
            Log.e("GPSRecordService", "Failed to remove location updates", e)
        }
        listener?.onRecordingStopped()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            locationManager.removeUpdates(textViewLocationListener)
        } catch (e: UninitializedPropertyAccessException) {
            Log.e("GPSRecordService", "Failed to remove location updates", e)
        }
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

    fun registerListener(listener: TextViewUpdateListener) {
        this.listener = listener
    }

    fun unregisterListener() {
        this.listener = null
    }

    fun updateTextView(speed: Float) {
        listener?.updateTextView(latitude, longitude, speed)
    }
}

interface TextViewUpdateListener {
    fun onRecordingStarted()
    fun onRecordingStopped()
    fun onFileSizeExceeded()
    fun updateTextView(latitude: Double, longitude: Double, speed: Float)
}
