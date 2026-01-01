# MyPhotoCloud 프로젝트 개발 규칙

> 이 문서는 AI와 개발자가 코드를 생성/수정할 때 반드시 따라야 할 규칙을 정의합니다.

---

## 📌 기본 정보

- **프로젝트명**: MyPhotoCloud
- **패키지명**: `com.myphotocloud.app`
- **언어**: Kotlin
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34
- **Compile SDK**: 34
- **Java Version**: 17
- **빌드 시스템**: Gradle (Kotlin DSL)

---
## 기타요구사항
- **질문 및 대답은 한국어로**
- **사용자가 질문이나 요청을 하면 수정 계획을 알려주고 확인을 받은 후 진행**
- **화면이나 프로그램은 최대한 재사용할 수 있는 형태로 작성**
- **빌드는 사용자가 직접 터미널에서 실행하고, 오류 발생시 사용자가 오류 메세지를 제공**

## 🏗️ 아키텍처 패턴

### 전체 아키텍처
- **패턴**: MVVM + Repository Pattern
- **UI**: Jetpack Compose (XML 레이아웃 **절대 사용 금지**)
- **의존성 주입**: Manual DI (필요시 나중에 Hilt 고려)
- **비동기 처리**: Coroutines + Flow
- **내비게이션**: Jetpack Compose Navigation

### 레이어 구조
```
UI Layer (Composables + ViewModel)
    ↓
Domain Layer (Use Cases - 선택사항)
    ↓
Data Layer (Repository + Data Sources)
```

### 앱 모드 구조
이 앱은 **3가지 모드**로 동작합니다:
1. **CLIENT_ONLY** - 백업 클라이언트만 (다른 서버로 백업)
2. **SERVER_ONLY** - 백업 서버만 (다른 기기에서 백업 받음)
3. **STANDALONE** - 서버 + 클라이언트 (자체 백업 및 서버 역할)

**모든 기능은 이 세 가지 모드를 고려해서 개발해야 합니다.**

---

## 📁 프로젝트 구조

### 패키지 구조
```
com.myphotocloud.app/
├── MyPhotoCloudApp.kt           # Application 클래스
├── MainActivity.kt              # 메인 액티비티
│
├── model/                       # 데이터 모델
│   ├── AppMode.kt              # 앱 모드 enum
│   ├── MediaFile.kt            # 미디어 파일 모델
│   └── User.kt                 # 사용자 모델
│
├── mode/                        # 모드 관리
│   └── AppModeManager.kt       # 모드 전환 및 관리
│
├── ui/                          # UI 레이어
│   ├── theme/                  # 테마 설정
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   ├── WelcomeScreen.kt        # 환영 화면 (모드 선택)
│   ├── MainScreen.kt           # 메인 화면 (탭 구조)
│   ├── client/                 # 클라이언트 화면들
│   ├── server/                 # 서버 화면들
│   └── standalone/             # 스탠드얼론 화면들
│
├── viewmodel/                   # ViewModel
│   ├── WelcomeViewModel.kt
│   └── MainViewModel.kt
│
├── repository/                  # 데이터 레이어
│   ├── MediaRepository.kt      # 미디어 데이터 소스
│   ├── UserRepository.kt       # 사용자 데이터
│   └── SettingsRepository.kt   # 설정 데이터
│
├── client/                      # 백업 클라이언트
│   ├── MediaScanner.kt         # 미디어 스캔
│   └── BackupWorker.kt         # 백업 워커
│
├── server/                      # 백업 서버
│   ├── BackupServerService.kt  # 서버 서비스
│   └── MediaRepository.kt      # 서버용 미디어 저장소
│
├── service/                     # 공통 서비스
│   └── BackupKeepAliveService.kt
│
└── receiver/                    # 브로드캐스트 리시버
    ├── AutoSyncAlarmReceiver.kt
    └── BootReceiver.kt
```

---

## 💻 코딩 컨벤션

### 네이밍 규칙

#### 파일명
- **Composable Screen**: `[Name]Screen.kt` (예: `WelcomeScreen.kt`)
- **ViewModel**: `[Screen]ViewModel.kt` (예: `WelcomeViewModel.kt`)
- **Repository**: `[Domain]Repository.kt` (예: `MediaRepository.kt`)
- **Service**: `[Purpose]Service.kt` (예: `BackupServerService.kt`)
- **Model**: `[Name].kt` (예: `MediaFile.kt`)

#### 함수명
- **Composable**: PascalCase (예: `WelcomeScreen()`, `MainTopBar()`)
- **일반 함수**: camelCase (예: `startBackup()`, `scanMedia()`)
- **ViewModel 함수**: camelCase (예: `onModeSelected()`, `loadData()`)

#### 변수명
- **일반 변수**: camelCase (예: `mediaFile`, `backupStatus`)
- **상수**: UPPER_SNAKE_CASE (예: `MAX_RETRY_COUNT`, `DEFAULT_PORT`)
- **Composable State**: `by remember` 또는 `rememberSaveable` 사용

### 코드 스타일
```kotlin
// ✅ 좋은 예
@Composable
fun WelcomeScreen(
    onModeSelected: (AppMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ...
    }
}

// ❌ 나쁜 예 - XML 레이아웃 사용
setContentView(R.layout.activity_main)
```

