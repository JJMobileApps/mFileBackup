package com.myphotocloud.app.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceApprovalDao {
    @Query("SELECT * FROM device_approvals ORDER BY firstSeenAt DESC")
    fun getAllFlow(): Flow<List<DeviceApprovalEntity>>

    @Query("SELECT * FROM device_approvals WHERE status = :status ORDER BY firstSeenAt DESC")
    fun getByStatusFlow(status: Int): Flow<List<DeviceApprovalEntity>>

    @Query("SELECT * FROM device_approvals WHERE deviceId = :deviceId LIMIT 1")
    suspend fun getByDeviceId(deviceId: String): DeviceApprovalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DeviceApprovalEntity)

    @Update
    suspend fun update(entity: DeviceApprovalEntity)

    @Query("UPDATE device_approvals SET status = :status, lastSeenAt = :timestamp WHERE deviceId = :deviceId")
    suspend fun updateStatus(deviceId: String, status: Int, timestamp: Long = System.currentTimeMillis())
}
