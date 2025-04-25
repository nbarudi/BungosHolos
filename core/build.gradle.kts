
repositories {
    maven("https://repo.aikar.co/content/groups/aikar/")
    maven("https://repo.extendedclip.com/releases/")
}

dependencies {
    compileOnly("io.freefair.lombok:io.freefair.lombok.gradle.plugin:8.12")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("io.github.miniplaceholders:miniplaceholders-api:2.3.0")
    implementation("co.aikar:acf-paper:0.5.1-SNAPSHOT")
    implementation(project(":nms:shared"))
}

tasks {
    compileJava {
        options.release = 21
        options.compilerArgs.add("-parameters")
    }
}