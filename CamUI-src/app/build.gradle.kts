import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Base64
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.camui"
    compileSdk = 37
    // Instrumentation tests target the Developer variant used by CI so the
    // KK7 Android-runtime glyph checks run against the same diagnostic build.
    testBuildType = "developer"

    defaultConfig {
        applicationId = "com.fujifilm.mode"
        minSdk = 29
        // Keep targetSdk unchanged to avoid changing runtime permission behavior.
        targetSdk = 37
        versionCode = 16
        versionName = "2.5.0"
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
            // The deliverable keeps the requested .developer application ID,
            // matching the Developer test variant and preserving install/upgrade identity.
            applicationIdSuffix = ".developer"
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
val kk7FontResourceDirectory = layout.projectDirectory.dir("src/main/res/font")
val kk7FontResource = kk7FontResourceDirectory.file("vcr_osd_mono_kk7.ttf")

val watermarkFontAssets = linkedMapOf(
    "watermark_jost_variable.ttf.b64" to "jost_watermark.ttf",
    "watermark_inter_variable.ttf.b64" to "inter_watermark.ttf",
    "watermark_ibm_plex_mono_regular.ttf.b64" to "ibm_plex_mono_watermark.ttf"
).map { (sourceName, resourceName) ->
    layout.projectDirectory.file("src/main/assets/fonts/$sourceName") to
        kk7FontResourceDirectory.file(resourceName)
}

val generateKk7FontResource = tasks.register("generateKk7FontResource") {
    val watermarkSources = watermarkFontAssets.map { it.first }
    val watermarkOutputs = watermarkFontAssets.map { it.second }
    inputs.file(kk7FontSource)
    inputs.files(watermarkSources)
    outputs.file(kk7FontResource)
    outputs.files(watermarkOutputs)
    doLast {
        val destination = kk7FontResource.asFile
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

        watermarkFontAssets.forEach { (source, resource) ->
            val encoded = source.asFile.readText(Charsets.US_ASCII)
            val bytes = Base64.getMimeDecoder().decode(encoded)
            check(bytes.size > 4_000 && bytes.copyOfRange(0, 4).contentEquals(byteArrayOf(0, 1, 0, 0))) {
                "Watermark font ${source.asFile.name} is not a valid TrueType font."
            }
            resource.asFile.parentFile.mkdirs()
            resource.asFile.writeBytes(bytes)
        }
    }
}

// Saira Semi Condensed is bundled as the three static OFL weights requested
// by the watermark renderer. These exact upstream files are placed in res/font
// before Android resource merging so both preview and exported JPEG use the same font.
val sairaFontFiles = linkedMapOf(
    "SairaSemiCondensed-Regular.ttf" to "saira_semicondensed_regular.ttf",
    "SairaSemiCondensed-Medium.ttf" to "saira_semicondensed_medium.ttf",
    "SairaSemiCondensed-SemiBold.ttf" to "saira_semicondensed_semibold.ttf",
    "SairaSemiCondensed-Bold.ttf" to "saira_semicondensed_bold.ttf"
)
val sairaFontOutputFiles = sairaFontFiles.values.map { kk7FontResourceDirectory.file(it) }
val sairaLicenseFile = layout.projectDirectory.file("src/main/assets/fonts/SAIRA-OFL.txt")

val downloadSairaFontResources = tasks.register("downloadSairaFontResources") {
    outputs.files(sairaFontOutputFiles)
    outputs.file(sairaLicenseFile)
    doLast {
        val upstream = "https://raw.githubusercontent.com/Omnibus-Type/Saira/master/Saira/fonts/ttf/"
        sairaFontFiles.forEach { (upstreamName, resourceName) ->
            val destination = kk7FontResourceDirectory.file(resourceName).asFile
            if (!destination.isFile || destination.length() < 20_000L) {
                destination.parentFile.mkdirs()
                val connection = URL(upstream + upstreamName).openConnection() as HttpURLConnection
                connection.connectTimeout = 20_000
                connection.readTimeout = 30_000
                connection.setRequestProperty("User-Agent", "FujiFilm-Mode-Android-Build")
                connection.getInputStream().use { input ->
                    destination.outputStream().use { output -> input.copyTo(output) }
                }
            }
            val signature = destination.inputStream().use { it.readNBytes(4) }
            check(signature.contentEquals(byteArrayOf(0, 1, 0, 0)) && destination.length() > 20_000L) {
                "Downloaded Saira font $upstreamName is not a valid TrueType resource."
            }
        }
        if (!sairaLicenseFile.asFile.isFile || sairaLicenseFile.asFile.length() < 500L) {
            sairaLicenseFile.asFile.parentFile.mkdirs()
            val licenseUrl = "https://raw.githubusercontent.com/Omnibus-Type/Saira/master/OFL.txt"
            val connection = URL(licenseUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = 20_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("User-Agent", "FujiFilm-Mode-Android-Build")
            connection.getInputStream().use { input ->
                sairaLicenseFile.asFile.outputStream().use { output -> input.copyTo(output) }
            }
        }
    }
}

// The Android source set receives an ordinary directory, not a Provider. Both
// the KK7 resources and Saira static weights are ready before Android merging.
android.sourceSets.getByName("main").res.srcDir(kk7FontResourceDirectory.asFile)
tasks.named("preBuild").configure {
    dependsOn(generateKk7FontResource, downloadSairaFontResources)
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
