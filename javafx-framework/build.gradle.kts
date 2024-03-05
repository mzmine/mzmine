plugins {
    id("io.github.mzmine.java-library-conv")
    id("io.github.mzmine.javafx-conv")
}

dependencies {
    implementation(project(":utils"))
    implementation(project(":memory-management"))
    implementation(project(":taskcontroller"))
    implementation(libs.guava)
}