pluginManagement {
    includeBuild("buildLogic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}


dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io")
        maven("https://plugins.gradle.org/m2/")
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

gradle.startParameter.excludedTaskNames.addAll(listOf(":buildLogic:convention:testClasses"))

rootProject.name = "Scrolless"

include(
    ":app",
    ":libraries:framework",
    ":libraries:components",
    ":libraries:testutils",
)
