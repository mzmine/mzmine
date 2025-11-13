plugins {
    id("java")
//    id("io.github.mzmine.java-library-conv")
}

group = "io.mzmine"
version = ""

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.bundles.jasper)
}

tasks.test {
    useJUnitPlatform()
}

java {
    // Configure a Java Toolchain
    toolchain {
        languageVersion = JavaLanguageVersion.of(8) // Or 11, 17, etc.
    }
}

// Attach an action to the 'jar' task to print Java version info
tasks.jar {
    doFirst {
        println("======================================================")
        println("Executing 'jar' task. Java compatibility information:")
        println("------------------------------------------------------")

        // Print configured source and target compatibility
        println("Project configured sourceCompatibility: ${project.the<JavaPluginExtension>().sourceCompatibility}")
        println("Project configured targetCompatibility: ${project.the<JavaPluginExtension>().targetCompatibility}")

        // Check if a toolchain is explicitly configured by checking its languageVersion property
        val toolchainExtension = project.the<JavaPluginExtension>().toolchain
        if (toolchainExtension.languageVersion.isPresent) { // <--- CORRECTED LINE HERE
            val toolchainLanguageVersion = toolchainExtension.languageVersion.get()
            println("Toolchain language version (used for compilation): ${toolchainLanguageVersion.asInt()}")
        } else {
            println("Java Toolchain is NOT explicitly configured. Relying on default JDK.")
        }
        println("======================================================")
    }
}
