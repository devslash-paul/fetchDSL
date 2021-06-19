val kotlinVersion: String by project
val ktorVersion: String by project
val junitVersion: String by project
val kotlinxCoroutinesVersion: String by project
val ktorNettyVersion: String by project

dependencies {
  implementation(kotlin("stdlib", kotlinVersion))
  implementation(kotlin("reflect", kotlinVersion))

  implementation(project(":api"))

  implementation("io.ktor:ktor-client-core:$ktorVersion")
  implementation("io.ktor:ktor-client-apache:$ktorVersion")

  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")

  implementation("com.fasterxml.jackson.core:jackson-core:2.9.8")
  implementation("com.fasterxml.jackson.core:jackson-databind:2.9.8")

  testImplementation("junit:junit:$junitVersion")
  testImplementation(project(":test-utils"))
  testImplementation("org.mockito:mockito-core:3.11.1")
  testImplementation("org.mockito.kotlin:mockito-kotlin:3.2.0")
  testImplementation("io.ktor:ktor-server-netty:$ktorNettyVersion")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlinxCoroutinesVersion")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug:$kotlinxCoroutinesVersion")
  testImplementation("org.hamcrest:hamcrest-core:1.3")
}
