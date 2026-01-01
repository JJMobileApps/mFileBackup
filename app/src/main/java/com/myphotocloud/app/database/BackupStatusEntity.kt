package com.myphotocloud.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 백업 상태를 저장하는 엔티티
 */
@Entity(tableName = "backup_status")
data class BackupStatusEntity(
    @PrimaryKey
    val fileUri: String,  // Content URI를 문자열로 저장
    
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val mimeType: String,
    val dateModified: Long,
    
    // 해시 정보
    val hash: String? = null,
    val hashCalculatedAt: Long? = null,
    
    // 백업 정보
    val isBackedUp: Boolean = false,
    val backupDate: Long? = null,
    val serverPath: String? = null,
    val uploadAttempts: Int = 0,
    val lastAttemptDate: Long? = null,
    val errorMessage: String? = null,
    
    // 메타데이터
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
