plugins {
    `kotlin-dsl`
    id("maven-publish")
    alias(libs.plugins.semver)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.javafx.plugin)
}

semver {
    properties = "../../mzmine-community/src/main/resources/mzmineversion.properties"
}

afterEvaluate {
    publishing {
        repositories {
            maven {
                name = "mzmine_community-convention-plugins"
                url = uri("https://maven.pkg.github.com/mzio-gmbh/mzio_mzmine")
                credentials {
                    username = System.getenv("PUBLISH_PACKAGE_USERNAME")
                    password = System.getenv("PUBLISH_PACKAGE_TOKEN")
                }
            }
            /*maven {
                url = uri(layout.projectDirectory.dir("../../local-repo/"))
        }*/
        }
        publications {
            register<MavenPublication>("gpr") {
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
