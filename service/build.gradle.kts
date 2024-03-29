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
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")
  implementation("com.fasterxml.jackson.core:jackson-core:2.12.3")
  implementation("com.fasterxml.jackson.core:jackson-databind:2.12.3")
  implementation("io.opentelemetry:opentelemetry-api:1.31.0");
  implementation("io.opentelemetry:opentelemetry-extension-kotlin:1.31.0")

  testImplementation("org.hamcrest:hamcrest:2.2")
  testImplementation("junit:junit:$junitVersion")
  testImplementation("org.mockito.kotlin:mockito-kotlin:3.2.0")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlinxCoroutinesVersion")
}
