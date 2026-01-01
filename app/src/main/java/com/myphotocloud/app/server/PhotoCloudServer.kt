package com.myphotocloud.app.server

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.myphotocloud.app.database.AppDatabase
import com.myphotocloud.app.database.BackupStatusEntity
import com.myphotocloud.app.database.UserEntity
import com.myphotocloud.app.utils.UserAuthUtils
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * MyPhotoCloud HTTP 서버 (다중 사용자 지원)
 */
class PhotoCloudServer(
    private val context: Context,
    port: Int = 8080
) : NanoHTTPD(port) {
    
    private val TAG = "PhotoCloudServer"
    private val database = AppDatabase.getDatabase(context)
    private val dao = database.backupStatusDao()
    private val userDao = database.userDao()
    
    // 기본 업로드 디렉토리 (파일 기반 저장소용)
    private val baseUploadDir: File by lazy {
        File(context.filesDir, "backups").apply {
            if (!exists()) mkdirs()
        }
    }
    
    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method
        
        Log.d(TAG, "Request: $method $uri")
        
        return try {
            // 상태 확인은 인증 없이 허용
            if (uri == "/api/status" && method == Method.GET) {
                return handleStatus(session)
            }

            // 그 외 API는 인증 필요
            val user = authenticate(session) ?: return newFixedLengthResponse(
                Response.Status.UNAUTHORIZED,
                "application/json",
                JSONObject().put("error", "Unauthorized").toString()
            )

            when {
                uri == "/api/check-hashes" && method == Method.POST -> handleCheckHashes(session, user)
                uri == "/api/upload" && method == Method.POST -> handleUpload(session, user)
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
     * Basic Auth 인증
     */
    private fun authenticate(session: IHTTPSession): UserEntity? {
        val authHeader = session.headers["authorization"] ?: return null
        if (!authHeader.startsWith("Basic ", ignoreCase = true)) return null
        
        try {
            val base64Credentials = authHeader.substring(6).trim()
            val credentials = String(Base64.decode(base64Credentials, Base64.DEFAULT)).split(":", limit = 2)
            if (credentials.size != 2) return null
            
            val username = credentials[0]
            val password = credentials[1]
            
            val user = runBlocking { userDao.getByUsername(username) } ?: return null
            if (user.passwordHash == UserAuthUtils.hashPassword(password)) {
                return user
            }
        } catch (e: Exception) {
            Log.e(TAG, "Auth error", e)
        }
        return null
    }

    private fun getUserDir(user: UserEntity): File {
        return File(baseUploadDir, user.storageFolderName).apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * POST /api/check-hashes
     */
    private fun handleCheckHashes(session: IHTTPSession, user: UserEntity): Response {
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
        
        // 저장소 모드 확인 (URI vs File)
        val isUriMode = user.storageFolderName.startsWith("content://")
        val userDirFile = if (!isUriMode) getUserDir(user) else null
        val userDocFile = if (isUriMode) DocumentFile.fromTreeUri(context, Uri.parse(user.storageFolderName)) else null
        
        for (i in 0 until requestHashes.length()) {
            val hash = requestHashes.getString(i)
            val fileName = "$hash.dat"
            
            val exists = if (isUriMode) {
                userDocFile?.findFile(fileName) != null
            } else {
                File(userDirFile, fileName).exists()
            }
            
            if (exists) existing.add(hash) else missing.add(hash)
        }
        
        val response = JSONObject().apply {
            put("existing", JSONArray(existing))
            put("missing", JSONArray(missing))
        }
        
        return newFixedLengthResponse(
            Response.Status.OK,
            "application/json",
            response.toString()
        )
    }
    
    /**
     * POST /api/upload
     */
    private fun handleUpload(session: IHTTPSession, user: UserEntity): Response {
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
        
        val tempFile = files["file"]?.let { File(it) }
        if (tempFile == null || !tempFile.exists()) {
            return newFixedLengthResponse(
                Response.Status.BAD_REQUEST,
                "application/json",
                JSONObject().put("error", "No file data").toString()
            )
        }
        
        // 저장 로직
        val savedFileName = "$hash.dat"
        var savedPath = ""
        var saveSuccess = false
        
        try {
            if (user.storageFolderName.startsWith("content://")) {
                // URI 모드 (시스템 폴더)
                val treeUri = Uri.parse(user.storageFolderName)
                val docDir = DocumentFile.fromTreeUri(context, treeUri)
                
                if (docDir != null && docDir.canWrite()) {
                    // 기존 파일 있으면 삭제 (덮어쓰기)
                    docDir.findFile(savedFileName)?.delete()
                    
                    val newFile = docDir.createFile("application/octet-stream", savedFileName)
                    if (newFile != null) {
                        context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                            tempFile.inputStream().use { input ->
                                input.copyTo(output)
                            }
                        }
                        savedPath = newFile.uri.toString()
                        saveSuccess = true
                    }
                } else {
                    Log.e(TAG, "Cannot write to URI: ${user.storageFolderName}")
                }
            } else {
                // File 모드 (앱 내부 저장소)
                val userDir = getUserDir(user)
                val destFile = File(userDir, savedFileName)
                tempFile.copyTo(destFile, overwrite = true)
                savedPath = destFile.absolutePath
                saveSuccess = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "File save error", e)
            saveSuccess = false
        }
        
        if (!saveSuccess) {
            return newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                "application/json",
                JSONObject().put("error", "Failed to save file").toString()
            )
        }
        
        // 전역 통계용 DB 기록 (경로는 저장된 경로 사용)
        runCatching {
            runBlocking {
                val existing = dao.getByHash(hash)
                if (existing != null) {
                    dao.update(existing.copy(updatedAt = System.currentTimeMillis()))
                } else {
                    dao.insert(
                        BackupStatusEntity(
                            fileUri = savedPath, // 실제 저장된 경로 (Uri 또는 File path)
                            fileName = fileName,
                            filePath = savedPath,
                            fileSize = fileSize ?: tempFile.length(),
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
        }
        
        val response = JSONObject().apply {
            put("success", true)
            put("hash", hash)
            put("user", user.username)
        }
        
        return newFixedLengthResponse(
            Response.Status.OK,
            "application/json",
            response.toString()
        )
    }

    private fun handleStatus(session: IHTTPSession): Response {
        val totalFiles = runCatching {
            runBlocking { dao.getAll().size }
        }.getOrDefault(0)
        
        val response = JSONObject().apply {
            put("status", "running")
            put("port", listeningPort)
            put("totalFiles", totalFiles)
            put("authRequired", true)
        }
        
        return newFixedLengthResponse(
            Response.Status.OK,
            "application/json",
            response.toString()
        )
    }
}
