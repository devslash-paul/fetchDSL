repositories {
  jcenter()
  maven("http://dl.bintray.com/kotlin/kotlin-eap")
}

val mockkVersion = "1.9.3"
val junitVersion = "5.5.0"
val kotlinVersion: String by project

dependencies {
  compile(kotlin("stdlib", kotlinVersion))
  compile(kotlin("reflect", kotlinVersion))
  compile(project(":api"))
  compile(project(":service"))

  implementation("com.google.code.gson:gson:2.3.1")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.2")

  testImplementation("io.mockk:mockk:$mockkVersion")
  testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")

  testCompile(project(":test-utils"))
  testCompile("org.hamcrest:hamcrest-core:1.3")
  testCompile("io.ktor:ktor-client-mock:1.0.0")
  testCompile("io.ktor:ktor-server-netty:1.0.0")
  testCompile("org.junit-pioneer:junit-pioneer:0.3.0")
}
