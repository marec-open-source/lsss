import no.marec.gradle.BuildUtils
import no.marec.gradle.BuildVersions
import no.marec.gradle.JoglBuildUtils

plugins {
   marec.`java-plugin`
}

val koronaPlugins: Set<Project> = project(":korona:plugins").subprojects
val lsssPlugins: Set<Project> = project(":lsss:plugins").subprojects

dependencies {
   api(project(":lsss"))
   koronaPlugins.forEach { api(project(it.path)) }
   lsssPlugins.forEach { api(project(it.path)) }
}

tasks.register<JavaExec>("runLsss") {
   group = "marec"
   classpath = sourceSets.main.get().runtimeClasspath
   mainClass.set("no.imr.lsss.LSSS")
}

val dist = tasks.register<Copy>("dist") {
   group = "marec"
   dependsOn(":korona:api:javadoc")
   into(layout.buildDirectory.dir("exploded"))
   from(rootProject.file("LICENSE"))
   from(project(":korona").file("src/dist"))
   from(project(":lsss").file("src/dist"))
   into("categorization") {
      from("$rootDir/categorization")
      include("trainingdatasets/categorizationBasic/**")
   }
   into("korona") {
      from(project(":korona").projectDir)
      include("data/**", "config/**", "api/build/docs/javadoc/**")
   }
   into("lib/jar") {
      from(configurations.runtimeClasspath)
   }
   into("lib") {
      from("$rootDir/lib")
   }
   into("lsss") {
      from(project(":lsss").projectDir)
      include("data/**")
   }
   koronaPlugins.forEach { p ->
      into("korona/plugins/${p.name}") {
         from(p.projectDir) {
            include("data/**", "config/**")
         }
      }
   }
   lsssPlugins.forEach { p ->
      into("lsss/plugins/${p.name}") {
         from(p.projectDir) {
            include("data/**", "config/**")
         }
      }
   }
   val addCommandForEachSh = BuildUtils.addCommandForEachSh(project, destinationDir)
   doLast {
      addCommandForEachSh()
      JoglBuildUtils.unpackAndDeleteNativeJars(destinationDir.resolve("lib/jar"), destinationDir.resolve("lib/native"))
   }
}

val distZip = tasks.register<Zip>("distZip") {
   group = "marec"
   dependsOn(dist)
   archiveBaseName.set("lsss")
   archiveVersion.set(BuildVersions.lsss)
   archiveClassifier.set(marecBuild.properties.buildTimestamp)
   from(dist.get().destinationDir) {
      exclude("**/*.sh", "**/*.command")
   }
   from(dist.get().destinationDir) {
      include("**/*.sh", "**/*.command")
      filePermissions {
         unix("755")
      }
   }
}

tasks.assemble {
   dependsOn(distZip)
}
