plugins {
   id("marec.java-plugin")
   `java-test-fixtures`
}

tasks.testFixturesJar {
   archiveBaseName.set(tasks.jar.get().archiveBaseName.get())
}
