import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("com.github.johnrengelman.shadow")
  id("me.champeau.jmh") apply false
}

repositories {
  maven("https://repo.typesafe.com/typesafe/releases/")
}

apply(plugin = "me.champeau.jmh")

tasks.named<KotlinCompile>("compileJmhKotlin") {
  kotlinOptions {
    jvmTarget = "11"
    freeCompilerArgs += "-Xjvm-default=enable"
  }
}

val jmhJarTask = tasks.named<Jar>("jmhJar") {
  archiveBaseName.set("benchmarks")
  destinationDirectory.file("$rootDir")
}

tasks {
  build {
    dependsOn(jmhJarTask)
  }
}

dependencies {
  implementation("org.openjdk.jmh:jmh-core:1.27")
  implementation("org.jetbrains.kotlin:kotlin-stdlib:1.7.21")
  implementation("io.ktor:ktor-server-netty:1.5.4")

  implementation(project(":api"))
  implementation(project(":service"))
  "jmhImplementation"(sourceSets.main.get().runtimeClasspath)
}
