package no.marec.gradle

import org.gradle.api.DefaultTask
import org.gradle.process.ExecOperations
import javax.inject.Inject

abstract class ExecOperationsTask : DefaultTask() {

   @get:Inject
   abstract val execOperations: ExecOperations
}
