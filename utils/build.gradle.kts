/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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
    id("io.github.mzmine.java-library-conv")
    id("io.github.mzmine.javafx-conv")
    id("maven-publish")
    alias(libs.plugins.semver)
}

repositories {
    mavenCentral()
    // local libraries
    maven { url = uri("file://" + layout.projectDirectory.dir("../local-repo")) }
}

dependencies {
    // jackson for json parsing
    implementation(libs.bundles.jackson)
    implementation(libs.commons.io)
    implementation(libs.guava)
    implementation(libs.fastutil)
    implementation(libs.mzio.global.events)
    implementation(libs.semver4j)
}

semver {
    properties = "../mzmine-community/src/main/resources/mzmineversion.properties"
}

afterEvaluate {
    // the repositories must be set in the afterEvaluate block, because the semver plugin will
    // not be initialised otherwise. The version of this library is bound to the mzmine community version.
    publishing {
        publications {
            register<MavenPublication>("publish-utils") {
                from(components["java"])
                pom {
                    group = "io.github.mzmine"
                    artifactId = "utils"
                    name = "mzmine-community-utils"
                    description = "mzmine-community utils"
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
