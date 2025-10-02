import no.marec.gradle.Libraries

plugins {
   marec.`java-plugin`
}

dependencies {
   api(project(":lsss"))
   api(project(":tools:jaxrs"))
   implementation(project(":tools:jogl"))

   implementation(Libraries.jersey_container_grizzly2_http)
   implementation(Libraries.jersey_hk2)
   implementation(Libraries.jackson_jakarta_rs_json_provider)

   runtimeOnly(Libraries.jersey_media_sse)
}
