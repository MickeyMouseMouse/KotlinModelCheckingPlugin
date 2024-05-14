plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm") version "1.9.23"
  id("org.jetbrains.intellij") version "1.17.2"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

dependencies {
  implementation(kotlin("reflect"))

  implementation(files("src/main/resources/modelcheckingannotations.jar"))

  implementation("org.soot-oss:sootup.core:1.2.0")
  implementation("org.soot-oss:sootup.java.core:1.2.0")
  implementation("org.soot-oss:sootup.java.sourcecode:1.2.0")
  implementation("org.soot-oss:sootup.java.bytecode:1.2.0")
  implementation("org.soot-oss:sootup.jimple.parser:1.2.0")
  implementation("org.soot-oss:sootup.callgraph:1.2.0")
  implementation("org.soot-oss:sootup.analysis:1.2.0")
}


// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
  version.set("2023.2.5")
  type.set("IC") // Target IDE Platform

  plugins.set(listOf("Kotlin"))
}

tasks {
  // Set the JVM compatibility versions
  withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
  }
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
  }

  patchPluginXml {
    sinceBuild.set("232")
    untilBuild.set("242.*")
  }

  signPlugin {
    certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
    privateKey.set(System.getenv("PRIVATE_KEY"))
    password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
  }

  publishPlugin {
    token.set(System.getenv("PUBLISH_TOKEN"))
  }
}
