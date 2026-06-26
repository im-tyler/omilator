import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvm("desktop") {
        compilations.all { kotlinOptions { jvmTarget = "17" } }
    }

    sourceSets {
        val desktopMain by getting {
            dependencies {
                implementation(project(":ui-shared"))
                implementation(project(":core-libretro"))
                implementation(project(":data-library"))
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.omilator.app.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Omilator"
            packageVersion = "1.0.0"

            macOS {
                bundleID = "com.omilator.app"
                dockName = "Omilator"
            }
            windows {
                packageName = "Omilator"
            }
            linux {
                packageName = "omilator"
            }
        }
    }
}
