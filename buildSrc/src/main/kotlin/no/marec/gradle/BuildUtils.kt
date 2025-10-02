package no.marec.gradle

import no.marec.gradle.node.NodeUtils
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import java.io.File

object BuildUtils {

   fun helpBuild(helpDir: File, version: String, context: NodeUtils.Context, marecBuild: MarecBuildExtension, toolsHelpInternal: Configuration) {
      context.execOperations.javaexec {
         mainClass.set("no.marec.tools.help.internal.HelpSetConverterMain")
         classpath(toolsHelpInternal)
         args(helpDir, version, marecBuild.properties.buildTime.toInstant().toString())
         workingDir = context.projectLayout.settingsDirectory.asFile
      }

      val makeLunrIndexJs = context.projectLayout.settingsDirectory.asFile.resolve("tools/help/internal/makeLunrIndex.js")
      NodeUtils.runNode(context, listOf(makeLunrIndexJs.toString(), "$helpDir/build"))

      context.fileSystemOperations.delete {
         delete("$helpDir/build/lunrData.json")
      }
   }

   fun addCommandForEachSh(project: Project, dir: File): () -> Unit {
      val commandTemplate = project.rootDir.resolve("misc/template.command")
      val fileTree = project.fileTree(dir) {
         include("**/*.sh")
         exclude("lib/FindJava.sh")
      }
      return {
         fileTree.forEach { file ->
            val commandFile = File(file.path.replace("\\.sh$".toRegex(), ".command"))
            commandTemplate.copyTo(commandFile, overwrite = true)
         }
      }
   }
}
