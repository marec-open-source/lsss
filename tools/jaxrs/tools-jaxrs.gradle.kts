import no.marec.gradle.Libraries

plugins {
   marec.`java-plugin`
}

dependencies {
   api(project(":tools:core"))

   api(Libraries.jakarta_ws_rs_api)
}
