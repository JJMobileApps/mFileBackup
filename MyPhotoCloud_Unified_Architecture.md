# 🔧 MyPhotoCloud - 아키텍처 설계 (통합 앱)

## 1. 앱 모드

### 1.1 세 가지 모드

```kotlin
enum class AppMode {
    CLIENT_ONLY,    // 백업만 (다른 서버로)
    SERVER_ONLY,    // 서버만 (다른 폰들의 백업 수신)
    STANDALONE      // 서버+클라이언트 (자기 자신에게 백업)
}
```

### 1.2 모드별 기능

| 기능 | CLIENT_ONLY | SERVER_ONLY | STANDALONE |
|------|-------------|-------------|------------|
| 미디어 백업 | ✅ (외부 서버) | ❌ | ✅ (로컬 서버) |
| 백업 수신 | ❌ | ✅ | ✅ |
| 웹 갤러리 | ❌ | ✅ | ✅ |
| 자동 스캔 | ✅ | ❌ | ✅ |
| 포그라운드 서비스 | ❌ | ✅ (필수) | ✅ (선택) |

---

## 2. UI 구조

### 2.1 첫 실행 화면

```kotlin
@Composable
fun WelcomeScreen() {
    Column {
        Text("MyPhotoCloud에 오신 것을 환영합니다!")
        
        Text("이 기기를 어떻게 사용하시겠습니까?")
        
        // 옵션 1
        Card(onClick = { setMode(AppMode.STANDALONE) }) {
            Icon(Icons.Default.PhoneAndroid)
            Text("🖥️ 서버 + 백업")
            Text("이 폰에서 사진 보관 및 백업")
        }
        
        // 옵션 2
        Card(onClick = { setMode(AppMode.CLIENT_ONLY) }) {
            Icon(Icons.Default.CloudUpload)
            Text("📤 백업만")
            Text("다른 기기로 백업")
        }
        
        // 옵션 3
        Card(onClick = { setMode(AppMode.SERVER_ONLY) }) {
            Icon(Icons.Default.Storage)
            Text("🖥️ 서버만")
            Text("가족 사진 보관 전용")
        }
    }
}
```

### 2.2 메인 화면 (모드별)

#### CLIENT_ONLY
```
┌─────────────────────────┐
│  MyPhotoCloud           │
├─────────────────────────┤
│  📸 미디어              │
│  ├─ 사진: 1,234장       │
│  └─ 동영상: 56개        │
├─────────────────────────┤
│  🔄 백업 상태           │
│  ├─ 서버: 192.168.1.5   │
│  ├─ 백업 완료: 1,200    │
│  └─ 대기 중: 90         │
├─────────────────────────┤
│  [지금 백업하기]         │
│  [설정]                 │
└─────────────────────────┘
```

#### SERVER_ONLY
```
┌─────────────────────────┐
│  MyPhotoCloud 서버      │
├─────────────────────────┤
│  🖥️ 서버 상태           │
│  ├─ 실행 중 ✅          │
│  ├─ IP: 192.168.1.5     │
│  └─ 포트: 8080          │
├─────────────────────────┤
│  📊 통계                │
│  ├─ 총 파일: 5,432      │
│  ├─ 사용자: 3명         │
│  └─ 용량: 45.2 GB       │
├─────────────────────────┤
│  [웹 갤러리 열기]        │
│  [사용자 관리]          │
│  [설정]                 │
└─────────────────────────┘
```

#### STANDALONE
```
┌─────────────────────────┐
│  MyPhotoCloud           │
├─────────────────────────┤
│  📸 내 사진              │
│  ├─ 타임라인            │
│  ├─ 앨범                │
│  └─ 검색                │
├─────────────────────────┤
│  🔄 자동 백업 중...      │
│  ├─ 백업 완료: 1,200    │
│  └─ 대기 중: 34         │
├─────────────────────────┤
│  [웹 갤러리]            │
│  [설정]                 │
└─────────────────────────┘
```

---

## 3. 코드 구조

### 3.1 모듈 구성

