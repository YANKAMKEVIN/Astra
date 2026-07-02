import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

// Gmail OAuth client id is read from the git-ignored local.properties (never committed).
val gmailAndroidClientId: String = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}.getProperty("GMAIL_ANDROID_CLIENT_ID", "")

// AppAuth redirect scheme is the reversed client id, e.g.
// com.googleusercontent.apps.1234567890-abcdef  (from 1234567890-abcdef.apps.googleusercontent.com)
val gmailRedirectScheme: String = if (gmailAndroidClientId.isNotBlank()) {
    "com.googleusercontent.apps." + gmailAndroidClientId.removeSuffix(".apps.googleusercontent.com")
} else {
    "com.googleusercontent.apps.placeholder"
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(projects.shared)

    implementation(libs.androidx.activity.compose)
    implementation(libs.koin.core)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)

    // Gmail OAuth (AppAuth) lives in the app module so its manifest redirect placeholder is
    // resolved here, not leaked into the shared library.
    implementation(libs.appauth)
    implementation(libs.androidx.security.crypto)
    implementation(libs.kotlinx.coroutines.core)
}

android {
    namespace = "com.kevin.astra"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.kevin.astra"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"

        buildConfigField("String", "GMAIL_ANDROID_CLIENT_ID", "\"$gmailAndroidClientId\"")

        // Consumed by AppAuth's RedirectUriReceiverActivity (manifest merge).
        manifestPlaceholders["appAuthRedirectScheme"] = gmailRedirectScheme
    }
    buildFeatures {
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
