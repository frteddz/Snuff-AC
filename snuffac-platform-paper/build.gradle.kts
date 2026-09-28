dependencies {
    api(project(":snuffac-core"))
    api(project(":snuffac-api"))

    compileOnly(libs.paper.api)
    compileOnly(libs.packetevents.api)
    compileOnly(libs.packetevents.spigot)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.processResources {
    val tokens = mapOf(
        "version" to project.version.toString(),
        "minecraft" to (project.findProperty("snuffac.minecraft") as String? ?: "1.21.11"),
    )
    inputs.properties(tokens)
    filesMatching("plugin.yml") {
        expand(tokens)
    }
}
