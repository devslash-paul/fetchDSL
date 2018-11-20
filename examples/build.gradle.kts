plugins {
  kotlin("jvm")
  `maven-publish`
}

repositories {
  jcenter()
}


dependencies {
  compile(kotlin("stdlib", "1.3.0"))
  compile(project(":api"))
  compile(project(":extensions"))
  compile(project(":service"))
  compile("io.ktor:ktor-server-netty:1.0.0")

  implementation("com.google.code.gson:gson:2.3.1")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.0.0")
}
