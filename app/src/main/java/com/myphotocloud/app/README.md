# MyPhotoCloud - 코드 템플릿

이 폴더에는 MyPhotoCloud 프로젝트의 핵심 파일들이 포함되어 있습니다.

## 📂 파일 구조

```
MyPhotoCloud_Code_Templates/
├── README.md                           ← 이 파일
├── MyPhotoCloudApp.kt                  ← Application 클래스
├── MainActivity.kt                     ← 메인 액티비티
│
├── model/
│   └── AppMode.kt                      ← 앱 모드 enum
│
├── mode/
│   └── AppModeManager.kt               ← 모드 관리자
│
├── ui/
│   ├── WelcomeScreen.kt                ← 환영 화면
│   └── MainScreen.kt                   ← 메인 화면
│
├── server/
│   └── BackupServerService.kt          ← 서버 서비스
│
└── receiver/
    └── AutoSyncAlarmReceiver.kt        ← 자동 백업 알람
```

## 🚀 사용 방법

### 1. Android Studio에서 새 프로젝트 생성

**MyPhotoCloud_Setup_Guide.md** 문서를 참고하여 새 프로젝트를 생성하세요.

### 2. 파일 복사

각 파일을 해당 위치에 복사:

```
MyPhotoCloudApp.kt → app/src/main/java/com/myphotocloud/app/
MainActivity.kt → app/src/main/java/com/myphotocloud/app/

model/AppMode.kt → app/src/main/java/com/myphotocloud/app/model/
mode/AppModeManager.kt → app/src/main/java/com/myphotocloud/app/mode/

ui/WelcomeScreen.kt → app/src/main/java/com/myphotocloud/app/ui/
ui/MainScreen.kt → app/src/main/java/com/myphotocloud/app/ui/

server/BackupServerService.kt → app/src/main/java/com/myphotocloud/app/server/
receiver/AutoSyncAlarmReceiver.kt → app/src/main/java/com/myphotocloud/app/receiver/
```

### 3. 추가 파일 생성 필요

다음 파일들은 직접 생성하거나 기존 프로젝트에서 복사:

#### 필수:
- `ui/theme/Color.kt` - Material 색상 정의
- `ui/theme/Theme.kt` - 테마 설정
- `ui/theme/Type.kt` - 타이포그래피
- `service/BackupKeepAliveService.kt` - KeepAlive 서비스
- `receiver/BootReceiver.kt` - 부팅 리시버

#### 선택 (나중에 추가):
- `client/MediaScanner.kt` - 미디어 스캔
- `client/BackupWorker.kt` - 백업 워커
- `model/MediaFile.kt` - 미디어 파일 모델
- `model/User.kt` - 사용자 모델

### 4. Gradle Sync

Android Studio에서:
```
File → Sync Project with Gradle Files
```

### 5. 빌드 및 실행

```
Build → Make Project
Run → Run 'app'
```

## ✅ 체크리스트

프로젝트 생성 후 확인:

- [ ] AndroidManifest.xml 수정 완료
- [ ] build.gradle.kts 의존성 추가 완료
- [ ] strings.xml 수정 완료
- [ ] 모든 코드 파일 복사 완료
- [ ] 패키지명 일치 확인 (`com.myphotocloud.app`)
- [ ] Gradle Sync 성공
- [ ] 빌드 성공
- [ ] 앱 실행 성공

## 📝 다음 단계

1. **테마 파일** 생성 (Color, Theme, Type)
2. **백업 로직** 구현 (MediaScanner, BackupWorker)
3. **서버 로직** 구현 (HTTP Server, API)
4. **UI 개선** (갤러리, 설정 화면 등)

## 🔗 참고 문서

- `MyPhotoCloud_Setup_Guide.md` - 프로젝트 생성 가이드
- `MyPhotoCloud_Technical_Spec.md` - 기술 명세서
- `MyPhotoCloud_Unified_Architecture.md` - 아키텍처 설계
- `MyPhotoCloud_Business_Plan.md` - 사업 계획서

---

**버전**: 1.0  
**작성일**: 2026-01-01
