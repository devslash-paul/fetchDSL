val kotlinVersion: String by project
val junitVersion: String by project
val ktorNettyVersion: String by project

dependencies {
  implementation(kotlin("stdlib", kotlinVersion))

  implementation("org.hamcrest:hamcrest-core:1.3")
  implementation("io.ktor:ktor-server-netty:$ktorNettyVersion")
  implementation("junit:junit:$junitVersion")
}
