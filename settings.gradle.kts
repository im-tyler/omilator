rootProject.name = "Omilator"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                releasesOnly()
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

include(":core-libretro")
include(":core-render")
include(":core-audio")
include(":core-input")
include(":data-library")
include(":data-settings")
include(":data-saves")
include(":ui-shared")
include(":app-desktop")
include(":app-android")
