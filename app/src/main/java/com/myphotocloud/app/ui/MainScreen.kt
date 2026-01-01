// MainScreen.kt
package com.myphotocloud.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.myphotocloud.app.model.AppMode

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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Storage,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "서버 모드",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "다른 기기의 백업을 수신합니다",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // TODO: 서버 상태 표시
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text("서버 상태", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("실행 중...", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// ClientScreen을 재사용 가능하도록 내용만 분리
@Composable
fun ClientScreenContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CloudUpload,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "백업 모드",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "사진과 동영상을 자동으로 백업합니다",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // TODO: 백업 상태 표시
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text("백업 상태", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("준비 중...", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

