import com.jfrog.bintray.gradle.BintrayExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  base
  `maven-publish`
  kotlin("jvm") version "1.3.30" apply false
  id("com.jfrog.bintray") version "1.8.4" apply false
}

repositories {
  jcenter()
}

allprojects {
  group = "net.devslash.fetchdsl"
  version = "0.13.2"

  repositories {
    jcenter()
  }

}

subprojects {
  apply {
    plugin("maven-publish")
    plugin("java-library")
    plugin("com.jfrog.bintray")

    from("../publishing.gradle")
  }

  tasks.withType<KotlinCompile>().configureEach {
    println("Configuring $name in project ${project.name}...")
    kotlinOptions {
      suppressWarnings = false
      jvmTarget = "1.8"
    }
  }

  val test by tasks.getting(Test::class) {
    useJUnitPlatform()
  }

  configure<BintrayExtension> {
    user = project.findProperty("bintrayUser") as String? ?: System.getenv("BINTRAY_USER")
    key = project.findProperty("bintrayApiKey") as String? ?: System.getenv("BINTRAY_API_KEY")

    setPublications("Publication")
    pkg.apply {
      repo = "FetchDSL"
      name = "fetchdsl"
      setLicenses("MIT")
      vcsUrl = "https://github.com/paulthom12345/FetchDSL"
      attributes = emptyMap<String, String>()
      override = true
      publish = true
      githubRepo = "paulthom12345/FetchDSL"
      version.apply {
        name = project.version.toString()
        vcsTag = project.version.toString()
        attributes = emptyMap<String, String>()
      }
    }
  }
}

dependencies {
  subprojects.forEach {
    archives(it)
  }
}
