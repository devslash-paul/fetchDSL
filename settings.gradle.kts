rootProject.name = "fetshdsl"
pluginManagement {
  repositories {
    mavenCentral()
    maven {
      url = uri("https://plugins.gradle.org/m2/")
    }
  }
}

include("api", "service", "examples", "lib-test")
