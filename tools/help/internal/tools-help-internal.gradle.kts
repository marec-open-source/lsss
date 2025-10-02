import no.marec.gradle.Libraries

plugins {
   marec.`java-plugin`
}

dependencies {
   implementation(project(":tools:help"))
   implementation(testFixtures(project(":tools:core")))

   implementation(Libraries.jersey_container_grizzly2_servlet)
   implementation(Libraries.jsoup)
}
