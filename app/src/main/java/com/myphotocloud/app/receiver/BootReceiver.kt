package com.myphotocloud.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * 부팅 완료 시 자동 시작을 위한 리시버
 */
class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Boot completed - checking app mode")
            
            // TODO: 앱 모드에 따라 적절한 서비스 시작
            // - SERVER_ONLY 또는 STANDALONE 모드면 서버 시작
            // - CLIENT_ONLY 또는 STANDALONE 모드면 자동 백업 알람 설정
        }
    }
}
