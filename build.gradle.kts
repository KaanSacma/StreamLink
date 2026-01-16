plugins {
    java
}

group = "com.kenta"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Add Hytale Server as compileOnly dependency (not bundled in final JAR)
    compileOnly(files("HytaleServer.jar"))

    implementation("org.java-websocket:Java-WebSocket:1.5.3")
}

tasks.jar {
    // Set the archive name
    archiveBaseName.set("streamlink")
    archiveVersion.set("1.0.0")

    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })

    // Handle duplicates (resources are already included by default)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from("src/main/resources")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
