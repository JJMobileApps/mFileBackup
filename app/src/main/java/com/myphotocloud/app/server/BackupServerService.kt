// BackupServerService.kt
package com.myphotocloud.app.server

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.myphotocloud.app.MyPhotoCloudApp
import com.myphotocloud.app.R

class BackupServerService : Service() {
    
    private var isRunning = false
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            startForeground(NOTIFICATION_ID, createNotification())
            isRunning = true
            
            // TODO: HTTP 서버 시작
            // server = HttpServer(port = 8080)
            // server.start()
        }
        
        return START_STICKY
    }
    
    override fun onDestroy() {
        isRunning = false
        
        // TODO: HTTP 서버 중지
        // server.stop()
        
        super.onDestroy()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, MyPhotoCloudApp.CHANNEL_SERVER)
            .setContentTitle("MyPhotoCloud 서버 실행 중")
            .setContentText("포트: 8080") // TODO: 실제 IP 주소 표시
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }
    
    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}
