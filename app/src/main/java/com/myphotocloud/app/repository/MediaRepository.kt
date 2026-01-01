package com.myphotocloud.app.repository

import android.content.Context
import android.provider.MediaStore
import android.net.Uri
import com.myphotocloud.app.database.AppDatabase
import com.myphotocloud.app.database.BackupStatusEntity
import com.myphotocloud.app.model.MediaFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.io.File
import java.io.FileInputStream

/**
 * 미디어 파일 관리 Repository
 */
class MediaRepository(private val context: Context) {
    
    private val database = AppDatabase.getDatabase(context)
    private val dao = database.backupStatusDao()
    
    /**
     * 모든 미디어 파일 스캔 (사진 + 동영상)
     */
    suspend fun scanAllMedia(): List<MediaFile> = withContext(Dispatchers.IO) {
        val images = scanImages()
        val videos = scanVideos()
        images + videos
    }
    
    /**
     * 사진 파일 스캔
     */
    private suspend fun scanImages(): List<MediaFile> = withContext(Dispatchers.IO) {
        val mediaFiles = mutableListOf<MediaFile>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT
        )
        
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn) ?: "unknown"
                val data = cursor.getString(dataColumn) ?: ""
                val size = cursor.getLong(sizeColumn)
                val mimeType = cursor.getString(mimeColumn) ?: "image/*"
                val dateAdded = cursor.getLong(dateAddedColumn) * 1000
                val dateModified = cursor.getLong(dateModifiedColumn) * 1000
                val width = cursor.getInt(widthColumn)
                val height = cursor.getInt(heightColumn)
                
                val uri = Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )
                
                // DB에서 백업 상태 조회
                val backupStatus = dao.getByUri(uri.toString())
                
                mediaFiles.add(
                    MediaFile(
                        id = id,
                        uri = uri,
                        fileName = name,
                        filePath = data,
                        fileSize = size,
                        mimeType = mimeType,
                        dateAdded = dateAdded,
                        dateModified = dateModified,
                        width = width,
                        height = height,
                        hash = backupStatus?.hash,
                        isBackedUp = backupStatus?.isBackedUp ?: false
                    )
                )
            }
        }
        
        mediaFiles
    }
    
    /**
     * 동영상 파일 스캔
     */
    private suspend fun scanVideos(): List<MediaFile> = withContext(Dispatchers.IO) {
        val mediaFiles = mutableListOf<MediaFile>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DURATION
        )
        
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn) ?: "unknown"
                val data = cursor.getString(dataColumn) ?: ""
                val size = cursor.getLong(sizeColumn)
                val mimeType = cursor.getString(mimeColumn) ?: "video/*"
                val dateAdded = cursor.getLong(dateAddedColumn) * 1000
                val dateModified = cursor.getLong(dateModifiedColumn) * 1000
                val width = cursor.getInt(widthColumn)
                val height = cursor.getInt(heightColumn)
                val duration = cursor.getLong(durationColumn)
                
                val uri = Uri.withAppendedPath(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )
                
                // DB에서 백업 상태 조회
                val backupStatus = dao.getByUri(uri.toString())
                
                mediaFiles.add(
                    MediaFile(
                        id = id,
                        uri = uri,
                        fileName = name,
                        filePath = data,
                        fileSize = size,
                        mimeType = mimeType,
                        dateAdded = dateAdded,
                        dateModified = dateModified,
                        width = width,
                        height = height,
                        duration = duration,
                        hash = backupStatus?.hash,
                        isBackedUp = backupStatus?.isBackedUp ?: false
                    )
                )
            }
        }
        
        mediaFiles
    }
    
    /**
     * 파일의 MD5 해시 계산
     */
    suspend fun calculateHash(filePath: String): String? = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) return@withContext null
            
            val digest = MessageDigest.getInstance("MD5")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var read: Int
                while (fis.read(buffer).also { read = it } > 0) {
                    digest.update(buffer, 0, read)
                }
            }
            
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 백업 상태 업데이트
     */
    suspend fun updateBackupStatus(
        fileUri: String,
        hash: String? = null,
        isBackedUp: Boolean = false,
        serverPath: String? = null
    ) = withContext(Dispatchers.IO) {
        val existing = dao.getByUri(fileUri)
        if (existing != null) {
            dao.update(
                existing.copy(
                    hash = hash ?: existing.hash,
                    hashCalculatedAt = if (hash != null) System.currentTimeMillis() else existing.hashCalculatedAt,
                    isBackedUp = isBackedUp,
                    backupDate = if (isBackedUp) System.currentTimeMillis() else existing.backupDate,
                    serverPath = serverPath ?: existing.serverPath,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }
    
    /**
     * Flow로 백업 상태 가져오기
     */
    fun getAllBackupStatusFlow(): Flow<List<BackupStatusEntity>> = dao.getAllFlow()
    
    fun getBackedUpCountFlow(): Flow<Int> = dao.getBackedUpCountFlow()
    
    fun getPendingCountFlow(): Flow<Int> = dao.getPendingFlow().map { it.size }
    
    fun getBackedUpSizeFlow(): Flow<Long> = dao.getBackedUpSizeFlow().map { it ?: 0L }
    
    fun getPendingSizeFlow(): Flow<Long> = dao.getPendingSizeFlow().map { it ?: 0L }
}
