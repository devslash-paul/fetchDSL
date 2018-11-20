pluginManagement {
  repositories {
    maven {
      url = uri("http://dl.bintray.com/kotlin/kotlin-eap")
    }

    mavenCentral()

    maven {
      url = uri("https://plugins.gradle.org/m2/")
    }
  }
}

enableFeaturePreview("STABLE_PUBLISHING")

include("api", "service", "extensions", "examples", "benchmarks")

rootProject.name = "fetshdsl"

