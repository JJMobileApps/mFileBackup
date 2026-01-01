// WelcomeScreen.kt
@file:OptIn(ExperimentalMaterial3Api::class)

package com.myphotocloud.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.myphotocloud.app.model.AppMode

@Composable
fun WelcomeScreen(
    onModeSelected: (AppMode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 타이틀
        Icon(
            imageVector = Icons.Default.CloudQueue,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "MyPhotoCloud에\n오신 것을 환영합니다",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "이 기기를 어떻게 사용하시겠습니까?",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // 모드 옵션들
        ModeOptionCard(
            icon = Icons.Default.PhoneAndroid,
            mode = AppMode.STANDALONE,
            onClick = { onModeSelected(AppMode.STANDALONE) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        ModeOptionCard(
            icon = Icons.Default.CloudUpload,
            mode = AppMode.CLIENT_ONLY,
            onClick = { onModeSelected(AppMode.CLIENT_ONLY) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        ModeOptionCard(
            icon = Icons.Default.Storage,
            mode = AppMode.SERVER_ONLY,
            onClick = { onModeSelected(AppMode.SERVER_ONLY) }
        )
    }
}

@Composable
private fun ModeOptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    mode: AppMode,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = mode.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = mode.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
