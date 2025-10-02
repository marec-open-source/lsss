import no.marec.gradle.Libraries

plugins {
   marec.`java-plugin`
}

dependencies {
   api(project(":tools:core"))

   Libraries.netcdf.forEach { api(it) }
}
