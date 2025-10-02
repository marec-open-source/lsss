import no.marec.gradle.Libraries

plugins {
   marec.`java-plugin`
}

idea {
   module {
      excludeDirs.add(file("src/main/resources/no/marec/tools/help/server/resources/build"))
   }
}

dependencies {
   implementation(project(":tools:jaxrs"))

   implementation(Libraries.jersey_container_grizzly2_http)
   implementation(Libraries.jersey_hk2)
   implementation(Libraries.jackson_jakarta_rs_json_provider)
}

val helpBuild = tasks.register<Sync>("helpBuild") {
   group = "marec"
   dependsOn(":tools:help:client:ngBuildProd")
   into("src/main/resources/no/marec/tools/help/server/resources/build/webapp")
   from("client/build/distProd/browser")
}
tasks.processResources {
   dependsOn(helpBuild)
}
