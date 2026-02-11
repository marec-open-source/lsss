import no.marec.gradle.MarecBuildExtension

plugins {
   base
   idea
}

idea {
   module {
      inheritOutputDirs = true
   }
}

extensions.create<MarecBuildExtension>("marecBuild")

tasks.withType<AbstractArchiveTask>().configureEach {
   isPreserveFileTimestamps = true
   useFileSystemPermissions()
}
