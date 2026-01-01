# 🔄 MyPhotoCloud - 마이그레이션 가이드

## 1. 기존 코드 분석

### 1.1 재사용 가능한 컴포넌트

현재 PhoneBackup_cursor3 프로젝트에서 **그대로 사용 가능한** 코드:

#### ✅ Background 패키지 (95% 재사용)
```
app/src/main/java/com/gt/backupapp/background/
├── AutoSyncWorker.kt          → MediaBackupWorker.kt (리네임)
├── FullBackupScheduler.kt     → 삭제 (미디어만 증분 백업)
└── RefreshBroadcast.kt        → 재사용
```

**수정 필요:**
- `AutoSyncWorker`: 미디어 파일만 필터링하도록 수정
- 전체 백업 관련 로직 제거

#### ✅ Manager 패키지 (80% 재사용)
```
app/src/main/java/com/gt/backupapp/manager/
├── BackupRunGuard.kt          → 재사용 ✓
├── BackupOrchestrator.kt      → 재사용 (수정)
├── BackupHistoryManager.kt    → 재사용 ✓
├── ProfileManager.kt          → 삭제 (단일 프로필)
├── SettingsManager.kt         → 재사용 (간소화)
└── FileManager.kt             → MediaFileManager.kt (수정)
```

**수정 필요:**
- `FileManager`: 미디어 파일만 스캔 (DCIM, Pictures, Movies 등)

#### ✅ Client 패키지 (70% 재사용)
```
app/src/main/java/com/gt/backupapp/client/
└── BackupClient.kt            → MediaClient.kt (수정)
```

**수정 필요:**
- 청크 업로드 로직 유지
- API 엔드포인트 변경 (/api/backup/upload → /api/media/upload)

#### ✅ Service 패키지 (100% 재사용)
```
app/src/main/java/com/gt/backupapp/service/
├── BackupKeepAliveService.kt  → 재사용 ✓
├── BackupServerService.kt     → 대폭 수정 (웹 갤러리 추가)
└── NotificationHelper.kt      → 재사용 ✓
```

#### ✅ Receiver 패키지 (100% 재사용)
```
app/src/main/java/com/gt/backupapp/receiver/
└── AutoSyncAlarmReceiver.kt   → 재사용 ✓
```

#### ✅ Security 패키지 (100% 재사용)
```
app/src/main/java/com/gt/backupapp/security/
├── Encryption.kt              → 재사용 ✓
└── AuthManager.kt             → 재사용 ✓
```

#### ❌ 삭제할 패키지
```
app/src/main/java/com/gt/backupapp/
├── webdav/                    → 삭제 (WebDAV 미지원)
├── sftp/                      → 삭제 (SFTP 미지원)
└── work/                      → 삭제 (불필요)
```

---

## 2. 새로 개발할 컴포넌트

### 2.1 미디어 스캐너

```kotlin
// app/src/main/java/com/myphotocloud/media/MediaScanner.kt
class MediaScanner(private val context: Context) {
    
    /**
     * 미디어 파일만 스캔 (사진 + 동영상)
     */
    fun scanMediaFiles(lastScanTime: Long = 0): List<MediaFile> {
        val mediaFiles = mutableListOf<MediaFile>()
        
        // 사진 스캔
        mediaFiles.addAll(scanPhotos(lastScanTime))
        
        // 동영상 스캔
        mediaFiles.addAll(scanVideos(lastScanTime))
        
        return mediaFiles
    }
    
    private fun scanPhotos(sinceTimestamp: Long): List<MediaFile> {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.MIME_TYPE
        )
        
        val selection = "${MediaStore.Images.Media.DATE_ADDED} > ?"
        val selectionArgs = arrayOf(sinceTimestamp.toString())
        
        // ContentResolver 쿼리...
    }
    
    private fun scanVideos(sinceTimestamp: Long): List<MediaFile> {
        // 동영상도 동일한 방식으로 스캔
    }
}
```

### 2.2 썸네일 생성기

```kotlin
// app/src/main/java/com/myphotocloud/media/ThumbnailGenerator.kt
class ThumbnailGenerator(private val context: Context) {
    
    fun generateThumbnail(
        sourcePath: String,
        targetPath: String,
        size: Int = 512
    ): Boolean {
        return try {
            val bitmap = BitmapFactory.decodeFile(sourcePath)
            val thumbnail = Bitmap.createScaledBitmap(
                bitmap,
                size,
                size,
                true
            )
            
            FileOutputStream(targetPath).use { out ->
                thumbnail.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            
            true
        } catch (e: Exception) {
            false
        }
    }
}
```

