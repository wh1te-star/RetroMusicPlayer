package code.name.monkey.retromusic.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class GPSRecordService : Service() {

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): GPSRecordService = this@GPSRecordService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Thread {
            recordCount()
        }.start()
        return START_STICKY
    }

    private fun recordCount() {
        var count = 1
        while (true) {
            Log.d("GPSRecordService", "count is $count")
            count++
            TimeUnit.SECONDS.sleep(3)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
