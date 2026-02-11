plugins {
   `kotlin-dsl`
   idea
}

repositories {
   mavenCentral()
   gradlePluginPortal()
}

@Suppress("ConstPropertyName")
object Version {
   const val dependencyCheckGradle: String = "12.2.0"
   const val jackson: String = "3.0.3"
   const val proguard: String = "7.8.2"
   const val spotbugsGradlePlugin: String = "6.4.4"
   const val tomcat: String = "11.0.13"
}

dependencies {
   implementation("org.owasp:dependency-check-gradle:${Version.dependencyCheckGradle}")
   implementation("tools.jackson.dataformat:jackson-dataformat-xml:${Version.jackson}")
   implementation("tools.jackson.module:jackson-module-kotlin:${Version.jackson}")
   implementation("com.guardsquare:proguard-gradle:${Version.proguard}")
   implementation("com.github.spotbugs.snom:spotbugs-gradle-plugin:${Version.spotbugsGradlePlugin}")
   implementation("org.apache.tomcat:tomcat-catalina-ant:${Version.tomcat}")
   implementation("org.apache.tomcat:tomcat-catalina:${Version.tomcat}")
}

idea {
   module {
      inheritOutputDirs = true

      // todo: Remove excluding generated sources when this is fixed: https://youtrack.jetbrains.com/issue/KTIJ-501/Inspect-code-false-positive-inspection-error-is-reported-in-excluded-auto-generated-extensions-for-Gradle-API
      //excludeDirs.add(file("build/generated-sources/kotlin-dsl-accessors/kotlin"))
      //excludeDirs.add(file("build/generated-sources/kotlin-dsl-external-plugin-spec-builders/kotlin"))
      //excludeDirs.add(file("build/generated-sources/kotlin-dsl-plugins/kotlin"))
   }
}