### 2.3 웹 갤러리 UI

```kotlin
// server/src/main/kotlin/com/myphotocloud/server/GalleryHandler.kt
class GalleryHandler(private val db: Database) {
    
    fun handleTimeline(call: ApplicationCall) {
        val userId = call.principal<UserPrincipal>()!!.userId
        val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
        val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
        
        val files = db.getMediaFiles(userId, offset, limit)
        
        call.respond(HttpStatusCode.OK, TimelineResponse(
            files = files,
            total = db.countMediaFiles(userId),
            hasMore = files.size >= limit
        ))
    }
}
```

---

## 3. 단계별 마이그레이션 계획

### Phase 1: 프로젝트 설정 (1주)

#### 1.1 저장소 생성
```bash
# 새 Git 저장소
git init MyPhotoCloud
cd MyPhotoCloud

# 기존 코드 복사
cp -r ../PhoneBackup_cursor3/app ./app
```

#### 1.2 패키지 리네임
```
Old: com.gt.backupapp
New: com.myphotocloud.app
```

**도구**: Android Studio의 Refactor → Rename Package

#### 1.3 build.gradle 수정
```kotlin
// app/build.gradle.kts
android {
    namespace = "com.myphotocloud.app"
    
    defaultConfig {
        applicationId = "com.myphotocloud.app"
        versionCode = 1
        versionName = "1.0.0"
    }
}

dependencies {
    // 새로운 의존성 추가
    implementation("io.coil-kt:coil-compose:2.5.0")  // 이미지 로딩
    implementation("com.drewnoakes:metadata-extractor:2.18.0")  // EXIF
    
    // 기존 의존성 유지
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
}
```

### Phase 2: 코드 정리 (1주)

#### 2.1 불필요한 파일 삭제
```bash
# WebDAV, SFTP 관련 삭제
rm -rf app/src/main/java/com/gt/backupapp/webdav
rm -rf app/src/main/java/com/gt/backupapp/sftp

# 다중 프로필 관련 삭제
rm app/src/main/java/com/gt/backupapp/manager/ProfileManager.kt
```

#### 2.2 FileManager → MediaFileManager 수정

**Before:**
```kotlin
// 모든 파일 타입 스캔
fun getFilteredLocalFiles(settings: ProfileSettings): List<FileInfo> {
    val allFiles = mutableListOf<FileInfo>()
    settings.selectedFolders.forEach { folder ->
        allFiles.addAll(scanFolder(folder))
    }
    return allFiles
}
```

**After:**
```kotlin
// 미디어 파일만 스캔
fun scanMediaFiles(): List<MediaFile> {
    return MediaScanner(context).scanMediaFiles()
}
```

### Phase 3: 새 기능 개발 (4주)

#### 3.1 서버 갤러리 API (2주)
- Timeline API 구현
- 썸네일 생성
- EXIF 메타데이터 추출

#### 3.2 웹 프론트엔드 (2주)
- React 프로젝트 설정
- 타임라인 뷰 구현
- 이미지 뷰어 (PhotoSwipe)

### Phase 4: 통합 테스트 (1주)

#### 4.1 백업 플로우 테스트
```
1. 앱에서 사진 촬영
2. 자동 백업 대기 (1시간)
3. 서버에서 파일 수신 확인
4. 썸네일 생성 확인
5. 웹 갤러리에서 표시 확인
```

#### 4.2 성능 테스트
- 1,000개 파일 백업 시간
- 웹 갤러리 로딩 시간
- 썸네일 생성 시간

---

## 4. 코드 변경 체크리스트

### 4.1 Android 앱

- [ ] 패키지명 변경: `com.gt.backupapp` → `com.myphotocloud.app`
- [ ] 앱 이름 변경: "Phone Backup" → "MyPhotoCloud"
- [ ] 아이콘 변경
- [ ] `FileManager` → `MediaFileManager` (미디어만 스캔)
- [ ] `ProfileManager` 삭제
- [ ] WebDAV/SFTP 관련 코드 삭제
- [ ] UI 간소화 (미디어 백업 전용)

### 4.2 서버

- [ ] 갤러리 API 추가 (`/api/gallery/*`)
- [ ] 썸네일 생성 로직 추가
- [ ] EXIF 메타데이터 추출
- [ ] 정적 파일 서빙 (웹 갤러리)
- [ ] 데이터베이스 스키마 변경

