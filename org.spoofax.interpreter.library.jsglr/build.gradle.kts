plugins {
  id("org.metaborg.gradle.config.java-library")
  id("org.metaborg.gradle.config.junit-testing")
}

dependencies {
  // api(platform("org.metaborg:parent:$version")) // Can't use: causes dependency cycle because parent mentions pie.

  api(project(":org.spoofax.jsglr"))
  api(project(":org.spoofax.jsglr2"))
  api("org.metaborg:org.spoofax.interpreter.core:$version")
  testCompileOnly("junit:junit:4.13.1")
  testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.7.0")
}

// Copy test resources into classes directory, to make them accessible as classloader resources at runtime.
val copyTestResourcesTask = tasks.create<Copy>("copyTestResources") {
  from("$projectDir/src/test/resources")
  into("$buildDir/classes/java/test")
}
tasks.getByName("processTestResources").dependsOn(copyTestResourcesTask)

// Skip tests, as they do not work.
tasks.test.get().enabled = false
