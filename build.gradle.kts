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
    "utils"
)


subprojects {

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
