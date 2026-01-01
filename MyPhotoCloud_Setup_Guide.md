# 🚀 MyPhotoCloud - 프로젝트 생성 가이드

## 1. Android Studio에서 새 프로젝트 생성

### 1.1 프로젝트 설정
```
1. Android Studio 실행
2. File → New → New Project
3. "Empty Activity" 선택 (Compose 기반)
4. 아래 설정 입력:

Name: MyPhotoCloud
Package name: com.myphotocloud.app
Save location: c:\dev\kotlin\ai-project\MyPhotoCloud
Language: Kotlin
Minimum SDK: API 26 (Android 8.0)
Build configuration language: Kotlin DSL (build.gradle.kts)

5. Finish 클릭
```

---

## 2. build.gradle.kts 수정

### 2.1 프로젝트 레벨 (build.gradle.kts)
```kotlin
// 이미 생성되어 있음, 수정 불필요
```

### 2.2 앱 레벨 (app/build.gradle.kts)

**전체 내용을 아래로 교체:**

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.myphotocloud.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.myphotocloud.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
    
    buildFeatures {
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.01.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    
    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    
    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")
    
    // WorkManager (백그라운드 작업)
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    
    // Network
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    
    // Image Loading
    implementation("io.coil-kt:coil-compose:2.5.0")
    
    // Room (로컬 DB)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.01.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

---

## 3. AndroidManifest.xml 수정

**app/src/main/AndroidManifest.xml** 파일을 아래로 교체:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- 권한 -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
    <uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />

    <application
        android:name=".MyPhotoCloudApp"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.MyPhotoCloud"
        tools:targetApi="31">
        
        <!-- 메인 액티비티 -->
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.MyPhotoCloud">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- 서버 서비스 -->
        <service
            android:name=".server.BackupServerService"
            android:enabled="true"
            android:exported="false"
            android:foregroundServiceType="dataSync" />

        <!-- KeepAlive 서비스 -->
        <service
            android:name=".service.BackupKeepAliveService"
            android:enabled="true"
            android:exported="false"
            android:foregroundServiceType="dataSync" />

        <!-- 알람 리시버 -->
        <receiver
            android:name=".receiver.AutoSyncAlarmReceiver"
            android:enabled="true"
            android:exported="false" />

        <!-- 부팅 리시버 -->
        <receiver
            android:name=".receiver.BootReceiver"
            android:enabled="true"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
            </intent-filter>
        </receiver>
    </application>
</manifest>
```

---

## 4. strings.xml 수정

**app/src/main/res/values/strings.xml:**

```xml
<resources>
    <string name="app_name">MyPhotoCloud</string>
    
    <!-- Welcome Screen -->
    <string name="welcome_title">MyPhotoCloud에 오신 것을 환영합니다</string>
    <string name="welcome_subtitle">이 기기를 어떻게 사용하시겠습니까?</string>
    
    <!-- Modes -->
    <string name="mode_standalone">서버 + 백업</string>
    <string name="mode_standalone_desc">이 폰에서 사진 보관 및 백업</string>
    <string name="mode_client">백업만</string>
    <string name="mode_client_desc">다른 기기로 백업</string>
    <string name="mode_server">서버만</string>
    <string name="mode_server_desc">가족 사진 보관 전용</string>
    
    <!-- Common -->
    <string name="settings">설정</string>
    <string name="start_backup">지금 백업하기</string>
    <string name="server_status">서버 상태</string>
    <string name="backup_status">백업 상태</string>
</resources>
```

---

## 5. 핵심 파일 생성 준비

다음 단계에서 아래 파일들을 생성하겠습니다:

```
app/src/main/java/com/myphotocloud/app/
├── MyPhotoCloudApp.kt              ← Application 클래스
├── MainActivity.kt                 ← 메인 액티비티
│
├── model/                          ← 데이터 모델
│   ├── AppMode.kt
│   ├── MediaFile.kt
│   └── User.kt
│
├── mode/                           ← 모드 관리
│   ├── AppModeManager.kt
│   └── ModeSelector.kt
│
├── ui/                             ← UI
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   ├── WelcomeScreen.kt
│   ├── MainScreen.kt
│   ├── client/
│   │   └── ClientScreen.kt
│   ├── server/
│   │   └── ServerScreen.kt
│   └── standalone/
│       └── StandaloneScreen.kt
│
├── client/                         ← 클라이언트 (백업)
│   ├── MediaScanner.kt
│   └── BackupWorker.kt
│
├── server/                         ← 서버
│   ├── BackupServerService.kt
│   └── MediaRepository.kt
│
├── service/                        ← 공통 서비스
│   └── BackupKeepAliveService.kt
│
└── receiver/                       ← 리시버
    ├── AutoSyncAlarmReceiver.kt
    └── BootReceiver.kt
```

---

## 6. Gradle Sync

파일 수정 후:
```
1. Android Studio 상단의 "Sync Now" 클릭
2. 또는: File → Sync Project with Gradle Files
```

---

**다음 단계**: 핵심 파일들을 생성하겠습니다!

준비되면 "다음" 또는 "파일 생성"이라고 말씀해주세요.
