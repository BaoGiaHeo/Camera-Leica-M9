import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Base64
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

androidComponents {
    // The 2.9.2 package publishes a single Debug variant; do not create a Release APK.
    beforeVariants(selector().withBuildType("release")) { variantBuilder ->
        variantBuilder.enable = false
    }
}

android {
    namespace = "com.example.camui"
    compileSdk = 37
    ndkVersion = "27.2.12479018"
    defaultConfig {
        applicationId = "com.fujiflim.mode"
        minSdk = 29
        // Keep targetSdk unchanged to avoid changing runtime permission behavior.
        targetSdk = 37
        versionCode = 25
        versionName = "3.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        externalNativeBuild {
            cmake {
                cppFlags += listOf("-std=c++17")
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    // 2.9.2 intentionally publishes one installable Debug APK only.
    // No developer/release variants, signing overrides, or minification are required.
    buildTypes {
        debug {
            buildConfigField("boolean", "DEVELOPER_LOG_ENABLED", "false")
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
        // The nested built-in LUT ZIP is already compressed; store it unchanged in APK assets.
        noCompress += listOf("mp3", "zip")
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
// The camera reference UI is bundled compressed so the whole source can be tracked as one
// deterministic input. It is expanded into a generated Kotlin source directory before compile.
val fuji320UiArchive = layout.projectDirectory.file("src/main/assets/fuji320_camera_ui.kt.gz.b64")
val fuji320UiGeneratedSource = layout.projectDirectory.file("src/main/java/com/example/camui/CameraUi.kt")
val generateFuji320ReferenceUi = tasks.register("generateFuji320ReferenceUi") {
    inputs.file(fuji320UiArchive)
    outputs.file(fuji320UiGeneratedSource)
    doLast {
        val encoded = fuji320UiArchive.asFile.readText(Charsets.US_ASCII)
        val packed = Base64.getMimeDecoder().decode(encoded)
        val output = ByteArrayOutputStream()
        GZIPInputStream(ByteArrayInputStream(packed)).use { it.copyTo(output) }
        var source = output.toString(Charsets.UTF_8.name())

        // Keep BoxWithConstraintsScope receivers intact: d(), fs() and CameraFilterRow()
        // read maxWidth/maxHeight, so stripping the receiver makes them uncompilable.
        if (!source.contains("import androidx.compose.foundation.layout.fillMaxWidth")) {
            source = source.replace(
                "package com.example.camui",
                "package com.example.camui\n\nimport androidx.compose.foundation.layout.fillMaxWidth",
                ignoreCase = false
            )
        }

        // 3.2.0 geometry corrections from the supplied 1846×852 reference.
        val geometryReplacements = listOf(
            "drawRect(Brush.verticalGradient(listOf(Color(0xFF1B1C1E), Color(0xFF070708))))" to "drawRect(Brush.verticalGradient(listOf(Color(0xFF1B1B1B), Color(0xFF111111))))",
            "        // thanh trên\n        drawRect(\n            Brush.verticalGradient(listOf(Color(0xFF2B2C2F), Color(0xFF0B0B0C))),\n            size = Size(size.width, py(72f)),\n        )\n        drawLine(Color(0x66FFFFFF), Offset(0f, py(72f)), Offset(size.width, py(72f)), py(1.5f))\n        for (i in 0..2) {\n            drawLine(\n                Color(0xFF9A9DA1), Offset(px(300f + i * 22f), py(62f)), Offset(px(345f + i * 22f), py(10f)),\n                px(5f), StrokeCap.Round,\n            )\n        }\n" to "",
            "Offset(px(10f), py(75f)), Size(px(1275f), py(715f)), CornerRadius(px(60f))" to "Offset(px(98f), py(24f)), Size(px(1030f), py(768f)), CornerRadius(px(76f))",
            "Offset(px(1095f), py(85f)), Size(px(190f), py(695f)), CornerRadius(px(30f))" to "Offset(px(1018f), py(96f)), Size(px(110f), py(580f)), CornerRadius(px(22f))",
            "Offset(px(1095f), py(85f)), Size(px(190f), py(695f)),\n            CornerRadius(px(30f))" to "Offset(px(1018f), py(96f)), Size(px(110f), py(580f)),\n            CornerRadius(px(22f))",
            "Box(r(1262f, 92f, 640f, 698f)" to "Box(r(1180f, 95f, 565f, 297f)",
            "r(18f, 22f, 52f, 52f)" to "r(18f, 48f, 52f, 52f)",
            "r(30f, 92f, 1075f, 680f)" to "r(112f, 48f, 913f, 682f)",
            "(30f + state.afX * 1075f)" to "(112f + state.afX * 913f)",
            "(92f + state.afY * 680f)" to "(48f + state.afY * 682f)",
            "r(1128f, 140f, 124f, 173f)" to "r(1028f, 110f, 84f, 80f)",
            "r(1128f, 348f, 124f, 167f)" to "r(1028f, 210f, 84f, 133f)",
            "r(1128f, 548f, 124f, 160f)" to "r(1028f, 377f, 84f, 164f)",
            "r(1302f, 96f, 584f, 300f)" to "r(1180f, 95f, 565f, 297f)",
            "r(1310f, 151f, 302f, 54f)" to "r(1188f, 151f, 300f, 54f)",
            "r(1310f, 211f, 302f, 54f)" to "r(1188f, 211f, 300f, 54f)",
            "r(1310f, 271f, 302f, 54f)" to "r(1188f, 271f, 300f, 54f)",
            "r(1620f, 126f, 232f, 246f)" to "r(1488f, 130f, 230f, 234f)",
            "r(1312f, 337f, 70f, 38f)" to "r(1214f, 337f, 45f, 38f)",
            "r(1370f, 338f, 265f, 36f)" to "r(1260f, 338f, 180f, 36f)",
            "r(1638f, 337f, 66f, 38f)" to "r(1448f, 337f, 62f, 38f)",
            "r(1310f, 418f, 180f, 48f)" to "r(1182f, 405f, 150f, 42f)",
            "r(1503f, 418f, 346f, 48f)" to "r(1344f, 405f, 222f, 42f)",
            "r(1512f, 418f, 328f, 20f)" to "r(1352f, 405f, 205f, 20f)",
            "r(1510f, 440f, 328f, 22f)" to "r(1350f, 425f, 205f, 18f)",
            "r(1720f, 327f, 98f, 105f)" to "r(1598f, 417f, 150f, 155f)",
            "r(1720f, 453f, 98f, 107f)" to "r(1598f, 581f, 150f, 154f)",
            "r(1368f, 515f, 235f, 235f)" to "r(1268f, 502f, 244f, 244f)",
            "r(540f, 795f, 220f, 55f)" to "r(438f, 734f, 235f, 43f)",
            "r(520f, 795f, 260f, 55f)" to "r(438f, 734f, 235f, 43f)",
            "val ly = py(818f)" to "val ly = py(752f)",
            "Offset(px(133f), ly), Offset(px(478f), ly)" to "Offset(px(150f), ly), Offset(px(436f), ly)",
            "Offset(px(823f), ly), Offset(px(1730f), ly)" to "Offset(px(672f), ly), Offset(px(1074f), ly)",
            "val left = if (selected) C.Yellow else Color(0xFFFF5A7A)" to "val left = if (selected) C.Yellow else C.White",
            "val right = if (selected) C.Yellow else Color(0xFF38C8FF)" to "val right = if (selected) C.Yellow else C.White",
            "TopBar(state, actions)" to "",
            "ExposureScale(state.ev)" to "",
            "Leather(leather, state.lightUi)" to ""
        )
        for ((before, after) in geometryReplacements) {
            if (before.isNotEmpty() && !source.contains(before)) {
                check(before == "TopBar(state, actions)" || before == "ExposureScale(state.ev)" || before == "Leather(leather, state.lightUi)") {
                    "Fuji 3.2 reference UI geometry anchor not found: $before"
                }
            }
            source = source.replace(before, after)
        }

        // The preview has no W/T zoom slider over the live image: zoom keys sit at far right.
        source = source.replace("            Box(r(335f, 105f, 55f, 45f), Alignment.Center) { Tx(\"W\", fs(28f)) }\n", "")
        source = source.replace("            ZoomBar(r(402f, 115f, 385f, 25f), state.zoom01, state.zoomText, fs(24f), actions.onZoomFraction, actions.onZoomFinished)\n", "")
        source = source.replace("            Box(r(793f, 105f, 40f, 45f), Alignment.Center) { Tx(\"T\", fs(28f)) }\n", "")
        source = source.replace("            ExposureScale(state.ev)\n", "")
        source = source.replace("            Box(r(50f, 645f, 44f, 46f).border(d(2f), C.White, RoundedCornerShape(d(4f))), Alignment.Center) {\n                Column(horizontalAlignment = Alignment.CenterHorizontally) {\n                    Tx(state.dr.take(2), fs(19f))\n                    Tx(state.dr.drop(2), fs(19f))\n                }\n            }\n", "")

        // Replace the old wide, multi-column overlay with the compact two-line reference HUD.
        val hudStart = source.indexOf("@Composable\nprivate fun BoxWithConstraintsScope.Hud(")
        val hudEnd = source.indexOf("\n// ---------------------------------------------------------------------\n// 10. MÀN CHỤP HOÀN CHỈNH", hudStart)
        check(hudStart >= 0 && hudEnd > hudStart) { "Fuji 3.2 HUD replacement anchors not found." }
        val hudReplacement = """@Composable
private fun BoxWithConstraintsScope.Hud(s: CameraUiState) {
    val dateTime = java.text.SimpleDateFormat("dd/MM/yyyy | HH:mm:ss", Locale.getDefault()).format(java.util.Date())
    Row(
        r(184f, 608f, 330f, 32f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(d(8f))
    ) {
        BatteryIcon(Modifier.width(d(56f)).height(d(25f)), s.batteryPct, s.batteryLook)
        Box(Modifier.width(d(30f)).height(d(27f)).border(d(1f), C.White, RoundedCornerShape(d(4f))), Alignment.Center) {
            Tx("AF-L", fs(15f), C.White, family = FontFamily.SansSerif)
        }
        Box(Modifier.width(d(58f)).height(d(27f)).background(Color(0xFF333638), RoundedCornerShape(d(4f))), Alignment.Center) {
            Tx("1080P/30FPS", fs(12f), C.White, family = FontFamily.SansSerif)
        }
        Box(Modifier.width(d(125f)).height(d(27f)), Alignment.CenterStart) {
            Tx("EV : " + String.format(Locale.US, "%.1f", s.ev), fs(21f), C.White, align = TextAlign.Start, family = FontFamily.SansSerif)
        }
    }
    Row(
        r(184f, 650f, 790f, 32f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(d(20f))
    ) {
        Tx(s.ss, fs(26f), C.White, family = FontFamily.SansSerif)
        Tx(s.iso, fs(26f), C.White, family = FontFamily.SansSerif)
        Tx(s.kelvin, fs(26f), C.White, family = FontFamily.SansSerif)
        Box(Modifier.width(d(1.5f)).height(d(25f)).background(Color.White.copy(alpha = 0.72f)))
        Tx(dateTime, fs(24f), C.White, family = FontFamily.SansSerif)
    }
}"""
        source = source.substring(0, hudStart) + hudReplacement + source.substring(hudEnd)

        // The theme switch is in the narrow camera-body column, under Settings.
        val settingsButton = """        MetalButton(r(1028f, 377f, 84f, 164f), d(22f), onClick = actions.onSetting) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                IconMenu(Modifier.size(d(46f)), C.White)
                Spacer(Modifier.height(d(8f)))
                Tx("SETTING", fs(22f))
            }
        }"""
        check(source.contains(settingsButton)) { "Fuji 3.2 Settings key anchor not found." }
        source = source.replace(settingsButton, settingsButton + """
        MetalButton(r(1028f, 588f, 84f, 82f), d(22f), onClick = actions.onTheme) {
            IconSunMoon(Modifier.size(d(48f)), state.lightUi)
        }""")
        check(source.startsWith("package com.example.camui")) {
            "Fuji 3.2 reference UI archive has an invalid Kotlin package."
        }
        val target = fuji320UiGeneratedSource.asFile
        target.parentFile.mkdirs()
        target.writeText(source, Charsets.UTF_8)
    }
}
// The generated CameraUi.kt is written into the regular main/java source tree before preBuild.

val shutterAudioBase64 = layout.projectDirectory.file("src/main/assets/fuji_mechanical_shutter_31.mp3.b64")
val shutterAudioResourceDirectory = layout.projectDirectory.dir("src/main/res/raw")
val shutterAudioResource = shutterAudioResourceDirectory.file("fuji_mechanical_shutter_31.mp3")

val generateFuji310ShutterAudio = tasks.register("generateFuji310ShutterAudio") {
    inputs.file(shutterAudioBase64)
    outputs.file(shutterAudioResource)
    doLast {
        val encoded = shutterAudioBase64.asFile.readText(Charsets.US_ASCII)
        val audioBytes = Base64.getMimeDecoder().decode(encoded)
        check(audioBytes.size > 2_000 && audioBytes.copyOfRange(0, 3).contentEquals("ID3".toByteArray())) {
            "Âm thanh màn trập 3.1.0 không phải MP3 hợp lệ."
        }
        shutterAudioResource.asFile.parentFile.mkdirs()
        shutterAudioResource.asFile.writeBytes(audioBytes)
    }
}

tasks.named("preBuild").configure {
    dependsOn(generateKk7FontResource, downloadSairaFontResources, generateFuji310ShutterAudio, generateFuji320ReferenceUi)
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
