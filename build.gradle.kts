plugins {
    java
}

group = "com.kirazium"
version = "0.1.0-SNAPSHOT"

val pluginVersion = version.toString()

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://mvn.lumine.io/repository/maven-public/")
    maven("https://repo.nexomc.com/releases")
    maven("https://repo.oraxen.com/releases")
}

dependencies {
    testImplementation("io.papermc.paper:paper-api:${property("paperVersion")}")
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
    compileOnly("net.dmulloy2:ProtocolLib:${property("protocolLibVersion")}") {
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
    inputs.property("version", pluginVersion)
    filesMatching("plugin.yml") {
        expand("version" to pluginVersion)
    }
}

tasks.jar {
    archiveBaseName.set("KiraziumEmotes")
}

val regressionTest by tasks.registering(JavaExec::class) {
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("com.kirazium.emotes.RegressionChecks")
}
tasks.check { dependsOn(regressionTest) }
