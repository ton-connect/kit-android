pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "AndroidDemo"

include(":app")
include(":designsystem")

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
