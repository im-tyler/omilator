plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    jvm("desktop") {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
        iosX64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "CoreLibretro"
            isStatic = true
        }
        iosTarget.compilations.getByName("main").cinterops {
            create("libretro") {
                defFile(project.file("src/nativeInterop/cinterop/libretro.def"))
                includeDirs(project.file("src/nativeInterop/cinterop"))
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.touchlab.kermit)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "com.omilator.core.libretro"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

tasks.register<JavaExec>("phase1Verify") {
    group = "verification"
    description = "Run Phase 1 libretro FFM bridge verification"

    val desktopTarget = kotlin.targets.getByName("desktop")
    val desktopCompilation = desktopTarget.compilations.getByName("main") as org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation

    classpath = desktopCompilation.output.allOutputs + desktopCompilation.compileDependencyFiles
    mainClass.set("com.omilator.core.libretro.jvm.Phase1VerificationKt")
    workingDir = rootProject.projectDir
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

tasks.register<JavaExec>("phase2Verify") {
    group = "verification"
    description = "Run Phase 2 video rendering verification"

    val desktopTarget = kotlin.targets.getByName("desktop")
    val desktopCompilation = desktopTarget.compilations.getByName("main") as org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation

    classpath = desktopCompilation.output.allOutputs + desktopCompilation.compileDependencyFiles
    mainClass.set("com.omilator.core.libretro.jvm.Phase2VerificationKt")
    workingDir = rootProject.projectDir
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

tasks.register<JavaExec>("phase5Verify") {
    group = "verification"
    description = "Run Phase 5 save state verification"

    val desktopTarget = kotlin.targets.getByName("desktop")
    val desktopCompilation = desktopTarget.compilations.getByName("main") as org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation

    classpath = desktopCompilation.output.allOutputs + desktopCompilation.compileDependencyFiles
    mainClass.set("com.omilator.core.libretro.jvm.Phase5VerificationKt")
    workingDir = rootProject.projectDir
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
