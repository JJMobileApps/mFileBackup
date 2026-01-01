package com.myphotocloud.app.model

import android.net.Uri

/**
 * 미디어 파일 정보
 */
data class MediaFile(
    val id: Long,
    val uri: Uri,
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val mimeType: String,
    val dateAdded: Long,
    val dateModified: Long,
    val width: Int = 0,
    val height: Int = 0,
    val duration: Long = 0,  // 동영상인 경우 (밀리초)
    val hash: String? = null,  // 계산된 해시값
    val isBackedUp: Boolean = false
) {
    val isVideo: Boolean
        get() = mimeType.startsWith("video/")
    
    val isImage: Boolean
        get() = mimeType.startsWith("image/")
    
    val sizeInMB: Float
        get() = fileSize / (1024f * 1024f)
    
    /**
     * 파일 크기를 사람이 읽기 쉬운 형식으로 변환
     */
    val formattedSize: String
        get() = when {
            fileSize >= 1024 * 1024 * 1024 -> "%.2f GB".format(fileSize / (1024f * 1024f * 1024f))
            fileSize >= 1024 * 1024 -> "%.2f MB".format(fileSize / (1024f * 1024f))
            fileSize >= 1024 -> "%.2f KB".format(fileSize / 1024f)
            else -> "$fileSize B"
        }
}
