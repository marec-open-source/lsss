buildscript {
   repositories {
      mavenCentral()
   }
}

plugins {
   marec.`base-plugin`
   id("com.github.ben-manes.versions") version "0.52.0"
}

repositories {
   mavenCentral()
}

idea {
   module {
      excludeDirs.add(file("misc/node"))
   }
}

tasks.wrapper {
   distributionType = Wrapper.DistributionType.ALL
   networkTimeout.set(180000)
}

tasks.dependencyUpdates {
   rejectVersionIf {
      candidate.version.matches("^[\\d.]+[.-](alpha|beta|cr|m|rc)-?\\d*$".toRegex(RegexOption.IGNORE_CASE))
   }
}
