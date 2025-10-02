import no.marec.gradle.BuildVersions

plugins {
   marec.`java-plugin`
   marec.`help-plugin`
}

dependencies {
   implementation(project(":lsss"))

   testImplementation(testFixtures(project(":lsss")))
}

marecHelp {
   version = BuildVersions.lsss
   relPaths = listOf("no/imr/deepvision/resources/help")
}
