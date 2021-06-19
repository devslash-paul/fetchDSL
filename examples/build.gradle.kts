val ktorNettyVersion: String by project
val kotlinxCoroutinesVersion: String by project
val kotlinVersion: String by project

dependencies {
  implementation(kotlin("stdlib", kotlinVersion))
  implementation(project(":api"))
  implementation(project(":service"))

  implementation("io.ktor:ktor-server-netty:$ktorNettyVersion")
  implementation("com.google.code.gson:gson:2.3.1")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")
}
