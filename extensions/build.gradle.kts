plugins {
  kotlin("jvm")
  `maven-publish`
  id("me.champeau.gradle.jmh") version "0.4.7"
}

repositories {
  jcenter()
  maven("http://dl.bintray.com/kotlin/kotlin-eap")
}


dependencies {
  compile(kotlin("stdlib", "1.3.0"))
  compile(project(":api"))
  compile(project(":service"))

  compile("org.jetbrains.exposed:exposed:0.10.5")
  compile("mysql:mysql-connector-java:8.0.12")

  implementation("com.google.code.gson:gson:2.3.1")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.0.0")

  testImplementation("org.junit.jupiter:junit-jupiter-api:5.3.1")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.3.1")

  jmh("org.openjdk.jmh:jmh-core:1.21")
  jmh("org.openjdk.jmh:jmh-generator-annprocess:1.21")

  testCompile("org.mock-server:mockserver-netty:5.4.1")
  testCompile("org.hamcrest:hamcrest-core:1.3")
  testCompile("org.junit-pioneer:junit-pioneer:0.3.0")
}
