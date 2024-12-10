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
    implementation(project(":utils"))
    implementation(project(":taskcontroller"))

    implementation("io.mzio:memory-management:1.0.0")
    implementation("io.mzio:taskcontroller:1.0.0")
    implementation(libs.guava)
}

semver {
    properties = "../mzmine-community/src/main/resources/mzmineversion.properties"
}

afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("publish-javafx-framework") {
                from(components["java"])
                pom {
                    group = "io.github.mzmine"
                    artifactId = "java-fx-framework"
                    name = "mzmine-javafx-framework"
                    description = "mzmine-community java fx framework"
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
