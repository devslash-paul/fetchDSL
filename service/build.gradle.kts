import org.gradle.api.tasks.bundling.Jar

plugins {
  java
  `maven-publish`
  kotlin("jvm")
}

repositories {
  jcenter()
}

dependencies {
  compile(kotlin("stdlib", "1.3.0"))
  compile(project(":api"))
  compile("io.ktor:ktor-client-cio:1.0.0")
  compile("io.ktor:ktor-client-apache:1.0.0")

  implementation("com.google.code.gson:gson:2.3.1")
  implementation("org.apache.httpcomponents:httpclient:4.5")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.0.0")

  testImplementation("org.junit.jupiter:junit-jupiter-api:5.1.0")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.1.0")
  testCompile("io.ktor:ktor-client-mock-jvm:1.0.0")
  testCompile("org.hamcrest:hamcrest-core:1.3")
}

