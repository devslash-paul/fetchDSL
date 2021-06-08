import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
  project.extra.apply {
    set("kotlinVersion", "1.4.20")
    set("ktorVersion", "1.5.4")
    set("junitVersion", "4.12")
    set("ktorNettyVersion", "1.5.4")
    set("kotlinxCoroutinesVersion", "1.4.2")
  }

  dependencies {
    classpath(kotlin("gradle-plugin", version = "1.4.20"))
  }
}

plugins {
  base
  kotlin("jvm") version "1.4.20" apply false
  `maven-publish`
  signing
}

allprojects {
  group = "net.devslash.fetchdsl"
  version = "0.18.4-SNAPSHOT"

  repositories {
    mavenCentral()
  }

}


subprojects {
  apply {
    plugin("maven-publish")
    plugin("java-library")
    plugin("org.jetbrains.kotlin.jvm")
    plugin("org.gradle.maven-publish")
    plugin("org.gradle.signing")
    from("../publishing.gradle")
  }

  publishing {
    val userName: String? by project
    val pw: String? by project
    publications {
      create<MavenPublication>("library") {
        from(components["kotlin"])
        artifact(tasks["sourceJar"])
        artifact(tasks["packageJavadoc"])
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
        url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
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

  signing {
    sign(publishing.publications["library"])
  }

  tasks.withType<KotlinCompile>().configureEach {
    println("Configuring $name in project ${project.name}...")
    kotlinOptions {
      suppressWarnings = false
      jvmTarget = "1.8"
    }
  }
}

