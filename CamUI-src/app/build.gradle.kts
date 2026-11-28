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
        versionCode = 28
        versionName = "3.5.0"
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

        // Scale helpers must work inside nested Compose scopes such as RowScope and
        // AnimatedVisibilityScope. Use screen configuration rather than implicit receivers.
        source = source.replace(
            "private fun BoxWithConstraintsScope.fs(px: Float): TextUnit = (px * maxWidth.value / RW).sp",
            "@Composable\nprivate fun fs(px: Float): TextUnit = (px * LocalConfiguration.current.screenWidthDp.toFloat() / RW).sp"
        )
        source = source.replace(
            "private fun BoxWithConstraintsScope.d(px: Float): Dp = maxWidth * (px / RW)",
            "@Composable\nprivate fun d(px: Float): Dp = (LocalConfiguration.current.screenWidthDp.toFloat() * (px / RW)).dp"
        )
        source = source.replace(
            "private fun BoxWithConstraintsScope.CameraFilterRow(",
            "private fun CameraFilterRow("
        )
        // Also accept a source archive that already contains the receiver-free signatures.
        if (source.contains("private fun fs(px: Float): TextUnit") &&
            !source.contains("@Composable\nprivate fun fs(px: Float): TextUnit")
        ) {
            source = source.replace(
                "private fun fs(px: Float): TextUnit",
                "@Composable\nprivate fun fs(px: Float): TextUnit"
            )
        }
        if (source.contains("private fun d(px: Float): Dp") &&
            !source.contains("@Composable\nprivate fun d(px: Float): Dp")
        ) {
            source = source.replace(
                "private fun d(px: Float): Dp",
                "@Composable\nprivate fun d(px: Float): Dp"
            )
        }
        check(source.contains("@Composable\nprivate fun fs(px: Float): TextUnit")) {
            "Fuji 3.2 source generator could not promote fs() to a composable helper."
        }
        check(source.contains("@Composable\nprivate fun d(px: Float): Dp")) {
            "Fuji 3.2 source generator could not promote d() to a composable helper."
        }
        check(source.contains("private fun CameraFilterRow(")) {
            "Fuji 3.2 source generator could not remove the nested CameraFilterRow receiver."
        }
        if (!source.contains("import androidx.compose.ui.platform.LocalConfiguration")) {
            source = source.replace(
                "import androidx.compose.ui.platform.LocalHapticFeedback",
                "import androidx.compose.ui.platform.LocalConfiguration\nimport androidx.compose.ui.platform.LocalHapticFeedback"
            )
        }
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

        val previewGestureBox = """            Box(
                Modifier.fillMaxSize().pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ -> if (zoom != 1f) a.onPinch(zoom) }
                }
            )"""
        val previewGestureBoxFixed = """            Box(
                Modifier.fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ -> if (zoom != 1f) a.onPinch(zoom) }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { point ->
                                a.onTapFocus(
                                    (point.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f),
                                    (point.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                                )
                            },
                            onLongPress = {
                                a.onAfL()
                                a.onAeL()
                            }
                        )
                    }
            )"""
        check(source.contains(previewGestureBox)) { "Fuji 3.2 preview gesture anchor not found." }
        source = source.replace(previewGestureBox, previewGestureBoxFixed)

        // The preview has no W/T zoom slider over the live image: zoom keys sit at far right.
        source = source.replace("            Box(r(335f, 105f, 55f, 45f), Alignment.Center) { Tx(\"W\", fs(28f)) }\n", "")
        source = source.replace("            ZoomBar(r(402f, 115f, 385f, 25f), state.zoom01, state.zoomText, fs(24f), actions.onZoomFraction, actions.onZoomFinished)\n", "")
        source = source.replace("            Box(r(793f, 105f, 40f, 45f), Alignment.Center) { Tx(\"T\", fs(28f)) }\n", "")
        source = source.replace("            ExposureScale(state.ev)\n", "")
        source = source.replace("            Box(r(50f, 645f, 44f, 46f).border(d(2f), C.White, RoundedCornerShape(d(4f))), Alignment.Center) {\n                Column(horizontalAlignment = Alignment.CenterHorizontally) {\n                    Tx(state.dr.take(2), fs(19f))\n                    Tx(state.dr.drop(2), fs(19f))\n                }\n            }\n", "")

        // T is the upper telephoto key; W is the lower wide-angle key.
        val zoomMinusHandler = """onHold = { actions.onZoomStep(-1) }, onHoldEnd = actions.onZoomFinished,
            onClick = { actions.onZoomStepTap(-1) },"""
        val zoomPlusHandler = """onHold = { actions.onZoomStep(1) }, onHoldEnd = actions.onZoomFinished,
            onClick = { actions.onZoomStepTap(1) },"""
        check(source.contains(zoomMinusHandler) && source.contains(zoomPlusHandler)) {
            "Fuji 3.2 T/W zoom handlers not found."
        }
        source = source.replace(zoomMinusHandler, "__FUJI_ZOOM_PLUS__")
        source = source.replace(zoomPlusHandler, zoomMinusHandler)
        source = source.replace("__FUJI_ZOOM_PLUS__", zoomPlusHandler)
        source = source.replace("""Tx("W", fs(66f))""", """Tx("__FUJI_ZOOM_TOP__", fs(66f))""")
        source = source.replace("""Tx("T", fs(66f))""", """Tx("W", fs(66f))""")
        source = source.replace("""Tx("__FUJI_ZOOM_TOP__", fs(66f))""", """Tx("T", fs(66f))""")

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

        // Keep the compact, icon-only physical camera keys shown in the reference.
        source = source.replace("Modifier.fillMaxWidth().height(d(37f)).padding(vertical = d(2f))",
            "Modifier.fillMaxWidth().height(d(56f)).padding(vertical = d(3f))")
        source = source.replace("""        Tx("F", fs(15f), if (selected) C.Yellow else C.White)
        Spacer(Modifier.width(d(8f)))
        Tx(name, fs(15f), if (lightUi) Color.Black else Color.White, align = TextAlign.Start, modifier = Modifier.weight(1f))
        if (selected) Tx("✓", fs(17f), C.Orange)""", """        Tx(
            name,
            fs(15f),
            if (lightUi) Color.Black else Color.White,
            align = TextAlign.Start,
            modifier = Modifier.weight(1f)
        )""")
        source = source.replace("""                    IconAlbum(Modifier.size(d(46f)), C.White)
                    Spacer(Modifier.height(d(10f)))
                    Tx("Album", fs(22f))""", """                    IconAlbum(Modifier.size(d(54f)), C.White)""")
        source = source.replace("""                IconFilter(Modifier.size(d(62f)), state.filterOn)
                Spacer(Modifier.height(d(10f)))
                Tx("BỘ LỌC", fs(22f))""", """                IconFilter(Modifier.size(d(62f)), state.filterOn)""")
        source = source.replace("""                IconMenu(Modifier.size(d(46f)), C.White)
                Spacer(Modifier.height(d(8f)))
                Tx("SETTING", fs(22f))""", """                IconMenu(Modifier.size(d(54f)), C.White)""")
        source = source.replace("            Box(r(1448f, 337f, 62f, 38f), Alignment.CenterStart) { Tx(evText, fs(22f), align = TextAlign.Start) }\n", "")

        // Animate the inline filter list instead of replacing the whole camera panel abruptly.
        source = source.replace(
            "import androidx.compose.animation.core.LinearEasing",
            "import androidx.compose.animation.AnimatedVisibility\nimport androidx.compose.animation.fadeIn\nimport androidx.compose.animation.fadeOut\nimport androidx.compose.animation.core.LinearEasing"
        )
        val filterPanelOld = """            if (state.filterExpanded) {
                Column(Modifier.fillMaxSize().padding(d(10f)).verticalScroll(rememberScrollState())) {
                    Row(Modifier.fillMaxWidth().padding(bottom = d(8f)), verticalAlignment = Alignment.CenterVertically) {
                        Tx("FUJIFILM", fs(19f), if (state.lightUi) Color.Black else Color.White, align = TextAlign.Start)
                        Spacer(Modifier.weight(1f))
                        Tx("${'$'}{state.filterCount}/${'$'}{state.filterCount}", fs(16f), C.Green)
                    }
                    CameraFilterRow("None", "Không (ảnh gốc)", state.activeFilterId == "None", state.lightUi) { a.onFilterSelect("None") }
                    state.filterRows.forEach { row ->
                        CameraFilterRow(row.first, row.second, state.activeFilterId == row.first, state.lightUi) { a.onFilterSelect(row.first) }
                    }
                }
            } else {
                Column(Modifier.fillMaxSize().padding(d(16f)), verticalArrangement = Arrangement.spacedBy(d(6f))) {
                    Tx(state.lunaSummary, fs(21f), if (state.lightUi) Color.Black else C.White, align = TextAlign.Start)
                    state.cameraSummary.take(5).forEach { line ->
                        Tx(line, fs(18f), if (state.lightUi) Color(0xFF262626) else C.White, align = TextAlign.Start)
                    }
                }
            }"""
        val filterPanelNew = """            if (!state.filterExpanded) {
                Column(Modifier.fillMaxSize().padding(d(16f)), verticalArrangement = Arrangement.spacedBy(d(6f))) {
                    Tx(state.lunaSummary, fs(21f), if (state.lightUi) Color.Black else C.White, align = TextAlign.Start)
                    state.cameraSummary.take(5).forEach { line ->
                        Tx(line, fs(18f), if (state.lightUi) Color(0xFF262626) else C.White, align = TextAlign.Start)
                    }
                }
            }
            AnimatedVisibility(
                visible = state.filterExpanded,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(140))
            ) {
                Column(Modifier.fillMaxSize().padding(d(10f)).verticalScroll(rememberScrollState())) {
                    Row(Modifier.fillMaxWidth().padding(bottom = d(8f)), verticalAlignment = Alignment.CenterVertically) {
                        Tx("FUJIFILM", fs(19f), if (state.lightUi) Color.Black else Color.White, align = TextAlign.Start)
                        Spacer(Modifier.weight(1f))
                        Tx("${'$'}{state.filterCount}/${'$'}{state.filterCount}", fs(16f), C.White)
                    }
                    state.filterRows.forEach { row ->
                        CameraFilterRow(row.first, "FUJIFILM: " + row.second, state.activeFilterId == row.first, state.lightUi) { a.onFilterSelect(row.first) }
                    }
                }
            }"""
        check(source.contains(filterPanelOld)) { "Fuji 3.2 filter panel animation anchor not found." }
        source = source.replace(filterPanelOld, filterPanelNew)

        // The theme switch is in the narrow camera-body column, under Settings.
        val settingsButton = """        MetalButton(r(1028f, 377f, 84f, 164f), d(22f), onClick = actions.onSetting) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                IconMenu(Modifier.size(d(54f)), C.White)
            }
        }"""
        check(source.contains(settingsButton)) { "Fuji 3.2 Settings key anchor not found." }
        source = source.replace(settingsButton, settingsButton + """
        MetalButton(r(1028f, 588f, 84f, 82f), d(22f), onClick = actions.onTheme) {
            IconSunMoon(Modifier.size(d(48f)), state.lightUi)
        }""")
        // 3.5.0: restore the supplied leather body panel without changing camera geometry.
        source = source.replace(
            "Box(r(1180f, 95f, 565f, 297f).background(Color(0xFF87898B), RoundedCornerShape(d(42f))).border(d(2f), Color(0xFF55585A), RoundedCornerShape(d(42f))))",
            "Leather(leather, state.lightUi)"
        )
        source = source.replace(
            """                                a.onAfL()
                                a.onAeL()""",
            """                                a.onAfL()"""
        )
        source = source.replace(
            """            Tx("EV : " + String.format(Locale.US, "%.1f", s.ev), fs(21f), C.White, align = TextAlign.Start, family = FontFamily.SansSerif)""",
            """            Tx("EV : " + String.format(Locale.US, "%.1f", s.ev) + if (s.countdown > 0) "  ·  " + s.countdown + "s" else "", fs(21f), C.White, align = TextAlign.Start, family = FontFamily.SansSerif)"""
        )
        source = source.replace(
            """    Row(
        r(184f, 650f, 790f, 32f),""",
            """    if (s.batteryLow && s.batteryBlinkOn) {
        Box(r(184f, 580f, 155f, 23f), Alignment.CenterStart) {
            Tx("BATTERY LOW", fs(13f), C.Red, align = TextAlign.Start, family = FontFamily.SansSerif)
        }
    }
    Row(
        r(184f, 650f, 790f, 32f),"""
        )
        source = source.replace(
            """        // MÀN HÌNH PREVIEW""",
            """        MetalButton(r(18f, 110f, 52f, 52f), d(26f), if (state.timerSeconds > 0) C.Blue else null, onClick = actions.onTimerMenu) {
            Tx(if (state.timerSeconds > 0) state.timerSeconds.toString() + "s" else "TMR", fs(20f))
        }
        if (state.timerMenuVisible) {
            Row(
                Modifier.absoluteOffset(maxWidth * (78f / RW), maxHeight * (110f / RH))
                    .size(maxWidth * (300f / RW), maxHeight * (52f / RH))
                    .clip(RoundedCornerShape(d(12f)))
                    .background(Color(0xE91C252B))
                    .border(d(1.5f), Color(0xFF42D9EA), RoundedCornerShape(d(12f)))
                    .padding(d(4f)),
                horizontalArrangement = Arrangement.spacedBy(d(5f)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(3, 5, 10, 0).forEach { seconds ->
                    Box(
                        Modifier.weight(1f).fillMaxSize()
                            .clip(RoundedCornerShape(d(8f)))
                            .background(if (state.timerSeconds == seconds) Color(0xFF17616C) else Color(0xFF30383D))
                            .clickable { actions.onTimerSelect(seconds) },
                        contentAlignment = Alignment.Center
                    ) {
                        Tx(if (seconds == 0) "OFF" else seconds.toString() + "s", fs(18f), C.White)
                    }
                }
            }
        }
        // MÀN HÌNH PREVIEW"""
        )
        source = source.replace(
            """            if (!state.hideOsd) {
                val m = Color(0xCCFFFFFF); val w = 2f * u""",
            """            if (!state.hideOsd && state.levelVisible) {
                val centerX = (112f + 913f * 0.5f) * sx
                val centerY = (48f + 682f * 0.5f) * sy
                rotate(state.horizonRollDegrees, Offset(centerX, centerY)) {
                    val levelColor = if (kotlin.math.abs(state.horizonRollDegrees) <= 0.5f) C.Green else C.Yellow
                    drawLine(levelColor.copy(alpha = 0.85f), Offset(190f * sx, centerY), Offset(947f * sx, centerY), 2.2f * u, StrokeCap.Round)
                    drawCircle(levelColor, 4f * u, Offset(centerX, centerY))
                }
            }
            if (!state.hideOsd) {
                val m = Color(0xCCFFFFFF); val w = 2f * u"""
        )
        source = source.replace(
            """        Tx("EV : " + String.format(Locale.US, "%.1f", s.ev), fs(21f), C.White, align = TextAlign.Start, family = FontFamily.SansSerif)""",
            """        Tx("EV : " + String.format(Locale.US, "%.1f", s.ev) + if (s.countdown > 0) "  ·  " + s.countdown + "s" else "", fs(21f), C.White, align = TextAlign.Start, family = FontFamily.SansSerif)"""
        )
        check(source.startsWith("package com.example.camui"))
        check(source.contains("Leather(leather, state.lightUi)"))
        check(source.contains("state.timerMenuVisible"))
        check(source.contains("BATTERY LOW"))
        check(source.contains("state.horizonRollDegrees"))
        check(!source.contains("a.onAeL()"))
        source = source.replace(
            """    val afState: AfState = AfState.IDLE,
)""",
            """    val afState: AfState = AfState.IDLE,
    val timerSeconds: Int = 0,
    val timerMenuVisible: Boolean = false,
    val countdown: Int = 0,
    val levelVisible: Boolean = false,
    val horizonRollDegrees: Float = 0f,
    val batteryLow: Boolean = false,
    val batteryBlinkOn: Boolean = true,
)"""
        )
        source = source.replace(
            """    val onKelvinFraction: (Float) -> Unit = {},
    val onZoomFinished: () -> Unit = {},""",
            """    val onKelvinFraction: (Float) -> Unit = {},
    val onShutterAuto: () -> Unit = {},
    val onIsoAuto: () -> Unit = {},
    val onKelvinAuto: () -> Unit = {},
    val onTimerMenu: () -> Unit = {},
    val onTimerSelect: (Int) -> Unit = {},
    val onZoomFinished: () -> Unit = {},"""
        )
        source = source.replace(
            """    onChange: (Float) -> Unit,
    onFinished: () -> Unit = {},
) {
    val current by rememberUpdatedState(onChange)
    var trackWidth by remember { mutableStateOf(1f) }""",
            """    onChange: (Float) -> Unit,
    onFinished: () -> Unit = {},
    onAuto: () -> Unit = {},
) {
    val current by rememberUpdatedState(onChange)
    var trackWidth by remember { mutableStateOf(1f) }
    var dragFraction by remember { mutableStateOf(fraction.coerceIn(0f, 1f)) }
    var isDragging by remember { mutableStateOf(false) }"""
        )
        source = source.replace(
            """                .pointerInput(Unit) {
                    detectHorizontalDragGestures(onDragEnd = onFinished, onDragCancel = onFinished) { change, _ ->
                        change.consume()
                        current((change.position.x / trackWidth).coerceIn(0f, 1f))
                    }
                }
                .pointerInput(Unit) { detectTapGestures { current((it.x / trackWidth).coerceIn(0f, 1f)); onFinished() } },""",
            """                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { start ->
                            isDragging = true
                            dragFraction = (start.x / trackWidth).coerceIn(0f, 1f)
                            current(dragFraction)
                        },
                        onDragEnd = { isDragging = false; onFinished() },
                        onDragCancel = { isDragging = false; onFinished() }
                    ) { change, _ ->
                        change.consume()
                        dragFraction = (change.position.x / trackWidth).coerceIn(0f, 1f)
                        current(dragFraction)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures {
                        val selected = (it.x / trackWidth).coerceIn(0f, 1f)
                        dragFraction = selected
                        current(selected)
                        onFinished()
                    }
                },"""
        )
        source = source.replace(
            """drawCircle(Color.White, size.height * 0.43f, Offset(size.width * fraction.coerceIn(0f, 1f), cy))""",
            """drawCircle(Color.White, size.height * 0.43f, Offset(size.width * (if (isDragging) dragFraction else fraction).coerceIn(0f, 1f), cy))"""
        )
        source = source.replace(
            """        Box(Modifier.width(d(72f)), Alignment.CenterStart) { Tx(rightText, fs(18f), Color.White, align = TextAlign.Start) }""",
            """        Box(Modifier.width(d(66f)), Alignment.CenterEnd) {
            Tx(if (valueText == "AUTO" || valueText == "ISO AUTO") "--" else valueText, fs(14f), Color.White, align = TextAlign.End)
        }
        Box(
            Modifier.width(d(57f)).height(d(30f))
                .clip(RoundedCornerShape(d(9f)))
                .background(Color(0xFF26343A))
                .border(d(1f), Color(0xFF55DCE8), RoundedCornerShape(d(9f)))
                .clickable(onClick = onAuto),
            Alignment.Center
        ) { Tx(rightText, fs(14f), Color(0xFF7CF4F4)) }"""
        )
        source = source.replace(
            """            r(1188f, 151f, 300f, 54f), "SS", state.ss, state.shutter01, "AUTO", actions.onShutterFraction, actions.onSensorFinished
        )""",
            """            r(1188f, 151f, 300f, 54f), "SS", state.ss, state.shutter01, "AUTO", actions.onShutterFraction, actions.onSensorFinished,
            onAuto = actions.onShutterAuto
        )"""
        )
        source = source.replace(
            """            r(1188f, 211f, 300f, 54f), "ISO", state.iso, state.iso01, "AUTO", actions.onIsoFraction, actions.onSensorFinished
        )""",
            """            r(1188f, 211f, 300f, 54f), "ISO", state.iso, state.iso01, "AUTO", actions.onIsoFraction, actions.onSensorFinished,
            onAuto = actions.onIsoAuto
        )"""
        )
        source = source.replace(
            """            r(1188f, 271f, 300f, 54f), "K", state.kelvin, state.kelvin01, "AUTO", actions.onKelvinFraction, actions.onSensorFinished
        )""",
            """            r(1188f, 271f, 300f, 54f), "K", state.kelvin, state.kelvin01, "AUTO", actions.onKelvinFraction, actions.onSensorFinished,
            onAuto = actions.onKelvinAuto
        )"""
        )
        check(source.contains("val timerSeconds: Int = 0")) { "Camera timer state generation failed." }
        check(source.contains("onKelvinAuto")) { "Auto WB action generation failed." }
        check(source.contains("onAuto = actions.onShutterAuto")) { "SS Auto control not wired." }
        check(source.contains("isDragging) dragFraction")) { "Slider thumb tracking missing." }
        source = source.replace(
            """    val onAfL: () -> Unit = {},""",
            """    val onAfL: (Float, Float) -> Unit = { _, _ -> },"""
        )
        source = source.replace(
            """                            onLongPress = {
                                a.onAfL()
                            }""",
            """                            onLongPress = { point ->
                                a.onAfL(
                                    (point.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f),
                                    (point.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                                )
                            }"""
        )
        source = source.replace(
            """onClick = a.onAfL""",
            """onClick = { a.onAfL(0.5f, 0.5f) }"""
        )
        source = source.replace(
            """            onAfL = { s = s.copy(afLocked = !s.afLocked) },""",
            """            onAfL = { _, _ -> s = s.copy(afLocked = !s.afLocked) },"""
        )
        check(source.contains("val onAfL: (Float, Float) -> Unit")) { "AF-L touch coordinate callback not generated." }
        check(source.contains("point.x / size.width")) { "AF-L long press point not forwarded." }
        check(source.startsWith("package com.example.camui")) {
            "Fuji 3.2 reference UI archive has an invalid Kotlin package."
        }

        // 3.5.0 follow-up: add clean preview fullscreen and remove demo telemetry placeholders.
        source = source.replace(
            """    val batteryBlinkOn: Boolean = true,
)""",
            """    val batteryBlinkOn: Boolean = true,
    val fullscreenPreview: Boolean = false,
)"""
        )
        source = source.replace(
            """    val onTimerSelect: (Int) -> Unit = {},
    val onZoomFinished: () -> Unit = {},""",
            """    val onTimerSelect: (Int) -> Unit = {},
    val onFullscreenPreview: () -> Unit = {},
    val onZoomFinished: () -> Unit = {},"""
        )
        source = source.replace(
            """val cameraSummary: List<String> = listOf("Template: 29°C", "CPU: -- | GPU: --", "1080P", "30FPS"),""",
            """val cameraSummary: List<String> = listOf("1080P", "30FPS"),"""
        )

        source = source.replace(
            """val lunaSummary: String = "LunaDream 3.0 · 180 MP",""",
            """val lunaSummary: String = "Original","""
        )
        source = source.replace(
            """Tx("LUNADREAM ${state.lunaLabel ?: "3.0"}", fs(20f))""",
            """Tx(if (state.lunaLabel == null) "ORIGINAL" else "LUNADREAM ${state.lunaLabel}", fs(20f))"""
        )
        check(source.contains("""if (state.lunaLabel == null) "ORIGINAL" """.trimEnd())) {
            "3.5.0 Original camera mode label missing."
        }
        val fullscreenButton = """        MetalButton(r(18f, 172f, 52f, 52f), d(26f), onClick = actions.onFullscreenPreview) {
            Canvas(Modifier.size(d(30f))) {
                val w = size.width
                val a = w * 0.16f
                val b = w * 0.42f
                val c = w * 0.58f
                val far = w * 0.84f
                val stroke = w * 0.075f
                drawLine(C.White, Offset(a, b), Offset(a, a), stroke, StrokeCap.Round)
                drawLine(C.White, Offset(a, a), Offset(b, a), stroke, StrokeCap.Round)
                drawLine(C.White, Offset(c, a), Offset(far, a), stroke, StrokeCap.Round)
                drawLine(C.White, Offset(far, a), Offset(far, b), stroke, StrokeCap.Round)
                drawLine(C.White, Offset(a, c), Offset(a, far), stroke, StrokeCap.Round)
                drawLine(C.White, Offset(a, far), Offset(b, far), stroke, StrokeCap.Round)
                drawLine(C.White, Offset(c, far), Offset(far, far), stroke, StrokeCap.Round)
                drawLine(C.White, Offset(far, c), Offset(far, far), stroke, StrokeCap.Round)
            }
        }
"""
        check(source.contains("        // MÀN HÌNH PREVIEW")) { "Preview insertion point missing." }
        source = source.replace(
            """        // MÀN HÌNH PREVIEW""",
            fullscreenButton + """        // MÀN HÌNH PREVIEW"""
        )
        val rootOpen = """BoxWithConstraints(modifier.fillMaxSize().background(if (state.lightUi) Color(0xFFE6E6E6) else Color(0xFF080809))) {
        Chrome()"""
        check(source.contains(rootOpen)) { "Fullscreen root insertion point missing." }
        source = source.replace(
            rootOpen,
            """BoxWithConstraints(modifier.fillMaxSize().background(if (state.lightUi) Color(0xFFE6E6E6) else Color(0xFF080809))) {
        if (state.fullscreenPreview) {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                preview()
                MetalButton(r(18f, 18f, 52f, 52f), d(26f), onClick = actions.onFullscreenPreview) {
                    Tx("×", fs(30f), C.White)
                }
            }
        } else {
        Chrome()"""
        )
        val uiEnd = """    }
}

// ---------------------------------------------------------------------
// 11. BẢN THỬ: gọi CameraUiDemo() để xem giao diện chạy được chưa"""
        check(source.contains(uiEnd)) { "Fullscreen branch closing point missing." }
        source = source.replace(
            uiEnd,
            """        }
    }
}

// ---------------------------------------------------------------------
// 11. BẢN THỬ: gọi CameraUiDemo() để xem giao diện chạy được chưa"""
        )
        check(source.contains("state.fullscreenPreview")) { "Fullscreen preview state was not generated." }
        check(source.contains("onFullscreenPreview: () -> Unit")) { "Fullscreen action was not generated." }
        check(source.contains("r(18f, 172f, 52f, 52f)")) { "Fullscreen button is not below timer." }
        check(!source.contains("CPU: -- | GPU: --")) { "CPU/GPU demo placeholder was not removed." }
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
