# 🔧 MyPhotoCloud - 기술명세서

## 1. 시스템 아키텍처

### 1.1 전체 구조

```
┌─────────────────────────────────────────────────────────────┐
│                     Android 클라이언트                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  백업 엔진    │  │  미디어 뷰어  │  │  설정 관리    │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│         ↓                  ↓                  ↓             │
│  ┌─────────────────────────────────────────────────┐        │
│  │          REST API 클라이언트 (OkHttp)            │        │
│  └─────────────────────────────────────────────────┘        │
└──────────────────────────┬───────────────────────────────────┘
                           │ HTTPS
                           ↓
┌─────────────────────────────────────────────────────────────┐
│                    백엔드 서버 (Kotlin)                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  백업 API     │  │  갤러리 API   │  │  인증 API     │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│         ↓                  ↓                  ↓             │
│  ┌─────────────────────────────────────────────────┐        │
│  │           비즈니스 로직 레이어                     │        │
│  │  - 파일 관리  - 썸네일 생성  - 메타데이터 추출    │        │
│  └─────────────────────────────────────────────────┘        │
│         ↓                                                   │
│  ┌─────────────────────────────────────────────────┐        │
│  │              스토리지 레이어                       │        │
│  │  - 로컬 파일 시스템  - SQLite DB  - 캐시          │        │
│  └─────────────────────────────────────────────────┘        │
└─────────────────────────────────────────────────────────────┘
                           ↑
                           │ HTTPS
┌──────────────────────────┴───────────────────────────────────┐
│                    웹 갤러리 (SPA)                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  타임라인     │  │  앨범 관리     │  │  검색/필터    │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│  Technology: React / Vue.js / Svelte                        │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. 기술 스택

### 2.1 Android 앱

| 레이어 | 기술 | 비고 |
|--------|------|------|
| **언어** | Kotlin | |
| **UI** | Jetpack Compose | 현대적 선언형 UI |
| **아키텍처** | MVVM | ViewModel + StateFlow |
| **네트워킹** | OkHttp + Retrofit | REST API 통신 |
| **로컬 DB** | Room | 백업 히스토리 |
| **이미지 로딩** | Coil | 비동기 이미지 로딩 |
| **백그라운드** | WorkManager + AlarmManager | 자동 백업 |
| **DI** | Hilt (선택) | 의존성 주입 |
| **최소 SDK** | API 26 (Android 8.0) | 95%+ 커버리지 |

### 2.2 백엔드 서버

| 컴포넌트 | 기술 | 비고 |
|----------|------|------|
| **언어** | Kotlin (JVM) | |
| **프레임워크** | Ktor 또는 경량 HTTP 서버 | |
| **데이터베이스** | SQLite (개발), PostgreSQL (프로덕션) | |
| **ORM** | Exposed (Kotlin SQL) | |
| **인증** | JWT | 토큰 기반 인증 |
| **스토리지** | 로컬 파일 시스템 + 메타데이터 DB | |
| **썸네일** | FFmpeg (비디오), ImageMagick (사진) | |
| **EXIF** | metadata-extractor | |
| **배포** | JAR (자체 실행형) | Docker도 지원 |

### 2.3 웹 갤러리

| 컴포넌트 | 기술 | 비고 |
|----------|------|------|
| **프레임워크** | React 또는 Vue.js | |
| **UI 라이브러리** | Material-UI / Vuetify | |
| **상태 관리** | Redux / Vuex | |
| **이미지 뷰어** | PhotoSwipe | 확대/슬라이드쇼 |
| **빌드** | Vite | 빠른 개발 서버 |
| **배포** | 정적 파일 (서버에서 서빙) | |

---

## 3. 데이터 모델

### 3.1 데이터베이스 스키마

#### Users (사용자)
```sql
CREATE TABLE users (
    user_id TEXT PRIMARY KEY,
    username TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    display_name TEXT,
    email TEXT,
    storage_quota_gb INTEGER DEFAULT 0, -- 0 = unlimited
    created_at INTEGER NOT NULL,
    last_login INTEGER
);
```

#### MediaFiles (미디어 파일)
```sql
CREATE TABLE media_files (
    file_id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    original_filename TEXT NOT NULL,
    file_path TEXT NOT NULL, -- 서버 내 경로
    file_size INTEGER NOT NULL,
    mime_type TEXT NOT NULL,
    media_type TEXT NOT NULL, -- 'photo', 'video'
    
    -- 메타데이터
    width INTEGER,
    height INTEGER,
    duration_seconds INTEGER, -- 비디오만
    taken_at INTEGER, -- EXIF 날짜
    
    -- 위치 정보
    latitude REAL,
    longitude REAL,
    location_name TEXT,
    
    -- 해시 (중복 제거)
    hash_sha256 TEXT,
    
    -- 썸네일
    thumbnail_path TEXT,
    
    -- 앨범/태그
    album_id TEXT,
    tags TEXT, -- JSON array
    
    -- 타임스탬프
    uploaded_at INTEGER NOT NULL,
    modified_at INTEGER,
    
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (album_id) REFERENCES albums(album_id)
);

