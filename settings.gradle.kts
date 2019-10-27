rootProject.name = "fetshdsl"
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

include("api", "service", "extensions", "examples", "benchmarks", "test-utils")