### 4.3 웹 갤러리

- [ ] React 프로젝트 생성
- [ ] 타임라인 컴포넌트
- [ ] 이미지 뷰어 (PhotoSwipe)
- [ ] 앨범 관리 UI
- [ ] 검색 UI

---

## 5. 새 프로젝트 구조

```
MyPhotoCloud/
├── android/                      # Android 앱
│   ├── app/
│   │   ├── src/main/java/com/myphotocloud/app/
│   │   │   ├── MainActivity.kt
│   │   │   ├── background/
│   │   │   │   └── MediaBackupWorker.kt
│   │   │   ├── manager/
│   │   │   │   ├── BackupRunGuard.kt
│   │   │   │   ├── BackupOrchestrator.kt
│   │   │   │   └── MediaFileManager.kt
│   │   │   ├── media/
│   │   │   │   ├── MediaScanner.kt
│   │   │   │   └── ThumbnailGenerator.kt
│   │   │   ├── client/
│   │   │   │   └── MediaClient.kt
│   │   │   └── ui/
│   │   │       ├── MainScreen.kt
│   │   │       ├── BackupScreen.kt
│   │   │       └── SettingsScreen.kt
│   │   └── build.gradle.kts
│   └── build.gradle.kts
│
├── server/                       # Kotlin 서버
│   ├── src/main/kotlin/com/myphotocloud/server/
│   │   ├── Application.kt
│   │   ├── api/
│   │   │   ├── AuthRoutes.kt
│   │   │   ├── MediaRoutes.kt
│   │   │   └── GalleryRoutes.kt
│   │   ├── database/
│   │   │   ├── Database.kt
│   │   │   └── MediaRepository.kt
│   │   ├── media/
│   │   │   ├── ThumbnailGenerator.kt
│   │   │   ├── ExifExtractor.kt
│   │   │   └── StorageManager.kt
│   │   └── security/
│   │       ├── JwtAuth.kt
│   │       └── PasswordHash.kt
│   └── build.gradle.kts
│
├── web/                          # React 웹 갤러리
│   ├── src/
│   │   ├── App.tsx
│   │   ├── components/
│   │   │   ├── Timeline.tsx
│   │   │   ├── ImageViewer.tsx
│   │   │   └── AlbumList.tsx
│   │   ├── api/
│   │   │   └── client.ts
│   │   └── styles/
│   ├── package.json
│   └── vite.config.ts
│
├── docs/                         # 문서
│   ├── Business_Plan.md
│   ├── Technical_Spec.md
│   ├── API_Reference.md
│   └── User_Guide.md
│
└── README.md
```

---

## 6. 개발 환경 설정

### 6.1 Android
```bash
# Android Studio 실행
studio .

# 의존성 설치
./gradlew build

# 에뮬레이터 실행
./gradlew installDebug
```

### 6.2 서버
```bash
cd server

# 빌드
./gradlew build

# 실행
java -jar build/libs/myphotocloud-server.jar
```

### 6.3 웹 갤러리
```bash
cd web

# 의존성 설치
npm install

# 개발 서버 실행
npm run dev

# 빌드
npm run build
```

---

## 7. 예상 일정

| Phase | 작업 | 기간 | 완료 조건 |
|-------|------|------|-----------|
| **1** | 프로젝트 설정 | 1주 | 빌드 성공 |
| **2** | 코드 정리 | 1주 | 불필요한 코드 삭제 완료 |
| **3.1** | 서버 갤러리 API | 2주 | API 테스트 통과 |
| **3.2** | 웹 프론트엔드 | 2주 | 타임라인 표시 |
| **4** | 통합 테스트 | 1주 | E2E 테스트 통과 |
| **5** | 베타 출시 | 1주 | 베타 테스터 10명 |
| **합계** | | **8주** | MVP 출시 |

---

## 8. 마이그레이션 우선순위

### High Priority (필수)
1. ✅ MediaScanner 구현
2. ✅ 서버 갤러리 API
3. ✅ 웹 타임라인 뷰
4. ✅ 썸네일 생성

### Medium Priority (중요)
1. 앨범 관리
2. EXIF 메타데이터
3. 검색 기능

### Low Priority (나중에)
1. AI 태깅
2. 얼굴 인식
3. 지도 뷰

---

**문서 작성일**: 2026-01-01  
**버전**: 1.0  
**작성자**: AI Assistant
