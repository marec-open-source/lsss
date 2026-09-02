import no.marec.gradle.HelpBuildExtension
import no.marec.gradle.HelpBuildTask

plugins {
   id("marec.java-plugin")
}

val marecHelp = extensions.create<HelpBuildExtension>("marecHelp")

val toolsHelpInternal = configurations.register("toolsHelpInternal")

dependencies {
   toolsHelpInternal(project(":tools:help:internal"))
}

val helpBuild = tasks.register<HelpBuildTask>("helpBuild") {
   group = "marec"
   dependsOn(toolsHelpInternal)
}

tasks.processResources {
   dependsOn(helpBuild)
}

afterEvaluate {
   idea {
      module {
         marecHelp.relPaths.forEach { relPath ->
            excludeDirs.add(file("src/main/resources/$relPath/build"))
         }
      }
   }
}
