package com.myphotocloud.app.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 백업 상태 DAO
 */
@Dao
interface BackupStatusDao {
    
    @Query("SELECT * FROM backup_status ORDER BY dateModified DESC")
    fun getAllFlow(): Flow<List<BackupStatusEntity>>
    
    @Query("SELECT * FROM backup_status ORDER BY dateModified DESC")
    suspend fun getAll(): List<BackupStatusEntity>
    
    @Query("SELECT * FROM backup_status WHERE isBackedUp = 1 ORDER BY dateModified DESC")
    fun getBackedUpFlow(): Flow<List<BackupStatusEntity>>
    
    @Query("SELECT * FROM backup_status WHERE isBackedUp = 0 ORDER BY dateModified DESC")
    fun getPendingFlow(): Flow<List<BackupStatusEntity>>
    
    @Query("SELECT * FROM backup_status WHERE fileUri = :uri")
    suspend fun getByUri(uri: String): BackupStatusEntity?
    
    @Query("SELECT * FROM backup_status WHERE hash = :hash")
    suspend fun getByHash(hash: String): BackupStatusEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(status: BackupStatusEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(statuses: List<BackupStatusEntity>)
    
    @Update
    suspend fun update(status: BackupStatusEntity)
    
    @Delete
    suspend fun delete(status: BackupStatusEntity)
    
    @Query("DELETE FROM backup_status WHERE fileUri = :uri")
    suspend fun deleteByUri(uri: String)
    
    @Query("DELETE FROM backup_status")
    suspend fun deleteAll()
    
    // 통계 쿼리
    @Query("SELECT COUNT(*) FROM backup_status")
    fun getTotalCountFlow(): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM backup_status WHERE isBackedUp = 1")
    fun getBackedUpCountFlow(): Flow<Int>
    
    @Query("SELECT SUM(fileSize) FROM backup_status WHERE isBackedUp = 1")
    fun getBackedUpSizeFlow(): Flow<Long?>
    
    @Query("SELECT SUM(fileSize) FROM backup_status WHERE isBackedUp = 0")
    fun getPendingSizeFlow(): Flow<Long?>
}
