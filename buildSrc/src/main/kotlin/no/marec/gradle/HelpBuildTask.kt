package no.marec.gradle

import no.marec.gradle.node.NodeContextTask
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getByType
import java.io.File

abstract class HelpBuildTask : NodeContextTask() {

   private val helpBuild = project.extensions.getByType<HelpBuildExtension>()
   private val marecBuild = project.extensions.getByType<MarecBuildExtension>()
   private val toolsHelpInternal = project.configurations.named("toolsHelpInternal")

   init {
      inputs.property("nodeVersion", Libraries.Version.node)
      outputs.dir(project.layout.buildDirectory.dir("helpBuild"))
      inputs.property("helpVersion", helpBuild.version)
      helpBuild.relPaths.forEach { relPath ->
         val helpDir = relPathToHelpDir(relPath)
         inputs.files(project.fileTree(helpDir) { exclude("**/build") })
         outputs.dir("$helpDir/build")
      }
   }

   private fun relPathToHelpDir(relPath: String): File {
      return projectLayout.projectDirectory.file("src/main/resources/$relPath").asFile
   }

   @TaskAction
   fun run() {
      helpBuild.relPaths.forEach { runForRelPath(it) }
   }

   private fun runForRelPath(relPath: String) {
      val helpDir = relPathToHelpDir(relPath)

      BuildUtils.helpBuild(helpDir, helpBuild.version, context, marecBuild, toolsHelpInternal.get())

      // Sync to project build dir so that project clean triggers rebuild help.
      fileSystemOperations.sync {
         from("$helpDir/build")
         into(projectLayout.buildDirectory.dir("helpBuild/$relPath/build"))
      }
   }
}
