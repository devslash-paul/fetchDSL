rootProject.name = "fetshdsl"
pluginManagement {
  plugins {
    // JMH
    id("me.champeau.jmh") version "0.6.6"
    id("com.github.johnrengelman.shadow") version "7.1.2"
  }
  repositories {
    mavenCentral()
    maven {
      url = uri("https://plugins.gradle.org/m2/")
    }
  }
}

include("api", "benchmarks", "service", "examples", "lib-test")
