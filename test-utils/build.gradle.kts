val kotlinVersion: String by project
dependencies {
  compile(kotlin("stdlib", kotlinVersion))

  compile("org.hamcrest:hamcrest-core:1.3")
  compile("io.ktor:ktor-client-mock:1.0.0")
  compile("io.ktor:ktor-server-netty:1.0.0")
  compile("org.junit-pioneer:junit-pioneer:0.3.0")
}