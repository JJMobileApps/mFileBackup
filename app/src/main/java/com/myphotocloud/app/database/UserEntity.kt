package com.myphotocloud.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val passwordHash: String, // 보안을 위해 해시된 비밀번호 저장
    val storageFolderName: String, // 사용자별 격리된 저장소 폴더 이름
    val createdAt: Long = System.currentTimeMillis()
)
