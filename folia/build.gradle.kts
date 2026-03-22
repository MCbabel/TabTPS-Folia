plugins {
  id("tabtps.platform.shadow")
}

indra {
  javaVersions {
    target(21)
    minimumToolchain(21)
  }
}

dependencies {
  implementation(projects.tabtpsCommon)

  compileOnly(libs.foliaApi)
  implementation(libs.adventurePlatformBukkit)
  implementation(libs.adventureTextSerializerPlain)
  implementation(libs.bstatsBukkit)
  implementation(libs.slf4jJdk14)

  implementation(libs.cloudPaper)
}

tasks {
  jar {
    archiveClassifier.set("unshaded")
  }

  shadowJar {
    archiveClassifier.set(null as String?)
    sequenceOf(
      "org.slf4j",
      "org.incendo.cloud",
      "io.leangen.geantyref",
      "net.kyori",
      "org.spongepowered.configurate",
      "org.checkerframework",
      "org.bstats",
      "xyz.jpenilla.pluginbase"
    ).forEach { pkg ->
      relocate(pkg, "${rootProject.group}.${rootProject.name.lowercase()}.lib.$pkg")
    }
    manifest {
      attributes("paperweight-mappings-namespace" to "mojang")
    }
  }

  processResources {
    val replacements = mapOf(
      "version" to version.toString(),
      "description" to project.description,
      "github" to "https://github.com/MCbabel/TabTPS-Folia"
    )
    inputs.properties(replacements)
    filesMatching("plugin.yml") {
      expand(replacements)
    }
  }
}

tabTPSPlatform {
  productionJar.set(tasks.shadowJar.flatMap { it.archiveFile })
}

publishMods.modrinth {
  modLoaders.add("folia")
}
