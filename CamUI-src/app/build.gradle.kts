import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.camui"
    compileSdk = 37
    // Instrumentation tests target the same canonical Developer variant that CI publishes.
    testBuildType = "developer"

    defaultConfig {
        applicationId = "com.fujifilm.mode"
        minSdk = 29
        // Keep targetSdk unchanged to avoid changing runtime permission behavior.
        targetSdk = 37
        versionCode = 18
        versionName = "2.6.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = ""
            buildConfigField("boolean", "DEVELOPER_LOG_ENABLED", "false")
        }
        create("developer") {
            initWith(getByName("debug"))
            applicationIdSuffix = ""
            versionNameSuffix = ""
            buildConfigField("boolean", "DEVELOPER_LOG_ENABLED", "true")
        }
        release {
            // The installable release uses the canonical application ID.
            isMinifyEnabled = false
            buildConfigField("boolean", "DEVELOPER_LOG_ENABLED", "false")
            // Debug signing keeps CI output directly installable for on-device testing.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    androidResources {
        noCompress += listOf("mp3")
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}


kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        optIn.add("androidx.camera.camera2.interop.ExperimentalCamera2Interop")
    }
}

dependencies {
    implementation("com.google.mlkit:face-detection:16.1.7")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    val camerax = "1.6.2"
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.window:window:1.4.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.camera:camera-core:$camerax")
    implementation("androidx.camera:camera-camera2:$camerax")
    implementation("androidx.camera:camera-lifecycle:$camerax")
    implementation("androidx.camera:camera-view:$camerax")
    implementation("androidx.camera:camera-video:$camerax")
    implementation("androidx.camera:camera-extensions:$camerax")
}
