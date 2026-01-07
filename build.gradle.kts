/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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
    `maven-publish`
}

// list of sub projects that create a java library
val publishingSubProjects = listOf(
    "convention-plugins",
    "java-convention",
    "javafx-framework",
    "mzmine-community",
    "taskcontroller",
    "utils",
    "reports"
)


subprojects {
    // this variable is set by running .\gradlew publish with the command line argument -Pmziorepo=true
    val pushOnline: Boolean = project.findProperty("mziorepo")?.toString()?.toBoolean() ?: false
    if(name in publishingSubProjects) {
        apply(plugin = "maven-publish")

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
                        url = uri(layout.projectDirectory.dir("../local-repo/"))
                    }
                }
            }
        }
    }
}
