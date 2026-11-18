plugins {
    id("com.android.application") version "9.1.1" apply false
    // AGP 9.1.1 bundles Kotlin 2.2.10; keep the Compose compiler on the same version.
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
}
