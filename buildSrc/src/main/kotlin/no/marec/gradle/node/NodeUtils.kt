package no.marec.gradle.node

import no.marec.gradle.Libraries
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.process.ExecOperations
import java.io.File
import java.net.URI
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.time.Instant
import java.time.temporal.ChronoUnit

object NodeUtils {
   private val IS_WINDOWS: Boolean = File.separator == "\\"

   data class Context(
      val archiveOperations: ArchiveOperations,
      val execOperations: ExecOperations,
      val fileSystemOperations: FileSystemOperations,
      val projectLayout: ProjectLayout,
   )

   fun runNode(context: Context, args: List<String>) {
      val nodeExecutable = getNodeExecutable(context)
      val theCommandLine = listOf(nodeExecutable.toString()) + args
      context.execOperations.exec {
         environment("PATH", "${nodeExecutable.parent}${File.pathSeparator}${environment["PATH"]}")
         commandLine = theCommandLine
         workingDir = context.projectLayout.projectDirectory.asFile
      }
   }

   fun runNpm(context: Context, args: List<String>) {
      val nodeModules = if (IS_WINDOWS) "node_modules" else "lib/node_modules"
      val npmCli = getNodeDir(context).resolve("$nodeModules/npm/bin/npm-cli.js")
      val nodeArgs = listOf(npmCli.toString()) + args
      runNode(context, nodeArgs)
   }

   fun runNg(context: Context, args: List<String>) {
      val nodeArgs = listOf("node_modules/@angular/cli/bin/ng") + args
      runNode(context, nodeArgs)
   }

   private fun getNodeExecutable(context: Context): Path {
      val nodeDir = getNodeDir(context)
      return nodeDir.resolve(if (IS_WINDOWS) "node.exe" else "bin/node")
   }

   private fun getNodeDir(context: Context): Path {
      val nodeHome = System.getenv("NODE_HOME")
      if (nodeHome != null) {
         return Path.of(nodeHome)
      }
      val baseDir = context.projectLayout.settingsDirectory.asFile.toPath().resolve("misc/node")
      val nodeDir = baseDir.resolve("node-${Libraries.Version.node}")
      downloadNodeIfNeeded(context, nodeDir)
      return nodeDir
   }

   @Synchronized
   private fun downloadNodeIfNeeded(context: Context, nodeDir: Path) {
      if (Files.exists(nodeDir)) {
         return
      }

      val baseDir = nodeDir.parent
      Files.createDirectories(baseDir)
      Files.walkFileTree(baseDir, setOf(), 1, object : SimpleFileVisitor<Path>() {
         override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
            if (attrs.creationTime().toInstant().isBefore(Instant.now().minus(7, ChronoUnit.DAYS))) {
               println("Deleting $dir")
               context.fileSystemOperations.delete {
                  delete(dir)
               }
            }
            return FileVisitResult.SKIP_SUBTREE
         }
      })

      val osType = if (IS_WINDOWS) "win-x64" else "linux-x64"
      val suffix = if (IS_WINDOWS) "zip" else "tar.gz"
      val downloadFileBaseName = "node-v${Libraries.Version.node}-$osType"
      val downloadFileName = "$downloadFileBaseName.$suffix"
      val downloadUrl = "https://nodejs.org/dist/v${Libraries.Version.node}/$downloadFileName"

      val downloadDir = baseDir.resolve("download")
      Files.createDirectories(downloadDir)
      val downloadFile = downloadDir.resolve(downloadFileName)
      if (!Files.exists(downloadFile)) {
         println("Downloading node from $downloadUrl")
         val tmpDownloadFile = downloadDir.resolve("$downloadFileName-tmp")
         Files.deleteIfExists(tmpDownloadFile)
         URI(downloadUrl).toURL().openStream().use { Files.copy(it, tmpDownloadFile) }
         Files.move(tmpDownloadFile, downloadFile)
      }

      context.fileSystemOperations.copy {
         into(downloadDir)
         if (suffix == "zip") {
            from(context.archiveOperations.zipTree(downloadFile))
         } else {
            from(context.archiveOperations.tarTree(downloadFile))
         }
      }
      context.fileSystemOperations.copy {
         into(baseDir.resolve("node-${Libraries.Version.node}"))
         from(downloadDir.resolve(downloadFileBaseName))
      }
      context.fileSystemOperations.delete {
         delete(downloadFile)
         delete(downloadDir.resolve(downloadFileBaseName))
      }
   }
}
