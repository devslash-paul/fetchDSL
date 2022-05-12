val ktorNettyVersion: String by project
val kotlinxCoroutinesVersion: String by project
val kotlinVersion: String by project

dependencies {
  // This project should contain as few dependencies as possible so that
  // we can always confirm that the library contains all that's necessary
  // to runction
  implementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.0")
  implementation(project(":api"))
  implementation(project(":service"))

  implementation("org.hamcrest:hamcrest:2.2")

  // Used for testing mocking
  testImplementation("io.ktor:ktor-server-netty:1.6.7")
  testImplementation("junit:junit:4.13.2")
}
