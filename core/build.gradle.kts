
repositories {
    maven("https://repo.aikar.co/content/groups/aikar/")
}

dependencies {
    compileOnly("io.freefair.lombok:io.freefair.lombok.gradle.plugin:8.12")
    implementation("co.aikar:acf-paper:0.5.1-SNAPSHOT")
    implementation(project(":nms:shared"))
}

tasks {
    compileJava {
        options.release = 21
        options.compilerArgs.add("-parameters")
    }
}