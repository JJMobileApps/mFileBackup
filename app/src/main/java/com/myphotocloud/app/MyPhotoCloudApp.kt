// MyPhotoCloudApp.kt
package com.myphotocloud.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class MyPhotoCloudApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // 알림 채널 생성
        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            
            // 서버 채널
            val serverChannel = NotificationChannel(
                CHANNEL_SERVER,
                "서버 상태",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "백업 서버 실행 상태 알림"
            }
            
            // 백업 채널
            val backupChannel = NotificationChannel(
                CHANNEL_BACKUP,
                "백업 진행",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "백업 진행 상황 알림"
            }
            
            notificationManager.createNotificationChannel(serverChannel)
            notificationManager.createNotificationChannel(backupChannel)
        }
    }
    
    companion object {
        const val CHANNEL_SERVER = "server_status"
        const val CHANNEL_BACKUP = "backup_progress"
    }
}
