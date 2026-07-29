import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    id("com.android.application")
}

android {
    namespace = "com.oxohang.fanfreeform"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.oxohang.fanfreeform"
        minSdk = 30
        targetSdk = 34
        versionCode = 83
        versionName = "beta0.9"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ""
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = false
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

tasks.configureEach {
    if (name == "assembleDebug") finalizedBy(archiveDebugApk)
}
