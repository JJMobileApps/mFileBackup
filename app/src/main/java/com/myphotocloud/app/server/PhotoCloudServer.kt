package com.myphotocloud.app.server

import android.content.Context
import android.util.Log
import com.myphotocloud.app.database.AppDatabase
import com.myphotocloud.app.database.BackupStatusEntity
import fi.iki.elonen.NanoHTTPD
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/**
 * MyPhotoCloud HTTP 서버
 */
class PhotoCloudServer(
    private val context: Context,
    port: Int = 8080
) : NanoHTTPD(port) {
    
    private val TAG = "PhotoCloudServer"
    private val database = AppDatabase.getDatabase(context)
    private val dao = database.backupStatusDao()
    
    // 업로드된 파일 저장 디렉토리
    private val uploadDir: File by lazy {
        File(context.filesDir, "uploads").apply {
            if (!exists()) mkdirs()
        }
    }
    
    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method
        
        Log.d(TAG, "Request: $method $uri")
        
        return try {
            when {
                uri == "/api/check-hashes" && method == Method.POST -> handleCheckHashes(session)
                uri == "/api/upload" && method == Method.POST -> handleUpload(session)
                uri == "/api/status" && method == Method.GET -> handleStatus(session)
                else -> newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling request", e)
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                "application/json",
                JSONObject().apply {
                    put("error", e.message ?: "Unknown error")
                }.toString()
            )
        }
    }
    
    /**
     * POST /api/check-hashes
     * Request: ["hash1", "hash2", ...]
     * Response: { "existing": ["hash1"], "missing": ["hash2"] }
     */
    private fun handleCheckHashes(session: IHTTPSession): Response {
        val files = HashMap<String, String>()
        session.parseBody(files)
        
        val body = files["postData"] ?: return newFixedLengthResponse(
            Response.Status.BAD_REQUEST,
            "application/json",
            JSONObject().put("error", "No body").toString()
        )
        
        val requestHashes = JSONArray(body)
        val existing = mutableListOf<String>()
        val missing = mutableListOf<String>()
        
        for (i in 0 until requestHashes.length()) {
            val hash = requestHashes.getString(i)
            
            // DB에서 확인
            val record = runCatching {
                kotlinx.coroutines.runBlocking {
                    dao.getByHash(hash)
                }
            }.getOrNull()
            
            // 파일도 실제로 존재하는지 확인
            val file = File(uploadDir, "$hash.dat")
            
            if (record != null && file.exists()) {
                existing.add(hash)
            } else {
                missing.add(hash)
                // DB에는 있는데 파일이 없으면 DB에서 삭제
                if (record != null && !file.exists()) {
                    runCatching {
                        kotlinx.coroutines.runBlocking {
                            dao.delete(record)
                        }
                    }
                }
            }
        }
        
        val response = JSONObject().apply {
            put("existing", JSONArray(existing))
            put("missing", JSONArray(missing))
        }
        
        Log.d(TAG, "Check hashes: ${requestHashes.length()} total, ${existing.size} existing, ${missing.size} missing")
        
        return newFixedLengthResponse(
            Response.Status.OK,
            "application/json",
            response.toString()
        )
    }
    
    /**
     * POST /api/upload
     * Multipart form data:
     * - file: 파일 데이터
     * - hash: 파일 해시 (MD5)
     * - fileName: 원본 파일명
     * - fileSize: 파일 크기
     * - mimeType: MIME 타입
     */
    private fun handleUpload(session: IHTTPSession): Response {
        val files = HashMap<String, String>()
        session.parseBody(files)
        
        val hash = session.parameters["hash"]?.firstOrNull()
        val fileName = session.parameters["fileName"]?.firstOrNull()
        val fileSize = session.parameters["fileSize"]?.firstOrNull()?.toLongOrNull()
        val mimeType = session.parameters["mimeType"]?.firstOrNull()
        
        if (hash == null || fileName == null) {
            return newFixedLengthResponse(
                Response.Status.BAD_REQUEST,
                "application/json",
                JSONObject().put("error", "Missing hash or fileName").toString()
            )
        }
        
        // 임시 파일에서 영구 저장소로 복사
        val tempFile = files["file"]?.let { File(it) }
        if (tempFile == null || !tempFile.exists()) {
            return newFixedLengthResponse(
                Response.Status.BAD_REQUEST,
                "application/json",
                JSONObject().put("error", "No file data").toString()
            )
        }
        
        // 해시 파일명으로 저장
        val savedFile = File(uploadDir, "$hash.dat")
        tempFile.copyTo(savedFile, overwrite = true)
        
        // DB에 저장
        runCatching {
            kotlinx.coroutines.runBlocking {
                val existing = dao.getByHash(hash)
                if (existing != null) {
                    // 이미 있으면 업데이트
                    dao.update(
                        existing.copy(
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                } else {
                    // 새로 생성
                    dao.insert(
                        BackupStatusEntity(
                            fileUri = "server://$hash",
                            fileName = fileName,
                            filePath = savedFile.absolutePath,
                            fileSize = fileSize ?: savedFile.length(),
                            mimeType = mimeType ?: "application/octet-stream",
                            dateModified = System.currentTimeMillis(),
                            hash = hash,
                            hashCalculatedAt = System.currentTimeMillis(),
                            isBackedUp = true,
                            backupDate = System.currentTimeMillis()
                        )
                    )
                }
            }
        }.onFailure { e ->
            Log.e(TAG, "Failed to save to DB", e)
        }
        
        Log.d(TAG, "Uploaded: $fileName ($hash) - ${savedFile.length()} bytes")
        
        val response = JSONObject().apply {
            put("success", true)
            put("hash", hash)
            put("fileName", fileName)
            put("size", savedFile.length())
        }
        
        return newFixedLengthResponse(
            Response.Status.OK,
            "application/json",
            response.toString()
        )
    }
    
    /**
     * GET /api/status
     * 서버 상태 조회
     */
    private fun handleStatus(session: IHTTPSession): Response {
        val totalFiles = runCatching {
            kotlinx.coroutines.runBlocking {
                dao.getAll().size
            }
        }.getOrDefault(0)
        
        val response = JSONObject().apply {
            put("status", "running")
            put("port", listeningPort)
            put("totalFiles", totalFiles)
            put("uploadDir", uploadDir.absolutePath)
        }
        
        return newFixedLengthResponse(
            Response.Status.OK,
            "application/json",
            response.toString()
        )
    }
}
