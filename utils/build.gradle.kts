plugins {
    id("io.github.mzmine.java-library-conv")
    id("io.github.mzmine.javafx-conv")
}

dependencies {
    implementation(libs.commons.io)
    implementation(libs.guava)
    implementation(libs.fastutil)
}
