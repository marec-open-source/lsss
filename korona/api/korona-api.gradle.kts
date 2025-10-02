import no.marec.gradle.Libraries

plugins {
   marec.`java-plugin`
}

dependencies {
   api(Libraries.jspecify)
}

tasks.withType<Javadoc>().configureEach {
   title = "KORONA API"
}