```
app/src/main/java/com/myphotocloud/
├── MainActivity.kt
├── mode/
│   ├── AppModeManager.kt           # 모드 관리
│   └── ModeSelector.kt             # 첫 실행 모드 선택
│
├── client/                          # 클라이언트 모듈
│   ├── backup/
│   │   ├── MediaScanner.kt         # 미디어 스캔
│   │   ├── BackupWorker.kt         # 자동 백업
│   │   └── BackupClient.kt         # API 호출
│   └── ui/
│       ├── BackupScreen.kt
│       └── MediaListScreen.kt
│
├── server/                          # 서버 모듈
│   ├── BackupServer.kt             # HTTP 서버
│   ├── api/
│   │   ├── MediaRoutes.kt
│   │   ├── GalleryRoutes.kt
│   │   └── AuthRoutes.kt
│   ├── database/
│   │   └── MediaDatabase.kt
│   ├── media/
│   │   ├── ThumbnailGenerator.kt
│   │   └── MetadataExtractor.kt
│   └── ui/
│       ├── ServerDashboard.kt
│       └── UserManagement.kt
│
├── gallery/                         # 웹 갤러리 (Compose WebView)
│   ├── GalleryWebView.kt
│   └── assets/
│       └── index.html              # React 빌드 결과
│
└── common/                          # 공통
    ├── model/
    │   ├── MediaFile.kt
    │   └── User.kt
    └── utils/
        └── NetworkUtils.kt
```

### 3.2 모드 관리

```kotlin
// app/src/main/java/com/myphotocloud/mode/AppModeManager.kt
class AppModeManager(context: Context) {
    private val prefs = context.getSharedPreferences("app_mode", Context.MODE_PRIVATE)
    
    var currentMode: AppMode
        get() = AppMode.valueOf(
            prefs.getString("mode", AppMode.STANDALONE.name)!!
        )
        set(value) {
            prefs.edit().putString("mode", value.name).apply()
            applyMode(value)
        }
    
    private fun applyMode(mode: AppMode) {
        when (mode) {
            AppMode.CLIENT_ONLY -> {
                stopServer()
                enableBackup()
            }
            AppMode.SERVER_ONLY -> {
                startServer()
                disableBackup()
            }
            AppMode.STANDALONE -> {
                startServer()
                enableBackup()
            }
        }
    }
    
    private fun startServer() {
        val intent = Intent(context, BackupServerService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }
    
    private fun enableBackup() {
        AutoSyncAlarmReceiver.schedule(context)
    }
}
```

### 3.3 메인 액티비티

```kotlin
// app/src/main/java/com/myphotocloud/MainActivity.kt
class MainActivity : ComponentActivity() {
    private lateinit var modeManager: AppModeManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        modeManager = AppModeManager(this)
        
        setContent {
            MyPhotoCloudTheme {
                val isFirstRun = remember { 
                    getSharedPreferences("app", MODE_PRIVATE)
                        .getBoolean("first_run", true)
                }
                
                if (isFirstRun) {
                    WelcomeScreen(onModeSelected = { mode ->
                        modeManager.currentMode = mode
                        getSharedPreferences("app", MODE_PRIVATE)
                            .edit()
                            .putBoolean("first_run", false)
                            .apply()
                    })
                } else {
                    MainScreen(mode = modeManager.currentMode)
                }
            }
        }
    }
}

@Composable
fun MainScreen(mode: AppMode) {
    when (mode) {
        AppMode.CLIENT_ONLY -> ClientScreen()
        AppMode.SERVER_ONLY -> ServerScreen()
        AppMode.STANDALONE -> StandaloneScreen()
    }
}
```

---

## 4. 서버 시작/중지

### 4.1 서버 서비스

