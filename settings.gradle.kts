pluginManagement {
    repositories {
        gradlePluginPortal() // if pluginManagement.repositories looks like this, it can be omitted as this is the default
        // general repos for all sub-modules
        mavenCentral()
    }
    includeBuild("convention-plugins")
}

// this should not be needed but can remove later once stable
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

rootProject.name = "mzmine"
include(
    "mzmine-community",
    "taskcontroller",
    "utils",
    "javafx-framework",
    "config",
)
//includeBuild("convention-plugins")
