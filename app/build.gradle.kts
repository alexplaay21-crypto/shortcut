plugins {
    id("com.android.application") version "9.4.0"

    // AGP 9 compiles Kotlin itself ("built-in Kotlin"), so org.jetbrains.kotlin.android
    // must NOT be applied here — it now fails the build. Declaring the Compose compiler
    // plugin at 2.4.20 is also what puts Kotlin 2.4.20 on the build classpath
    // (AGP's own default is 2.2.10).
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20"

    // Room's annotation processor. KSP releases are independent of the Kotlin version.
    id("com.google.devtools.ksp") version "2.3.12"
}

android {
    namespace = "com.sunflower.shortcut"

    // Compose 1.12 (BOM 2026.08.00) is built against API 37.
    compileSdk = 37

    defaultConfig {
        // Do not add a debug applicationIdSuffix: AutomationPermissionChecker compares
        // against "com.sunflower.shortcut/.accessibility.ShortcutAccessibilityService",
        // so a different package would make the Accessibility check fail in debug builds.
        applicationId = "com.sunflower.shortcut"

        // 26 = the first version with NotificationChannel, which TriggerMonitorService
        // uses without a version check; it also lets the adaptive icon live in mipmap-anydpi.
        minSdk = 26

        // Google Play requires 36+ for new apps and updates since 31 Aug 2026.
        targetSdk = 36

        versionCode = 1
        versionName = "1.0.0" // keep equal to AppConfig.APP_VERSION_NAME
    }

    buildTypes {
        release {
            // Off until the first successful build. Before turning R8 on, keep the JS bridge:
            //   -keepclassmembers class * { @android.webkit.JavascriptInterface <methods>; }
            // (JsRuntime's NativeBridge is called by name from the WebView; R8 would strip it
            // and every scenario would fail silently in release only.)
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
        compilerOptions.freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
    }

    buildFeatures {
        compose = true
        buildConfig = true // BuildConfig.DEBUG / APPLICATION_ID are read in the UI and ShortcutApplication
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

ksp {
    // Room writes its schema JSON here (ShortcutDatabase has exportSchema = true).
    // Commit the folder: it is what future Migrations are checked against.
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // ---- Compose ----
    val composeBom = platform("androidx.compose:compose-bom:2026.08.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    // Needed, not optional: the UI uses Code, Schema, History, Save, Upload, ContentCopy,
    // PowerSettingsNew, AutoAwesome... none of which are in the small icons-core set.
    implementation("androidx.compose.material:material-icons-extended")

    // ---- AndroidX ----
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.activity:activity-compose:1.12.0")
    implementation("androidx.navigation:navigation-compose:2.9.8")
    implementation("androidx.datastore:datastore-preferences:1.2.0")
    implementation("androidx.work:work-runtime-ktx:2.11.2")

    // ---- Room ----
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    // ---- Coroutines (Dispatchers.Main is used by ScenarioSharer) ----
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
}
