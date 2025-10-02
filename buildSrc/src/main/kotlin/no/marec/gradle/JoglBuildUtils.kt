package no.marec.gradle

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.ZipFile

object JoglBuildUtils {
   fun unpackAndDeleteNativeJars(jarDir: File, nativeDir: File) {
      jarDir.listFiles()?.forEach { jar ->
         if (jar.name.matches("^(jogl-all|gluegen-rt)-(main|.*-natives)-.*".toRegex())) {
            nativeSubDirName(jar.name)?.let { subDirName ->
               unzip(jar, nativeDir.resolve(subDirName))
            }
            jar.delete()
         }
      }
   }

   private fun unzip(jar: File, destinationDir: File) {
      destinationDir.mkdirs()
      ZipFile(jar).use { zipFile ->
         zipFile.stream().forEach { entry ->
            if (!entry.isDirectory && entry.name.startsWith("natives/")) {
               val fileName = Path.of(entry.name).fileName
               val destinationFile = destinationDir.toPath().resolve(fileName)
               zipFile.getInputStream(entry).use { entryInputStream ->
                  Files.copy(entryInputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING)
               }
            }
         }
      }
   }

   private fun nativeSubDirName(fileName: String): String? {
      return when {
         fileName.contains("-linux-amd64") -> "linux64"
         fileName.contains("-windows-amd64") -> "win64"
         fileName.contains("-macosx-universal") -> "macos64"
         else -> null
      }
   }
}