---

## ✅ 필수 규칙

### 1. UI 개발
- ✅ **Jetpack Compose만 사용**
- ✅ 모든 문자열은 `strings.xml`에 정의
- ✅ 색상은 `Color.kt`에 정의
- ✅ 공통 UI 컴포넌트는 재사용 가능하게 작성
- ✅ Preview 함수 작성 권장
```kotlin
@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    MyPhotoCloudTheme {
        WelcomeScreen(onModeSelected = {})
    }
}
```

### 2. 비동기 처리
- ✅ ViewModel에서는 `viewModelScope` 사용
- ✅ Repository에서는 `suspend` 함수 사용
- ✅ UI에서 직접 코루틴 사용 시 `LaunchedEffect` 사용
```kotlin
// ✅ 올바른 코루틴 사용
viewModelScope.launch {
    repository.fetchData()
}

LaunchedEffect(key1) {
    // 초기화 로직
}
```

### 3. 상태 관리
- ✅ ViewModel에서 `StateFlow` 또는 `MutableState` 사용
- ✅ UI에서는 `collectAsState()` 또는 `by remember` 사용
- ✅ 단방향 데이터 플로우 유지 (UI → Event → ViewModel → State → UI)

### 4. 데이터 저장
- ✅ 설정값: DataStore (SharedPreferences 대신)
- ✅ 구조화된 데이터: Room Database
- ✅ 파일: 내부/외부 저장소 (권한 체크 필수)

### 5. 권한 처리
- ✅ 런타임 권한은 `Accompanist Permissions` 또는 `ActivityResultContracts` 사용
- ✅ 권한이 거부되었을 때 사용자에게 이유 설명
- ✅ 필수 권한:
  - `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO` (Android 13+)
  - `READ_EXTERNAL_STORAGE` (Android 12 이하)
  - `INTERNET`, `ACCESS_NETWORK_STATE`
  - `FOREGROUND_SERVICE`, `POST_NOTIFICATIONS`

---

## ❌ 금지 사항

### 절대 하지 말아야 할 것
1. ❌ **XML 레이아웃 사용** - Compose만 사용
2. ❌ **Main Thread에서 네트워크/DB 작업**
3. ❌ **하드코딩된 문자열** - strings.xml 사용 필수
4. ❌ **하드코딩된 색상** - Color.kt 사용
5. ❌ **직접적인 Context 사용** - ViewModel에서 Context 사용 금지
6. ❌ **비권장 API 사용** - Deprecated API 사용 금지
7. ❌ **GlobalScope 사용** - 항상 적절한 scope 사용
8. ❌ **!!（강제 언래핑）남용** - null 안전성 확보

### 주의 사항
- ⚠️ 파일 I/O는 항상 백그라운드 스레드에서
- ⚠️ 큰 파일 처리 시 메모리 사용량 체크
- ⚠️ 서버 모드에서는 배터리 소모 최소화
- ⚠️ 사용자 데이터는 암호화 저장 고려

---

## 🧪 테스트 규칙

### 단위 테스트
- Repository 로직은 테스트 작성 권장
- ViewModel은 중요 로직에 대해 테스트 작성

### UI 테스트
- 주요 사용자 플로우에 대해 Compose UI 테스트 권장

---

## 📦 의존성 관리

### 필수 라이브러리
- **Compose BOM**: Material3, Navigation
- **Lifecycle**: ViewModel, Runtime
- **Coroutines**: kotlinx-coroutines-android
- **Network**: OkHttp, Retrofit
- **Image**: Coil
- **Storage**: Room, DataStore
- **Background**: WorkManager

### 새 라이브러리 추가 시
- 목적과 필요성 명확히
- 버전 호환성 확인
- 앱 크기 증가 고려

---

## 🔐 보안 규칙

1. **API 키/비밀**: 절대 코드에 하드코딩 금지
2. **사용자 데이터**: 암호화 저장 고려
3. **네트워크 통신**: HTTPS 권장
4. **권한**: 최소 권한 원칙

---

## 📝 문서화 규칙

### 주석
```kotlin
/**
 * 미디어 파일을 스캔하고 백업이 필요한 파일 목록을 반환합니다.
 *
 * @param lastSyncTime 마지막 동기화 시간 (밀리초)
 * @return 백업이 필요한 미디어 파일 목록
 */
suspend fun scanMediaFiles(lastSyncTime: Long): List<MediaFile>
```

### 복잡한 로직
- 비즈니스 로직이 복잡한 경우 주석으로 설명
- TODO, FIXME 태그 적극 활용

---

## 🚀 빌드 & 배포

### 빌드 설정
- **Debug**: 로그 출력, 디버깅 정보 포함
- **Release**: ProGuard/R8 활성화, 로그 제거

### 버전 관리
- **versionCode**: 자동 증가
- **versionName**: 시맨틱 버저닝 (예: 1.0.0)

---

## 📅 업데이트 이력

- **2026-01-01**: 초기 문서 작성

---

**이 규칙은 프로젝트 진행에 따라 업데이트될 수 있습니다.**
