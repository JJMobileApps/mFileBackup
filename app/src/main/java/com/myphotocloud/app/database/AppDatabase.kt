package com.myphotocloud.app.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * MyPhotoCloud 앱 데이터베이스
 */
@Database(
    entities = [BackupStatusEntity::class, UserEntity::class, DeviceApprovalEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun backupStatusDao(): BackupStatusDao
    abstract fun userDao(): UserDao
    abstract fun deviceApprovalDao(): DeviceApprovalDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "myphotocloud_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
