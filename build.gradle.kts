plugins {
    `java-library`
    //id("io.papermc.paperweight.userdev") version "2.0.0-beta.16"
    id("xyz.jpenilla.run-paper") version "2.3.1" // Adds runServer and runMojangMappedServer tasks for testing
    id("io.freefair.lombok") version "8.12"
    id("com.gradleup.shadow") version "8.3.0"
}

group = "ca.bungo"
version = "1.0.0-SNAPSHOT"

repositories {
    maven("https://repo.aikar.co/content/groups/aikar/")
    maven("https://hub.spigotmc.org/nexus/content/groups/public/")
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

java {
    // Configure the java toolchain. This allows gradle to auto-provision JDK 21 on systems that only have JDK 11 installed for example.
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

dependencies {
    implementation(project(":core"))
    implementation(project(":nms:shared"))
    implementation(project(":nms:v1_21_R1"))
}

subprojects {
    apply {
        plugin("java-library")
        plugin("io.freefair.lombok")
        plugin("com.gradleup.shadow")
    }

    repositories {
        maven {
            name = "papermc"
            url = uri("https://repo.papermc.io/repository/maven-public/")
        }
    }

    dependencies {
        compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    }

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    }

}

tasks {
    build {
        dependsOn(shadowJar)
    }
    compileJava {
        options.release = 21
        options.compilerArgs.add("-parameters")
    }
    javadoc {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything
    }

    // Configure Shadow plugin
    shadowJar {
        manifest {
            attributes["paperweight-mappings-namespace"] = "mojang"
        }
        archiveClassifier.set("")
        relocate("co.aikar.commands", "ca.bungo.holos.acf")
        relocate("co.aikar.locales", "ca.bungo.holos.locales")
    }

    runServer {
        minecraftVersion("1.21.4")
    }
}
