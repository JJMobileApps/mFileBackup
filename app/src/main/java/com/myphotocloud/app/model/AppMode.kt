// AppMode.kt
package com.myphotocloud.app.model

/**
 * 앱 실행 모드
 */
enum class AppMode {
    /**
     * 클라이언트 전용: 다른 서버로 백업만
     */
    CLIENT_ONLY,
    
    /**
     * 서버 전용: 다른 기기들의 백업 수신
     */
    SERVER_ONLY,
    
    /**
     * 독립 모드: 서버 + 클라이언트 (자기 자신에게 백업)
     */
    STANDALONE;
    
    val displayName: String
        get() = when (this) {
            CLIENT_ONLY -> "백업만"
            SERVER_ONLY -> "서버만"
            STANDALONE -> "서버 + 백업"
        }
    
    val description: String
        get() = when (this) {
            CLIENT_ONLY -> "다른 기기로 백업"
            SERVER_ONLY -> "가족 사진 보관 전용"
            STANDALONE -> "이 폰에서 사진 보관 및 백업"
        }
    
    val requiresServer: Boolean
        get() = this != CLIENT_ONLY
    
    val requiresBackup: Boolean
        get() = this != SERVER_ONLY
}
