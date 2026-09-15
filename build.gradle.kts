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

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.jar {
    archiveBaseName.set("KiraziumEmotes")
}
