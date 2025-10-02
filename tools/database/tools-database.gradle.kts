import no.marec.gradle.Libraries

plugins {
   marec.`java-plugin`
   marec.`java-test-fixtures-plugin`
}

dependencies {
   api(project(":tools:core"))

   Libraries.hibernate.forEach { api(it) }

   runtimeOnly(Libraries.hsqldb)
   Libraries.derby.forEach { runtimeOnly(it) }

   testFixturesApi(Libraries.junit_api)
}
