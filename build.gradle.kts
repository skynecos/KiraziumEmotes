import java.net.URI
import java.security.MessageDigest

plugins {
    java
}

group = "com.kirazium"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://mvn.lumine.io/repository/maven-public/")
    maven("https://repo.nexomc.com/releases")
    maven("https://repo.oraxen.com/releases")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:${property("paperVersion")}")
    compileOnly("com.ticxo.modelengine:ModelEngine:${property("modelEngineVersion")}") {
        isTransitive = false
    }
    compileOnly("com.nexomc:nexo:${property("nexoVersion")}") {
        isTransitive = false
    }
    compileOnly("beer.devs:itemsadder-api:${property("itemsAdderApiVersion")}") {
        isTransitive = false
    }
    compileOnly("io.th0rgal:oraxen:${property("oraxenVersion")}") {
        isTransitive = false
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
}

val generatedResources = layout.buildDirectory.dir("generated/kiraziumemotes-resources")
val prepareBundledFloss = tasks.register("prepareBundledFloss") {
    val outputFile = generatedResources.map { it.file("bundled/modelengine/player_floss.bbmodel") }
    outputs.file(outputFile)

    doLast {
        val target = outputFile.get().asFile
        target.parentFile.mkdirs()

        val source = URI(
            "https://raw.githubusercontent.com/Utruna/DanseAvecLaStare/3d6edc427ea45a87160a003ab2683daa1681d397/models/player_floss.bbmodel"
        ).toURL()
        val bytes: ByteArray = source.openStream().use { input -> input.readBytes() }

        // Verify the immutable Git blob, not merely the URL. Expected blob SHA was read
        // from the pinned upstream commit before this task was added.
        val digest = MessageDigest.getInstance("SHA-1")
        digest.update("blob ${bytes.size}\u0000".toByteArray(Charsets.UTF_8))
        digest.update(bytes)
        val gitBlobSha = digest.digest().joinToString("") { byte -> "%02x".format(byte) }
        check(gitBlobSha == "4bdeedb41accfa6b7daeaa44abddc84f6f55fd67") {
            "player_floss.bbmodel failed pinned Git blob verification: $gitBlobSha"
        }

        target.writeBytes(bytes)
    }
}

sourceSets {
    main {
        resources.srcDir(generatedResources)
    }
}

tasks.processResources {
    dependsOn(prepareBundledFloss)
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.jar {
    archiveBaseName.set("KiraziumEmotes")
}