CREATE INDEX idx_media_user_taken ON media_files(user_id, taken_at DESC);
CREATE INDEX idx_media_hash ON media_files(hash_sha256);
CREATE INDEX idx_media_album ON media_files(album_id);
```

#### Albums (앨범)
```sql
CREATE TABLE albums (
    album_id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    album_name TEXT NOT NULL,
    description TEXT,
    cover_file_id TEXT,
    created_at INTEGER NOT NULL,
    modified_at INTEGER,
    
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (cover_file_id) REFERENCES media_files(file_id)
);
```

#### Tags (태그)
```sql
CREATE TABLE tags (
    tag_id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    tag_name TEXT NOT NULL,
    color TEXT, -- UI 색상
    
    UNIQUE(user_id, tag_name),
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);
```

#### MediaTags (다대다 연결)
```sql
CREATE TABLE media_tags (
    file_id TEXT NOT NULL,
    tag_id TEXT NOT NULL,
    
    PRIMARY KEY (file_id, tag_id),
    FOREIGN KEY (file_id) REFERENCES media_files(file_id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags(tag_id) ON DELETE CASCADE
);
```

### 3.2 파일 저장 구조

```
/storage/
  /users/
    /{user_id}/
      /originals/
        /2025/
          /01/
            /photo_123.jpg
            /video_456.mp4
      /thumbnails/
        /small/   # 200x200
        /medium/  # 800x800
        /large/   # 1920x1920
      /metadata/
        /exif_cache.db
```

---

## 4. API 명세

### 4.1 인증 API

#### POST /api/auth/register
사용자 등록
```json
Request:
{
  "username": "johndoe",
  "password": "securePass123",
  "email": "john@example.com"
}

Response (200):
{
  "user_id": "uuid-1234",
  "username": "johndoe",
  "token": "jwt-token-here"
}
```

#### POST /api/auth/login
로그인
```json
Request:
{
  "username": "johndoe",
  "password": "securePass123"
}

Response (200):
{
  "user_id": "uuid-1234",
  "username": "johndoe",
  "token": "jwt-token-here",
  "expires_at": 1672531199
}
```

### 4.2 백업 API

#### POST /api/backup/upload
파일 업로드 (청크 지원)
```http
POST /api/backup/upload
Authorization: Bearer {token}
Content-Type: multipart/form-data

file: (binary)
filename: "photo.jpg"
file_size: 2048000
chunk_index: 0
total_chunks: 1
hash: "sha256-hash"
taken_at: 1672531199 (optional)
latitude: 37.5665 (optional)
longitude: 126.9780 (optional)
```

Response (200):
```json
{
  "file_id": "uuid-5678",
  "status": "uploaded",
  "duplicate": false,
  "thumbnail_generated": true
}
```

#### GET /api/backup/status
백업 상태 조회
```json
Response (200):
{
  "total_files": 1234,
  "total_size_bytes": 5368709120,
  "photos": 1000,
  "videos": 234,
  "storage_used_gb": 5.0,
  "storage_quota_gb": 0 // unlimited
}
```

### 4.3 갤러리 API

#### GET /api/gallery/timeline
타임라인 조회 (페이징)
```http
GET /api/gallery/timeline?offset=0&limit=50&start_date=2025-01-01&end_date=2025-12-31
Authorization: Bearer {token}
```

Response (200):
```json
{
  "files": [
    {
      "file_id": "uuid-1",
      "filename": "photo.jpg",
      "thumbnail_url": "/api/media/thumbnail/uuid-1",
      "original_url": "/api/media/original/uuid-1",
      "taken_at": 1672531199,
      "width": 4032,
      "height": 3024,
      "location": "Seoul, Korea",
      "tags": ["family", "vacation"]
    }
  ],
  "total": 1234,
  "has_more": true
}
```

#### GET /api/gallery/albums
앨범 목록
```json
Response (200):
{
  "albums": [
    {
      "album_id": "uuid-a1",
      "album_name": "Family Trip 2025",
      "cover_url": "/api/media/thumbnail/uuid-1",
      "file_count": 234,
      "created_at": 1672531199
    }
  ]
}
```

#### GET /api/gallery/search
검색
```http
GET /api/gallery/search?q=beach&tags=vacation&start_date=2025-01-01
```

### 4.4 미디어 API

#### GET /api/media/thumbnail/{file_id}
썸네일 조회 (스트리밍)

#### GET /api/media/original/{file_id}
원본 파일 조회 (스트리밍, 다운로드)

#### DELETE /api/media/{file_id}
파일 삭제

---

## 5. 핵심 기능 구현

### 5.1 자동 백업 (Android)

#### 파일 감지
```kotlin
class MediaScanner(context: Context) {
    fun scanNewMedia(): List<MediaFile> {
        val contentResolver = context.contentResolver
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATA
        )
        
        val selection = "${MediaStore.Images.Media.DATE_ADDED} > ?"
        val selectionArgs = arrayOf(lastScanTimestamp.toString())
        
        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )
        
        // 동영상도 동일하게 스캔 (MediaStore.Video.Media)
    }
}
```

#### 백업 워커
```kotlin
class AutoBackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        if (!isWifiConnected()) return Result.retry()
        if (!isCharging() && settings.chargingOnly) return Result.retry()
        
        val scanner = MediaScanner(applicationContext)
        val newFiles = scanner.scanNewMedia()
        
        newFiles.forEach { file ->
            uploadFile(file)
        }
        
        return Result.success()
    }
}
```

### 5.2 썸네일 생성 (서버)

```kotlin
class ThumbnailGenerator {
    fun generateThumbnail(
        originalPath: String,
        mediaType: MediaType,
        sizes: List<ThumbnailSize>
    ): Map<ThumbnailSize, String> {
        return when (mediaType) {
            MediaType.PHOTO -> generatePhotoThumbnails(originalPath, sizes)
            MediaType.VIDEO -> generateVideoThumbnails(originalPath, sizes)
        }
    }
    
