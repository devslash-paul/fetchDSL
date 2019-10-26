tasks.withType<Test> {
  useJUnitPlatform()
  testLogging {
    events("passed", "skipped", "failed")
  }
}

dependencies {
//  testImplementation("org.junit.jupiter:junit-jupiter:5.5.2")
}
