import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.zip.GZIPInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.camui"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.fujifilm.mode"
        minSdk = 29
        // Keep targetSdk unchanged to avoid changing runtime permission behavior.
        targetSdk = 35
        versionCode = 16
        versionName = "2.5.0Dream"
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
            applicationIdSuffix = ".developer"
            versionNameSuffix = ""
            buildConfigField("boolean", "DEVELOPER_LOG_ENABLED", "true")
        }
        release {
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


val kk7FontSource = layout.projectDirectory.file("src/main/assets/fonts/vcr_osd_mono_kk7.ttf.gz")
val generatedKk7FontResources = layout.buildDirectory.dir("generated/res/kk7-font")

val generateKk7FontResource = tasks.register("generateKk7FontResource") {
    val generatedFont = generatedKk7FontResources.map {
        it.file("font/vcr_osd_mono_kk7.ttf")
    }
    inputs.file(kk7FontSource)
    outputs.file(generatedFont)
    doLast {
        val destination = generatedFont.get().asFile
        destination.parentFile.mkdirs()
        GZIPInputStream(kk7FontSource.asFile.inputStream()).use { input ->
            destination.outputStream().use { output -> input.copyTo(output) }
        }
        val signature = destination.inputStream().use { it.readNBytes(4) }
        check(signature.contentEquals(byteArrayOf(0, 1, 0, 0))) {
            "KK7 font did not decompress to a valid TrueType resource."
        }
        check(destination.length() > 4_000L) {
            "KK7 font resource is unexpectedly small."
        }
    }
}

android.sourceSets.getByName("main").res.srcDir(generatedKk7FontResources)
tasks.named("preBuild").configure {
    dependsOn(generateKk7FontResource)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        optIn.add("androidx.camera.camera2.interop.ExperimentalCamera2Interop")
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    val camerax = "1.6.2"
    implementation("androidx.core:core-ktx:1.15.0")
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
