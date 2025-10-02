package no.marec.gradle

import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType

abstract class MarecBuildExtension(project: Project) {
   val properties: BuildProperties

   init {
      properties = if (project == project.rootProject) {
         BuildProperties()
      } else {
         project.rootProject.extensions.getByType<MarecBuildExtension>().properties
      }
   }
}
