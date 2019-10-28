val ktorNettyVersion: String by project

dependencies {
  implementation(kotlin("stdlib", "1.3.30"))
  implementation(project(":api"))
  implementation(project(":extensions"))
  implementation(project(":service"))
  implementation("io.ktor:ktor-server-netty:$ktorNettyVersion")

  implementation("com.google.code.gson:gson:2.3.1")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.2")
}
