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

  implementation("com.google.code.gson:gson:2.3.1")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.0.0")

  testImplementation("org.junit.jupiter:junit-jupiter-api:5.3.1")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.3.1")

  testCompile("org.hamcrest:hamcrest-core:1.3")
  testCompile("io.ktor:ktor-client-mock:1.0.0")
  testCompile("io.ktor:ktor-server-netty:1.0.0")
  testCompile("org.junit-pioneer:junit-pioneer:0.3.0")
}
