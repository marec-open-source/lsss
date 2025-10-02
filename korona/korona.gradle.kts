import no.marec.gradle.BuildVersions
import org.apache.tools.ant.filters.ReplaceTokens

plugins {
   marec.`java-plugin`
   marec.`java-test-fixtures-plugin`
   marec.`help-plugin`
}

dependencies {
   api(project(":tools:core"))
   implementation(project(":korona:api"))
   implementation(project(":tools:netcdf"))

   testFixturesApi(testFixtures(project(":tools:core")))
   testFixturesImplementation(project(":korona:api"))
}

marecHelp {
   version = BuildVersions.lsss
   relPaths = listOf("no/imr/korona/resources/help", "no/imr/korona/resources/incubator/help")
}

tasks.filterSrcMainJava {
   filesMatching("**/Korona.java") {
      val tokens = mapOf("LSSS_VERSION" to BuildVersions.lsss)
      filter(ReplaceTokens::class, "tokens" to tokens)
   }
}
