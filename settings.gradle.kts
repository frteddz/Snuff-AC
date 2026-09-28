pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/") { name = "papermc" }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "snuffac"

include(
    "snuffac-api",
    "snuffac-core",
    "snuffac-platform-paper",
    "snuffac-platform-velocity",
    "snuffac-dist",
)
