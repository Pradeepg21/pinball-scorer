try {
    val processEnvironment = Class.forName("java.lang.ProcessEnvironment")
    val theEnvironmentField = processEnvironment.getDeclaredField("theEnvironment")
    theEnvironmentField.isAccessible = true
    val map = theEnvironmentField.get(null) as MutableMap<String, String>
    map.remove("ANDROID_PREFS_ROOT")

    val theCaseInsensitiveEnvironmentField = processEnvironment.getDeclaredField("theCaseInsensitiveEnvironment")
    theCaseInsensitiveEnvironmentField.isAccessible = true
    val ciMap = theCaseInsensitiveEnvironmentField.get(null) as MutableMap<String, String>
    ciMap.remove("ANDROID_PREFS_ROOT")
} catch (e: Exception) {
    // Fallback if not supported on this JDK
}

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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "pinballscorer"
include(":app")
