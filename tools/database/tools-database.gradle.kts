import no.marec.gradle.Libraries

plugins {
   marec.`java-plugin`
   marec.`java-test-fixtures-plugin`
}

dependencies {
   api(project(":tools:core"))

   api(Libraries.hibernate)
   Libraries.hibernate_runtime.forEach { runtimeOnly(it) }

   runtimeOnly(Libraries.hsqldb)
   Libraries.derby.forEach { runtimeOnly(it) }

   testFixturesApi(Libraries.junit_api)
}
