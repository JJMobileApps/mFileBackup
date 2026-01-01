package com.myphotocloud.app.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object ServerApiClient {
    private val client: OkHttpClient = OkHttpClient.Builder().build()
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    data class ApiResult(
        val httpCode: Int,
        val rawBody: String,
        val error: String? = null
    )

    suspend fun checkStatus(baseUrl: String): ApiResult = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(baseUrl.trimEnd('/') + "/api/status")
            .get()
            .build()

        runCatching {
            client.newCall(request).execute().use { resp ->
                ApiResult(
                    httpCode = resp.code,
                    rawBody = resp.body?.string().orEmpty()
                )
            }
        }.getOrElse { e ->
            ApiResult(httpCode = -1, rawBody = "", error = e.message ?: "Unknown error")
        }
    }

    suspend fun requestApproval(context: Context, baseUrl: String): ApiResult = withContext(Dispatchers.IO) {
        val deviceId = DeviceIdUtils.getOrCreateDeviceId(context)
        val deviceName = DeviceIdUtils.getDeviceName()

        val request = Request.Builder()
            .url(baseUrl.trimEnd('/') + "/api/approval/request")
            .post("{}".toRequestBody(JSON_MEDIA_TYPE))
            .header("X-Device-Id", deviceId)
            .header("X-Device-Name", deviceName)
            .build()

        runCatching {
            client.newCall(request).execute().use { resp ->
                ApiResult(
                    httpCode = resp.code,
                    rawBody = resp.body?.string().orEmpty()
                )
            }
        }.getOrElse { e ->
            ApiResult(httpCode = -1, rawBody = "", error = e.message ?: "Unknown error")
        }
    }

    fun formatResultForUi(result: ApiResult): String {
        if (result.error != null) return "네트워크 오류: ${result.error}"

        val body = result.rawBody.trim()
        val json = runCatching { if (body.startsWith("{")) JSONObject(body) else null }.getOrNull()

        val error = json?.optString("error")?.takeIf { it.isNotBlank() }
        val status = json?.optString("status")?.takeIf { it.isNotBlank() }
        val message = json?.optString("message")?.takeIf { it.isNotBlank() }

        return buildString {
            append("HTTP ${result.httpCode}")
            if (error != null) append(" / error=$error")
            if (status != null) append(" / status=$status")
            if (message != null) append(" / message=$message")
            if (error == null && status == null && message == null && body.isNotBlank()) {
                append("\n")
                append(body)
            }
        }
    }
}
