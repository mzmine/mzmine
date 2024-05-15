plugins {
    id("io.github.mzmine.java-library-conv")
    id("io.github.mzmine.javafx-conv")
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