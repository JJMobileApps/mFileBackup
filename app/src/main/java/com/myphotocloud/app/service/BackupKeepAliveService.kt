package com.myphotocloud.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.myphotocloud.app.MyPhotoCloudApp
import com.myphotocloud.app.R

/**
 * 백그라운드 백업을 유지하기 위한 Foreground 서비스
 */
class BackupKeepAliveService : Service() {
    
    companion object {
        private const val NOTIFICATION_ID = 1001
    }
    
    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, createNotification())
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 서비스 유지
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    private fun createNotification(): Notification {
        val notificationManager = getSystemService(NotificationManager::class.java)
        
        // Android O 이상에서는 채널이 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                MyPhotoCloudApp.CHANNEL_BACKUP,
                "백업 진행",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "백업 진행 상황 알림"
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        return NotificationCompat.Builder(this, MyPhotoCloudApp.CHANNEL_BACKUP)
            .setContentTitle("백업 서비스 실행 중")
            .setContentText("백그라운드에서 백업이 실행 중입니다")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