    private fun generatePhotoThumbnails(
        path: String,
        sizes: List<ThumbnailSize>
    ): Map<ThumbnailSize, String> {
        val thumbnails = mutableMapOf<ThumbnailSize, String>()
        
        sizes.forEach { size ->
            val outputPath = getThumbnailPath(path, size)
            
            // ImageMagick 또는 Kotlin 이미지 라이브러리
            val process = ProcessBuilder(
                "convert",
                path,
                "-resize", "${size.width}x${size.height}>",
                "-quality", "85",
                outputPath
            ).start()
            
            process.waitFor()
            thumbnails[size] = outputPath
        }
        
        return thumbnails
    }
    
    private fun generateVideoThumbnails(
        path: String,
        sizes: List<ThumbnailSize>
    ): Map<ThumbnailSize, String> {
        // FFmpeg로 첫 프레임 추출
        val framePath = extractFrame(path, timeSeconds = 1.0)
        return generatePhotoThumbnails(framePath, sizes)
    }
}
```

### 5.3 EXIF 메타데이터 추출

```kotlin
import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.ExifSubIFDDirectory
import com.drew.metadata.exif.GpsDirectory

class ExifExtractor {
    fun extractMetadata(filePath: String): FileMetadata {
        val metadata = ImageMetadataReader.readMetadata(File(filePath))
        
        val exifDir = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory::class.java)
        val gpsDir = metadata.getFirstDirectoryOfType(GpsDirectory::class.java)
        
