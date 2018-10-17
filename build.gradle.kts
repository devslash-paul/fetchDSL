import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "1.3.0-rc-116"
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
  version = "1.0"

  repositories {
    jcenter()
  }
}

subprojects {
  tasks.withType<KotlinCompile>().configureEach {
    println("Configuring $name in project ${project.name}...")
    kotlinOptions {
      suppressWarnings = false
    }
  }
}

dependencies {
  subprojects.forEach {
    archives(it)
  }
}
