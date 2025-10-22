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
 *
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
    `kotlin-dsl`
    id("maven-publish")
    alias(libs.plugins.semver)
}

semver {
    properties = "../../mzmine-community/src/main/resources/mzmineversion.properties"
}

// this variable is set by running .\gradlew publish with the command line argument -Pmziorepo=true
val pushOnline: Boolean = project.findProperty("mziorepo")?.toString()?.toBoolean() ?: false


//kotlin {
//    jvmToolchain(23)
//}

dependencies {
    implementation(libs.javafx.plugin)
}

afterEvaluate {
    // the repositories must be set in the afterEvaluate block, because the semver plugin will
    // not be initialised otherwise. The convention plugin version is bound to the mzmine community version.
    publishing {
        repositories {
            if (pushOnline) {
                maven {
                    url = uri("https://maven.pkg.github.com/mzio-gmbh/mzio_mzmine")
                    credentials {
                        username = System.getenv("PUBLISH_PACKAGE_USERNAME")
                        password = System.getenv("PUBLISH_PACKAGE_TOKEN")
                    }
                }
            } else {
                maven {
                    url = uri(layout.projectDirectory.dir("../../local-repo/"))
                }
            }
        }
        publications {
            register<MavenPublication>("publish-convention-plugins") {
                from(components["java"])
                pom {
                    group = "io.github.mzmine"
                    artifactId = "convention-plugins"
                    name = "mzmine-community convention-plugins"
                    description = "mzmine-community convention plugins"
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
