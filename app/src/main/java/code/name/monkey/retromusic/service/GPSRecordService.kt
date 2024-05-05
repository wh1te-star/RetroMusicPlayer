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
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class GPSRecordService() : Service(), LocationListener {
    private val binder = LocalBinder()
    private var listener: TextViewUpdateListener? = null

    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

    private lateinit var locationManager: LocationManager
    private lateinit var recordingFile: File
    private val storageSizeLimit = 20000000000 //[byte] = 20GB
    var doesFileSizeExceed = false

    companion object {
        val RECORDING_STARTED = "code.name.monkey.retromusic.RECORDING_STARTED"
        val RECORDING_STOPPED = "code.name.monkey.retromusic.RECORDING_STOPPED"
        val FILE_SIZE_EXCEEDED = "code.name.monkey.retromusic.FILE_SIZE_EXCEEDED"
    }

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
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0.001f, this)
        } catch (e: SecurityException) {
            Log.e("GPSRecordService", "Location permission not granted", e)
        }

        val fileName = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
            .format(Calendar.getInstance().time)
        recordingFile = File(getExternalFilesDir(null), fileName)
        if (recordingFile.exists()) {
            recordingFile.delete()
        }
        recordingFile.createNewFile()

        sendBroadcast(Intent(RECORDING_STARTED))

        return START_NOT_STICKY
    }

    private fun writeToFile(localFile: File, recordedValue: ByteArray) {
        FileOutputStream(localFile, true).use { fos ->
            fos.write(recordedValue)
        }
    }

    override fun onLocationChanged(location: Location) {
        val recordedValue = ByteArray(24)
        val buffer = ByteBuffer.wrap(recordedValue).order(ByteOrder.LITTLE_ENDIAN)

        latitude = location.latitude
        longitude = location.longitude

        val currentTime = System.currentTimeMillis()
        buffer.putLong(currentTime)
        buffer.putDouble(latitude)
        buffer.putDouble(longitude)

        updateTextView()

        try {
            writeToFile(recordingFile, recordedValue)
        } catch (e: IOException) {
            Log.e("GPSRecordService", "Error writing to file", e)
        }

        if (recordingFile.length() > storageSizeLimit){
            if(!doesFileSizeExceed) {
                sendBroadcast(Intent(FILE_SIZE_EXCEEDED))
                doesFileSizeExceed = true
            }
        }
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) { }

    override fun onProviderEnabled(provider: String) { }

    override fun onProviderDisabled(provider: String) { }

    override fun onDestroy() {
        super.onDestroy()
        try {
            locationManager.removeUpdates(this)
        } catch (e: UninitializedPropertyAccessException) {
            Log.e("GPSRecordService", "Failed to remove location updates", e)
        }
        sendBroadcast(Intent(RECORDING_STOPPED))
    }

    fun registerListener(listener: TextViewUpdateListener) {
        this.listener = listener
    }

    fun unregisterListener() {
        this.listener = null
    }

    fun updateTextView() {
        listener?.updateTextView(latitude, longitude)
    }
}

interface TextViewUpdateListener {
    fun updateTextView(latitude: Double, longitude: Double)
}
