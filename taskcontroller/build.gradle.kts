plugins {
    id("io.github.mzmine.java-library-conv")
    id("io.github.mzmine.javafx-conv")
}

dependencies {
    implementation(project(":memory-management"))
    implementation(project(":utils"))
    implementation(libs.guava)
}