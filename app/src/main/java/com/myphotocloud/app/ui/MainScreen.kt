// MainScreen.kt
package com.myphotocloud.app.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.myphotocloud.app.model.AppMode
import com.myphotocloud.app.database.AppDatabase
import com.myphotocloud.app.database.DeviceApprovalEntity
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import android.os.storage.StorageManager
import android.provider.DocumentsContract

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

private const val KEY_REQUIRE_APPROVAL = "require_approval"

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

    val serverPrefs = remember {
        context.getSharedPreferences("server_settings", android.content.Context.MODE_PRIVATE)
    }

    var requireApproval by remember {
        mutableStateOf(serverPrefs.getBoolean(KEY_REQUIRE_APPROVAL, false))
    }

    val deviceApprovalDao = remember {
        AppDatabase.getDatabase(context).deviceApprovalDao()
    }
    val pendingApprovals by deviceApprovalDao
        .getByStatusFlow(DeviceApprovalEntity.STATUS_PENDING)
        .collectAsState(initial = emptyList())
    
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
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "연결주소:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
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

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "사용자 접속 승인 사용",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "클라이언트가 최초 접속 시 서버의 승인 후 접속 가능",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = requireApproval,
                        onCheckedChange = { checked ->
                            requireApproval = checked
                            serverPrefs.edit().putBoolean(KEY_REQUIRE_APPROVAL, checked).apply()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (requireApproval) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "승인 요청 목록",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "대기: ${pendingApprovals.size}개",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (pendingApprovals.isEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "승인 대기 중인 단말이 없습니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        pendingApprovals.forEach { approval ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = approval.deviceName ?: "알 수 없는 단말",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = approval.deviceId,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(
                                            onClick = {
                                                val scope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO)
                                                scope.launch {
                                                    deviceApprovalDao.updateStatus(
                                                        approval.deviceId,
                                                        DeviceApprovalEntity.STATUS_DENIED
                                                    )
                                                }
                                            }
                                        ) {
                                            Text("거절")
                                        }
                                        Button(
                                            onClick = {
                                                val scope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO)
                                                scope.launch {
                                                    deviceApprovalDao.updateStatus(
                                                        approval.deviceId,
                                                        DeviceApprovalEntity.STATUS_APPROVED
                                                    )
                                                }
                                            }
                                        ) {
                                            Text("승인")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

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
        
        // 사용자 관리 버튼
        var showUserManagement by remember { mutableStateOf(false) }
        val userRepository = remember { com.myphotocloud.app.repository.UserRepository(context) }
        
        OutlinedButton(
            onClick = { showUserManagement = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Person, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("사용자 관리")
        }
        
        if (showUserManagement) {
            UserManagementDialog(
                userRepository = userRepository,
                onDismiss = { showUserManagement = false }
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
                    text = "1. '사용자 관리'에서 사용자를 생성하세요",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "2. '서버 시작' 버튼을 클릭하세요",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "3. 클라이언트 기기에서 아이디/비번을 입력하여 연결하세요",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementDialog(
    userRepository: com.myphotocloud.app.repository.UserRepository,
    onDismiss: () -> Unit
) {
    val users by userRepository.allUsers.collectAsState(initial = emptyList())
    val context = LocalContext.current
    // 0: 목록 화면, 1: 사용자 추가 화면
    var currentScreen by remember { mutableStateOf(0) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            if (currentScreen == 0) {
                // 사용자 목록 화면
                Column(modifier = Modifier.fillMaxSize()) {
                    TopAppBar(
                        title = { Text("사용자 관리") },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "닫기")
                            }
                        },
                        actions = {
                            TextButton(onClick = { currentScreen = 1 }) {
                                Text("추가")
                            }
                        }
                    )
                    
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        items(users.size) { index ->
                            val user = users[index]
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            user.username, 
                                            style = MaterialTheme.typography.titleMedium, 
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        val folderDisplay = formatStorageFolderNameForDisplay(
                                            context = context,
                                            storageFolderName = user.storageFolderName
                                        )
                                        Text(
                                            "저장소: $folderDisplay", 
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = {
                                        val scope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO)
                                        scope.launch {
                                            userRepository.deleteUser(user)
                                        }
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "삭제", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                        
                        if (users.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "등록된 사용자가 없습니다.\n우측 상단 '추가' 버튼을 눌러보세요.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 16.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // 사용자 추가 화면
                AddUserScreen(
                    onBack = { currentScreen = 0 },
                    onAdd = { username, password, folderName ->
                        val scope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO)
                        scope.launch {
                            userRepository.addUser(username, password, folderName)
                        }
                        currentScreen = 0
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUserScreen(
    onBack: () -> Unit,
    onAdd: (String, String, String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var folderName by remember { mutableStateOf("") }
    var folderDisplayName by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    
    // 폴더 선택 Launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            // 영구적 권한 획득 (앱 재실행 후에도 접근 가능하도록)
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(it, takeFlags)
                folderName = it.toString()
                
                // 폴더 이름 가져오기
                val docFile = DocumentFile.fromTreeUri(context, it)
                folderDisplayName = docFile?.name ?: it.lastPathSegment ?: "외부 저장소"
                
            } catch (e: Exception) {
                e.printStackTrace()
                folderName = it.toString()
                folderDisplayName = "선택됨 (권한 오류 가능성)"
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("새 사용자 추가") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "뒤로")
                }
            },
            actions = {
                TextButton(
                    onClick = { onAdd(username, password, folderName) },
                    enabled = username.isNotEmpty() && password.isNotEmpty() && folderName.isNotEmpty()
                ) {
                    Text("저장")
                }
            }
        )
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("아이디") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("비밀번호") },
                singleLine = true,
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            
            Column {
                Text(
                    "저장소",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    // 폴더가 URI 형태이면 표시 이름을 보여주고, 아니면 직접 입력/생성된 값 보여줌
                    value = if (folderName.startsWith("content://") && folderDisplayName.isNotEmpty()) 
                                folderDisplayName 
                            else 
                                folderName,
                    onValueChange = { 
                        folderName = it 
                        folderDisplayName = "" // 수동 입력 시 표시 이름 초기화
                    },
                    label = { Text("폴더 경로") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false, // 직접 수정보다는 선택 권장
                    trailingIcon = {
                        IconButton(onClick = { launcher.launch(null) }) {
                            Icon(Icons.Default.FolderOpen, contentDescription = "폴더 선택")
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = { launcher.launch(null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("시스템 폴더에서 선택")
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ClientScreenContent() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { com.myphotocloud.app.repository.MediaRepository(context) }

    val clientPrefs = remember {
        context.getSharedPreferences("client_settings", android.content.Context.MODE_PRIVATE)
    }

    var serverHost by remember { mutableStateOf(clientPrefs.getString("server_host", "") ?: "") }
    var serverPort by remember { mutableStateOf(clientPrefs.getInt("server_port", 8080).toString()) }
    var connectionResult by remember { mutableStateOf<String?>(null) }
    var isConnecting by remember { mutableStateOf(false) }

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
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "서버 연결",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = serverHost,
                    onValueChange = {
                        serverHost = it
                        clientPrefs.edit().putString("server_host", it).apply()
                    },
                    label = { Text("서버 IP/도메인") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = serverPort,
                    onValueChange = {
                        serverPort = it
                        it.toIntOrNull()?.let { p -> clientPrefs.edit().putInt("server_port", p).apply() }
                    },
                    label = { Text("포트") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val host = serverHost.trim()
                            val port = serverPort.toIntOrNull() ?: -1
                            if (host.isBlank() || port !in 1..65535) {
                                connectionResult = "서버 주소/포트를 확인해 주세요."
                                return@Button
                            }

                            val baseUrl = "http://$host:$port"
                            isConnecting = true
                            connectionResult = null
                            coroutineScope.launch {
                                val result = com.myphotocloud.app.utils.ServerApiClient.checkStatus(baseUrl)
                                connectionResult = "연결 테스트: " + com.myphotocloud.app.utils.ServerApiClient.formatResultForUi(result)
                                isConnecting = false
                            }
                        },
                        enabled = !isConnecting,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isConnecting) "확인 중..." else "연결 테스트")
                    }

                    OutlinedButton(
                        onClick = {
                            val host = serverHost.trim()
                            val port = serverPort.toIntOrNull() ?: -1
                            if (host.isBlank() || port !in 1..65535) {
                                connectionResult = "서버 주소/포트를 확인해 주세요."
                                return@OutlinedButton
                            }

                            val baseUrl = "http://$host:$port"
                            isConnecting = true
                            connectionResult = null
                            coroutineScope.launch {
                                val result = com.myphotocloud.app.utils.ServerApiClient.requestApproval(context, baseUrl)
                                connectionResult = "승인 요청: " + com.myphotocloud.app.utils.ServerApiClient.formatResultForUi(result)
                                isConnecting = false
                            }
                        },
                        enabled = !isConnecting,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("승인 요청")
                    }
                }

                if (connectionResult != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        connectionResult!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 상단 통계
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
private fun MediaFileItem(
    file: com.myphotocloud.app.model.MediaFile,
    onUploadClick: () -> Unit = {}
) {
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

private fun formatStorageFolderNameForDisplay(
    context: android.content.Context,
    storageFolderName: String
): String {
    if (!storageFolderName.startsWith("content://")) return storageFolderName

    return runCatching {
        val uri = Uri.parse(storageFolderName)
        val docId = DocumentsContract.getTreeDocumentId(uri)
        val split = docId.split(":", limit = 2)
        val volumeId = split.getOrNull(0)?.takeIf { it.isNotBlank() }
        val relativePath = split.getOrNull(1)?.takeIf { it.isNotBlank() }

        val volumeLabel = when {
            volumeId == null -> "외부"
            volumeId.equals("primary", ignoreCase = true) -> "내부"
            else -> resolveExternalVolumeLabel(context, volumeId)
        }

        if (relativePath == null) volumeLabel else "$volumeLabel/$relativePath"
    }.getOrElse {
        "외부"
    }
}

private fun resolveExternalVolumeLabel(context: android.content.Context, volumeId: String): String {
    val storageManager = context.getSystemService(StorageManager::class.java) ?: return "외부"
    val volumes = runCatching { storageManager.storageVolumes }.getOrNull().orEmpty()

    val nonPrimary = volumes.filter { !it.isPrimary }
    val target = nonPrimary.firstOrNull { it.uuid.equals(volumeId, ignoreCase = true) } ?: return "외부"

    val removable = nonPrimary.filter { it.isRemovable }
    val (sdVolumes, usbVolumes) = removable
        .sortedBy { it.uuid ?: "" }
        .partition {
            runCatching { it.getDescription(context) }.getOrNull()
                ?.let { desc -> desc.contains("sd", ignoreCase = true) || desc.contains("카드") }
                ?: false
        }

    val sdIndex = sdVolumes.indexOfFirst { it.uuid.equals(volumeId, ignoreCase = true) }
    if (sdIndex >= 0) return "SD${sdIndex + 1}"

    val usbIndex = usbVolumes.indexOfFirst { it.uuid.equals(volumeId, ignoreCase = true) }
    if (usbIndex >= 0) return "외장${usbIndex + 1}"

    return "외부"
}


