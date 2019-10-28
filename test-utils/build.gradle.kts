val kotlinVersion: String by project
val junitVersion: String by project

dependencies {
  implementation(kotlin("stdlib", kotlinVersion))

  implementation("org.hamcrest:hamcrest-core:1.3")
  implementation("io.ktor:ktor-client-mock:1.0.0")
  implementation("io.ktor:ktor-server-netty:1.0.0")
  implementation("junit:junit:$junitVersion")
}
