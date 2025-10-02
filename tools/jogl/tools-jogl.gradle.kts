import no.marec.gradle.Libraries

plugins {
   marec.`java-plugin`
}

dependencies {
   api(project(":tools:core"))

   Libraries.jogl.forEach { api(it) }
}
