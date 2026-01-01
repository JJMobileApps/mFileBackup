package com.myphotocloud.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "device_approvals")
data class DeviceApprovalEntity(
    @PrimaryKey val deviceId: String,
    val deviceName: String?,
    val status: Int,
    val firstSeenAt: Long = System.currentTimeMillis(),
    val lastSeenAt: Long = System.currentTimeMillis(),
    val lastIp: String?
) {
    companion object {
        const val STATUS_PENDING = 0
        const val STATUS_APPROVED = 1
        const val STATUS_DENIED = 2
    }
}
