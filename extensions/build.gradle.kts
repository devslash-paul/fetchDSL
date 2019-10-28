val mockkVersion = "1.9.3"
val junitVersion: String by project
val kotlinVersion: String by project
val ktorVersion: String by project
val ktorNettyVersion: String by project

dependencies {
  implementation(kotlin("stdlib", kotlinVersion))
  implementation(kotlin("reflect", kotlinVersion))
  implementation(project(":api"))
  implementation(project(":service"))

  implementation("io.ktor:ktor-client-apache:$ktorVersion")
  implementation("io.ktor:ktor-client-core:$ktorVersion")

  implementation("com.google.code.gson:gson:2.3.1")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.2")

  testImplementation("io.mockk:mockk:$mockkVersion")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug:1.3.2")
  testImplementation("junit:junit:$junitVersion")
  testImplementation(project(":test-utils"))
  testImplementation("org.hamcrest:hamcrest-core:1.3")

  testImplementation("io.ktor:ktor-client-mock:1.0.0")
  testImplementation("io.ktor:ktor-server-netty:$ktorNettyVersion")
}
