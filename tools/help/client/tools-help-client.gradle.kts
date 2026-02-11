import no.marec.gradle.Libraries
import no.marec.gradle.node.NpmTask

plugins {
   marec.`base-plugin`
}

tasks.register<NpmTask>("npmCi") {
   group = "marec"
   args = listOf("ci")
}

tasks.register<Delete>("npmClean") {
   group = "marec"
   delete("node_modules")
   delete("package-lock.json")
}

val npmInstall = tasks.register<NpmTask>("npmInstall") {
   group = "marec"
   args = listOf("install")

   inputs.property("nodeVersion", Libraries.Version.node)
   inputs.files(file("package.json"), file("package-lock.json"))
   outputs.dir("node_modules")
}

tasks.register<NpmTask>("npmOutdated") {
   group = "marec"
   args = listOf("outdated")
}

tasks.register<NpmTask>("ngBuildDev") {
   group = "marec"
   dependsOn(npmInstall)
   args = listOf("run", "buildDev")

   inputs.property("nodeVersion", Libraries.Version.node)
   inputs.files(fileTree(projectDir) {
      exclude(".angular/**", "build/**", "node_modules/**")
   })
   outputs.dir("build/distDev")
}

tasks.register<NpmTask>("ngBuildProd") {
   group = "marec"
   dependsOn(npmInstall)
   args = listOf("run", "buildProd")

   inputs.property("nodeVersion", Libraries.Version.node)
   inputs.files(fileTree(projectDir) {
      exclude(".angular/**", "build/**", "node_modules/**")
   })
   outputs.dir("build/distProd")
}

tasks.register<NpmTask>("ngLint") {
   group = "marec"
   args = listOf("run", "lint")
}

tasks.register<NpmTask>("ngTest") {
   group = "marec"
   args = listOf("run", "test")
}
