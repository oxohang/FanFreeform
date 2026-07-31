import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    id("com.android.application")
}

val releaseKeystorePath = providers.gradleProperty("HYPERGESTURE_KEYSTORE")
    .orElse(providers.environmentVariable("HYPERGESTURE_KEYSTORE"))
    .orNull
val releaseStorePassword = providers.gradleProperty("HYPERGESTURE_STORE_PASSWORD")
    .orElse(providers.environmentVariable("HYPERGESTURE_STORE_PASSWORD"))
    .orNull
val releaseKeyAlias = providers.gradleProperty("HYPERGESTURE_KEY_ALIAS")
    .orElse(providers.environmentVariable("HYPERGESTURE_KEY_ALIAS"))
    .orNull
val releaseKeyPassword = providers.gradleProperty("HYPERGESTURE_KEY_PASSWORD")
    .orElse(providers.environmentVariable("HYPERGESTURE_KEY_PASSWORD"))
    .orNull
val releaseSigningReady = listOf(
    releaseKeystorePath,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { !it.isNullOrBlank() }

android {
    namespace = "com.oxohang.fanfreeform"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.oxohang.fanfreeform"
        minSdk = 30
        targetSdk = 34
        versionCode = 131
        versionName = "1.0"
    }

    signingConfigs {
        create("release") {
            if (releaseSigningReady) {
                storeFile = file(releaseKeystorePath!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ""
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = false
            if (releaseSigningReady) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    compileOnly("de.robv.android.xposed:api:82")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}

val archiveDebugApk by tasks.registering {
    doLast {
        val archiveVersionName = android.defaultConfig.versionName ?: "unknown"
        val archiveVersionCode = android.defaultConfig.versionCode ?: 0
        val stamp = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
        )
        val archiveDir = rootProject.layout.projectDirectory.dir("release-archive").asFile
        archiveDir.mkdirs()
        copy {
            from(layout.buildDirectory.file("outputs/apk/debug/app-debug.apk"))
            into(archiveDir)
            rename {
                "HyperGesture-$archiveVersionName-debug-build$archiveVersionCode-$stamp.apk"
            }
        }
    }
}

val archiveReleaseApk by tasks.registering {
    doLast {
        val archiveVersionName = android.defaultConfig.versionName ?: "unknown"
        val archiveVersionCode = android.defaultConfig.versionCode ?: 0
        val stamp = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
        )
        val archiveDir = rootProject.layout.projectDirectory.dir("release-archive").asFile
        archiveDir.mkdirs()
        val outputDir = layout.buildDirectory.dir("outputs/apk/release").get().asFile
        val signedApk = outputDir.resolve("app-release.apk")
        val unsignedApk = outputDir.resolve("app-release-unsigned.apk")
        val sourceApk = if (signedApk.isFile) signedApk else unsignedApk
        if (!sourceApk.isFile) {
            throw GradleException("Release APK was not produced")
        }
        val signatureLabel = if (sourceApk == signedApk) "" else "-unsigned"
        copy {
            from(sourceApk)
            into(archiveDir)
            rename {
                "HyperGesture-$archiveVersionName$signatureLabel-build$archiveVersionCode-$stamp.apk"
            }
        }
    }
}

tasks.configureEach {
    if (name == "assembleDebug") finalizedBy(archiveDebugApk)
    if (name == "assembleRelease") finalizedBy(archiveReleaseApk)
}
