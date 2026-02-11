plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.chibashr.allthewebhooks"
version = "1.3.2"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT")
    implementation("org.reflections:reflections:0.10.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation("org.mockito:mockito-junit-jupiter:5.14.2")
    testImplementation("io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT")
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.named<Jar>("jar") {
    enabled = false
}

tasks.named("shadowJar") {
    (this as org.gradle.api.tasks.bundling.Jar).archiveClassifier.set("")
}

tasks.named("build") {
    dependsOn(tasks.named("shadowJar"))
}
