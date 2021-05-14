val kotlinVersion: String by project
val ktorVersion: String by project
val junitVersion: String by project
val kotlinxCoroutinesVersion: String by project
val ktorNettyVersion: String by project

dependencies {
  implementation(kotlin("stdlib", kotlinVersion))
  implementation(project(":api"))
  implementation("io.ktor:ktor-client-core:$ktorVersion")
  implementation("io.ktor:ktor-client-apache:$ktorVersion")

  implementation("com.fasterxml.jackson.core:jackson-core:2.9.8")
  implementation("com.fasterxml.jackson.core:jackson-databind:2.9.8")

  testImplementation("junit:junit:$junitVersion")
  testImplementation(project(":test-utils"))
  testImplementation("io.ktor:ktor-server-netty:$ktorNettyVersion")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlinxCoroutinesVersion")
  testImplementation("org.hamcrest:hamcrest-core:1.3")
}
