rootProject.name = "fetshdsl"
pluginManagement {
  repositories {
    maven {
      url = uri("https://dl.bintray.com/kotlin/kotlin-eap")
    }
    mavenCentral()
    maven {
      url = uri("https://plugins.gradle.org/m2/")
    }
  }
}

include("api", "service", "examples", "test-utils")
