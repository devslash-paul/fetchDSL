import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  base
  kotlin("jvm") version "1.3.0-rc-116" apply false
  id("maven-publish")
}

repositories {
  jcenter()
  maven("http://dl.bintray.com/kotlin/kotlin-eap")
}
buildscript {
  repositories {
    jcenter()
    maven("http://dl.bintray.com/kotlin/kotlin-eap")
  }
}

allprojects {
  group = "net.devslash.fetchdsl"
  version = "0.1-SNAPSHOT"

  repositories {
    jcenter()
  }

}

subprojects {
  apply {
    plugin("maven-publish")
  }

  tasks.withType<KotlinCompile>().configureEach {
    println("Configuring $name in project ${project.name}...")
    kotlinOptions {
      suppressWarnings = false
    }
  }

  configure<PublishingExtension> {
    publications {
      register(project.name, MavenPublication::class) {
      }
    }
  }
}

dependencies {
  subprojects.forEach {
    archives(it)
  }
}
