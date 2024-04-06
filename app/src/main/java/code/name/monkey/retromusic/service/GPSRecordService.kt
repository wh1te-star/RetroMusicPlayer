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

    inner class LocalBinder : Binder() {
        fun getService(): GPSRecordService = this@GPSRecordService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 0f, this)
        } catch (e: SecurityException) {
            Log.e("GPSRecordService", "Location permission not granted", e)
        }
        return START_STICKY
    }

    override fun onLocationChanged(location: Location) {
        Log.d("GPSRecordService", "Latitude: ${location.latitude}, Longitude: ${location.longitude}")
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
