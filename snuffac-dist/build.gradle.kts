plugins {
    alias(libs.plugins.shadow)
}

val distJarFolder: String = "snuffac-1.21.x+paper/purpur"
val distJarName: String = "velocity-v${project.version}.jar"

dependencies {
    implementation(project(":snuffac-api"))
    implementation(project(":snuffac-core"))
    implementation(project(":snuffac-platform-paper"))
    implementation(project(":snuffac-platform-velocity"))

    implementation(libs.packetevents.api)
    implementation(libs.packetevents.spigot)
    implementation(libs.packetevents.velocity)
}

tasks.shadowJar {
    archiveClassifier.set("")
    archiveBaseName.set("snuffac-bundled")

    relocate("com.github.retrooper.packetevents", "dev.snuffac.libs.packetevents")
    relocate("io.github.retrooper.packetevents", "dev.snuffac.libs.packetevents.impl")

    exclude("META-INF/maven/**")
    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")
    exclude("module-info.class")
    exclude("**/module-info.class")
    exclude("META-INF/versions/**/module-info.class")
}

val finalJar = tasks.register<Copy>("finalJar") {
    group = "distribution"
    description = "Copies the shaded artifact to the canonical Snuff AC distribution path."
    from(tasks.shadowJar)
    into(rootProject.layout.buildDirectory.dir("dist/$distJarFolder"))
    rename { distJarName }
}

val publishJar = tasks.register<Copy>("publishJar") {
    group = "distribution"
    description = "Places the canonical artifact at the repository root for easy access."
    from(tasks.shadowJar)
    into(rootProject.layout.projectDirectory.dir(distJarFolder))
    rename { distJarName }
}

tasks.named("assemble") {
    dependsOn(finalJar, publishJar)
}
