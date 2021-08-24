group = "net.devslash.fetchdsl"
version = "0.20.5-SNAPSHOT"

val ktorNettyVersion: String by project
val kotlinxCoroutinesVersion: String by project
val kotlinVersion: String by project

dependencies {
  // This project should contain as few dependencies as possible so that
  // we can always confirm that the library contains all that's necessary
  // to runction
  implementation(kotlin("stdlib", kotlinVersion))
  implementation(project(":api"))
  implementation(project(":service"))
  implementation("org.hamcrest:hamcrest-core:2.2")

  // Used for testing mocking
  testImplementation("io.ktor:ktor-server-netty:$ktorNettyVersion")
  testImplementation("junit:junit:4.13.2")
}
