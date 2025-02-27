# `libs.versions.toml` – Gradle Version Catalogs  

## 📌 Introduction  
The `libs.versions.toml` file is part of **Gradle Version Catalogs**, introduced in Gradle 7.0. It allows centralized dependency management in a structured and maintainable way, reducing duplication across multiple modules.  

This file is located in:  
```
gradle/libs.versions.toml
```

With Version Catalogs, you can define dependencies, versions, and plugin configurations in a single place, making it easier to update and maintain dependencies.

---

## 📂 File Structure  

A `libs.versions.toml` file consists of three main sections:  
1. **[versions]** – Defines reusable version numbers.  
2. **[libraries]** – Defines dependencies, referencing the versions.  
3. **[plugins]** – Defines Gradle plugins.  

---

## 📜 Example: `libs.versions.toml`  
```toml
[versions]
kotlin = "1.8.0"
android-gradle-plugin = "8.1.0"
core-ktx = "1.9.0"
compose = "1.5.0"
hilt = "2.48"

[libraries]
androidx-core-ktx = { module = "androidx.core:core-ktx", version.ref = "core-ktx" }
compose-ui = { module = "androidx.compose.ui:ui", version.ref = "compose" }
compose-material = { module = "androidx.compose.material:material", version.ref = "compose" }
hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-compiler = { module = "com.google.dagger:hilt-compiler", version.ref = "hilt" }

[plugins]
android-application = { id = "com.android.application", version.ref = "android-gradle-plugin" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

---

## ⚙️ How to Use Version Catalogs in Your Project  

### **Using Dependencies in `build.gradle.kts` (Module-level)**
Once the `libs.versions.toml` file is set up, use it in your `build.gradle.kts` (module-level) like this:

#### **Apply Plugins**
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
}
```

#### **Declare Dependencies**
```kotlin
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.compose.ui)
    implementation(libs.compose.material)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
}
```

---

## 🚀 Benefits of Using `libs.versions.toml`  

✅ **Centralized dependency management** – Manage all versions in one place.  
✅ **Better readability** – Dependencies are easier to track.  
✅ **Faster updates** – Change a version once, and it's updated everywhere.  
✅ **Eliminates duplication** – Avoid repeating the same version across multiple `build.gradle.kts` files.  

For more details, check out the **[Gradle Version Catalogs Documentation](https://docs.gradle.org/current/userguide/version_catalogs.html)**.  

---

## 🎯 Conclusion  
Using `libs.versions.toml` makes dependency management easier and more scalable in Android projects. If you are working with multiple modules, this approach significantly improves maintainability.  

Happy coding! 🚀🔥  
