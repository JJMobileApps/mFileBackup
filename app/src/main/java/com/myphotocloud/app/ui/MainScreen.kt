// MainScreen.kt
package com.myphotocloud.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.myphotocloud.app.model.AppMode
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    mode: AppMode,
    onSettingsClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MyPhotoCloud") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, "설정")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (mode) {
                AppMode.CLIENT_ONLY -> ClientScreen()
                AppMode.SERVER_ONLY -> ServerScreen()
                AppMode.STANDALONE -> StandaloneScreen()
            }
        }
    }
}

@Composable
private fun ClientScreen() {
    ClientScreenContent()
}

@Composable
private fun ServerScreen() {
    ServerScreenContent()
}

@Composable
private fun StandaloneScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("서버", "백업")
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 탭 레이아웃
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) },
                    icon = {
                        Icon(
                            imageVector = if (index == 0) Icons.Default.Storage else Icons.Default.CloudUpload,
                            contentDescription = title
                        )
                    }
                )
            }
        }
        
        // 탭 내용
        when (selectedTab) {
            0 -> ServerScreenContent()
            1 -> ClientScreenContent()
        }
    }
}

// ServerScreen을 재사용 가능하도록 내용만 분리
@Composable
fun ServerScreenContent() {
    val context = LocalContext.current
    val serverManager = remember { com.myphotocloud.app.server.ServerManager(context) }
    val serverState by serverManager.serverState.collectAsState()
    var portText by remember { mutableStateOf("8080") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 서버 상태 카드
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when (serverState) {
                    is com.myphotocloud.app.server.ServerManager.ServerState.Running -> 
                        MaterialTheme.colorScheme.primaryContainer
                    is com.myphotocloud.app.server.ServerManager.ServerState.Error -> 
                        MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "서버 상태",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = when (serverState) {
                            is com.myphotocloud.app.server.ServerManager.ServerState.Running -> Icons.Default.CheckCircle
                            is com.myphotocloud.app.server.ServerManager.ServerState.Error -> Icons.Default.Error
                            else -> Icons.Default.Circle
                        },
                        contentDescription = null,
                        tint = when (serverState) {
                            is com.myphotocloud.app.server.ServerManager.ServerState.Running -> 
                                MaterialTheme.colorScheme.primary
                            is com.myphotocloud.app.server.ServerManager.ServerState.Error -> 
                                MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                when (val state = serverState) {
                    is com.myphotocloud.app.server.ServerManager.ServerState.Running -> {
                        Text(
                            text = "실행 중",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "IP 주소: ${state.ipAddress}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "포트: ${state.port}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "클라이언트 연결 주소:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "http://${state.ipAddress}:${state.port}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    is com.myphotocloud.app.server.ServerManager.ServerState.Error -> {
                        Text(
                            text = "오류",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    else -> {
                        Text(
                            text = "중지됨",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 포트 번호 입력 (서버가 중지된 경우에만)
        if (serverState !is com.myphotocloud.app.server.ServerManager.ServerState.Running) {
            OutlinedTextField(
                value = portText,
                onValueChange = { portText = it.filter { char -> char.isDigit() } },
                label = { Text("포트 번호") },
                placeholder = { Text("8080") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // 시작/중지 버튼
        Button(
            onClick = {
                when (serverState) {
                    is com.myphotocloud.app.server.ServerManager.ServerState.Running -> {
                        serverManager.stopServer()
                    }
                    else -> {
                        val port = portText.toIntOrNull() ?: 8080
                        if (port in 1..65535) {
                            serverManager.startServer(port)
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = if (serverState is com.myphotocloud.app.server.ServerManager.ServerState.Running) {
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            } else {
                ButtonDefaults.buttonColors()
            }
        ) {
            Icon(
                imageVector = if (serverState is com.myphotocloud.app.server.ServerManager.ServerState.Running) 
                    Icons.Default.Stop 
                else 
                    Icons.Default.PlayArrow,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (serverState is com.myphotocloud.app.server.ServerManager.ServerState.Running)
                    "서버 중지"
                else
                    "서버 시작"
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 사용 안내
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "사용 방법",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "1. '서버 시작' 버튼을 클릭하세요",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "2. 표시된 IP 주소를 클라이언트 기기에 입력하세요",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "3. 클라이언트 기기에서 백업을 시작하세요",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

// ClientScreen을 재사용 가능하도록 내용만 분리
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ClientScreenContent() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { com.myphotocloud.app.repository.MediaRepository(context) }
    
    var mediaFiles by remember { mutableStateOf<List<com.myphotocloud.app.model.MediaFile>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }
    var isCalculatingHash by remember { mutableStateOf(false) }
    var hashProgress by remember { mutableStateOf<com.myphotocloud.app.repository.MediaRepository.HashProgress?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    
    // Android 버전에 따른 권한 요청
    val permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        // Android 13+
        listOf(
            android.Manifest.permission.READ_MEDIA_IMAGES,
            android.Manifest.permission.READ_MEDIA_VIDEO
        )
    } else {
        // Android 12 이하
        listOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    
    val permissionState = rememberMultiplePermissionsState(permissions)
    
    // 통계
    val totalFiles = mediaFiles.size
    val backedUpFiles = mediaFiles.count { it.isBackedUp }
    val pendingFiles = totalFiles - backedUpFiles
    val totalSize = mediaFiles.sumOf { it.fileSize }
    val backedUpSize = mediaFiles.filter { it.isBackedUp }.sumOf { it.fileSize }
    
    // 미디어 스캔 함수
    fun scanMedia() {
        if (permissionState.allPermissionsGranted) {
            isScanning = true
            errorMessage = null
            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val scannedFiles = repository.scanAllMedia()
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        mediaFiles = scannedFiles
                        isScanning = false
                    }
                } catch (e: Exception) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        errorMessage = e.message ?: "알 수 없는 오류"
                        isScanning = false
                    }
                }
            }
        } else {
            showPermissionDialog = true
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 상단 통계
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "백업 상태",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "전체: ${totalFiles}개",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "완료: ${backedUpFiles}개",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "대기: ${pendingFiles}개",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "%.1f MB".format(totalSize / (1024f * 1024f)),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "%.1f MB".format(backedUpSize / (1024f * 1024f)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 스캔 버튼
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { scanMedia() },
                modifier = Modifier.weight(1f),
                enabled = !isScanning && !isCalculatingHash
            ) {
                Text(if (isScanning) "스캔 중..." else "파일 스캔")
            }
            
            Button(
                onClick = {
                    if (mediaFiles.isNotEmpty()) {
                        isCalculatingHash = true
                        hashProgress = null
                        coroutineScope.launch {
                            repository.calculateHashesWithProgress(mediaFiles)
                                .collect { progress ->
                                    withContext(Dispatchers.Main) {
                                        hashProgress = progress
                                    }
                                }
                            withContext(Dispatchers.Main) {
                                isCalculatingHash = false
                                // 해시 계산 후 파일 목록 새로고침
                                scanMedia()
                            }
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !isScanning && !isCalculatingHash && mediaFiles.isNotEmpty()
            ) {
                Text(if (isCalculatingHash) "계산 중..." else "해시 계산")
            }
        }
        
        // 해시 계산 진행률
        hashProgress?.let { progress ->
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "해시 계산 중",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = "${progress.processed}/${progress.total} (${progress.percentage.toInt()}%)",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = progress.percentage / 100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = progress.currentFileName,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
        
        // 에러 메시지
        errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "오류: $error",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 파일 목록
        if (mediaFiles.isEmpty() && !isScanning) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "스캔 버튼을 눌러 미디어 파일을 찾으세요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn {
                items(mediaFiles.size) { index ->
                    val file = mediaFiles[index]
                    MediaFileItem(file)
                }
            }
        }
    }
    
    // 권한 요청 다이얼로그
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("미디어 접근 권한 필요") },
            text = {
                Text(
                    "사진과 동영상을 스캔하려면 미디어 파일 접근 권한이 필요합니다.\n\n" +
                    "권한을 허용하시겠습니까?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        permissionState.launchMultiplePermissionRequest()
                        showPermissionDialog = false
                    }
                ) {
                    Text("허용")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("취소")
                }
            }
        )
    }
}

@Composable
private fun MediaFileItem(file: com.myphotocloud.app.model.MediaFile) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = file.fileName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    // 해시 상태 아이콘
                    if (file.hash != null) {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = "해시 계산됨",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${if (file.isVideo) "🎥" else "📷"} ${file.formattedSize}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 백업 상태 아이콘
            Icon(
                imageVector = if (file.isBackedUp) Icons.Default.CheckCircle else Icons.Default.CloudUpload,
                contentDescription = if (file.isBackedUp) "백업 완료" else "백업 대기",
                tint = if (file.isBackedUp) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


