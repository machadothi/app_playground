# Android Project Structure & Essential Concepts

This document provides a basic overview of key components in an Android project.

## 1. ADB (Android Debug Bridge)
**ADB** is a command-line tool that allows developers to communicate with an Android device or emulator. It is used for debugging, installing apps, transferring files, and executing shell commands.

**Common ADB commands:**
```sh
adb devices         # Lists connected devices
adb install app.apk # Installs an APK on a device
adb logcat          # Displays system logs for debugging
adb shell           # Opens an interactive shell on the device
```

## 2. Gradle
**Gradle** is the build system used in Android development. It automates the process of compiling, linking, and packaging Android applications. Gradle is responsible for dependency management, code compilation, and generating APKs or AABs.

Gradle uses **Kotlin DSL (`.kts`)** or **Groovy (`.gradle`)** for configuration.

---

## 3. `build.gradle.kts` (Project & Module Level)

### 3.1 Project-level `build.gradle.kts`
Located at the **root** of an Android project, this file manages global configurations, including:
- Plugin dependencies

**Example (`build.gradle.kts` at the project level):**
```kotlin
plugins {
    id("com.android.application") version "8.1.0" apply false
    id("org.jetbrains.kotlin.android") version "1.8.0" apply false
}
```

---

### 3.2 Module-level `build.gradle.kts`
Located inside the app/module directory, this file defines:
- SDK versions
- Dependencies
- Build types and flavors

**Example (`build.gradle.kts` at the module level):**
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.app"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.compose.ui:ui:1.5.0")
}
```

---

## 4. `settings.gradle.kts`
This file configures the project's structure, including:
- Declaring project modules
- Dependency management mode
- Repositories for dependency resolution

**Example:**
```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

include(":app") // Includes the app module in the project
```

---

## 5. `local.properties`
A file that stores local configuration values (not committed to version control). Commonly used to store:
- SDK path
- API keys (not recommended)

**Example:**
```
sdk.dir=/Users/username/Library/Android/sdk
```

---

## 6. `src/main` and `src/main/res`
### 6.1 `src/main`
The main source directory of an Android module, containing:
- Kotlin/Java source code (`src/main/java` or `src/main/kotlin`)
- XML resources (`src/main/res`)
- Manifest file (`src/main/AndroidManifest.xml`)

### 6.2 `src/main/res`
Holds all resources such as:
- **Layouts** (`res/layout/`) – UI XML files
- **Drawables** (`res/drawable/`) – Images, icons, vectors
- **Values** (`res/values/`) – Strings, colors, styles
- **Mipmaps** (`res/mipmap/`) – App launcher icons

---

## 7. Jetpack Compose
Jetpack Compose is **Android’s modern UI toolkit** for building native UIs using **Kotlin** instead of XML. It follows a declarative approach.

**Example:**
```kotlin
@Composable
fun Greeting(name: String) {
    Text(text = "Hello, $name!")
}
```

---

## 8. Dependency Injection (DI)
Dependency Injection (DI) is a design pattern used in Android to manage dependencies efficiently, improving testability and maintainability. Popular DI frameworks:
- **Hilt** (Recommended by Google)
- **Dagger**

**Example (Hilt Usage):**
```kotlin
@HiltAndroidApp
class MyApp : Application()
```

---

This README provides a high-level overview. For more details, refer to the **[Android Developer Documentation](https://developer.android.com/)**. 🚀
