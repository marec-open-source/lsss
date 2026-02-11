import java.nio.charset.Charset

rootProject.name = "marec"

include(
   "korona",
   "korona:api",
   "korona:plugins",
   "lsss",
   "lsss:api",
   "lsss:plugins",
   "lsss:plugins:deepvision",
   "lsss:plugins:server",
   "lsss:release",
   "tools",
   "tools:core",
   "tools:database",
   "tools:help",
   "tools:help:client",
   "tools:help:internal",
   "tools:jaxrs",
   "tools:jogl",
   "tools:netcdf",
)

println(
   "Gradle ${gradle.gradleVersion}" +
         ", Java ${System.getProperty("java.version")}" +
         ", Charset ${Charset.defaultCharset()}" +
         ", Max memory ${Runtime.getRuntime().maxMemory() / 1e9} GB" +
         ", Usable disk space ${rootDir.usableSpace / 1e9} GB"
)

fun adaptProject(project: ProjectDescriptor, name: String) {
   project.buildFileName = "$name.gradle.kts"
   project.children.forEach { adaptProject(it, "$name-${it.name}") }
}

rootProject.children.forEach { adaptProject(it, it.name) }
