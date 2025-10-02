package no.marec.gradle.node

import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.tasks.Internal
import org.gradle.process.ExecOperations
import javax.inject.Inject

abstract class NodeContextTask : DefaultTask() {

   @get:Inject
   abstract val archiveOperations: ArchiveOperations

   @get:Inject
   abstract val execOperations: ExecOperations

   @get:Inject
   abstract val fileSystemOperations: FileSystemOperations

   @get:Inject
   abstract val projectLayout: ProjectLayout

   @get:Internal
   val context get() = NodeUtils.Context(archiveOperations, execOperations, fileSystemOperations, projectLayout)
}
