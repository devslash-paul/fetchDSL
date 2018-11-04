plugins {
  kotlin("jvm")
  `maven-publish`
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
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:0.30.0-eap13")

  testImplementation("org.junit.jupiter:junit-jupiter-api:5.3.1")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.3.1")
  testCompile("org.mock-server:mockserver-netty:5.4.1")
  testCompile("org.hamcrest:hamcrest-core:1.3")
  testCompile("org.junit-pioneer:junit-pioneer:0.3.0")
}
