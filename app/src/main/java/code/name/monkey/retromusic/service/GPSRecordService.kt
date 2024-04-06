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
import java.util.concurrent.TimeUnit

class GPSRecordService : Service(), LocationListener {

    private val binder = LocalBinder()
    private lateinit var locationManager: LocationManager
    private var latitude = 0.0
    private var longitude = 0.0

    inner class LocalBinder : Binder() {
        fun getService(): GPSRecordService = this@GPSRecordService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 50, 0.001f, this)
        } catch (e: SecurityException) {
            Log.e("GPSRecordService", "Location permission not granted", e)
        }
        //wait for the first onLocationChanged call
        Thread {
            recordCount()
        }.start()
        return START_STICKY
    }

    private fun recordCount() {
        while (true) {
            Log.d("GPSRecordService", "Latitude: ${latitude}, Longitude: ${longitude}")
            TimeUnit.SECONDS.sleep(3)
        }
    }

    override fun onLocationChanged(location: Location) {
        latitude = location.latitude
        longitude = location.longitude
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) { }

    override fun onProviderEnabled(provider: String) { }

    override fun onProviderDisabled(provider: String) { }

    override fun onDestroy() {
        super.onDestroy()
        try {
            locationManager.removeUpdates(this)
        } catch (e: SecurityException) {
            Log.e("GPSRecordService", "Failed to remove location updates", e)
        }
    }
}
