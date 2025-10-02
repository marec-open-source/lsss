import no.marec.gradle.BuildVersions
import no.marec.gradle.Libraries
import org.apache.tools.ant.filters.ReplaceTokens

plugins {
   marec.`java-plugin`
   marec.`java-test-fixtures-plugin`
   marec.`help-plugin`
}

dependencies {
   api(project(":korona"))
   api(project(":tools:database"))

   runtimeOnly(project(":tools:help"))
   runtimeOnly(Libraries.postgresql)

   testFixturesApi(testFixtures(project(":korona")))
   testFixturesApi(testFixtures(project(":tools:database")))
}

marecHelp {
   version = BuildVersions.lsss
   relPaths = listOf("no/imr/lsss/resources/help", "no/imr/lsss/resources/incubator/help")
}

tasks.filterSrcMainJava {
   filesMatching("**/LSSS.java") {
      val tokens = mapOf("LSSS_VERSION" to BuildVersions.lsss)
      filter(ReplaceTokens::class, "tokens" to tokens)
   }
}
