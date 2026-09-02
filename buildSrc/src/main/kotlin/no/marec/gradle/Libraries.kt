package no.marec.gradle

@Suppress("ConstPropertyName", "SpellCheckingInspection")
object Libraries {
   object Version {
      const val java: Int = 25 // Also in README.md, docker-compose.yml
      const val javaRuntime: String = "25.0.1" // Also in jlink.bat, jlink.command, jlink.sh

      // ---
      const val checkstyle: String = "13.8.0"
      const val dependencyCheckGradle: String = "12.2.2" // Also in buildSrc/build.gradle.kts
      const val derby: String = "10.17.1.0"
      const val hibernate: String = "7.1.11.Final"
      const val jackson: String = "3.2.0" // Also in buildSrc/build.gradle.kts
      const val jacoco: String = "0.8.15"
      const val jaxb_api: String = "4.0.5"
      const val jaxb_runtime: String = "4.0.9"
      const val jersey: String = "4.0.2"
      const val jogl: String = "2.6.0"
      const val junit: String = "6.1.2"
      const val netcdf_c: String = "4.10.0"
      const val netcdf_java: String = "5.9.1"
      const val node: String = "24.16.0"
      const val pmd: String = "7.26.0"
      const val proguard: String = "7.9.1" // Also in buildSrc/build.gradle.kts
      const val slf4j: String = "2.0.18"
      const val spotbugs: String = "4.10.3"
      const val spotbugsGradlePlugin: String = "6.5.9" // Also in buildSrc/build.gradle.kts
      const val tomcat: String = "11.0.23" // Also in buildSrc/build.gradle.kts, docker-compose.yml
   }

   const val batik_bridge: String = "org.apache.xmlgraphics:batik-bridge:1.19"
   const val commons_math: String = "org.apache.commons:commons-math3:3.6.1"
   const val commons_net: String = "commons-net:commons-net:3.13.0"
   const val commons_numbers_complex: String = "org.apache.commons:commons-numbers-complex:1.3"
   const val commons_numbers_rootfinder: String = "org.apache.commons:commons-numbers-rootfinder:1.3"
   const val commons_statistics_descriptive: String = "org.apache.commons:commons-statistics-descriptive:1.3"
   const val commons_statistics_distribution: String = "org.apache.commons:commons-statistics-distribution:1.3"
   val derby: List<String> = listOf(
      "org.apache.derby:derby:${Version.derby}",
      "org.apache.derby:derbyshared:${Version.derby}",
      "org.apache.derby:derbytools:${Version.derby}", // Contains the jdbc driver.
   )
   const val dom4j: String = "org.dom4j:dom4j:2.2.0"
   const val guava: String = "com.google.guava:guava:33.6.0-jre"
   const val hibernate: String = "org.hibernate.orm:hibernate-core:${Version.hibernate}"
   val hibernate_runtime: List<String> = listOf(
      "org.hibernate.orm:hibernate-c3p0:${Version.hibernate}",
      "org.hibernate.orm:hibernate-community-dialects:${Version.hibernate}", // Contains DerbyDialect (HHH-18037).
   )
   const val hsqldb: String = "org.hsqldb:hsqldb:2.7.4"
   const val jackson_databind: String = "tools.jackson.core:jackson-databind:${Version.jackson}"
   const val jackson_jakarta_rs_json_provider: String = "tools.jackson.jakarta.rs:jackson-jakarta-rs-json-provider:${Version.jackson}"
   const val jakarta_mail: String = "org.eclipse.angus:jakarta.mail:2.0.5"
   const val jakarta_servlet_api: String = "jakarta.servlet:jakarta.servlet-api:6.1.0"
   const val jakarta_ws_rs_api: String = "jakarta.ws.rs:jakarta.ws.rs-api:4.0.0"
   const val jaxb_api: String = "jakarta.xml.bind:jakarta.xml.bind-api:${Version.jaxb_api}"
   const val jaxb_runtime: String = "org.glassfish.jaxb:jaxb-runtime:${Version.jaxb_runtime}"
   const val jaxb_txw2: String = "org.glassfish.jaxb:txw2:${Version.jaxb_runtime}" // Contains IndentingXMLStreamWriter.
   const val jaxen: String = "jaxen:jaxen:2.0.6"
   const val jersey_container_grizzly2_http: String = "org.glassfish.jersey.containers:jersey-container-grizzly2-http:${Version.jersey}"
   const val jersey_container_grizzly2_servlet: String = "org.glassfish.jersey.containers:jersey-container-grizzly2-servlet:${Version.jersey}"
   const val jersey_container_servlet: String = "org.glassfish.jersey.containers:jersey-container-servlet:${Version.jersey}"
   const val jersey_hk2: String = "org.glassfish.jersey.inject:jersey-hk2:${Version.jersey}"
   const val jersey_media_sse: String = "org.glassfish.jersey.media:jersey-media-sse:${Version.jersey}"
   const val jersey_test_framework_provider_inmemory: String = "org.glassfish.jersey.test-framework.providers:jersey-test-framework-provider-inmemory:${Version.jersey}"
   const val jfreechart: String = "org.jfree:jfreechart:1.5.6"
   val jogl: List<String> = listOf(
      "org.jogamp.jogl:jogl-all-main:${Version.jogl}",
      "org.jogamp.gluegen:gluegen-rt-main:${Version.jogl}",
   )
   const val jol: String = "org.openjdk.jol:jol-core:0.17"
   const val jopt_simple: String = "net.sf.jopt-simple:jopt-simple:5.0.4"
   const val jsoup: String = "org.jsoup:jsoup:1.22.2"
   const val jspecify: String = "org.jspecify:jspecify:1.0.0"
   const val jtransforms: String = "com.github.wendykierp:JTransforms:3.2" // Depends transitively via JLargeArrays on junit, should be optional, excluded in tools-core.gradle.kts.
   const val junit_api: String = "org.junit.jupiter:junit-jupiter-api:${Version.junit}"
   val junit_runtime: List<String> = listOf(
      "org.junit.jupiter:junit-jupiter-engine:${Version.junit}",
      "org.junit.platform:junit-platform-launcher:${Version.junit}",
   )
   val netcdf: List<String> = listOf(
      "edu.ucar:cdm-core:${Version.netcdf_java}",
      "edu.ucar:netcdf4:${Version.netcdf_java}",
   )
   const val postgresql: String = "org.postgresql:postgresql:42.7.11"
   const val proguard_retrace: String = "com.guardsquare:proguard-retrace:${Version.proguard}"
   const val rome: String = "com.rometools:rome:2.1.0"
   const val slf4j_api: String = "org.slf4j:slf4j-api:${Version.slf4j}"
   const val slf4j_jdk14: String = "org.slf4j:slf4j-jdk14:${Version.slf4j}"
}
