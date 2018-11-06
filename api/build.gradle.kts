plugins {
  kotlin("jvm")
  `maven-publish`
}


dependencies {
  compile(kotlin("stdlib", "1.3.0"))

  compile("org.jetbrains.kotlin:kotlin-stdlib")
  compile("com.github.kittinunf.fuel:fuel:1.15.0")
  compile("com.github.kittinunf.fuel:fuel-coroutines:1.15.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.0.0")
}