```kotlin
// app/src/main/java/com/myphotocloud/server/BackupServerService.kt
class BackupServerService : Service() {
    private var server: BackupServer? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 포그라운드 알림
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        
        // 서버 시작
        server = BackupServer(applicationContext)
        server?.start(port = 8080)
        
        return START_STICKY
    }
    
    override fun onDestroy() {
        server?.stop()
        super.onDestroy()
    }
    
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MyPhotoCloud 서버 실행 중")
            .setContentText("IP: ${getLocalIpAddress()}")
            .setSmallIcon(R.drawable.ic_server)
            .setOngoing(true)
            .build()
    }
}
```

### 4.2 자동 시작 (선택)

```kotlin
// app/src/main/java/com/myphotocloud/receiver/BootReceiver.kt
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val modeManager = AppModeManager(context)
            
            // 서버 모드이고, 자동 시작 설정이면
            if (modeManager.currentMode != AppMode.CLIENT_ONLY) {
                val settings = getSharedPreferences("server_settings", MODE_PRIVATE)
                if (settings.getBoolean("start_on_boot", false)) {
                    val intent = Intent(context, BackupServerService::class.java)
                    ContextCompat.startForegroundService(context, intent)
                }
            }
        }
    }
}
```

---

## 5. 설정 화면

```kotlin
@Composable
fun SettingsScreen() {
    val modeManager = remember { AppModeManager(LocalContext.current) }
    var currentMode by remember { mutableStateOf(modeManager.currentMode) }
    
    Column {
        Text("앱 모드", style = MaterialTheme.typography.h6)
        
        RadioButton(
            selected = currentMode == AppMode.CLIENT_ONLY,
            onClick = { 
                currentMode = AppMode.CLIENT_ONLY
                modeManager.currentMode = currentMode
            }
        ) {
            Text("백업만 (클라이언트)")
        }
        
        RadioButton(
            selected = currentMode == AppMode.SERVER_ONLY,
            onClick = { 
                currentMode = AppMode.SERVER_ONLY
                modeManager.currentMode = currentMode
            }
        ) {
            Text("서버만")
        }
        
        RadioButton(
            selected = currentMode == AppMode.STANDALONE,
            onClick = { 
                currentMode = AppMode.STANDALONE
                modeManager.currentMode = currentMode
            }
        ) {
            Text("서버 + 백업")
        }
        
        Divider()
        
        // 서버 설정 (서버 모드일 때만)
        if (currentMode != AppMode.CLIENT_ONLY) {
            ServerSettings()
        }
        
        // 백업 설정 (클라이언트 모드일 때만)
        if (currentMode != AppMode.SERVER_ONLY) {
            BackupSettings()
        }
    }
}
```

---

## 6. 장점 요약

### ✅ 하나의 앱 (통합)

1. **간편한 배포**
   - APK 하나만 관리
   - 업데이트 간편

2. **유연성**
   - 언제든 모드 변경 가능
   - 테스트/개발 용이

3. **코드 재사용**
   - 서버와 클라이언트가 같은 모델 사용
   - API 중복 제거

4. **사용자 경험**
   - 혼란 없음 (앱 하나만 설치)
   - 직관적인 모드 선택

---

## 7. 예시 시나리오

### 시나리오: 가족 사진 백업

**설정:**
1. **아빠 폰** (Galaxy S24, 항상 충전)
   - MyPhotoCloud 설치
   - 모드: **SERVER_ONLY**
   - IP: 192.168.1.5

2. **엄마 폰** (iPhone → Android로 가정)
   - MyPhotoCloud 설치  
   - 모드: **CLIENT_ONLY**
   - 서버 주소: 192.168.1.5

3. **자녀 폰** (Galaxy A54)
   - MyPhotoCloud 설치
   - 모드: **CLIENT_ONLY**
   - 서버 주소: 192.168.1.5

**결과:**
- 엄마, 자녀가 사진 찍으면 → 아빠 폰으로 자동 백업
- 아빠 폰에서 웹 갤러리 → 모든 가족 사진 보기
- 침대 옆 충전 중인 아빠 폰 = 가족 사진 서버 🎉

---

**결론: 하나의 통합 앱이 최선입니다!** ✅

**문서 작성일**: 2026-01-01  
**버전**: 2.0 (통합 앱)  
**작성자**: AI Assistant
