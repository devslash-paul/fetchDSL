import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*

buildscript {
  project.extra.apply {
    set("kotlinVersion", "1.9.10")
    set("ktorVersion", "1.6.2")
    set("junitVersion", "4.12")
    set("ktorNettyVersion", "1.6.2")
    set("kotlinxCoroutinesVersion", "1.5.1")
  }

  dependencies {
    classpath(kotlin("gradle-plugin", version = "1.7.21"))
  }
}

plugins {
  base
  kotlin("jvm") version "1.9.10" apply false
  id("org.jetbrains.dokka") version "1.8.10"
  jacoco
  java
  `maven-publish`
  signing
}

allprojects {
  group = "net.devslash.fetchdsl"
  version = "0.26.3-SNAPSHOT"

  repositories {
    mavenCentral()
  }
}

subprojects {
  apply {
    plugin("maven-publish")
    plugin("java-library")
    plugin("jacoco")
    plugin("org.jetbrains.kotlin.jvm")
    plugin("org.gradle.maven-publish")
    plugin("org.jetbrains.dokka")
    plugin("org.gradle.signing")
    from("../publishing.gradle")
  }

  jacoco {
    toolVersion = "0.8.7"
  }

  java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    withJavadocJar()
  }

  val javadocJar = tasks.named<Jar>("javadocJar") {
    from(tasks.named("dokkaHtml"))
  }

  publishing {
    val userName: String? by project
    val pw: String? by project
    publications {
      create<MavenPublication>("library") {
        from(components["kotlin"])
        artifact(tasks["sourceJar"])
        artifact(tasks["javadocJar"])
        pom {
          name.set("fetchDSL")
          description.set("A DSL for HTTP requests")
          url.set("https://fetchdsl.dev")
          licenses {
            license {
              name.set("MIT License")
              url.set("http://www.opensource.org/licenses/mit-license.php")
              distribution.set("repo")
            }
          }
          developers {
            developer {
              id.set("devslash-paul")
              name.set("Paul Thompson")
              email.set("paul@devslash.net")
            }
          }
          scm {
            url.set("https://github.com/devslash-paul/fetchdsl")
          }
        }
      }
    }
    repositories {
      maven {
        url =
          uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
        credentials {
          username = userName
          password = pw
        }
        mavenContent {
          releasesOnly()
        }
        authentication {
          create<BasicAuthentication>("basic")
        }
      }
    }
  }

  tasks.register("writeVersionProps") {
    dependsOn("processResources")
    doLast {
      File("$buildDir/resources/main/").mkdirs()
      File("$buildDir/resources/main/fetchdsl_version.properties").let {
        val p = Properties()
        p["version"] = project.version.toString()
        p.store(it.writer(), "Project version")
      }
    }
  }

  signing {
    sign(publishing.publications["library"])
  }

  tasks.jacocoTestReport {
    reports {
      html.isEnabled = false
      xml.isEnabled = true
      xml.destination = file("$buildDir/jacoco.xml")
    }
  }

  tasks.withType<KotlinCompile>().configureEach {
    dependsOn("writeVersionProps")
    println("Configuring $name in project ${project.name}...")
    kotlinOptions {
      suppressWarnings = false
      jvmTarget = "11"
    }
  }
}