        return FileMetadata(
            takenAt = exifDir?.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)?.time,
            width = exifDir?.getInt(ExifSubIFDDirectory.TAG_IMAGE_WIDTH),
            height = exifDir?.getInt(ExifSubIFDDirectory.TAG_IMAGE_HEIGHT),
            latitude = gpsDir?.geoLocation?.latitude,
            longitude = gpsDir?.geoLocation?.longitude,
            cameraMake = exifDir?.getString(ExifSubIFDDirectory.TAG_MAKE),
            cameraModel = exifDir?.getString(ExifSubIFDDirectory.TAG_MODEL)
        )
    }
}
```

---

## 6. 성능 최적화

### 6.1 썸네일 전략
- **Lazy Loading**: 화면에 보이는 썸네일만 로드
- **Responsive Images**: 화면 크기별 적절한 썸네일 크기
- **CDN/캐싱**: HTTP 캐시 헤더 활용

### 6.2 데이터베이스 최적화
- **인덱스**: `user_id`, `taken_at`, `hash_sha256`
- **페이징**: Offset/Limit 대신 Cursor 기반 페이징
- **Connection Pooling**: HikariCP

### 6.3 네트워크 최적화
- **청크 업로드**: 10MB 단위 분할
- **Resume 지원**: 실패 시 이어 올리기
- **압축**: gzip, brotli

---

## 7. 보안

### 7.1 인증 및 권한
- JWT 토큰 (만료: 7일)
- Refresh Token (만료: 30일)
- HTTPS Only (TLS 1.3)

### 7.2 파일 보호
- 사용자별 디렉토리 분리
- 파일 경로 검증 (Path Traversal 방지)
- 업로드 파일 타입 검증

### 7.3 API 보안
- Rate Limiting (100 req/min/user)
- CORS 설정
- SQL Injection 방지 (Prepared Statements)

---

## 8. 배포

### 8.1 Android 앱
- Google Play Store
- F-Droid (오픈소스)
- GitHub Releases (APK)

### 8.2 서버
```bash
# JAR 직접 실행
java -jar myphotocloud-server.jar --port=8080 --storage=/path/to/storage

# Docker
docker run -p 8080:8080 -v /path/to/storage:/storage myphotocloud/server

# Docker Compose
version: '3.8'
services:
  server:
    image: myphotocloud/server
    ports:
      - "8080:8080"
    volumes:
      - ./storage:/storage
      - ./data:/data
    environment:
      - DATABASE_URL=postgresql://localhost/myphotocloud
```

---

## 9. 테스트 전략

### 9.1 단위 테스트
- JUnit 5 + Mockk (Kotlin)
- 커버리지 목표: 80%+

### 9.2 통합 테스트
- API 엔드포인트 테스트
- 데이터베이스 마이그레이션 테스트

### 9.3 UI 테스트
- Android: Espresso
- Web: Cypress

---

## 10. 모니터링

### 10.1 메트릭
- 업로드 성공률
- 평균 업로드 시간
- API 응답 시간
- 에러율

### 10.2 로깅
- Logback (서버)
- Timber (Android)
- 로그 레벨: INFO (프로덕션), DEBUG (개발)

---

**문서 작성일**: 2026-01-01  
**버전**: 1.0  
**작성자**: AI Assistant
