import no.marec.gradle.Libraries
import no.marec.gradle.MarecBuildExtension

plugins {
   id("marec.base-plugin")
   `java-library`
}

repositories {
   mavenCentral()
   maven {
      url = uri("https://artifacts.unidata.ucar.edu/repository/unidata-releases/")
   }
   maven {
      url = uri("https://jogamp.org/deployment/maven/")
   }
}

dependencies {
   testImplementation(Libraries.junit_api)
   Libraries.junit_runtime.forEach { testRuntimeOnly(it) }

   modules {
      module("org.hibernate:hibernate-core") { // Used by hibernate-c3p0
         replacedBy("org.hibernate:hibernate-core-jakarta")
      }
   }
}

val marecBuild = extensions.getByType<MarecBuildExtension>()

tasks.withType<JavaCompile>().configureEach {
   options.release.set(Libraries.Version.java)
   options.compilerArgs.add("-Xlint:all,-serial,-this-escape")
}

tasks.withType<Javadoc>().configureEach {
   title = "marec" + project.path.replace(":", "-")
   options {
      this as StandardJavadocDocletOptions // Needed since `bottom` is not part of `MinimalJavadocOptions`.
      bottom = "Copyright © ${marecBuild.properties.buildYear} NORCE Research AS."
      addStringOption("Xdoclint:all,-missing", "-quiet") // todo: fix all doclint issues
   }
}

val filterSrcMainJava = tasks.register<Copy>("filterSrcMainJava") {
   group = "marec"
   from(sourceSets.main.get().java)
   into(layout.buildDirectory.dir("filteredSrc/main/java"))
   // Individual projects should configure this task with filtering as needed.
}

tasks.compileJava {
   dependsOn(filterSrcMainJava)
   source = fileTree(filterSrcMainJava.get().destinationDir)
}

tasks.processResources {
   exclude("**/*do-not-copy*")
}

tasks.jar {
   archiveBaseName.set("marec" + project.path.replace(":", "-"))
}

tasks.test {
   useJUnitPlatform()
   maxHeapSize = "1024m"
   systemProperty("java.util.prefs.PreferencesFactory", "no.imr.tools.InMemoryPreferencesFactory")
   systemProperty("java.util.logging.config.class", "no.imr.tools.logging.LogConfig")
}
