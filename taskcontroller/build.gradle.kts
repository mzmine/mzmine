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
    implementation("io.mzio:memory-management:1.0.0")
    implementation(project(":utils"))
    implementation(libs.guava)
}

semver {
    properties = "../mzmine-community/src/main/resources/mzmineversion.properties"
}

publishing {
    repositories {
        maven {
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
                artifactId = "task-controller"
                name = "mzmine-community-task-controller"
                description = "mzmine-community task controller"
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
