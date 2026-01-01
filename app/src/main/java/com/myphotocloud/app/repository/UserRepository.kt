package com.myphotocloud.app.repository

import android.content.Context
import com.myphotocloud.app.database.AppDatabase
import com.myphotocloud.app.database.UserEntity
import com.myphotocloud.app.utils.UserAuthUtils
import kotlinx.coroutines.flow.Flow

class UserRepository(context: Context) {
    private val dao = AppDatabase.getDatabase(context).userDao()

    val allUsers: Flow<List<UserEntity>> = dao.getAllUsersFlow()

    /**
     * 새 사용자 추가
     */
    suspend fun addUser(username: String, password: String, storageFolderName: String): Result<Unit> {
        if (dao.getByUsername(username) != null) {
            return Result.failure(Exception("이미 존재하는 사용자입니다."))
        }

        val finalStorageFolderName = if (storageFolderName.startsWith("content://")) {
            storageFolderName
        } else {
            // 폴더 이름 유효성 검사 (간단하게)
            val safeFolderName = storageFolderName.replace(Regex("[^a-zA-Z0-9_-]"), "")
            if (safeFolderName.isEmpty()) {
                return Result.failure(Exception("유효하지 않은 폴더 이름입니다."))
            }
            safeFolderName
        }

        val user = UserEntity(
            username = username,
            passwordHash = UserAuthUtils.hashPassword(password),
            storageFolderName = finalStorageFolderName
        )
        dao.insertUser(user)
        return Result.success(Unit)
    }

    /**
     * 사용자 삭제
     */
    suspend fun deleteUser(user: UserEntity) {
        dao.deleteUser(user)
    }

    /**
     * 사용자 인증
     */
    suspend fun authenticate(username: String, password: String): UserEntity? {
        val user = dao.getByUsername(username) ?: return null
        if (user.passwordHash == UserAuthUtils.hashPassword(password)) {
            return user
        }
        return null
    }
}
