/*
 * Copyright (c) 2004-2025 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

plugins {
    id("java")
    id("maven-publish")
    alias(libs.plugins.semver)
}

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
    /*toolchain {
        languageVersion = JavaLanguageVersion.of(8) // Or 11, 17, etc.
    }*/
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
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

afterEvaluate {
    // the repositories must be set in the afterEvaluate block, because the semver plugin will
    // not be initialised otherwise. The version of this library is bound to the mzmine community version.
    publishing {
        publications {
            register<MavenPublication>("publish-reports") {
                from(components["java"])
                pom {
                    group = "io.github.mzmine"
                    artifactId = "reports"
                    name = "mzmine-reports"
                    description = "mzmine-community reports"
                    url = "https://github.com/mzmine/mzmine"
                    version = semver.version
                    developers {
                        developer {
                            id = "mzmine"
                            name = "mzmine"
                        }
                    }
                }
            }
        }
    }
}
