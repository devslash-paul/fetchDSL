val kotlinVersion: String by project
val ktorVersion: String by project
val junitVersion: String by project
val ktorNettyVersion: String by project

dependencies {
  implementation(kotlin("stdlib", kotlinVersion))
  implementation(project(":api"))
  implementation("io.ktor:ktor-client-core:$ktorVersion")
  implementation("io.ktor:ktor-client-apache:$ktorVersion")
  implementation("io.ktor:ktor-server-netty:$ktorNettyVersion")

  implementation("com.fasterxml.jackson.core:jackson-core:2.9.8")
  implementation("com.fasterxml.jackson.core:jackson-databind:2.9.8")
  implementation("com.fasterxml.jackson.core:jackson-annotations:2.9.8")

  implementation("org.apache.httpcomponents:httpclient:4.5")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.2")

  testImplementation("junit:junit:$junitVersion")
  testImplementation(project(":test-utils"))
  testImplementation("io.ktor:ktor-client-mock-jvm:$ktorVersion")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.3.2")
  testImplementation("org.hamcrest:hamcrest-core:1.3")
}
