plugins {
    `java-library`
    idea
    id("net.neoforged.moddev") version "2.0.105"
}

val modVersion: String by extra
val neoVersion: String by extra
val parchmentMappingsVersion: String by extra
val parchmentMinecraftVersion: String by extra
val loaderVersionRange: String by extra
val neoVersionRange: String by extra
val minecraftVersionRange: String by extra

version = modVersion
group = "com.mystic"

repositories {
    mavenLocal()
}

base {
    archivesName = "recipefixer"
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

neoForge {
    version = neoVersion

    parchment {
        mappingsVersion = parchmentMappingsVersion
        minecraftVersion = parchmentMinecraftVersion
    }

    runs {
        create("client") {
            client()
            systemProperty("neoforge.enabledGameTestNamespaces", "recipefixer")
        }
        
        create("server") {
            server()
            programArgument("--nogui")
            systemProperty("neoforge.enabledGameTestNamespaces", "recipefixer")
        }
        
        create("gameTestServer") {
            type = "gameTestServer"
            systemProperty("neoforge.enabledGameTestNamespaces", "recipefixer")
        }
        
        create("data") {
            data()
            programArguments.addAll( "--mod", "recipefixer", "--all",
                    "--output", file("src/generated/resources/").absolutePath,
                    "--existing", file("src/main/resources/").absolutePath
            )
        }

        configureEach {
            systemProperty("forge.logging.markers", "REGISTRIES")
            logLevel = org.slf4j.event.Level.DEBUG
        }
    }

    mods {
        create("recipefixer") {
            sourceSet(sourceSets.main.get())
        }
    }
}

// Include generated metadata
sourceSets.main.get().resources {
    srcDir("src/generated/resources")
}


// Expand properties for metadata
var generateModMetadata = tasks.register("generateModMetadata", ProcessResources::class) {
    var replaceProperties = mapOf(
            "minecraftVersionRange" to minecraftVersionRange,
            "neoVersionRange"       to neoVersionRange,
            "loaderVersionRange"    to loaderVersionRange,
            "modVersion"             to modVersion)
    
    inputs.properties(replaceProperties)
    expand(replaceProperties)
    from("src/main/templates")
    into("build/generated/sources/modMetadata")
}

sourceSets.main.get().resources.srcDir(generateModMetadata)
neoForge.ideSyncTask(generateModMetadata)

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
