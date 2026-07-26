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
        versionCode = 12
        versionName = "0.3.9"
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
