plugins {
  kotlin("jvm")
  `maven-publish`
}

repositories {
  jcenter()
  maven("http://dl.bintray.com/kotlin/kotlin-eap")
}

dependencies {
  compile(kotlin("stdlib", "1.3.0-rc-116"))

  compile("org.jetbrains.kotlin:kotlin-stdlib")
  compile("com.github.kittinunf.fuel:fuel:1.15.0")
  compile("com.github.kittinunf.fuel:fuel-coroutines:1.15.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:0.30.0-eap13")
}

publishing {
  publications {
    register("mavenJava", MavenPublication::class) {
      from(components["java"])
    }
  }
}
