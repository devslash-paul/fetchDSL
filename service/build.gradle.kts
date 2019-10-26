val kotlinVersion by extra("kotlinVersion")
val ktorVersion: String by project

tasks.test {
  useJUnitPlatform()
}

dependencies {
  compile(kotlin("stdlib", kotlinVersion))
  compile(project(":api"))
  compile("io.ktor:ktor-client-core:$ktorVersion")
  compile("io.ktor:ktor-client-apache:$ktorVersion")

  compile("com.fasterxml.jackson.core:jackson-core:2.9.8")
  compile("com.fasterxml.jackson.core:jackson-databind:2.9.8")
  compile("com.fasterxml.jackson.core:jackson-annotations:2.9.8")

  implementation("org.apache.httpcomponents:httpclient:4.5")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.0-M1")

  testImplementation("org.junit.jupiter:junit-jupiter-api:5.1.0")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.1.0")
  testCompile("io.ktor:ktor-client-mock-jvm:$ktorVersion")
  testCompile("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.3.2")
  testCompile("org.hamcrest:hamcrest-core:1.3")
}
