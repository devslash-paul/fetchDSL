plugins {
  kotlin("jvm")
  `maven-publish`
}

dependencies {
  compile(kotlin("stdlib", "1.3.0"))

  compile("org.jetbrains.kotlin:kotlin-stdlib")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.0.0")
}
