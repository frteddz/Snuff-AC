val javaRelease: String = providers.gradleProperty("snuffac.javaRelease").get()
val minecraftVersion: String = providers.gradleProperty("snuffac.minecraft").get()
val packeteventsVersion: String = providers.gradleProperty("snuffac.packetevents").get()

extra["javaRelease"] = javaRelease
extra["minecraftVersion"] = minecraftVersion
extra["packeteventsVersion"] = packeteventsVersion

subprojects {
    group = rootProject.group
    version = rootProject.version

    apply(plugin = "java")
    apply(plugin = "java-library")

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/") { name = "papermc" }
        maven("https://repo.codemc.io/repository/maven-releases/") { name = "codemc" }
    }

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(javaRelease.toInt()))
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(javaRelease.toInt())
        options.compilerArgs.addAll(listOf(
            "-Xlint:all,-serial,-processing,-deprecation,-this-escape,-overloads",
        ))
    }

    tasks.withType<Javadoc>().configureEach {
        (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = false
        }
    }
}
