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


kotlin {
    jvmToolchain(23)
}

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
