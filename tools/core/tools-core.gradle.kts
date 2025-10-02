import no.marec.gradle.Libraries
import org.apache.tools.ant.filters.ReplaceTokens

plugins {
   marec.`java-plugin`
   marec.`java-test-fixtures-plugin`
}

dependencies {
   api(project(":lsss:api"))

   implementation(Libraries.batik_bridge) {
      exclude("xml-apis", "xml-apis")
   }
   api(Libraries.commons_math)
   api(Libraries.commons_numbers_complex)
   api(Libraries.commons_numbers_rootfinder)
   api(Libraries.commons_statistics_descriptive)
   api(Libraries.commons_statistics_distribution)
   api(Libraries.dom4j) {
      // Additional dom4j workaround needed since IDEA 2021.1.1. See settings.gradle.kts.
      isTransitive = false
   }
   api(Libraries.guava)
   Libraries.jackson_databind.forEach { api(it) }
   api(Libraries.jaxb_api)
   api(Libraries.jaxb_txw2)
   api(Libraries.jfreechart)
   api(Libraries.jopt_simple)
   api(Libraries.jspecify)
   api(Libraries.jtransforms)
   implementation(Libraries.slf4j_api)

   runtimeOnly(Libraries.jaxb_runtime)
   runtimeOnly(Libraries.jaxen)
   runtimeOnly(Libraries.slf4j_jdk14)

   testFixturesApi(Libraries.junit_api)
}

tasks.filterSrcMainJava {
   filesMatching("**/Utils.java") {
      val tokens = mapOf(
         "BUILD_TIME" to marecBuild.properties.buildTime.toInstant().toEpochMilli().toString(),
         "GIT_COMMIT" to marecBuild.properties.gitCommit,
         "END_BLOCK_COMMENT" to "*/",
         "LINE_COMMENT" to "//",
      )
      filter(ReplaceTokens::class, "tokens" to tokens)
   }
}
