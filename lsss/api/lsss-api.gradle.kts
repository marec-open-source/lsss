import no.marec.gradle.BuildUtils
import no.marec.gradle.BuildVersions
import no.marec.gradle.Libraries
import no.marec.gradle.node.NodeContextTask

plugins {
   marec.`java-plugin`
}

val toolsHelpInternal by configurations.registering

dependencies {
   api(Libraries.jspecify)

   toolsHelpInternal(project(":tools:help:internal"))
}

tasks.withType<Javadoc>().configureEach {
   title = "LSSS API version ${BuildVersions.lsssApi}"
   options {
      overview = "src/main/java/overview.html"
   }
}

fun requiredProperty(key: String): String {
   return properties[key]?.toString() ?: throw IllegalArgumentException("Missing property '$key'")
}

tasks.register<NodeContextTask>("lsssPluginHelpBuild") {
   group = "marec"
   description = "Builds help pages for an LSSS plugin - to be called from a separate gradle project"
   dependsOn(toolsHelpInternal)
   val helpDir = file(requiredProperty("lsssPluginHelpBuild.helpDir"))
   val version = requiredProperty("lsssPluginHelpBuild.version")
   doLast {
      BuildUtils.helpBuild(helpDir, version, context, marecBuild, toolsHelpInternal.get())
   }
}
