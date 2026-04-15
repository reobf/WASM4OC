
plugins {
    id("com.gtnewhorizons.gtnhconvention")
}
tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    exclude("META-INF/versions/**")
    exclude("META-INF/services/java.nio.file.spi.FileSystemProvider")
    mergeServiceFiles()
}

tasks.compileJava {
    // this is a dummy file to keep eclipse compiler shut, so do not actually compile it
    project.sourceSets {
        main {
            java {
                exclude("cpw/mods/fml/common/patcher/ClassPatchManager.java")
            }
        }
    }
}
configurations {
    create("winOnly")
    create("linuxOnly")
}

dependencies {
	"linuxOnly"(files("src/main/resources/assets/wasm4oc/linux/emsdk.zip"))
    "winOnly"(files("src/main/resources/assets/wasm4oc/win/emsdk.zip"))
}


tasks.named<Jar>("sourcesJar") {
    exclude("assets/wasm4oc/win/**")
    exclude("assets/wasm4oc/linux/**")
}
tasks.named<ProcessResources>("processResources") {
    exclude("assets/wasm4oc/win/**")
    exclude("assets/wasm4oc/linux/**")
}

tasks.register<Jar>("buildLinux") {
 	group = "build"
    description = "Build jar with Linux emsdk included"


    val reobf = tasks.named("reobfJar")
    dependsOn(reobf)
    archiveClassifier.set("linux")


    from(zipTree(reobf.get().outputs.files.singleFile))


    from("src/main/resources") {
        include("assets/wasm4oc/linux/**")
    }
}
tasks.register<Jar>("buildWin") {
 	group = "build"
    description = "Build jar with Windows emsdk included"


    val reobf = tasks.named("reobfJar")
    dependsOn(reobf)
    archiveClassifier.set("windows")


    from(zipTree(reobf.get().outputs.files.singleFile))


    from("src/main/resources") {
        include("assets/wasm4oc/win/**")
    }
}
