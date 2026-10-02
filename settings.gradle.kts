pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "PixelBot"
include(":app")
include(":body")
include(":ears")
include(":hands")
include(":brain")
include(":widget")
include(":memory")
include(":safety")