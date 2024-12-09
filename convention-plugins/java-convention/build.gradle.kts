plugins {
    `kotlin-dsl`
    id("maven-publish")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.javafx.plugin)
}

afterEvaluate {
    publishing {
        repositories {
            maven {
                name = "mzmine_community-convention-plugins-1.0.0"
                url = uri("https://maven.pkg.github.com/mzio-gmbh/mzio_mzmine")
                credentials {
                    username = System.getenv("PUBLISH_PACKAGE_USERNAME")
                    password = System.getenv("PUBLISH_PACKAGE_TOKEN")
                }
            }
            /*maven {
            url = uri(layout.projectDirectory.dir("../local-repo/"))
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
                    version = "1.0.0"
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
