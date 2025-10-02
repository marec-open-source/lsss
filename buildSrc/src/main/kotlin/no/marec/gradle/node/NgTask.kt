package no.marec.gradle.node

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

abstract class NgTask : NodeContextTask() {

   @Input
   var args: List<String> = listOf()

   @TaskAction
   fun run() {
      NodeUtils.runNg(context, args)
   }
}
