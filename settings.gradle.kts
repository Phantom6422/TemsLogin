pluginManagement {
    repositories {
        maven {
            url = uri("https://repo.kyngs.xyz/gradle-plugins")
        }
        gradlePluginPortal()
    }
}

rootProject.name = "TemsLogin"
include("Plugin")
include("API")
