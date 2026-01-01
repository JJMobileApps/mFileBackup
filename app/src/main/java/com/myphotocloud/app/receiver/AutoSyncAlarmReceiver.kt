// AutoSyncAlarmReceiver.kt
package com.myphotocloud.app.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class AutoSyncAlarmReceiver : BroadcastReceiver() {
    
    companion object {
        private const val ACTION_AUTO_SYNC = "com.myphotocloud.app.ACTION_AUTO_SYNC"
        private const val SYNC_INTERVAL_MS = 60L * 60L * 1000L // 1시간
        
        fun schedule(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AutoSyncAlarmReceiver::class.java).apply {
                action = ACTION_AUTO_SYNC
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or 
                    if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
            )
            
            val triggerTime = System.currentTimeMillis() + SYNC_INTERVAL_MS
            
            try {
                if (Build.VERSION.SDK_INT >= 31) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                    }
                } else if (Build.VERSION.SDK_INT >= 23) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        fun cancel(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AutoSyncAlarmReceiver::class.java).apply {
                action = ACTION_AUTO_SYNC
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or 
                    if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
            )
            alarmManager.cancel(pendingIntent)
        }
    }
    
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_AUTO_SYNC) return
        
        // TODO: WorkManager로 백업 작업 시작
        // val workRequest = OneTimeWorkRequestBuilder<BackupWorker>().build()
        // WorkManager.getInstance(context).enqueueUniqueWork(...)
        
        // 다음 알람 예약
        schedule(context)
    }
}
