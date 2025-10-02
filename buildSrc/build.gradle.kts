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
   const val dependencyCheckGradle: String = "12.1.2"
   const val jackson: String = "2.19.0"
   const val licenseGradlePlugin: String = "0.16.1"
   const val proguard: String = "7.7.0"
   const val spotbugsGradlePlugin: String = "6.2.0"
   const val tomcat: String = "11.0.9"
}

dependencies {
   implementation("org.owasp:dependency-check-gradle:${Version.dependencyCheckGradle}")
   implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:${Version.jackson}")
   implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${Version.jackson}")
   implementation("gradle.plugin.com.hierynomus.gradle.plugins:license-gradle-plugin:${Version.licenseGradlePlugin}")
   implementation("com.guardsquare:proguard-gradle:${Version.proguard}")
   implementation("com.github.spotbugs.snom:spotbugs-gradle-plugin:${Version.spotbugsGradlePlugin}")
   implementation("org.apache.tomcat:tomcat-catalina-ant:${Version.tomcat}")
   implementation("org.apache.tomcat:tomcat-catalina:${Version.tomcat}")
}

idea {
   module {
      inheritOutputDirs = true
   }
}
