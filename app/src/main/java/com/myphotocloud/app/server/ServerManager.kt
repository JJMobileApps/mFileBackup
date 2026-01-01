package com.myphotocloud.app.server

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.InetAddress

/**
 * 서버 상태 관리
 */
class ServerManager(private val context: Context) {
    
    private val TAG = "ServerManager"
    private var server: PhotoCloudServer? = null
    private val defaultPort = 8080
    
    private val _serverState = MutableStateFlow<ServerState>(ServerState.Stopped)
    val serverState: StateFlow<ServerState> = _serverState.asStateFlow()
    
    /**
     * 서버 시작
     */
    fun startServer(port: Int = defaultPort): Result<String> {
        return try {
            if (server != null) {
                return Result.failure(Exception("서버가 이미 실행 중입니다"))
            }
            
            server = PhotoCloudServer(context, port).apply {
                start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
            }
            
            val ipAddress = getWifiIpAddress()
            _serverState.value = ServerState.Running(ipAddress, port)
            
            Log.d(TAG, "Server started on $ipAddress:$port")
            Result.success("http://$ipAddress:$port")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start server", e)
            _serverState.value = ServerState.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }
    
    /**
     * 서버 중지
     */
    fun stopServer() {
        try {
            server?.stop()
            server = null
            _serverState.value = ServerState.Stopped
            Log.d(TAG, "Server stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop server", e)
        }
    }
    
    /**
     * WiFi IP 주소 가져오기
     */
    private fun getWifiIpAddress(): String {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ipInt = wifiManager.connectionInfo.ipAddress
            
            if (ipInt == 0) {
                "Not connected to WiFi"
            } else {
                val ip = InetAddress.getByAddress(
                    byteArrayOf(
                        (ipInt and 0xff).toByte(),
                        (ipInt shr 8 and 0xff).toByte(),
                        (ipInt shr 16 and 0xff).toByte(),
                        (ipInt shr 24 and 0xff).toByte()
                    )
                )
                ip.hostAddress ?: "Unknown"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get IP address", e)
            "Unknown"
        }
    }
    
    /**
     * 서버 상태
     */
    sealed class ServerState {
        object Stopped : ServerState()
        data class Running(val ipAddress: String, val port: Int) : ServerState()
        data class Error(val message: String) : ServerState()
    }
}
